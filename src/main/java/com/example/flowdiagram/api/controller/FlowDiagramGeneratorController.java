package com.example.flowdiagram.api.controller;

import com.example.flowdiagram.api.controller.FlowDiagramGeneratorRequest.ActionSpecEntity;
import com.example.flowdiagram.api.controller.FlowDiagramGeneratorRequest.StateSpecEntity;
import com.example.flowdiagram.api.service.FlowDiagramGeneratorService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
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
public class FlowDiagramGeneratorController {

    private static final String HTML = MediaType.TEXT_HTML_VALUE + ";charset=UTF-8";

    @Autowired
    private FlowDiagramGeneratorService flowDiagramGeneratorService;

    @PostMapping(path = "/api/diagram", produces = HTML)
    public ResponseEntity<String> diagram(@Valid @RequestBody FlowDiagramGeneratorRequest request) {
        validateNextReferencesExist(request);
        return flowDiagramGeneratorService.diagram(request);
    }

    /** next参照チェック処理。 */
    private void validateNextReferencesExist(FlowDiagramGeneratorRequest request) {
        if (request == null || request.getStates() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "states が指定されていません");
        }
        Set<String> labels = new HashSet<>();
        for (StateSpecEntity s : request.getStates()) {
            if (s != null && s.getLabel() != null) {
                labels.add(s.getLabel());
            }
        }
        for (StateSpecEntity s : request.getStates()) {
            if (s == null || s.getActions() == null) {
                continue;
            }
            for (ActionSpecEntity a : s.getActions()) {
                if (a == null || a.getNext() == null) {
                    continue;
                }
                for (String next : a.getNext()) {
                    if (next != null && !next.isBlank() && !labels.contains(next)) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "パラメータ不足: actions[].next が存在しない label を指しています: " + next);
                    }
                }
            }
        }
    }
}
