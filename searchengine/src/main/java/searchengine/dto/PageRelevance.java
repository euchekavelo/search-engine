package searchengine.dto;

import lombok.Data;
import searchengine.model.Page;

@Data
public class PageRelevance implements Comparable<PageRelevance> {

    private double absolutePageRelevance;
    private double relativePageRelevance;
    private Page page;

    @Override
    public int compareTo(PageRelevance o) {
        int compare = -Double.compare(this.getRelativePageRelevance(), o.getRelativePageRelevance());
        if (compare == 0) {
            compare = Integer.compare(this.hashCode(), o.hashCode());
        }

        return compare;
    }
}
