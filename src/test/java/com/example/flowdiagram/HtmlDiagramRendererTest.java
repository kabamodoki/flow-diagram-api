package com.example.flowdiagram;

import com.example.flowdiagram.core.CssBuilder;
import com.example.flowdiagram.core.Html;
import com.example.flowdiagram.core.HtmlDiagramRenderer;
import com.example.flowdiagram.core.HtmlPageWriter;
import com.example.flowdiagram.core.Layout;
import com.example.flowdiagram.core.LayoutEngine;
import com.example.flowdiagram.core.Palette;
import com.example.flowdiagram.model.ActionSpec;
import com.example.flowdiagram.model.FlowSpec;
import com.example.flowdiagram.model.StateSpec;
import com.example.flowdiagram.model.Theme;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HtmlDiagramRendererTest {

    private static FlowSpec sample() {
        FlowSpec spec = new FlowSpec();
        spec.title = "テスト<フロー>";
        StateSpec a = new StateSpec("未開始");
        a.kind = StateSpec.KIND_STATUS;
        a.actions = new ArrayList<>(List.of(new ActionSpec("button", "開始", "審査")));
        StateSpec b = new StateSpec("審査");
        b.kind = StateSpec.KIND_PROCEDURE;
        b.actions = new ArrayList<>(List.of(
                new ActionSpec("flow", "自動連携", "審査"),
                new ActionSpec("button", "戻る", "未開始")));
        spec.states = new ArrayList<>(List.of(a, b));
        return spec;
    }

    private static String renderCanvas(FlowSpec spec) {
        Theme theme = spec.resolvedTheme();
        Layout layout = new LayoutEngine(theme).build(spec);
        return new HtmlDiagramRenderer(theme).render(layout);
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
        // label がそのまま識別子を兼ねるため、next の参照整合性を保ったまま
        // 最初から不正な文字列を label に持つ state を作る（sample() を後から書き換えない）
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
    void kindBadgeAndClassAppear() {
        String html = renderCanvas(sample());
        assertTrue(html.contains("class=\"node k-status lbl-" + Html.labelToken("未開始") + "\""));
        assertTrue(html.contains("class=\"node k-procedure lbl-" + Html.labelToken("審査") + "\""));
        assertTrue(html.contains(Palette.KIND_BADGE_LABEL_STATUS));
        assertTrue(html.contains(Palette.KIND_BADGE_LABEL_PROCEDURE));
    }

    @Test
    void actionTypeClassesAppear() {
        String html = renderCanvas(sample());
        assertTrue(html.contains("class=\"action a-button\""));
        assertTrue(html.contains("class=\"action a-flow\""));
    }

    @Test
    void selfLoopAndBackEdgeProduceCloneNodesNotBackLines() {
        String html = renderCanvas(sample());
        assertTrue(html.contains("class=\"node-clone-ref"));
        // 複製ノードは fromStateLabel のみを持つ edge（常に前進）として描画される
        assertFalse(html.contains("data-to="), "旧仕様の後退辺表現が残っていないこと");
    }

    @Test
    void noIdConceptRemainsInMarkup() {
        // basic-design.md v2.5: id は廃止。data-id ではなく data-label のみが出ること
        String html = renderCanvas(sample());
        assertFalse(html.contains("data-id="));
        assertTrue(html.contains("data-label=\"未開始\""));
        assertTrue(html.contains("data-label=\"審査\""));
    }

    @Test
    void cloneHighlightUsesCssOnlyNoJs() {
        String html = renderCanvas(sample());
        // basic-design.md v2.3: クリック/scrollIntoView のJSは廃止し、:hover + :has() の CSS のみ
        assertFalse(html.contains("addEventListener"));
        assertFalse(html.contains("scrollIntoView"));
        String tokenA = Html.labelToken("未開始");
        String tokenB = Html.labelToken("審査");
        assertTrue(html.contains("lbl-" + tokenA) && html.contains("lbl-" + tokenB),
                "label ハッシュ化済みクラスが付与されること");
        assertTrue(html.contains(":has(~ .node-clone-ref.lbl-"), "複製がある label については :has() ルールが出ること");
    }

    @Test
    void cloneNodeHasTitleButRealNodeDoesNot() {
        String html = renderCanvas(sample());
        assertTrue(html.contains("class=\"node-clone-ref") && html.contains("title=\""), "複製には説明用の title を付与する");
        // 実ノード（クローンでない「未開始」）の開始タグに title が付いていないこと
        String tag = "class=\"node k-status lbl-" + Html.labelToken("未開始") + "\" data-label=\"未開始\"";
        int realTagStart = html.indexOf(tag);
        assertTrue(realTagStart >= 0);
        int tagEnd = html.indexOf('>', realTagStart);
        assertFalse(html.substring(realTagStart, tagEnd).contains("title="));
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
    void paletteColorsAppearInCss() {
        String css = new CssBuilder(sample().resolvedTheme()).build();
        assertTrue(css.contains(Palette.STATUS_HEADER_BG));
        assertTrue(css.contains(Palette.PROCEDURE_HEADER_BG));
        assertTrue(css.contains(Palette.BUTTON_BG));
        assertTrue(css.contains(Palette.FLOW_BORDER));
        assertTrue(css.contains(Palette.EDGE_COLOR), "関係線は Palette.EDGE_COLOR で統一されていること");
    }

    @Test
    void actionBordersUsePaletteWidthConstants() {
        String css = new CssBuilder(sample().resolvedTheme()).build();
        assertTrue(css.contains(".a-button{background:" + Palette.BUTTON_BG + ";color:" + Palette.BUTTON_TEXT
                + ";border:" + Palette.BUTTON_BORDER_WIDTH + " solid " + Palette.BUTTON_BORDER));
        assertTrue(css.contains(".a-flow{background:" + Palette.FLOW_BG + ";color:" + Palette.FLOW_TEXT
                + ";border:" + Palette.FLOW_BORDER_WIDTH + " solid " + Palette.FLOW_BORDER));
    }

    @Test
    void allEdgesUseSingleEdgeColorVariable() {
        // 種別ごとの線色クラス（e-button 等）は廃止され、edge クラスは単一
        String css = new CssBuilder(sample().resolvedTheme()).build();
        long edgeColorOccurrences = css.lines().filter(l -> l.contains("--edge-color")).count();
        assertTrue(edgeColorOccurrences >= 1);
        assertFalse(css.contains(".edge.e-"));
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
    void isolatedNodeGetsUnconnectedMarkerButConnectedTerminalDoesNot() {
        // basic-design.md 6.6/v2.6: 完全に孤立したノードだけに data-unconnected と点線マーカーを出す
        FlowSpec spec = new FlowSpec();
        StateSpec a = new StateSpec("A");
        a.actions = new ArrayList<>(List.of(new ActionSpec("button", "next", "B")));
        StateSpec b = new StateSpec("B"); // 終端だが a から参照されている → 未接続ではない
        StateSpec lonely = new StateSpec("浮いてるやつ"); // 誰からも参照されずアクションも無い → 未接続
        spec.states = new ArrayList<>(List.of(a, b, lonely));
        String html = renderCanvas(spec);

        assertTrue(html.contains("data-unconnected=\"true\""));
        assertTrue(html.contains(Palette.UNCONNECTED_BADGE_LABEL));

        String bTag = "class=\"node k-status lbl-" + Html.labelToken("B") + "\" data-label=\"B\"";
        int bStart = html.indexOf(bTag);
        assertTrue(bStart >= 0);
        assertFalse(html.substring(bStart, html.indexOf('>', bStart)).contains("data-unconnected"),
                "矢印で繋がっている通常の終端には付けない");
    }

    @Test
    void noIsolatedNodeMeansNoUnconnectedMarkupOrLegendEntry() {
        String html = renderCanvas(sample());
        assertFalse(html.contains("data-unconnected"));
        assertFalse(html.contains(Palette.UNCONNECTED_BADGE_LABEL));
        assertFalse(html.contains("swatch unconnected"));
    }

    @Test
    void legendShowsFixedFourCategories() {
        String html = renderCanvas(sample());
        assertTrue(html.contains("class=\"legend\""));
        assertTrue(html.contains("swatch status"));
        assertTrue(html.contains("swatch procedure"));
        assertTrue(html.contains("chip button"));
        assertTrue(html.contains("chip flow"));
    }

    @Test
    void fullPageIsSelfContained() {
        FlowSpec spec = sample();
        Theme theme = spec.resolvedTheme();
        Layout layout = new LayoutEngine(theme).build(spec);
        String css = new CssBuilder(theme).build();
        String canvas = new HtmlDiagramRenderer(theme).render(layout);
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
        String fragment = new HtmlPageWriter(theme).fragment(
                new CssBuilder(theme).build(),
                new HtmlDiagramRenderer(theme).render(layout));
        assertFalse(fragment.contains("<!DOCTYPE"));
        assertTrue(fragment.contains("<style>"));
        assertTrue(fragment.contains("fd-canvas"));
    }
}
