package searchengine.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.SiteConfig;
import searchengine.config.SitesListConfig;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SiteServiceImpl implements SiteService {

    private final SitesListConfig sitesListConfig;

    @Override
    public List<String> getSitePaths() {
        return sitesListConfig.getSiteConfigs().stream()
                .map(SiteConfig::getUrl)
                .toList();
    }
}
