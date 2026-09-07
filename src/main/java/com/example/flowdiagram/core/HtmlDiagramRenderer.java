package com.example.flowdiagram.core;

import com.example.flowdiagram.model.ActionSpec;
import com.example.flowdiagram.model.StateSpec;
import com.example.flowdiagram.model.Theme;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 図本体の HTML（div 構造）を組み立てる（basic-design.md 7章）。
 * 画像（SVG / PNG / canvas）は一切使わない。
 */
public class HtmlDiagramRenderer {

    private final Theme theme;

    public HtmlDiagramRenderer(Theme theme) {
        this.theme = theme;
    }

    /** `<div class="fd-canvas">…</div>` を返す。 */
    public String render(Layout layout) {
        StringBuilder sb = new StringBuilder(8192);
        sb.append("<div class=\"fd-canvas\" style=\"width:").append(layout.canvasWidth)
          .append("px;height:").append(layout.canvasHeight).append("px\">\n");

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
     * 複製ボックスにカーソルを合わせたとき、対応する元のボックスも一緒に強調するための
     * 図固有の CSS（basic-design.md 7章・v2.3）。JS は使わず `:has()` の前方兄弟参照で実現する。
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
        for (Layout.Segment s : e.segments) {
            if (s.horizontal) {
                sb.append("    <div class=\"seg h\" style=\"left:").append(s.x)
                  .append("px;top:calc(").append(s.y).append("px - var(--edge-w) / 2);width:calc(")
                  .append(s.width).append("px + var(--edge-w))\"></div>\n");
            } else {
                sb.append("    <div class=\"seg v\" style=\"left:calc(").append(s.x)
                  .append("px - var(--edge-w) / 2);top:").append(s.y).append("px;height:calc(")
                  .append(s.height).append("px + var(--edge-w))\"></div>\n");
            }
        }
        sb.append("    <div class=\"arrow\" style=\"left:calc(").append(e.arrowX)
          .append("px - var(--arrow-size));top:calc(").append(e.arrowY)
          .append("px - var(--arrow-size) * 0.6)\"></div>\n");
        sb.append("  </div>\n");
    }

    private void renderCloneRef(StringBuilder sb, Layout.NodeBox n) {
        sb.append("  <div class=\"node-clone-ref lbl-").append(Html.labelToken(n.state.label))
          .append("\" style=\"left:").append(n.x).append("px;top:").append(n.y)
          .append("px;width:").append(n.width).append("px\"")
          .append(" title=\"").append(Html.esc(n.state.label + " と同じものです（ホバーで元のボックスを強調表示）")).append('"')
          .append(">").append(Html.esc(n.state.label)).append("</div>\n");
    }

    private void renderNode(StringBuilder sb, Layout.NodeBox n) {
        if (n.clone) {
            renderCloneRef(sb, n);
            return;
        }
        StateSpec s = n.state;
        String kindClass = s.isProcedure() ? "k-procedure" : "k-status";
        String badge = s.isProcedure() ? Palette.KIND_BADGE_LABEL_PROCEDURE : Palette.KIND_BADGE_LABEL_STATUS;

        sb.append("  <div class=\"node ").append(kindClass);
        if (n.isolated) {
            sb.append(" is-unconnected");
        }
        sb.append(" lbl-").append(Html.labelToken(s.label))
          .append("\" data-label=\"").append(Html.esc(s.label)).append('"');
        if (n.isolated) {
            sb.append(" data-unconnected=\"true\" title=\"")
              .append(Html.esc("どこからも参照されておらず、遷移先も無いステータス/手続きです"))
              .append('"');
        }
        sb.append(" style=\"left:").append(n.x).append("px;top:").append(n.y)
          .append("px;width:").append(n.width).append("px;height:").append(n.height)
          .append("px\">\n");

        sb.append("    <div class=\"node-header\">")
          .append("<span class=\"kind-badge\">").append(Html.esc(badge)).append("</span>");
        if (n.isolated) {
            sb.append("<span class=\"unconnected-badge\">")
              .append(Html.esc(Palette.UNCONNECTED_BADGE_LABEL)).append("</span>");
        }
        sb.append("<span class=\"state-label\">").append(Html.esc(s.label)).append("</span>")
          .append("</div>\n");

        List<ActionSpec> actions = s.safeActions();
        if (!actions.isEmpty()) {
            sb.append("    <div class=\"node-actions\">\n");
            for (ActionSpec a : actions) {
                String typeClass = a.isFlow() ? "a-flow" : "a-button";
                sb.append("      <div class=\"action ").append(typeClass).append("\">")
                  .append("<span class=\"action-label\">").append(Html.esc(a.label)).append("</span>");
                if (a.next != null && !a.next.isBlank()) {
                    sb.append("<span class=\"chev\">&rsaquo;</span>");
                }
                sb.append("</div>\n");
            }
            sb.append("    </div>\n");
        }
        sb.append("  </div>\n");
    }

    private void renderLegend(StringBuilder sb, Layout layout) {
        if (!Boolean.TRUE.equals(theme.showLegend)) {
            return;
        }
        int top = layout.canvasHeight - theme.canvasPadding - 20;
        sb.append("  <div class=\"legend\" style=\"left:").append(theme.canvasPadding)
          .append("px;top:").append(top).append("px\">\n")
          .append("    <span class=\"item\"><span class=\"swatch status\"></span>")
          .append(Palette.KIND_BADGE_LABEL_STATUS).append("</span>\n")
          .append("    <span class=\"item\"><span class=\"swatch procedure\"></span>")
          .append(Palette.KIND_BADGE_LABEL_PROCEDURE).append("</span>\n")
          .append("    <span class=\"item\"><span class=\"chip button\"></span>ボタン</span>\n")
          .append("    <span class=\"item\"><span class=\"chip flow\"></span>フロー</span>\n")
          .append("    <span class=\"item\"><span class=\"bar\"></span>遷移</span>\n");
        boolean anyIsolated = layout.nodes.stream().anyMatch(n -> n.isolated);
        if (anyIsolated) {
            sb.append("    <span class=\"item\"><span class=\"swatch unconnected\"></span>未接続（点線）</span>\n");
        }
        sb.append("  </div>\n");
    }
}
