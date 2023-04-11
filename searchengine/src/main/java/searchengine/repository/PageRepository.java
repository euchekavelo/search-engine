package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Page;
import searchengine.model.Site;

import java.util.Optional;


@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {

    Optional<Page> findPageByPathAndSite(String path, Site site);

    void deletePageByPathAndSite(String path, Site site);
}
