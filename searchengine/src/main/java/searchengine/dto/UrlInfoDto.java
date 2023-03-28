package searchengine.dto;

import lombok.Data;
import org.jsoup.nodes.Document;

@Data
public class UrlInfoDto {
    private int CodeStatus;
    private Document document;
}
