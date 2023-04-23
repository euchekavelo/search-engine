package searchengine.service;

import searchengine.dto.search.SearchResultsResponse;
import searchengine.exception.ErrorCustomException;

public interface SearchService {

    SearchResultsResponse search(String query, String site, Integer offset, Integer limit) throws ErrorCustomException;
}
