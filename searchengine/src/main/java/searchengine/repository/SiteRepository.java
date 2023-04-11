package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Site;
import searchengine.model.enums.Status;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SiteRepository extends JpaRepository<Site, Integer> {

    void deleteByUrl(String url);
    Optional<Site> findSiteEntityByIdAndStatus(Integer id, Status status);
    Optional<Site> findSiteByUrl(String url);
    List<Site> findSiteEntitiesByUrlInAndStatus(Collection<String> url, Status status);
}
