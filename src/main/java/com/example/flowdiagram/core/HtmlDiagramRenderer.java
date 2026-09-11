package com.example.flowdiagram.core;

import com.example.flowdiagram.model.ActionSpec;
import com.example.flowdiagram.model.ActionTypeStyle;
import com.example.flowdiagram.model.KindStyle;
import com.example.flowdiagram.model.StateSpec;
import com.example.flowdiagram.model.Theme;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 図本体の HTML（div 構造）を組み立てる（basic-design.md 8章）。
 * 画像（SVG / PNG / canvas）は一切使わない。
 * kind/type の色は {@link StyleRegistry} が解決した結果をそのままCSS化するだけで、
 * このクラスは kind/type の意味を一切知らない。
 */
public class HtmlDiagramRenderer {

    private final Theme theme;
    private final Map<String, KindStyle> kindStyles;
    private final Map<String, ActionTypeStyle> typeStyles;

    public HtmlDiagramRenderer(Theme theme, Map<String, KindStyle> kindStyles,
            Map<String, ActionTypeStyle> typeStyles) {
        this.theme = theme;
        this.kindStyles = kindStyles;
        this.typeStyles = typeStyles;
    }

    /** `<div class="fd-canvas">…</div>` を返す。 */
    public String render(Layout layout) {
        StringBuilder sb = new StringBuilder(8192);
        sb.append("<div class=\"fd-canvas\" style=\"width:").append(layout.canvasWidth)
          .append("px;height:").append(layout.canvasHeight).append("px\">\n");

        renderKindTypeCss(sb);

        if (!layout.title.isEmpty()) {
            sb.append("  <h1 class=\"diagram-title\" style=\"left:").append(theme.canvasPadding)
              .append("px;top:").append(theme.canvasPadding).append("px\">")
              .append(Html.esc(layout.title)).append("</h1>\n");
        }

        for (Layout.EdgeRoute e : layout.edges) {
            renderEdge(sb, e);
        }
        for (Layout.NodeBox n : layout.nodes) {
            renderNode(sb, n);
        }
        renderLegend(sb, layout);
        renderCloneHighlightCss(sb, layout);

        sb.append("</div>\n");
        return sb.toString();
    }

    /**
     * このリクエストで実際に使われている kind/type ごとに、色のCSSを出力する（basic-design.md 7章）。
     * JSON の {@code kinds}/{@code types} と既定値をマージ済みの {@link #kindStyles}/{@link #typeStyles}
     * をそのままCSS化する。キー文字列は {@link Html#labelToken} でハッシュ化してクラス名にする。
     */
    private void renderKindTypeCss(StringBuilder sb) {
        sb.append("  <style>\n");
        for (Map.Entry<String, KindStyle> e : kindStyles.entrySet()) {
            String token = Html.labelToken(e.getKey());
            KindStyle s = e.getValue();
            sb.append("    .fd-canvas .node.kind-").append(token).append("{background:")
              .append(Html.cssValue(s.background, "#fbfcfd")).append(";border-color:")
              .append(Html.cssValue(s.border, "#c7ccd3")).append(";}\n");
            sb.append("    .fd-canvas .node.kind-").append(token).append(" .node-header{background:")
              .append(Html.cssValue(s.headerBackground, "#eef0f2")).append(";color:")
              .append(Html.cssValue(s.textColor, "#4a5057")).append(";}\n");
            sb.append("    .fd-canvas .node.kind-").append(token).append(" .kind-badge{color:")
              .append(Html.cssValue(s.textColor, "#4a5057")).append(";}\n");
        }
        for (Map.Entry<String, ActionTypeStyle> e : typeStyles.entrySet()) {
            String token = Html.labelToken(e.getKey());
            ActionTypeStyle s = e.getValue();
            sb.append("    .fd-canvas .action.type-").append(token).append("{background:")
              .append(Html.cssValue(s.background, "#ffffff")).append(";color:")
              .append(Html.cssValue(s.textColor, "#4a5057")).append(";border:")
              .append(Html.px(s.borderWidth == null ? 1.5 : s.borderWidth)).append(" solid ")
              .append(Html.cssValue(s.border, "#c7ccd3")).append(";");
            if (s.shadow != null && !s.shadow.isBlank()) {
                sb.append("box-shadow:").append(Html.cssValue(s.shadow, "none")).append(";");
            }
            sb.append("}\n");
        }
        sb.append("  </style>\n");
    }

