package com.example.flowdiagram.api.controller;

import com.example.flowdiagram.api.controller.FlowSpecEntity.ActionSpecEntity;
import com.example.flowdiagram.api.controller.FlowSpecEntity.StateSpecEntity;
import com.example.flowdiagram.api.service.DiagramApiService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.Set;

/** APIコントローラ。 */
@RestController
public class DiagramController {

    private static final String HTML = MediaType.TEXT_HTML_VALUE + ";charset=UTF-8";

    private final DiagramApiService service;

    public DiagramController(DiagramApiService service) {
        this.service = service;
    }

    @PostMapping(path = "/api/diagram", produces = HTML)
    public ResponseEntity<String> diagram(@RequestBody FlowSpecEntity spec) {
        validateNextReferencesExist(spec);
        return service.diagram(spec);
    }

    /** next参照チェック処理。 */
    private void validateNextReferencesExist(FlowSpecEntity spec) {
        if (spec == null || spec.states == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "states が指定されていません");
        }
        Set<String> labels = new HashSet<>();
        for (StateSpecEntity s : spec.states) {
            if (s != null && s.label != null) {
                labels.add(s.label);
            }
        }
        for (StateSpecEntity s : spec.states) {
            if (s == null || s.actions == null) {
                continue;
            }
            for (ActionSpecEntity a : s.actions) {
                if (a == null || a.next == null) {
                    continue;
                }
                for (String next : a.next) {
                    if (next != null && !next.isBlank() && !labels.contains(next)) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "パラメータ不足: actions[].next が存在しない label を指しています: " + next);
                    }
                }
            }
        }
    }
}
