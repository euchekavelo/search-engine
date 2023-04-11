package searchengine.service.parse;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.ConnectionProperties;
import searchengine.dto.UrlInfoDto;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.enums.Status;
import searchengine.repository.SiteRepository;
import searchengine.service.LemmaService;
import searchengine.service.PageService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicBoolean;

public class PageParseTask extends RecursiveAction {

    private static volatile boolean stopFlag;
    private static ApplicationContext applicationContext;
    private final String url;
    private final Object object = 1;
    private String shortUrl;
    private static SiteRepository siteRepository;
    private static PageService pageService;
    private static LemmaService lemmaService;
    private static List<String> wrongTypes;
    private final Set<PageParseTask> setChildTasks = new HashSet<>();
    private static final InheritableThreadLocal<AtomicBoolean> isInterrupt = new InheritableThreadLocal<>();
    private static final InheritableThreadLocal<String> globalPage = new InheritableThreadLocal<>();
    private static final InheritableThreadLocal<Site> siteEntityThread = new InheritableThreadLocal<>();

    public PageParseTask(Site site, List<String> wrongTypes) {
        siteEntityThread.set(site);
        url = site.getUrl();
        shortUrl = "/";
        globalPage.set(url);
        siteRepository = applicationContext.getBean(SiteRepository.class);
        pageService = applicationContext.getBean(PageService.class);
        lemmaService = applicationContext.getBean(LemmaService.class);
        PageParseTask.wrongTypes = wrongTypes;
        PageParseTask.isInterrupt.set(new AtomicBoolean(false));
        stopFlag = false;
    }

    public static void setStopFlag(boolean flag) {
        PageParseTask.stopFlag = flag;
    }

    public static boolean getStopFlag() {
        return stopFlag;
    }

    public static void setApplicationContext(ApplicationContext applicationContext) {
        PageParseTask.applicationContext = applicationContext;
    }

    public PageParseTask(String url) {
        this.url = url.toLowerCase();
        shortUrl = url.replace(globalPage.get(), "");
        if (shortUrl.isEmpty()) {
            shortUrl = "/";
        }
    }

    @Override
    protected void compute() {
        if (stopFlag || isInterrupt.get().get()) {
            return;
        }

        synchronized (object) {
            getUniqueUrlData();
        }

        for (PageParseTask element : setChildTasks) {
            element.join();
        }
    }

    private void getUniqueUrlData() {
        Site site = siteEntityThread.get();
        Optional<Page> optionalPageEntity = pageService.getPageByPathAndSite(shortUrl, site);
        if (optionalPageEntity.isPresent()) {
            return;
        }

        try {
            UrlInfoDto urlInfoDto = pageService.getUrlInfoDto(url);
            int codeStatus = urlInfoDto.getCodeStatus();
            if (codeStatus >= 400 && codeStatus <= 599) {
                return;
            }
            Document document = urlInfoDto.getDocument();
            if (document != null) {
                Page page = savePageEntityAndUpdateSiteStatusTime(site, shortUrl, urlInfoDto);
                lemmaService.saveLemmasAndIndexes(urlInfoDto.getDocument().html(), site, page);
                Elements elements = document.select("a");
                fillSetChildTasks(setChildTasks, elements);
            }
        } catch (IOException e) {
            fixIndexingError(site, e);
        }
    }

    private void fixIndexingError(Site site, IOException ex) {
        site.setLastError("Ошибка индексации: " + ex.getMessage() + " - " + url);
        site.setStatus(Status.FAILED);
        siteRepository.save(site);

        isInterrupt.set(new AtomicBoolean(true));
    }

    private void fillSetChildTasks(Set<PageParseTask> setChildTasks, Elements elements) {
        for (Element element : elements) {
            String link = element.absUrl("href");

            if (link.startsWith("/")) {
                link = globalPage.get().concat(link);
            }

            if (isCorrectLink(link)) {
                PageParseTask gettingLinks = new PageParseTask(link);
                gettingLinks.fork();
                setChildTasks.add(gettingLinks);
            }
        }
    }

    private Page savePageEntityAndUpdateSiteStatusTime(Site site, String link, UrlInfoDto urlInfoDto) {
        Page page = pageService.savePageEntity(site, link, urlInfoDto);
        site.setStatusTime(LocalDateTime.now());
        siteRepository.save(site);

        return page;
    }

    private boolean isCorrectLink(String link) {
        return !link.isEmpty() && link.startsWith(globalPage.get())
                && !link.contains("#") && !link.contains("@")
                && !wrongTypes.contains(link.substring(link.lastIndexOf(".") + 1));
    }
}