    /**
     * 複製ボックスにカーソルを合わせたとき、対応する元のボックスも一緒に強調するための
     * 図固有の CSS（basic-design.md 9章）。JS は使わず `:has()` の前方兄弟参照で実現する。
     * label をそのまま CSS 属性値に埋め込むと壊れうるため、{@link Html#labelToken} で
     * ハッシュ化したトークンをクラス名として使い、実際の label 文字列は CSS に出さない。
     */
    private void renderCloneHighlightCss(StringBuilder sb, Layout layout) {
        Set<String> labelsWithClone = new LinkedHashSet<>();
        for (Layout.NodeBox n : layout.nodes) {
            if (n.clone) {
                labelsWithClone.add(Html.labelToken(n.state.label));
            }
        }
        if (labelsWithClone.isEmpty()) {
            return;
        }
        sb.append("  <style>\n");
        for (String token : labelsWithClone) {
            sb.append("    .fd-canvas .node.lbl-").append(token)
              .append(":has(~ .node-clone-ref.lbl-").append(token)
              .append(":hover){outline-color:var(--edge-color);}\n");
        }
        sb.append("  </style>\n");
    }

    private void renderEdge(StringBuilder sb, Layout.EdgeRoute e) {
        sb.append("  <div class=\"edge\" data-from=\"").append(Html.esc(e.fromStateLabel)).append("\">\n");
        List<Layout.Segment> segs = e.segments;
        for (int i = 0; i < segs.size(); i++) {
            Layout.Segment s = segs.get(i);
            Layout.Segment prev = i > 0 ? segs.get(i - 1) : null;
            Layout.Segment next = i < segs.size() - 1 ? segs.get(i + 1) : null;
            int startX = s.x;
            int startY = s.y;
            int endX = s.horizontal ? s.x + s.width : s.x;
            int endY = s.horizontal ? s.y : s.y + s.height;
            boolean extendStart = touchesEndpoint(startX, startY, prev) || touchesEndpoint(startX, startY, next);
            boolean extendEnd = touchesEndpoint(endX, endY, prev) || touchesEndpoint(endX, endY, next);

            if (s.horizontal) {
                String left = extendStart ? "calc(" + s.x + "px - var(--edge-w) / 2)" : s.x + "px";
                String width = extendWidth(s.width, extendStart, extendEnd);
                sb.append("    <div class=\"seg h\" style=\"left:").append(left)
                  .append(";top:calc(").append(s.y).append("px - var(--edge-w) / 2);width:").append(width)
                  .append("\"></div>\n");
            } else {
                String top = extendStart ? "calc(" + s.y + "px - var(--edge-w) / 2)" : s.y + "px";
                String height = extendWidth(s.height, extendStart, extendEnd);
                sb.append("    <div class=\"seg v\" style=\"left:calc(").append(s.x)
                  .append("px - var(--edge-w) / 2);top:").append(top).append(";height:").append(height)
                  .append("\"></div>\n");
            }
        }
        sb.append("    <div class=\"arrow\" style=\"left:calc(").append(e.arrowX)
          .append("px - var(--arrow-size));top:calc(").append(e.arrowY)
          .append("px - var(--arrow-size) * 0.6)\"></div>\n");
        sb.append("  </div>\n");
    }

    /**
     * (x,y) が隣接セグメント {@code other} のどちらかの端点と一致するか（basic-design.md 9章）。
     * 一致する＝経路内部の継ぎ目なので、その端は `--edge-w` 分延長してよい。
     * 一致しない＝経路全体の起点（発火元ボックス側）または終点（矢印の先端）なので延長しない。
     */
    private static boolean touchesEndpoint(int x, int y, Layout.Segment other) {
        if (other == null) {
            return false;
        }
        int oStartX = other.x;
        int oStartY = other.y;
        int oEndX = other.horizontal ? other.x + other.width : other.x;
        int oEndY = other.horizontal ? other.y : other.y + other.height;
        return (x == oStartX && y == oStartY) || (x == oEndX && y == oEndY);
    }

