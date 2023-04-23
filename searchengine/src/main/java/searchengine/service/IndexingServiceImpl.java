package searchengine.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.SiteConfig;
import searchengine.config.SitesListConfig;
import searchengine.dto.UrlInfo;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.exception.ErrorCustomException;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.enums.Status;
import searchengine.repository.SiteRepository;
import searchengine.parse.PageParseTask;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    private final SiteRepository siteRepository;
    private final PageServiceImpl pageService;
    private final LemmaService lemmaService;
    private final SitesListConfig sites;
    @Value("#{'${wrong-types}'.split(',')}")
    private final List<String> wrongTypes;
    private static final int PARALLELISM = 2;
    private final List<Future<?>> futures = new ArrayList<>();
    private ExecutorService executor = Executors.newFixedThreadPool(PARALLELISM);

    @Override
    public IndexingResponse indexSites() throws ErrorCustomException {
        if (isIndexingStarted()) {
            throw new ErrorCustomException("Индексация уже запущена.");
        }

        futures.clear();
        siteRepository.deleteAll();
        List<Thread> threadList = new ArrayList<>();
        List<SiteConfig> siteConfigList = sites.getSiteConfigs();
        siteConfigList.forEach(siteConfig -> threadList.add(new Thread(() -> indexSite(siteConfig))));

        if (executor.isShutdown()) {
            executor = Executors.newFixedThreadPool(PARALLELISM);
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
        PageParseTask.setStopFlag(true);

        return createIndexResponse();
    }

    @Transactional
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
            removeIndexedPageSiteData(shortUrl, site);

            UrlInfo urlInfo = pageService.getUrlInfoDto(url);
            Page page = pageService.savePageEntity(site, shortUrl, urlInfo);
            lemmaService.saveLemmasAndIndexes(urlInfo.getDocument().html(), site, page);

        } catch (IOException e) {
            throw new ErrorCustomException("При попытке получения данных страницы произошла ошибка: " + e.getMessage());
        }

        return createIndexResponse();
    }

    private void removeIndexedPageSiteData(String url, Site site) {
        Optional<Page> optionalPage = pageService.getPageByPathAndSite(url, site);
        if (optionalPage.isPresent()) {
            Page page = optionalPage.get();

            List<Integer> indexIds = new ArrayList<>();
            List<Lemma> modifiedLemmas = new ArrayList<>();
            List<Integer> removedLemmaIds = new ArrayList<>();

            page.getIndexEntities().forEach(index -> {
                indexIds.add(index.getId());

                Lemma lemma = index.getLemma();
                Integer frequency = lemma.getFrequency();
                if (frequency > 1) {
                    lemma.setFrequency(frequency - 1);
                    modifiedLemmas.add(lemma);
                } else {
                    removedLemmaIds.add(lemma.getId());
                }
            });

            lemmaService.saveLemmas(modifiedLemmas);
            lemmaService.deleteLemmaIndexesByIds(indexIds);
            lemmaService.deleteLemmaByIdIn(removedLemmaIds);
            pageService.deletePageByPath(url, site);
        }
    }

    private IndexingResponse createIndexResponse() {
        IndexingResponse indexingResponse = new IndexingResponse();
        indexingResponse.setResult(true);
        return indexingResponse;
    }

    private void indexSite(SiteConfig siteConfig) {
        Site site = createSiteEntity(siteConfig);

        PageParseTask pageParseTask = new PageParseTask(site, wrongTypes);

        ForkJoinPool pool = new ForkJoinPool(PARALLELISM);
        pool.invoke(pageParseTask);
        pool.shutdown();

        checkIfTheStatusNeedsToBeChanged(site);
    }

    private void checkIfTheStatusNeedsToBeChanged(Site site) {
        Optional<Site> optionalSiteEntity =
                siteRepository.findSiteEntityByIdAndStatus(site.getId(), Status.FAILED);

        if (optionalSiteEntity.isEmpty() && !PageParseTask.getStopFlag()) {
            site.setStatus(Status.INDEXED);
            site.setStatusTime(LocalDateTime.now());
            siteRepository.save(site);
        } else if (optionalSiteEntity.isEmpty() && PageParseTask.getStopFlag()) {
            site.setStatus(Status.FAILED);
            site.setLastError("Произведена преднамеренная остановка индексации.");
            site.setStatusTime(LocalDateTime.now());
            siteRepository.save(site);
        }
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
                break;
            }
        }

        return isIndexingStarted;
    }
}
