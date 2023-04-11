package searchengine.service;

import searchengine.dto.UrlInfoDto;
import searchengine.model.Page;
import searchengine.model.Site;

import java.io.IOException;
import java.util.Optional;

public interface PageService {

    Page savePageEntity(Site site, String link, UrlInfoDto urlInfoDto);

    UrlInfoDto getUrlInfoDto(String url) throws IOException;

    void deletePageByPath(String path, Site site);

    Optional<Page> getPageByPathAndSite(String path, Site site);

    void deleteAll();
}
