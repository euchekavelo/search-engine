package searchengine.service;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import searchengine.dto.PageRelevance;
import searchengine.dto.search.SearchResult;
import searchengine.dto.search.SearchResultsResponse;
import searchengine.exception.ErrorCustomException;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.enums.Status;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.util.*;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final LemmaService lemmaService;
    private final LemmaRepository lemmaRepository;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private static final int COUNT_CHARACTERS_SNIPPET = 225;
    private int countCharactersSearchQuery;

    @Override
    public SearchResultsResponse search(String query, String site, Integer offset, Integer limit)
            throws ErrorCustomException {

        if (query.isEmpty()) {
            throw new ErrorCustomException("Задан пустой поисковой запрос.");
        }

        countCharactersSearchQuery = query.length();
        Set<String> setLemmasFromSearchQuery = lemmaService.getQuantityLemmasInTheText(query).keySet();
        int countLemmasFromSearchQuery = setLemmasFromSearchQuery.size();

        List<Integer> lemmaIds = getListOfSortedLemmaIds(site, setLemmasFromSearchQuery);
        List<Page> pageList = pageRepository.getListOfPagesByListOfLemmaIds(lemmaIds, countLemmasFromSearchQuery);
        TreeSet<PageRelevance> pageRelevanceTreeSet = performPageRelevanceCalculation(pageList);

        List<SearchResult> searchResultList = formSearchResultList(pageRelevanceTreeSet, query);
        List<SearchResult> partOfList = getPartOfList(offset, limit, searchResultList);

        return createSearchResultsResponse(partOfList, searchResultList.size());
    }

    private List<Integer> getListOfSortedLemmaIds(String site, Set<String> setLemmasFromSearchQuery)
            throws ErrorCustomException {

        List<Integer> lemmaIds;
        List<Status> statusList = List.of(Status.INDEXED, Status.FAILED);
        if (site.equals("All sites")) {
            List<Site> siteList = siteRepository.findSitesByStatusNotIn(statusList);
            if (!siteList.isEmpty()) {
                throw new ErrorCustomException("Все сайты польностью еще не проиндексированы.");
            }

            lemmaIds = lemmaRepository.findLemmaByLemmaInOrderByFrequencyAsc(setLemmasFromSearchQuery)
                    .stream()
                    .map(Lemma::getId)
                    .toList();;
        } else {
            Optional<Site> optionalSite = siteRepository.findSiteByUrlAndStatusIn(site, statusList);
            if (optionalSite.isEmpty()) {
                throw new ErrorCustomException("Указанный сайт еще полностью не проиндексирован.");
            }

            lemmaIds = lemmaRepository.findLemmaBySite_UrlAndLemmaInOrderByFrequencyAsc(site, setLemmasFromSearchQuery)
                    .stream()
                    .map(Lemma::getId)
                    .toList();
        }

        return lemmaIds;
    }

    private List<SearchResult> formSearchResultList(TreeSet<PageRelevance> pageRelevanceTreeSet, String query) {
        List<SearchResult> searchResultList = new ArrayList<>();
        for (PageRelevance pageRelevance : pageRelevanceTreeSet) {
            String content = pageRelevance.getPage().getContent();
            Optional<String> optionalSearchSnippet = searchSnippet(content, query);
            if (optionalSearchSnippet.isEmpty()) {
                continue;
            }
            String title = Jsoup.parse(content).title();

            SearchResult searchResult = new SearchResult();
            searchResult.setRelevance(pageRelevance.getRelativePageRelevance());
            searchResult.setUri(pageRelevance.getPage().getPath());
            searchResult.setTitle(title);
            searchResult.setSnippet(optionalSearchSnippet.get());
            searchResult.setSite(pageRelevance.getPage().getSite().getUrl());
            searchResult.setSiteName(pageRelevance.getPage().getSite().getName());
            searchResultList.add(searchResult);
        }

        return searchResultList;
    }

    private List<SearchResult> getPartOfList(Integer offset, Integer limit, List<SearchResult> searchResultList) {
        Pageable nextPage = PageRequest.of(offset, limit);
        int start = Math.min((int)nextPage.getOffset(), searchResultList.size());
        int end = Math.min((start + nextPage.getPageSize()), searchResultList.size());
        PageImpl<SearchResult> page =
                new PageImpl<>(searchResultList.subList(start, end), nextPage, searchResultList.size());

        return page.getContent();
    }

    private SearchResultsResponse createSearchResultsResponse(List<SearchResult> searchResultList, int totalCount) {
        SearchResultsResponse searchResultsResponse = new SearchResultsResponse();
        searchResultsResponse.setResult(true);
        searchResultsResponse.setData(searchResultList);
        searchResultsResponse.setCount(totalCount);

        return searchResultsResponse;
    }

    private TreeSet<PageRelevance> performPageRelevanceCalculation(List<Page> pageList) {
        List<PageRelevance> pageRelevanceList = new ArrayList<>();
        double maxSearchRank = 0.0;
        for (Page page : pageList) {
            List<Index> indexList = page.getIndexEntities();
            double absolutePageRelevance = indexList.stream()
                    .mapToDouble(Index::getRank)
                    .sum();

            maxSearchRank = Math.max(absolutePageRelevance, maxSearchRank);

            PageRelevance pageRelevance = new PageRelevance();
            pageRelevance.setPage(page);
            pageRelevance.setAbsolutePageRelevance(absolutePageRelevance);
            pageRelevanceList.add(pageRelevance);
        }

        TreeSet<PageRelevance> pageRelevanceTreeSet = new TreeSet<>();
        for (PageRelevance pageRelevance : pageRelevanceList) {
            double relativePageRelevance = pageRelevance.getAbsolutePageRelevance() / maxSearchRank;
            pageRelevance.setRelativePageRelevance(relativePageRelevance);
            pageRelevanceTreeSet.add(pageRelevance);
        }

        return pageRelevanceTreeSet;
    }

    private Optional<String> searchSnippet(String pageContent, String originalSearchText) {
        StringBuilder cleanedPageContent = new StringBuilder(lemmaService.cleanHtml(pageContent));
        int lengthCleanedPageContent = cleanedPageContent.length();
        int startIndex = cleanedPageContent.indexOf(originalSearchText);
        if (startIndex == -1) {
            return Optional.empty();
        }

        int endIndex = startIndex + countCharactersSearchQuery;

        String highlightedMatch = new StringBuilder(cleanedPageContent.substring(startIndex, endIndex))
                .insert(0, "<b>")
                .insert(countCharactersSearchQuery + 3, "</b>").toString();

        StringBuilder fragmentTextBuilder = new StringBuilder();
        if (lengthCleanedPageContent - (startIndex + 1) > COUNT_CHARACTERS_SNIPPET) {
            fragmentTextBuilder.append(cleanedPageContent.substring(startIndex, startIndex + COUNT_CHARACTERS_SNIPPET));
            fragmentTextBuilder.append("...");
        } else {
            fragmentTextBuilder.append(cleanedPageContent.substring(startIndex, lengthCleanedPageContent));
        }

        if (startIndex != 0) {
            fragmentTextBuilder.insert(0, "...");
        }

        String finalSnippet = fragmentTextBuilder.toString().replaceAll(originalSearchText, highlightedMatch);
        return Optional.of(finalSnippet);
    }
}
