package searchengine.service;

import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;

import java.util.HashMap;
import java.util.List;

public interface LemmaService {

    HashMap<String, Integer> getQuantityLemmasInTheText(String text);

    Lemma createOrUpdateLemma(String lemma, Site site);

    void saveLemmasAndIndexes(String text, Site site, Page page);

    void deleteLemmaByIdIn(List<Integer> listIdentifiers);

    void deleteLemmaIndexesByIds(List<Integer> indexList);

    void saveLemmas(List<Lemma> lemmaList);

    void deleteAll();
}
