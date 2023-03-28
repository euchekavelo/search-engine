package searchengine.service;

import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import searchengine.config.ConnectionProperties;
import searchengine.dto.UrlInfoDto;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repository.PageRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PageServiceImpl {

    private final PageRepository pageRepository;
    private final ConnectionProperties connectionProperties;

    public Page savePageEntity(Site site, String link, UrlInfoDto urlInfoDto) {
        Page page = new Page();
        page.setCode(urlInfoDto.getCodeStatus());
        page.setSite(site);
        page.setPath(link);
        page.setContent(urlInfoDto.getDocument().html());
        pageRepository.save(page);

        return page;
    }

    public UrlInfoDto getUrlInfoDto(String url) throws IOException {
        UrlInfoDto urlInfoDto = new UrlInfoDto();
        Connection.Response response = Jsoup.connect(url)
                .userAgent(connectionProperties.getUserAgent())
                .referrer(connectionProperties.getReferer())
                .ignoreHttpErrors(true)
                .execute();

        urlInfoDto.setCodeStatus(response.statusCode());
        urlInfoDto.setDocument(response.parse());

        return urlInfoDto;
    }

    public void deletePageByPath(String path) {
        pageRepository.deletePageByPath(path);
    }

    public Optional<Page> getPageByPathAndSite(String path, Site site) {
        return pageRepository.findPageByPathAndSite(path, site);
    }
}