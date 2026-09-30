package com.example.flowdiagram.api.controller;

import com.example.flowdiagram.api.service.DiagramApiService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * API の入り口（basic-design.md 10章）。
 * 基本的には何もせず {@link DiagramApiService} を呼び出すだけ。
 * 提供するのは HTML を返す1本のみ。
 */
@RestController
public class DiagramController {

    private static final String HTML = MediaType.TEXT_HTML_VALUE + ";charset=UTF-8";

    private final DiagramApiService service;

    public DiagramController(DiagramApiService service) {
        this.service = service;
    }

    @PostMapping(path = "/api/diagram", produces = HTML)
    public ResponseEntity<String> diagram(@RequestBody FlowSpecEntity spec) {
        return service.diagram(spec);
    }
}
