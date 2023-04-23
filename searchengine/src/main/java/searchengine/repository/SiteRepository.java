package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Site;
import searchengine.model.enums.Status;

import java.util.List;
import java.util.Optional;

@Repository
public interface SiteRepository extends JpaRepository<Site, Integer> {

    Optional<Site> findSiteEntityByIdAndStatus(Integer id, Status status);

    Optional<Site> findSiteByUrl(String url);

    List<Site> findSitesByStatusNotIn(List<Status> statusList);

    Optional<Site> findSiteByUrlAndStatusIn(String url, List<Status> status);
}
