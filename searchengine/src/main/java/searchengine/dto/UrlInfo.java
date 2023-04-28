package searchengine.dto;

import lombok.Data;
import org.jsoup.nodes.Document;

@Data
public class UrlInfo {

    private int codeStatus;
    private Document document;
}
