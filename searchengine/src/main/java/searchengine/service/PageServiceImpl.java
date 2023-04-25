package searchengine.service;

import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import searchengine.config.ConnectionPropertiesConfig;
import searchengine.dto.UrlInfo;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repository.PageRepository;

import java.io.IOException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PageServiceImpl implements PageService {

    private final PageRepository pageRepository;
    private final ConnectionPropertiesConfig connectionPropertiesConfig;

    @Override
    public Page savePageEntity(Site site, String link, UrlInfo urlInfo) {
        Page page = new Page();
        page.setCode(urlInfo.getCodeStatus());
        page.setSite(site);
        page.setPath(link);
        page.setContent(urlInfo.getDocument().html());
        pageRepository.save(page);

        return page;
    }

    @Override
    public UrlInfo getUrlInfoDto(String url) throws IOException {
        UrlInfo urlInfo = new UrlInfo();
        Connection.Response response = Jsoup.connect(url)
                .userAgent(connectionPropertiesConfig.getUserAgent())
                .referrer(connectionPropertiesConfig.getReferer())
                .ignoreHttpErrors(true)
                .execute();

        urlInfo.setCodeStatus(response.statusCode());
        urlInfo.setDocument(response.parse());

        return urlInfo;
    }

    @Override
    public void deletePageByPath(String path, Site site) {
        pageRepository.deletePageByPathAndSite(path, site);
    }

    @Override
    public Optional<Page> getPageByPathAndSite(String path, Site site) {
        return pageRepository.findPageByPathAndSite(path, site);
    }
}
