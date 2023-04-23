package searchengine.parse;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.context.ApplicationContext;
import searchengine.dto.UrlInfo;
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
    private String shortUrl;
    private static SiteRepository siteRepository;
    private static PageService pageService;
    private static LemmaService lemmaService;
    private static List<String> wrongTypes;
    private final Set<PageParseTask> setChildTasks = new HashSet<>();
    private static final InheritableThreadLocal<AtomicBoolean> IS_INTERRUPT = new InheritableThreadLocal<>();
    private static final InheritableThreadLocal<String> GLOBAL_PAGE = new InheritableThreadLocal<>();
    private static final InheritableThreadLocal<Site> SITE_ENTITY_THREAD = new InheritableThreadLocal<>();

    public PageParseTask(Site site, List<String> wrongTypes) {
        SITE_ENTITY_THREAD.set(site);
        url = site.getUrl();
        shortUrl = "/";
        GLOBAL_PAGE.set(url);
        siteRepository = applicationContext.getBean(SiteRepository.class);
        pageService = applicationContext.getBean(PageService.class);
        lemmaService = applicationContext.getBean(LemmaService.class);
        PageParseTask.wrongTypes = wrongTypes;
        PageParseTask.IS_INTERRUPT.set(new AtomicBoolean(false));
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
        shortUrl = url.replace(GLOBAL_PAGE.get(), "");
        if (shortUrl.isEmpty()) {
            shortUrl = "/";
        }
    }

    @Override
    protected void compute() {
        if (stopFlag || IS_INTERRUPT.get().get()) {
            setChildTasks.clear();
            return;
        }

        synchronized (siteRepository) {
            getUniqueUrlData();
        }

        for (PageParseTask element : setChildTasks) {
            element.join();
        }
    }

    private void getUniqueUrlData() {
        Site site = SITE_ENTITY_THREAD.get();
        Optional<Page> optionalPageEntity = pageService.getPageByPathAndSite(shortUrl, site);
        if (optionalPageEntity.isPresent()) {
            return;
        }

        try {
            UrlInfo urlInfo = pageService.getUrlInfoDto(url);
            int codeStatus = urlInfo.getCodeStatus();
            Document document = urlInfo.getDocument();
            if (document != null) {
                Page page = savePageEntityAndUpdateSiteStatusTime(site, shortUrl, urlInfo);
                saveLemmasAndIndexes(codeStatus, urlInfo, site, page);
                Elements elements = document.select("a");
                fillSetChildTasks(setChildTasks, elements);
            }
        } catch (IOException e) {
            fixIndexingError(site, e);
        }
    }

    private void saveLemmasAndIndexes(int codeStatus, UrlInfo urlInfo, Site site, Page page) {
        if (codeStatus < 400) {
            lemmaService.saveLemmasAndIndexes(urlInfo.getDocument().html(), site, page);
        }
    }

    private void fixIndexingError(Site site, IOException ex) {
        site.setLastError("Ошибка индексации: " + ex.getMessage() + " - " + url);
        site.setStatus(Status.FAILED);
        siteRepository.save(site);

        IS_INTERRUPT.set(new AtomicBoolean(true));
    }

    private void fillSetChildTasks(Set<PageParseTask> setChildTasks, Elements elements) {
        for (Element element : elements) {
            String link = element.absUrl("href");

            if (link.startsWith("/")) {
                link = GLOBAL_PAGE.get().concat(link);
            }

            if (isCorrectLink(link)) {
                PageParseTask gettingLinks = new PageParseTask(link);
                gettingLinks.fork();
                setChildTasks.add(gettingLinks);
            }
        }
    }

    private Page savePageEntityAndUpdateSiteStatusTime(Site site, String link, UrlInfo urlInfo) {
        Page page = pageService.savePageEntity(site, link, urlInfo);
        site.setStatusTime(LocalDateTime.now());
        siteRepository.save(site);

        return page;
    }

    private boolean isCorrectLink(String link) {
        return !link.isEmpty() && link.startsWith(GLOBAL_PAGE.get())
                && !link.contains("#") && !link.contains("@")
                && !wrongTypes.contains(link.substring(link.lastIndexOf(".") + 1));
    }
}
