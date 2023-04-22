package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;
import searchengine.model.Site;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {

    Optional<Lemma> findLemmaByLemmaAndSite(String lemma, Site site);

    void deleteLemmaByIdIn(List<Integer> listIdentifiers);

    List<Lemma> findLemmaBySite_UrlAndLemmaInOrderByFrequencyAsc(String site_url, Collection<String> lemma);

    List<Lemma> findLemmaByLemmaInOrderByFrequencyAsc(Collection<String> lemma);
}
