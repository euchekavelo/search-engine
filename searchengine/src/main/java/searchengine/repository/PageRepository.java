package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.Page;
import searchengine.model.Site;

import java.util.Collection;
import java.util.List;
import java.util.Optional;


@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {

    Optional<Page> findPageByPathAndSite(String path, Site site);

    void deletePageByPathAndSite(String path, Site site);

    @Query(value =
            "WITH page_ids AS (\n" +
            "\tSELECT\ti.page_id\n" +
            "\tFROM \tsearch_engine.index i\n" +
            "\tWHERE \ti.lemma_id IN (:lemmaIds)\n" +
            "\tGROUP BY\ti.page_id\n" +
            "\tHAVING\tCOUNT(*) = :countLemmasFromQuery\n" +
            ")\n" +
            "\n" +
            "SELECT\t*\n" +
            "FROM\tsearch_engine.page p\n" +
            "WHERE\tp.id IN (SELECT * FROM page_ids)", nativeQuery = true)
    List<Page> getListOfPagesByListOfLemmaIds(@Param("lemmaIds") Collection<Integer> collectionLemmas,
                                              @Param("countLemmasFromQuery") Integer countLemmasFromQuery);
}
