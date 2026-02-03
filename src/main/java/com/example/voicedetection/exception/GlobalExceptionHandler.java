package com.example.voicedetection.exception;



import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(
            IllegalArgumentException ex) {

        Map<String, Object> error = new LinkedHashMap<>();
        error.put("status", "error");
        error.put("error", "BAD_REQUEST");
        error.put("message", ex.getMessage());
        error.put("timestamp", Instant.now().toString());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericError(
            Exception ex) {
    	
    	//ex.printStackTrace();

        Map<String, Object> error = new LinkedHashMap<>();
        error.put("status", "error");
        error.put("error", "INTERNAL_SERVER_ERROR");
        error.put("message", "Unexpected server error");
        error.put("timestamp", Instant.now().toString());

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error);
    }
}

