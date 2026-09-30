package com.example.flowdiagram.api;

import com.example.flowdiagram.api.entity.FlowSpecEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * API の入り口（basic-design.md 10章）。
 * 基本的には何もせず {@link DiagramApiService} を呼び出すだけ。
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

    @PostMapping(path = "/api/diagram/fragment", produces = HTML)
    public ResponseEntity<String> fragment(@RequestBody FlowSpecEntity spec) {
        return service.fragment(spec);
    }

    @GetMapping(path = "/api/sample", produces = MediaType.APPLICATION_JSON_VALUE)
    public FlowSpecEntity sample() {
        return service.sample();
    }

    @GetMapping(path = "/api/sample/1", produces = MediaType.APPLICATION_JSON_VALUE)
    public FlowSpecEntity sample1() {
        return service.sample1();
    }

    @GetMapping(path = "/api/sample/2", produces = MediaType.APPLICATION_JSON_VALUE)
    public FlowSpecEntity sample2() {
        return service.sample2();
    }
}
