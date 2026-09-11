package com.example.flowdiagram;

import com.example.flowdiagram.core.CssBuilder;
import com.example.flowdiagram.core.Html;
import com.example.flowdiagram.core.HtmlDiagramRenderer;
import com.example.flowdiagram.core.HtmlPageWriter;
import com.example.flowdiagram.core.Layout;
import com.example.flowdiagram.core.LayoutEngine;
import com.example.flowdiagram.core.Palette;
import com.example.flowdiagram.core.StyleRegistry;
import com.example.flowdiagram.model.ActionSpec;
import com.example.flowdiagram.model.ActionTypeStyle;
import com.example.flowdiagram.model.FlowSpec;
import com.example.flowdiagram.model.KindStyle;
import com.example.flowdiagram.model.StateSpec;
import com.example.flowdiagram.model.Theme;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HtmlDiagramRendererTest {

    /** kinds/types をJSONで明示的に定義したサンプル（basic-design.md v3.0）。 */
    private static FlowSpec sample() {
        FlowSpec spec = new FlowSpec();
        spec.title = "テスト<フロー>";

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
        spec.kinds = kinds;

        Map<String, ActionTypeStyle> types = new LinkedHashMap<>();
        ActionTypeStyle button = new ActionTypeStyle();
        button.background = "#ffffff";
        button.border = "#c4c9d0";
        button.borderWidth = 1.5;
        button.textColor = "#2d3748";
        types.put("button", button);
        ActionTypeStyle flow = new ActionTypeStyle();
        flow.background = "#f8fbfe";
        flow.border = "#7fb8ee";
        flow.borderWidth = 1.5;
        flow.textColor = "#2c6291";
        types.put("flow", flow);
        spec.types = types;

        StateSpec a = new StateSpec("未開始");
        a.kind = "ステータス";
        a.actions = new ArrayList<>(List.of(new ActionSpec("button", "開始", "審査")));
        StateSpec b = new StateSpec("審査");
        b.kind = "手続き";
        b.actions = new ArrayList<>(List.of(
                new ActionSpec("flow", "自動連携", "審査"),
                new ActionSpec("button", "戻る", "未開始")));
        spec.states = new ArrayList<>(List.of(a, b));
        return spec;
    }

    private static String renderCanvas(FlowSpec spec) {
        Theme theme = spec.resolvedTheme();
        Layout layout = new LayoutEngine(theme).build(spec);
        Map<String, KindStyle> kindStyles = StyleRegistry.resolveKindStyles(spec);
        Map<String, ActionTypeStyle> typeStyles = StyleRegistry.resolveTypeStyles(spec);
        return new HtmlDiagramRenderer(theme, kindStyles, typeStyles).render(layout);
    }

    @Test
    void producesCanvasDiv() {
        String html = renderCanvas(sample());
        assertTrue(html.startsWith("<div class=\"fd-canvas\""), html.substring(0, Math.min(80, html.length())));
    }

    @Test
    void containsNoImageBasedRendering() {
        String html = renderCanvas(sample());
        assertFalse(html.contains("<svg"), "SVG を使わないこと");
        assertFalse(html.contains("<canvas"), "canvas を使わないこと");
        assertFalse(html.contains("<img"), "img を使わないこと");
    }

    @Test
    void escapesHtml() {
        FlowSpec spec = new FlowSpec();
        StateSpec a = new StateSpec("<script>alert(1)</script>");
        spec.states = new ArrayList<>(List.of(a));
        String html = renderCanvas(spec);
        assertFalse(html.contains("<script>"));
        assertTrue(html.contains("&lt;script&gt;"));
    }

    @Test
    void titleIsEscaped() {
        String html = renderCanvas(sample());
        assertTrue(html.contains("テスト&lt;フロー&gt;"));
    }

    @Test
    void kindBadgeShowsRawKindValueAndDynamicClass() {
        // basic-design.md v3.0: バッジの文言は kind の値そのもの。クラスは kind-<token>
        String html = renderCanvas(sample());
        assertTrue(html.contains("class=\"node kind-" + Html.labelToken("ステータス")
                + " lbl-" + Html.labelToken("未開始") + "\""));
        assertTrue(html.contains("class=\"node kind-" + Html.labelToken("手続き")
                + " lbl-" + Html.labelToken("審査") + "\""));
        assertTrue(html.contains(">ステータス</span>"));
        assertTrue(html.contains(">手続き</span>"));
    }

    @Test
    void actionTypeClassesAreDynamicTokens() {
        String html = renderCanvas(sample());
        assertTrue(html.contains("class=\"action type-" + Html.labelToken("button") + "\""));
        assertTrue(html.contains("class=\"action type-" + Html.labelToken("flow") + "\""));
    }

    @Test
    void undefinedKindAndTypeFallBackToNeutralDefault() {
        // kinds/types に定義が無いキーを使っても壊れず、既定スタイルが使われること
        FlowSpec spec = new FlowSpec();
        StateSpec a = new StateSpec("A");
        a.kind = "謎の種別";
        a.actions = new ArrayList<>(List.of(new ActionSpec("謎のタイプ", "next", (String) null)));
        spec.states = new ArrayList<>(List.of(a));
        String html = renderCanvas(spec);
        String css = new CssBuilder(spec.resolvedTheme()).build();

        assertTrue(html.contains("kind-" + Html.labelToken("謎の種別")));
        assertTrue(html.contains("type-" + Html.labelToken("謎のタイプ")));
        assertTrue(html.contains(">謎の種別</span>"));
    }

    @Test
    void omittedKindAndTypeUseDefaultKey() {
        FlowSpec spec = new FlowSpec();
        StateSpec a = new StateSpec("A");
        a.actions = new ArrayList<>(List.of(new ActionSpec(null, "next", (String) null)));
        spec.states = new ArrayList<>(List.of(a));
        String html = renderCanvas(spec);
        assertTrue(html.contains("kind-" + Html.labelToken("default")));
        assertTrue(html.contains("type-" + Html.labelToken("default")));
    }

    @Test
    void selfLoopAndBackEdgeProduceCloneNodesNotBackLines() {
        String html = renderCanvas(sample());
        assertTrue(html.contains("class=\"node-clone-ref"));
        assertFalse(html.contains("data-to="), "旧仕様の後退辺表現が残っていないこと");
    }

    @Test
    void arrowTipIsNotOvershotByLastSegmentExtension() {
        // basic-design.md v3.6: v3.4の対称延長は経路の終点（矢印の先端）にもかかってしまい、
        // 矢印の三角形を線が突き抜けて見える不具合を生んでいた。矢印の直前に来るセグメントが
        // 「両端とも --edge-w 分延長された」形（内部の継ぎ目用の延長）であってはならない
        String html = renderCanvas(sample());
        java.util.regex.Pattern overshoot = java.util.regex.Pattern.compile(
                "var\\(--edge-w\\)\\)\"></div>\\s*<div class=\"arrow\"");
        assertFalse(overshoot.matcher(html).find(),
                "矢印直前のセグメントが両端延長（内部継ぎ目用）になっていないこと");
    }

    @Test
    void lastSegmentStopsAtArrowBaseNotTip() {
        // basic-design.md v3.7: 矢印の三角形は先端に近づくほど細くなるため、線を先端まで伸ばすと
        // 斜辺からはみ出して見える。矢印に接続する最後のセグメントは arrowX - arrowSize（根元）で
        // 止め、arrowX（先端）までは伸ばさないこと
        FlowSpec spec = sample();
        Theme theme = spec.resolvedTheme();
        Layout layout = new LayoutEngine(theme).build(spec);
        String html = renderCanvas(spec);
        for (Layout.EdgeRoute e : layout.edges) {
            Layout.Segment last = e.segments.get(e.segments.size() - 1);
            assertTrue(last.horizontal, "最後のセグメントは常に水平（矢印は右向き）");
            assertEquals(e.arrowX, last.x + last.width, "セグメントの幾何座標自体は矢印の先端と一致する");

            // 直前のセグメントと繋がる側（extendStart）の有無で式の形が変わるので両方許容する
            String base = last.width - theme.arrowSize + "px";
            String withHalfEdge = "calc(" + (last.width - theme.arrowSize) + "px + var(--edge-w) / 2)";
            assertTrue(html.contains("width:" + base) || html.contains("width:" + withHalfEdge),
                    "最後のセグメントの描画幅は、幾何幅から arrowSize 分短縮されていること");
            assertFalse(html.contains("width:" + last.width + "px"),
                    "最後のセグメントが短縮前の幾何幅そのままで描画されていないこと");
        }
    }

    @Test
    void noIdConceptRemainsInMarkup() {
        String html = renderCanvas(sample());
        assertFalse(html.contains("data-id="));
        assertTrue(html.contains("data-label=\"未開始\""));
        assertTrue(html.contains("data-label=\"審査\""));
    }

    @Test
    void cloneHighlightUsesCssOnlyNoJs() {
        String html = renderCanvas(sample());
        assertFalse(html.contains("addEventListener"));
        assertFalse(html.contains("scrollIntoView"));
        String tokenA = Html.labelToken("未開始");
        String tokenB = Html.labelToken("審査");
        assertTrue(html.contains("lbl-" + tokenA) && html.contains("lbl-" + tokenB),
                "label ハッシュ化済みクラスが付与されること");
        assertTrue(html.contains(":has(~ .node-clone-ref.lbl-"), "複製がある label については :has() ルールが出ること");
    }

    @Test
    void cloneNodeHasNoTitleButKeepsHoverHighlight() {
        // v3.4: title によるツールチップ文言は廃止。CSSの:hover/:has()による強調表示は維持する
        String html = renderCanvas(sample());
        assertTrue(html.contains("class=\"node-clone-ref"), "複製ノードは描画される");
        assertFalse(html.contains("title=\""), "title 属性は出力しない");
        assertTrue(html.contains(":has(~ .node-clone-ref.lbl-"), "ホバー強調用のCSSルールは維持する");
    }

    @Test
    void noCloneNoHighlightStyleBlock() {
        FlowSpec spec = new FlowSpec();
        spec.title = "no-clone";
        StateSpec a = new StateSpec("A");
        a.actions = new ArrayList<>(List.of(new ActionSpec("button", "next", "B")));
        StateSpec b = new StateSpec("B");
        spec.states = new ArrayList<>(List.of(a, b));
        String html = renderCanvas(spec);
        assertFalse(html.contains(":has(~ .node-clone-ref.lbl-"), "複製が無い図には強調用CSSを出さない");
    }

    @Test
    void cloneNodeRendersNoActions() {
        Theme theme = Theme.defaults();
        Layout layout = new LayoutEngine(theme).build(sample());
        Layout.NodeBox clone = layout.nodes.stream().filter(n -> n.clone).findFirst().orElseThrow();
        assertEquals(LayoutEngine.CLONE_TEXT_HEIGHT, clone.height, "複製はテキスト表示のみの高さ");
    }

    @Test
    void noDescriptionOrNoteMarkupExists() {
        String html = renderCanvas(sample());
        assertFalse(html.contains("node-desc"));
        assertFalse(html.contains("edge-note"));
    }

    @Test
    void jsonDefinedKindAndTypeColorsAppearInMarkupCss() {
        // basic-design.md v3.0: kind/type の色は JSON 由来で、fd-canvas 内の動的CSSに出る
        FlowSpec spec = sample();
        String html = renderCanvas(spec);
        assertTrue(html.contains("#eaf4ff"), "ステータスのheaderBackgroundが出ること");
        assertTrue(html.contains("#eaf8ee"), "手続きのheaderBackgroundが出ること");
        assertTrue(html.contains("#c4c9d0"), "buttonのborderが出ること");
        assertTrue(html.contains("#7fb8ee"), "flowのborderが出ること");
    }

    @Test
    void edgeColorComesFromPaletteNotKindOrType() {
        String css = new CssBuilder(sample().resolvedTheme()).build();
        assertTrue(css.contains(Palette.EDGE_COLOR), "関係線は Palette.EDGE_COLOR で統一されていること");
    }

    @Test
    void allEdgesUseSingleEdgeColorVariable() {
        String css = new CssBuilder(sample().resolvedTheme()).build();
        long edgeColorOccurrences = css.lines().filter(l -> l.contains("--edge-color")).count();
        assertTrue(edgeColorOccurrences >= 1);
        assertFalse(css.contains(".edge.e-"));
    }

    @Test
    void fontFamilyDefaultsToMeiryoUi() {
        // basic-design.md v3.5: フォント既定値を Meiryo UI 優先へ変更
        String css = new CssBuilder(Theme.defaults()).build();
        assertTrue(css.contains("Meiryo UI"), "既定フォントに Meiryo UI が含まれること");
    }

    @Test
    void actionLabelWrapsInsteadOfEllipsis() {
        // basic-design.md v3.5: ボタン名は折り返し表示にし、ellipsis で省略しない
        String css = new CssBuilder(Theme.defaults()).build();
        int actionLabelRuleStart = css.indexOf(".action .action-label{");
        assertTrue(actionLabelRuleStart >= 0);
        int ruleEnd = css.indexOf('}', actionLabelRuleStart);
        String rule = css.substring(actionLabelRuleStart, ruleEnd);
        assertTrue(rule.contains("white-space:normal"), "折り返しを許可すること");
        assertTrue(rule.contains("overflow-wrap:anywhere"), "日本語でも折り返せること");
        assertFalse(rule.contains("text-overflow:ellipsis"), "省略記号は出さないこと");
    }

    @Test
    void longActionLabelGetsTallerMinHeightInline() {
        FlowSpec spec = new FlowSpec();
        StateSpec a = new StateSpec("A");
        a.actions = new ArrayList<>(List.of(
                new ActionSpec("button", "短い", "B"),
                new ActionSpec("button", "非常に長いボタンのラベルで折り返しが必要になるはずのテキストです", "B")));
        StateSpec b = new StateSpec("B");
        spec.states = new ArrayList<>(List.of(a, b));
        Theme theme = spec.resolvedTheme();
        Layout layout = new LayoutEngine(theme).build(spec);
        String html = new HtmlDiagramRenderer(theme, StyleRegistry.resolveKindStyles(spec),
                StyleRegistry.resolveTypeStyles(spec)).render(layout);

        assertTrue(html.contains("min-height:" + theme.actionRowHeight + "px"), "短いラベルは既定の高さのまま");
        boolean hasTallerRow = layout.nodeByLabel().get("A").actionRowHeights.stream()
                .anyMatch(h -> h > theme.actionRowHeight);
        assertTrue(hasTallerRow, "長いラベルのチップは既定より高い min-height を持つこと");
    }

    @Test
    void themeLayoutOverrideIsApplied() {
        FlowSpec spec = sample();
        spec.theme = new Theme();
        spec.theme.nodeWidth = 300;
        Theme resolved = spec.resolvedTheme();
        assertEquals(300, resolved.nodeWidth);
        assertEquals(50, resolved.headerHeight, "未指定フィールドは既定値のまま");
    }

    @Test
    void cssValueRejectsInjection() {
        FlowSpec spec = sample();
        spec.theme = new Theme();
        spec.theme.background = "#fff;} body{display:none} .x{a:b";
        String css = new CssBuilder(spec.resolvedTheme()).build();
        assertFalse(css.contains("body{display:none}"), "CSS インジェクションを弾くこと");
    }

    @Test
    void unconnectedNodeFeatureIsRemoved() {
        // basic-design.md v3.1: 未接続ノード機能は廃止。孤立ノードも通常の終端と同じ見た目で描画される
        FlowSpec spec = new FlowSpec();
        StateSpec a = new StateSpec("A");
        a.actions = new ArrayList<>(List.of(new ActionSpec("button", "next", "B")));
        StateSpec b = new StateSpec("B");
        StateSpec lonely = new StateSpec("浮いてるやつ"); // 誰からも参照されずアクションも無い
        spec.states = new ArrayList<>(List.of(a, b, lonely));
        String html = renderCanvas(spec);

        assertFalse(html.contains("data-unconnected"));
        assertFalse(html.contains("is-unconnected"));
        assertFalse(html.contains("swatch unconnected"));
        assertFalse(html.contains("未接続"));
    }

    @Test
    void legendListsActualKindsAndTypesUsed() {
        String html = renderCanvas(sample());
        assertTrue(html.contains("class=\"legend\""));
        assertTrue(html.contains(">ステータス</span>"));
        assertTrue(html.contains(">手続き</span>"));
        assertTrue(html.contains(">button</span>"));
        assertTrue(html.contains(">flow</span>"));
    }

    @Test
    void oneActionWithMultipleNextTargetsRendersOneChevronAndTwoEdges() {
        // basic-design.md 3.3/6.4 v3.1: 1つのアクションから複数の矢印を出せる
        FlowSpec spec = new FlowSpec();
        StateSpec a = new StateSpec("A");
        a.actions = new ArrayList<>(List.of(new ActionSpec("button", "go", List.of("B", "C"))));
        StateSpec b = new StateSpec("B");
        StateSpec c = new StateSpec("C");
        spec.states = new ArrayList<>(List.of(a, b, c));
        String html = renderCanvas(spec);

        long edgeCount = html.lines().filter(l -> l.contains("class=\"edge\" data-from=\"A\"")).count();
        assertEquals(2, edgeCount, "1つのアクションのnextが2件なら2本のエッジが出ること");
        long chevronCount = html.lines().filter(l -> l.contains("chev")).count();
        assertEquals(1, chevronCount, "アクションチップ自体は1つ、矢印マークも1つだけ表示する");
    }

    @Test
    void fullPageIsSelfContained() {
        FlowSpec spec = sample();
        Theme theme = spec.resolvedTheme();
        Layout layout = new LayoutEngine(theme).build(spec);
        Map<String, KindStyle> kindStyles = StyleRegistry.resolveKindStyles(spec);
        Map<String, ActionTypeStyle> typeStyles = StyleRegistry.resolveTypeStyles(spec);
        String css = new CssBuilder(theme).build();
        String canvas = new HtmlDiagramRenderer(theme, kindStyles, typeStyles).render(layout);
        String page = new HtmlPageWriter(theme).page(spec.title, css, canvas);

        assertTrue(page.startsWith("<!DOCTYPE html>"));
        assertTrue(page.contains("<style>"));
        assertFalse(page.contains("http://"), "外部参照を含まないこと");
        assertFalse(page.contains("https://"), "外部参照を含まないこと");
        assertFalse(page.contains("%CANVAS%"));
        assertFalse(page.contains("%CSS%"));
    }

    @Test
    void fragmentHasNoDoctype() {
        FlowSpec spec = sample();
        Theme theme = spec.resolvedTheme();
        Layout layout = new LayoutEngine(theme).build(spec);
        Map<String, KindStyle> kindStyles = StyleRegistry.resolveKindStyles(spec);
        Map<String, ActionTypeStyle> typeStyles = StyleRegistry.resolveTypeStyles(spec);
        String fragment = new HtmlPageWriter(theme).fragment(
                new CssBuilder(theme).build(),
                new HtmlDiagramRenderer(theme, kindStyles, typeStyles).render(layout));
        assertFalse(fragment.contains("<!DOCTYPE"));
        assertTrue(fragment.contains("<style>"));
        assertTrue(fragment.contains("fd-canvas"));
    }
}
