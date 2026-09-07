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

    @GetMapping(path = "/api/sample/1", produces = MediaType.APPLICATION_JSON_VALUE)
    public FlowSpec sample1() {
        return sampleSpec();
    }

    @GetMapping(path = "/api/sample/2", produces = MediaType.APPLICATION_JSON_VALUE)
    public FlowSpec sample2() {
        return sample2Spec();
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
        spec.title = "サンプル1（ボタン型）";

        StateSpec a = new StateSpec("ステータスA");
        a.kind = StateSpec.KIND_STATUS;
        a.actions = List.of(
                action("button", "ボタン1", "ステータスB"),
                action("button", "ボタン2", "ステータスE"),
                action("button", "ボタン3", "ステータスF"),
                action("button", "ボタン4", "ステータスG"));

        StateSpec b = new StateSpec("ステータスB");
        b.kind = StateSpec.KIND_STATUS;
        b.actions = List.of(
                action("button", "次へ", "ステータスC"),
                action("button", "Aに戻る", "ステータスA"));

        StateSpec c = new StateSpec("ステータスC");
        c.kind = StateSpec.KIND_STATUS;
        c.actions = List.of(
                action("button", "次へ", "ステータスD"),
                action("button", "Aに戻る", "ステータスA"));

        StateSpec d = new StateSpec("ステータスD"); d.kind = StateSpec.KIND_PROCEDURE;
        StateSpec e = new StateSpec("ステータスE"); e.kind = StateSpec.KIND_STATUS;
        StateSpec f = new StateSpec("ステータスF"); f.kind = StateSpec.KIND_STATUS;
        StateSpec g = new StateSpec("ステータスG"); g.kind = StateSpec.KIND_STATUS;
        StateSpec z = new StateSpec("ステータスZ"); z.kind = StateSpec.KIND_STATUS;

        spec.states = List.of(a, b, c, d, e, f, g, z);
        return spec;
    }

    static FlowSpec sample2Spec() {
        FlowSpec spec = new FlowSpec();
        spec.title = "サンプル2（フロー型）";

        StateSpec a = new StateSpec("手続きA");
        a.kind = StateSpec.KIND_PROCEDURE;
        a.actions = List.of(action("flow", "フロー1", "手続きB"));

        StateSpec b = new StateSpec("手続きB");
        b.kind = StateSpec.KIND_PROCEDURE;
        b.actions = List.of(action("flow", "フロー2", "手続きC"));

        StateSpec c = new StateSpec("手続きC");
        c.kind = StateSpec.KIND_PROCEDURE;
        c.actions = List.of(
                action("flow", "フロー3", "手続きD"),
                action("flow", "フロー4", null),
                action("flow", "フロー5", null),
                action("flow", "フロー6", null),
                action("flow", "フロー7", null));

        StateSpec d = new StateSpec("手続きD"); d.kind = StateSpec.KIND_PROCEDURE;

        spec.states = List.of(a, b, c, d);
        return spec;
    }

    private static ActionSpec action(String type, String label, String next) {
        return new ActionSpec(type, label, next);
    }
}
