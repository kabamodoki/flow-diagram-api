package com.example.flowdiagram.web;

import com.example.flowdiagram.core.CssBuilder;
import com.example.flowdiagram.core.FlowValidator;
import com.example.flowdiagram.core.HtmlDiagramRenderer;
import com.example.flowdiagram.core.HtmlPageWriter;
import com.example.flowdiagram.core.Layout;
import com.example.flowdiagram.core.LayoutEngine;
import com.example.flowdiagram.model.ActionSpec;
import com.example.flowdiagram.model.FlowSpec;
import com.example.flowdiagram.model.StateSpec;
import com.example.flowdiagram.model.Theme;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** API（basic-design.md 9章）。 */
@RestController
public class DiagramController {

    private static final String HTML = MediaType.TEXT_HTML_VALUE + ";charset=UTF-8";

    @PostMapping(path = "/api/diagram", produces = HTML)
    public ResponseEntity<String> diagram(@RequestBody FlowSpec spec) {
        Rendered r = render(spec);
        return ResponseEntity.ok()
                .header("Content-Type", HTML)
                .body(r.writer.page(spec.displayTitle(), r.css, r.canvas));
    }

    @PostMapping(path = "/api/diagram/fragment", produces = HTML)
    public ResponseEntity<String> fragment(@RequestBody FlowSpec spec) {
        Rendered r = render(spec);
        return ResponseEntity.ok()
                .header("Content-Type", HTML)
                .body(r.writer.fragment(r.css, r.canvas));
    }

    @GetMapping(path = "/api/sample", produces = MediaType.APPLICATION_JSON_VALUE)
    public FlowSpec sample() {
        return sampleSpec();
    }

    // --- 内部 ---

    private record Rendered(String css, String canvas, HtmlPageWriter writer) {
    }

    private Rendered render(FlowSpec spec) {
        FlowValidator.validate(spec);
        Theme theme = spec.resolvedTheme();
        Layout layout = new LayoutEngine(theme).build(spec);
        String css = new CssBuilder(theme).build();
        String canvas = new HtmlDiagramRenderer(theme).render(layout);
        return new Rendered(css, canvas, new HtmlPageWriter(theme));
    }

    static FlowSpec sampleSpec() {
        FlowSpec spec = new FlowSpec();
        spec.title = "申請ワークフロー";

        StateSpec draft = new StateSpec("未申請");
        draft.kind = StateSpec.KIND_STATUS;
        draft.actions = List.of(
                action("button", "申請する", "審査中"),
                action("button", "一時保存", null),
                action("button", "コピーを作成", "未申請"),
                action("button", "破棄", "破棄"));

        StateSpec review = new StateSpec("審査中");
        review.kind = StateSpec.KIND_PROCEDURE;
        review.actions = List.of(
                action("button", "承認", "承認済み"),
                action("button", "差戻し", "未申請"),
                action("flow", "タイムアウト", "失効"));

        StateSpec approved = new StateSpec("承認済み");
        approved.kind = StateSpec.KIND_STATUS;
        approved.actions = List.of(
                action("flow", "後続手続きへ連携", "外部手続きへ引継ぎ"));

        StateSpec handoff = new StateSpec("外部手続きへ引継ぎ");
        handoff.kind = StateSpec.KIND_PROCEDURE;

        StateSpec expired = new StateSpec("失効");
        expired.kind = StateSpec.KIND_STATUS;

        StateSpec discarded = new StateSpec("破棄");
        discarded.kind = StateSpec.KIND_STATUS;

        spec.states = List.of(draft, review, approved, handoff, expired, discarded);
        return spec;
    }

    private static ActionSpec action(String type, String label, String next) {
        return new ActionSpec(type, label, next);
    }
}
