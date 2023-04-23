package searchengine.dto;

import lombok.Data;
import org.jsoup.nodes.Document;

@Data
public class UrlInfo {

    private int CodeStatus;
    private Document document;
}
