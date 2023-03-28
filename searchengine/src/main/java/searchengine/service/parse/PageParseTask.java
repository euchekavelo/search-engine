package searchengine.service.parse;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.context.ApplicationContext;
import searchengine.config.ConnectionProperties;
import searchengine.dto.UrlInfoDto;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.enums.Status;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicBoolean;

public class PageParseTask extends RecursiveAction {

    private static ApplicationContext applicationContext;
    private final String url;
    private final Object object = 1;
    private String shortUrl;
    private static SiteRepository siteRepository;
    private static PageRepository pageRepository;
    private static String referer;
    private static String userAgent;
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
        PageParseTask.siteRepository = applicationContext.getBean(SiteRepository.class);
        PageParseTask.pageRepository = applicationContext.getBean(PageRepository.class);
        PageParseTask.userAgent = applicationContext.getBean(ConnectionProperties.class).getUserAgent();
        PageParseTask.referer = applicationContext.getBean(ConnectionProperties.class).getReferer();
        PageParseTask.wrongTypes = wrongTypes;
        isInterrupt.set(new AtomicBoolean(false));
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
        if (isInterrupt.get().get()) {
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
        Optional<Page> optionalPageEntity = pageRepository.findPageByPathAndSite(shortUrl, site);
        if (optionalPageEntity.isPresent()) {
            return;
        }

        try {
            UrlInfoDto urlInfoDto = getUrlInfoDto(url);
            Document document = urlInfoDto.getDocument();
            if (document != null) {
                savePageEntity(site, shortUrl, urlInfoDto);
                Elements elements = document.select("a");
                fillSetChildTasks(setChildTasks, elements);
            }
        } catch (IOException e) {
            fixIndexingError(site, e);
        }
    }

    private void fixIndexingError(Site site, IOException ex) {
        site.setLastError("Ошибка индексации: " + ex.getMessage());
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

    private void savePageEntity(Site site, String link, UrlInfoDto urlInfoDto) {
        Page page = new Page();
        page.setCode(urlInfoDto.getCodeStatus());
        page.setSite(site);
        page.setPath(link);
        page.setContent(urlInfoDto.getDocument().html());
        pageRepository.save(page);

        site.setStatusTime(LocalDateTime.now());
        siteRepository.save(site);
    }

    private boolean isCorrectLink(String link) {
        return !link.isEmpty() && link.startsWith(globalPage.get())
                && !link.contains("#") && !link.contains("@")
                && !wrongTypes.contains(link.substring(link.lastIndexOf(".") + 1));
    }

    private UrlInfoDto getUrlInfoDto(String url) throws IOException {
        UrlInfoDto urlInfoDto = new UrlInfoDto();
        Connection.Response response = Jsoup.connect(url)
                .userAgent(userAgent)
                .referrer(referer)
                .ignoreHttpErrors(true)
                .execute();

        urlInfoDto.setCodeStatus(response.statusCode());
        urlInfoDto.setDocument(response.parse());

        return urlInfoDto;
    }
}
