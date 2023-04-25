package searchengine.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import searchengine.dto.ErrorResponse;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ErrorCustomException.class)
    public ResponseEntity<ErrorResponse> handEmptyException(ErrorCustomException ex) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setError(ex.getMessage());
        errorResponse.setResult(false);
        return ResponseEntity.badRequest().body(errorResponse);
    }
}
