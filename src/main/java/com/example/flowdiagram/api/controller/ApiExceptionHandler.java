package com.example.flowdiagram.api.controller;

import com.example.flowdiagram.FlowDiagramException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Map;

/** エラー応答（basic-design.md 9章）。 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(FlowDiagramException.class)
    public ResponseEntity<Map<String, Object>> onValidation(FlowDiagramException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "VALIDATION_ERROR", "messages", e.getMessages()));
    }

    /** プレビュー画面（v3.11で廃止）等、存在しない静的リソースへのアクセスは素直に404にする。 */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> onNoResource(NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "NOT_FOUND", "messages", List.of("APIのみ提供しています")));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> onBrokenJson(HttpMessageNotReadableException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "VALIDATION_ERROR",
                        "messages", List.of("JSON を解釈できませんでした: " + rootMessage(e))));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> onOther(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "INTERNAL_ERROR",
                        "messages", List.of(String.valueOf(e.getMessage()))));
    }

    private static String rootMessage(Throwable t) {
        Throwable c = t;
        while (c.getCause() != null) {
            c = c.getCause();
        }
        String m = c.getMessage();
        if (m == null) {
            return c.getClass().getSimpleName();
        }
        int nl = m.indexOf('\n');
        return nl > 0 ? m.substring(0, nl) : m;
    }
}
