package searchengine.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.Site;
import searchengine.repository.SiteRepository;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final SiteRepository siteRepository;

    @Override
    public StatisticsResponse getStatistics() {
        List<Site> siteList = siteRepository.findAll();

        TotalStatistics total = new TotalStatistics();
        total.setSites(siteList.size());
        total.setIndexing(true);
        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        for (Site site : siteList) {
            DetailedStatisticsItem detailedStatisticsItem = new DetailedStatisticsItem();
            detailedStatisticsItem.setStatus(site.getStatus().toString());
            detailedStatisticsItem.setName(site.getName());
            detailedStatisticsItem.setUrl(site.getUrl());
            detailedStatisticsItem.setError(site.getLastError());
            detailedStatisticsItem.setStatusTime(site.getStatusTime().atZone(ZoneId.systemDefault())
                    .toInstant().toEpochMilli());
            detailedStatisticsItem.setPages(site.getPageEntities().size());
            detailedStatisticsItem.setLemmas(site.getLemmaEntities().size());

            total.setPages(total.getPages() + site.getPageEntities().size());
            total.setLemmas(total.getLemmas() + site.getLemmaEntities().size());
            detailed.add(detailedStatisticsItem);
        }

        return getStatisticsResponse(total, detailed);
    }

    private StatisticsResponse getStatisticsResponse(TotalStatistics total, List<DetailedStatisticsItem> detailed) {
        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }
}
