package searchengine.service;

import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.ConnectionProperties;
import searchengine.config.SiteConfig;
import searchengine.config.SitesListConfig;
import searchengine.dto.UrlInfoDto;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.exception.ErrorCustomException;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.enums.Status;
import searchengine.repository.IndexRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.service.parse.PageParseTask;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    private final SiteRepository siteRepository;
    private final PageServiceImpl pageService;
    private final IndexRepository indexRepository;
    private final LemmaService lemmaService;
    private final SitesListConfig sites;
    @Value("#{'${wrong-types}'.split(',')}")
    private final List<String> wrongTypes;
    private static final int PARALLELISM = 2;
    private final List<Future<?>> futures = new ArrayList<>();
    private ThreadPoolExecutor executor = new ThreadPoolExecutor(PARALLELISM, PARALLELISM, 0L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>());
    private final Set<ForkJoinPool> forkJoinPools = ConcurrentHashMap.newKeySet();

    @Override
    public IndexingResponse indexSites() throws ErrorCustomException {
        if (isIndexingStarted()) {
            throw new ErrorCustomException("Индексация уже запущена.");
        }

        List<Thread> threadList = new ArrayList<>();
        List<SiteConfig> siteConfigList = sites.getSiteConfigs();
        siteConfigList.forEach(siteConfig -> threadList.add(new Thread(() -> indexSite(siteConfig))));

        if (executor.isShutdown()) {
            executor = new ThreadPoolExecutor(PARALLELISM, PARALLELISM, 0L, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>());
        }
        threadList.forEach(thread -> futures.add(executor.submit(thread)));
        executor.shutdown();
        threadList.clear();

        return createIndexResponse();
    }

    @Override
    public IndexingResponse stopSiteIndexing() throws ErrorCustomException {
        if (!isIndexingStarted()) {
            throw new ErrorCustomException("Индексация не запущена.");
        }

        stopMultipleTasks();
        List<String> urlSites = sites.getSiteConfigs().stream()
                .map(SiteConfig::getUrl)
                .toList();

        List<Site> siteList = siteRepository.findSiteEntitiesByUrlInAndStatus(urlSites, Status.INDEXING);
        if (!siteList.isEmpty()) {
            for (Site site : siteList) {
                site.setStatus(Status.FAILED);
                site.setLastError("Произведена преднамеренная остановка индексации.");
                site.setStatusTime(LocalDateTime.now());
            }

            siteRepository.saveAll(siteList);
        }

        return createIndexResponse();
    }

    /*@Transactional*/
    @Override
    public IndexingResponse indexPage(String url) throws ErrorCustomException {
        Optional<Site> optionalSite = Optional.empty();
        for (SiteConfig siteConfig : sites.getSiteConfigs()) {
            if (url.startsWith(siteConfig.getUrl())) {
                optionalSite = Optional.of(createSiteEntity(siteConfig));
                break;
            }
        }

        if (optionalSite.isEmpty()) {
            throw new ErrorCustomException("Данная страница находится за пределами сайтов, " +
                    "указанных в конфигурационном файле.");
        }

        try {
            Site site = optionalSite.get();
            String shortUrl = url.replaceAll(site.getUrl(), "");

            UrlInfoDto urlInfoDto = pageService.getUrlInfoDto(url);
            Page page = pageService.savePageEntity(site, shortUrl, urlInfoDto);
            HashMap<String, Integer> quantityLemmasInTheText =
                    lemmaService.getQuantityLemmasInTheText(urlInfoDto.getDocument().html());

            List<Index> indexList = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : quantityLemmasInTheText.entrySet()) {
                Lemma lemma = lemmaService.createOrUpdateLemma(entry.getKey(), site);

                Index index = new Index();
                index.setLemma(lemma);
                index.setPage(page);
                index.setRank(Double.valueOf(entry.getValue()));
                indexList.add(index);
            }
            indexRepository.saveAll(indexList);

        } catch (IOException e) {
            throw new ErrorCustomException("При попытке получения данных страницы произошла ошибка: " + e.getMessage());
        }


        return createIndexResponse();
    }

    private void stopMultipleTasks() {
        executor.getQueue().clear();
        forkJoinPools.forEach(pool -> {
            pool.shutdownNow();
            pool.shutdownNow();
            try {
                pool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        forkJoinPools.clear();
        futures.clear();
    }

    private IndexingResponse createIndexResponse() {
        IndexingResponse indexingResponse = new IndexingResponse();
        indexingResponse.setResult(true);
        return indexingResponse;
    }

    private void indexSite(SiteConfig siteConfig) {
        clearSiteData(siteConfig.getUrl());
        Site site = createSiteEntity(siteConfig);

        PageParseTask pageParseTask = new PageParseTask(site, wrongTypes);

        ForkJoinPool pool = new ForkJoinPool(PARALLELISM);
        forkJoinPools.add(pool);
        pool.invoke(pageParseTask);
        pool.shutdown();

        checkIfTheStatusNeedsToBeChanged(site);
    }

    private void checkIfTheStatusNeedsToBeChanged(Site site) {
        Optional<Site> optionalSiteEntity =
                siteRepository.findSiteEntityByIdAndStatus(site.getId(), Status.FAILED);

        if (optionalSiteEntity.isEmpty()) {
            site.setStatus(Status.INDEXED);
            site.setStatusTime(LocalDateTime.now());
            siteRepository.save(site);
        }
    }

    private void clearSiteData(String url) {
        siteRepository.deleteByUrl(url);
    }

    private Site createSiteEntity(SiteConfig siteConfig) {
        Optional<Site> siteOptional = siteRepository.findSiteByUrl(siteConfig.getUrl());
        if (siteOptional.isEmpty()) {
            Site site = new Site();
            site.setName(siteConfig.getName());
            site.setUrl(siteConfig.getUrl());
            site.setStatus(Status.INDEXING);
            site.setStatusTime(LocalDateTime.now());
            siteRepository.save(site);
            return site;
        } else {
            return siteOptional.get();
        }
    }

    private boolean isIndexingStarted() {
        boolean isIndexingStarted = false;
        for (Future<?> future : futures) {
            if (!future.isDone()) {
                isIndexingStarted = true;
            }
        }

        return isIndexingStarted;
    }
}
