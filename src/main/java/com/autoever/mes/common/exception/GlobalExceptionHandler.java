package com.autoever.mes.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(SQLException.class)
    public ResponseEntity<Map<String, String>> handleSqlException(SQLException ex) {
        log.error("SQL Error: Code={}, Message={}", ex.getErrorCode(), ex.getMessage(), ex);
        Map<String, String> error = new HashMap<>();
        error.put("error", "DATABASE_ERROR");
        error.put("message", "Database operation failed");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception ex) {
        log.error("Unexpected error", ex);
        Map<String, String> error = new HashMap<>();
        error.put("error", "INTERNAL_ERROR");
        error.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