    private static String extendWidth(int base, boolean extendStart, boolean extendEnd) {
        if (extendStart && extendEnd) {
            return "calc(" + base + "px + var(--edge-w))";
        }
        if (extendStart || extendEnd) {
            return "calc(" + base + "px + var(--edge-w) / 2)";
        }
        return base + "px";
    }

    private void renderCloneRef(StringBuilder sb, Layout.NodeBox n) {
        sb.append("  <div class=\"node-clone-ref lbl-").append(Html.labelToken(n.state.label))
          .append("\" style=\"left:").append(n.x).append("px;top:").append(n.y)
          .append("px;width:").append(n.width).append("px\">")
          .append(Html.esc(n.state.label)).append("</div>\n");
    }

    private void renderNode(StringBuilder sb, Layout.NodeBox n) {
        if (n.clone) {
            renderCloneRef(sb, n);
            return;
        }
        StateSpec s = n.state;
        String kindClass = "kind-" + Html.labelToken(s.effectiveKind());
        String badge = s.effectiveKind();

        sb.append("  <div class=\"node ").append(kindClass)
          .append(" lbl-").append(Html.labelToken(s.label))
          .append("\" data-label=\"").append(Html.esc(s.label)).append('"');
        sb.append(" style=\"left:").append(n.x).append("px;top:").append(n.y)
          .append("px;width:").append(n.width).append("px;height:").append(n.height)
          .append("px\">\n");

        sb.append("    <div class=\"node-header\">")
          .append("<span class=\"kind-badge\">").append(Html.esc(badge)).append("</span>")
          .append("<span class=\"state-label\">").append(Html.esc(s.label)).append("</span>")
          .append("</div>\n");

        List<ActionSpec> actions = s.safeActions();
        if (!actions.isEmpty()) {
            sb.append("    <div class=\"node-actions\">\n");
            for (int j = 0; j < actions.size(); j++) {
                ActionSpec a = actions.get(j);
                String typeClass = "type-" + Html.labelToken(a.effectiveType());
                sb.append("      <div class=\"action ").append(typeClass)
                  .append("\" style=\"min-height:").append(n.actionRowHeights.get(j)).append("px\">")
                  .append("<span class=\"action-label\">").append(Html.esc(a.label)).append("</span>");
                if (!a.effectiveNextTargets().isEmpty()) {
                    sb.append("<span class=\"chev\">&rsaquo;</span>");
                }
                sb.append("</div>\n");
            }
            sb.append("    </div>\n");
        }
        sb.append("  </div>\n");
    }

    /** 実際に使われている kind/type を動的に列挙する（basic-design.md 8章）。 */
    private void renderLegend(StringBuilder sb, Layout layout) {
        if (!Boolean.TRUE.equals(theme.showLegend)) {
            return;
        }
        int top = layout.canvasHeight - theme.canvasPadding - 20;
        sb.append("  <div class=\"legend\" style=\"left:").append(theme.canvasPadding)
          .append("px;top:").append(top).append("px\">\n");

        for (Map.Entry<String, KindStyle> e : kindStyles.entrySet()) {
            KindStyle s = e.getValue();
            sb.append("    <span class=\"item\"><span class=\"swatch\" style=\"background:")
              .append(Html.cssValue(s.headerBackground, "#eef0f2")).append("\"></span>")
              .append(Html.esc(e.getKey())).append("</span>\n");
        }
        for (Map.Entry<String, ActionTypeStyle> e : typeStyles.entrySet()) {
            ActionTypeStyle s = e.getValue();
            sb.append("    <span class=\"item\"><span class=\"chip\" style=\"background:")
              .append(Html.cssValue(s.background, "#ffffff")).append(";border-color:")
              .append(Html.cssValue(s.border, "#c7ccd3")).append("\"></span>")
              .append(Html.esc(e.getKey())).append("</span>\n");
        }
        sb.append("    <span class=\"item\"><span class=\"bar\"></span>遷移</span>\n");
        sb.append("  </div>\n");
    }
}
