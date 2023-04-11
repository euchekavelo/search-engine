package searchengine.service;

import lombok.RequiredArgsConstructor;
import org.apache.lucene.morphology.LuceneMorphology;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;

import java.util.*;

@Service
@RequiredArgsConstructor
public class LemmaServiceImpl implements LemmaService {

    private final LuceneMorphology russianLuceneMorphology;
    private final LuceneMorphology englishLuceneMorphology;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private static final List<String> INCORRECT_PARTS_OF_SPEECH = List.of("МЕЖД", "СОЮЗ", "ПРЕДЛ", "ЧАСТ",
            "INT", "CONJ", "PREP", "PART");

    @Override
    public HashMap<String, Integer> getQuantityLemmasInTheText(String text) {
        HashMap<String, Integer> lemmaStatistics = new HashMap<>();
        String cleanText = cleanHtml(text);
        List<String> words = List.of(getAnArrayOfWords(cleanText.trim().toLowerCase()));

        for (String word : words) {
            addValidWordForms(word, lemmaStatistics);
        }

        return lemmaStatistics;
    }

    @Override
    public Lemma createOrUpdateLemma(String lemma, Site site) {
        Optional<Lemma> optionalLemma = lemmaRepository.findLemmaByLemmaAndSite(lemma, site);

        Lemma updatedLemma;
        if (optionalLemma.isPresent()) {
            updatedLemma = optionalLemma.get();
            updatedLemma.setFrequency(updatedLemma.getFrequency() + 1);
        } else {
            updatedLemma = new Lemma();
            updatedLemma.setLemma(lemma);
            updatedLemma.setFrequency(1);
            updatedLemma.setSite(site);
        }

        lemmaRepository.save(updatedLemma);
        return updatedLemma;
    }

    @Override
    public void saveLemmasAndIndexes(String text, Site site, Page page) {
        HashMap<String, Integer> quantityLemmasInTheText = getQuantityLemmasInTheText(text);

        List<Index> indexList = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : quantityLemmasInTheText.entrySet()) {
            Lemma lemma = createOrUpdateLemma(entry.getKey(), site);

            Index index = new Index();
            index.setLemma(lemma);
            index.setPage(page);
            index.setRank(Double.valueOf(entry.getValue()));
            indexList.add(index);
        }

        indexRepository.saveAll(indexList);
    }

    @Override
    public void deleteLemmaByIdIn(List<Integer> listIdentifiers) {
        lemmaRepository.deleteLemmaByIdIn(listIdentifiers);
    }

    @Override
    public void deleteLemmaIndexesByIds(List<Integer> indexList) {
        indexRepository.deleteIndexByIdIn(indexList);
    }

    @Override
    public void saveLemmas(List<Lemma> lemmaList) {
        lemmaRepository.saveAll(lemmaList);
    }

    @Override
    public void deleteAll() {
        indexRepository.deleteAll();
        lemmaRepository.deleteAll();
    }

    private String cleanHtml(String html) {
        return Jsoup.parse(html).text();
    }

    private void addLemmasToTheCollection(String word, HashMap<String, Integer> lemmaStatistics,
                                                 LuceneMorphology luceneMorphology) {

        List<String> wordBaseForms = luceneMorphology.getNormalForms(word);
        for (String wordForm : wordBaseForms) {
            if (lemmaStatistics.containsKey(wordForm)) {
                Integer count = lemmaStatistics.get(wordForm) + 1;
                lemmaStatistics.put(wordForm, count);
            } else {
                lemmaStatistics.put(wordForm, 1);
            }
        }
    }

    private void addValidWordForms(String word, HashMap<String, Integer> lemmaStatistics) {
        if (isThisACyrillicWord(word)) {
            if (!partOfSpeechIsCorrect(russianLuceneMorphology, word)) {
                return;
            }
            addLemmasToTheCollection(word, lemmaStatistics, russianLuceneMorphology);
        } else if (isThisALatinWord(word)) {
            if (!partOfSpeechIsCorrect(englishLuceneMorphology, word)) {
                return;
            }
            addLemmasToTheCollection(word, lemmaStatistics, englishLuceneMorphology);
        }
    }

    private boolean partOfSpeechIsCorrect(LuceneMorphology luceneMorphology, String word) {
        List<String> wordBaseForms = luceneMorphology.getMorphInfo(word);
        for (String wordBase : wordBaseForms) {
            if (!isTheWordAllowed(wordBase)) {
                return false;
            }
        }

        return true;
    }

    private boolean isTheWordAllowed(String word) {
        String[] splitExpression = word.split(" ");
        String shortPartOfSpeech = splitExpression[1];

        for (String partSpeech : INCORRECT_PARTS_OF_SPEECH) {
            if (shortPartOfSpeech.equals(partSpeech)) {
                return false;
            }
        }

        return true;
    }

    private boolean isThisACyrillicWord(String word) {
        String regex = "[а-яА-Я]+";
        return word.matches(regex);
    }

    private boolean isThisALatinWord(String word) {
        String regex = "[a-zA-Z]+";
        return word.matches(regex);
    }

    private String[] getAnArrayOfWords(String text) {
        return text.replaceAll("[^а-яА-Яa-zA-Z\\s]", " ").trim().split("\\s+");
    }
}
