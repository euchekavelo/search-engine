package searchengine.service;

import searchengine.model.Lemma;
import searchengine.model.Site;

import java.util.HashMap;
import java.util.List;

public interface LemmaService {

    HashMap<String, Integer> getQuantityLemmasInTheText(String text);
    Lemma createOrUpdateLemma(String lemma, Site site);
    void saveLemmas(List<Lemma> lemmaList);
    void deleteLemmaByIdIn(List<Integer> listIdentifiers);
}
