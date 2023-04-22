package searchengine.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.SearchResultsResponse;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.exception.ErrorCustomException;
import searchengine.service.IndexingService;
import searchengine.service.SearchService;
import searchengine.service.StatisticsService;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SearchService searchService;

    @Autowired
    public ApiController(StatisticsService statisticsService, IndexingService indexingService,
                         SearchService searchService) {

        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
        this.searchService = searchService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<IndexingResponse> startIndexing() throws ErrorCustomException {
        return ResponseEntity.ok(indexingService.indexSites());
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<IndexingResponse> stopIndexing() throws ErrorCustomException {
        return ResponseEntity.ok(indexingService.stopSiteIndexing());
    }

    @PostMapping("/indexPage")
    public ResponseEntity<IndexingResponse> indexPage(@Param("url") String url) throws ErrorCustomException {
        return ResponseEntity.ok(indexingService.indexPage(url));
    }

    @GetMapping("/search")
    public ResponseEntity<SearchResultsResponse> search(@RequestParam("query") String query,
                                                        @RequestParam("site") String site,
                                                        @RequestParam("offset") Integer offset,
                                                        @RequestParam("limit") Integer limit) {

        return ResponseEntity.ok(searchService.search(query, site, offset, limit));
    }
}
