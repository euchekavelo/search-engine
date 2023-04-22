package searchengine.service;

import searchengine.dto.SearchResultsResponse;

public interface SearchService {

    SearchResultsResponse search(String query, String site, Integer offset, Integer limit);
}
