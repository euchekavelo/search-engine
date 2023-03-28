package searchengine.dto;

import lombok.Data;

@Data
public class ErrorResponse {

    private boolean result;
    private String error;
}
