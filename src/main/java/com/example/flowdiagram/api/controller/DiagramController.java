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

/**
 * API の入り口（basic-design.md 10章）。
 * 基本的には何もせず {@link DiagramApiService} を呼び出すだけ。
 * 提供するのは HTML を返す1本のみ。
 *
 * <p>ただし {@code actions[].next} が {@code states[].label} として定義されていない場合、
 * model層まで処理が進んでから気づかれにくいシステムエラー（basic-design.md 5章・10章）に
 * なってしまうため、ここでモデル層へ渡す前にあらかじめチェックし、パラメータ不足である旨を
 * 明示した {@link ResponseStatusException}（400）として返す（v3.16、ユーザー指示）。</p>
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
        validateNextReferencesExist(spec);
        return service.diagram(spec);
    }

    /**
     * {@code actions[].next} が {@code states[].label} として定義されているかを検証する。
     * 未定義の参照が1件でもあれば、パラメータ不足である旨のメッセージ付きで400を返す。
     */
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
