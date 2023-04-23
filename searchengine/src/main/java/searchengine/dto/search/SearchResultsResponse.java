package searchengine.dto.search;

import lombok.Data;

import java.util.List;

@Data
public class SearchResultsResponse {

    private boolean result;
    private int count;
    private List<SearchResult> data;
}
