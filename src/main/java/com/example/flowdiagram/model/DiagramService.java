package com.example.flowdiagram.model;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 図の生成に関する処理を全て行う model 層のサービス（検証・レイアウト計算・スタイル解決・
 * HTML/CSS描画・ページ組み立て）。API層（{@code com.example.flowdiagram.api}）はこのクラスを
 * 呼び出すだけで、ここに書かれた処理の内容を知らない。
 */
@Service
public class DiagramService {

    /** ブラウザでそのまま開ける完結した HTML を返す。 */
    public String renderPage(FlowSpec spec) {
        Rendered r = render(spec);
        return r.writer.page(spec.displayTitle(), r.css, r.canvas);
    }

    /** 既存ページへの埋め込み用の HTML 断片を返す。 */
    public String renderFragment(FlowSpec spec) {
        Rendered r = render(spec);
        return r.writer.fragment(r.css, r.canvas);
    }

    private record Rendered(String css, String canvas, HtmlPageWriter writer) {
    }

    private Rendered render(FlowSpec spec) {
        FlowValidator.validate(spec);
        Theme theme = spec.resolvedTheme();
        Layout layout = new LayoutEngine(theme).build(spec);
        Map<String, KindStyle> kindStyles = StyleRegistry.resolveKindStyles(spec);
        Map<String, ActionTypeStyle> typeStyles = StyleRegistry.resolveTypeStyles(spec);
        String css = new CssBuilder(theme).build();
        String canvas = new HtmlDiagramRenderer(theme, kindStyles, typeStyles).render(layout);
        return new Rendered(css, canvas, new HtmlPageWriter(theme));
    }

    // --- サンプルデータ ---

    public FlowSpec sampleSpec() {
        FlowSpec spec = new FlowSpec();
        spec.title = "サンプル1（ボタン型）";
        spec.kinds = defaultKinds();
        spec.types = defaultTypes();

        StateSpec a = new StateSpec("ステータスA");
        a.kind = "ステータス";
        a.actions = List.of(
                action("button", "ボタン1", "ステータスB"),
                action("button", "ボタン2", "ステータスX"),
                action("button", "ボタン3", "ステータスY"));

        StateSpec b = new StateSpec("ステータスB");
        b.kind = "ステータス";
        b.actions = List.of(
                action("button", "次へ", "ステータスC"),
                action("button", "Aに戻る", "ステータスA"));

        StateSpec c = new StateSpec("ステータスC");
        c.kind = "ステータス";
        c.actions = List.of(
                // v3.2: 1つのアクションから複数の遷移先を指定できる例（手続き1・ステータスDへの2本）
                action("button", "次へ", List.of("手続き1", "ステータスD")),
                action("button", "Bに戻る", "ステータスB"));

        StateSpec x = new StateSpec("ステータスX"); x.kind = "ステータス";
        StateSpec y = new StateSpec("ステータスY"); y.kind = "ステータス";
        StateSpec proc1 = new StateSpec("手続き1"); proc1.kind = "手続き";
        StateSpec d = new StateSpec("ステータスD"); d.kind = "ステータス";

        spec.states = List.of(a, b, c, x, y, proc1, d);
        return spec;
    }

    public FlowSpec sample2Spec() {
        FlowSpec spec = new FlowSpec();
        spec.title = "サンプル2（フロー型）";
        spec.kinds = defaultKinds();
        spec.types = defaultTypes();

        StateSpec a = new StateSpec("手続きA");
        a.kind = "手続き";
        a.actions = List.of(action("flow", "フロー1", "手続きB"));

        StateSpec b = new StateSpec("手続きB");
        b.kind = "手続き";
        b.actions = List.of(action("flow", "フロー2", "手続きC"));

        StateSpec c = new StateSpec("手続きC");
        c.kind = "手続き";
        c.actions = List.of(
                action("flow", "フロー3", "手続きD"),
                action("flow", "フロー4"),
                action("flow", "フロー5"),
                action("flow", "フロー6"),
                action("flow", "フロー7"));

        StateSpec d = new StateSpec("手続きD"); d.kind = "手続き";

        spec.states = List.of(a, b, c, d);
        return spec;
    }

    /** サンプル用の kinds 定義。旧v2系の「ステータス=薄い青／手続き=薄い緑」の見た目をJSON側で再現する。 */
    private static Map<String, KindStyle> defaultKinds() {
        Map<String, KindStyle> kinds = new LinkedHashMap<>();

        KindStyle status = new KindStyle();
        status.headerBackground = "#eaf4ff";
        status.background = "#f8fbfe";
        status.border = "#cfe3f5";
        status.textColor = "#2c6291";
        kinds.put("ステータス", status);

        KindStyle procedure = new KindStyle();
        procedure.headerBackground = "#eaf8ee";
        procedure.background = "#f8fcf9";
        procedure.border = "#cdeada";
        procedure.textColor = "#2f7a52";
        kinds.put("手続き", procedure);

        return kinds;
    }

    /** サンプル用の types 定義。旧v2系の「ボタン=白／フロー=薄い青枠」の見た目をJSON側で再現する。 */
    private static Map<String, ActionTypeStyle> defaultTypes() {
        Map<String, ActionTypeStyle> types = new LinkedHashMap<>();

        ActionTypeStyle button = new ActionTypeStyle();
        button.background = "#ffffff";
        button.border = "#c4c9d0";
        button.borderWidth = 1.5;
        button.textColor = "#2d3748";
        button.shadow = "0 1px 2px rgba(15,23,42,.10)";
        types.put("button", button);

        ActionTypeStyle flow = new ActionTypeStyle();
        flow.background = "#f8fbfe";
        flow.border = "#7fb8ee";
        flow.borderWidth = 1.5;
        flow.textColor = "#2c6291";
        types.put("flow", flow);

        return types;
    }

    private static ActionSpec action(String type, String label) {
        return new ActionSpec(type, label, (List<String>) null);
    }

    private static ActionSpec action(String type, String label, String next) {
        return new ActionSpec(type, label, next);
    }

    private static ActionSpec action(String type, String label, List<String> next) {
        return new ActionSpec(type, label, next);
    }
}
