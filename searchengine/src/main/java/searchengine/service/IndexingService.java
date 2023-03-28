package searchengine.service;

import searchengine.dto.indexing.IndexingResponse;
import searchengine.exception.ErrorCustomException;

public interface IndexingService {

    IndexingResponse indexSites() throws ErrorCustomException;
    IndexingResponse stopSiteIndexing() throws ErrorCustomException;
    IndexingResponse indexPage(String url) throws ErrorCustomException;
}
