package com.example.flowdiagram.core;

import com.example.flowdiagram.model.Theme;

/**
 * テーマから CSS を組み立てる（basic-design.md 4章・6.6章・8章）。
 * レイアウト・タイポグラフィは Theme（CSS カスタムプロパティ）から、
 * 色は {@link Palette} のソース定数から直接埋め込む（ユーザー指示: 色は設定値ではなくソース定数）。
 */
public class CssBuilder {

    private final Theme theme;

    public CssBuilder(Theme theme) {
        this.theme = theme;
    }

    public String build() {
        StringBuilder sb = new StringBuilder(4096);
        rootVariables(sb);
        structure(sb);
        return sb.toString();
    }

    private void rootVariables(StringBuilder sb) {
        sb.append(".fd-root{")
          .append("--node-width:").append(theme.nodeWidth).append("px;")
          .append("--header-height:").append(theme.headerHeight).append("px;")
          .append("--action-height:").append(theme.actionRowHeight).append("px;")
          .append("--action-gap:").append(theme.actionGap).append("px;")
          .append("--node-padding-top:").append(theme.nodePaddingTop).append("px;")
          .append("--node-padding-bottom:").append(theme.nodePaddingBottom).append("px;")
          .append("--font-family:").append(Html.cssValue(theme.fontFamily, "sans-serif")).append(';')
          .append("--title-font-size:").append(theme.titleFontSize).append("px;")
          .append("--badge-font-size:").append(theme.badgeFontSize).append("px;")
          .append("--state-font-size:").append(theme.stateFontSize).append("px;")
          .append("--action-font-size:").append(theme.actionFontSize).append("px;")
          .append("--bg:").append(Html.cssValue(theme.background, "#f7f8fa")).append(';')
          .append("--title-color:").append(Html.cssValue(theme.titleColor, "#1f2933")).append(';')
          .append("--node-shadow:").append(Html.cssValue(theme.nodeShadow, "none")).append(';')
          .append("--edge-w:").append(theme.edgeWidth).append("px;")
          .append("--arrow-size:").append(theme.arrowSize).append("px;")
          .append("--edge-color:").append(Palette.EDGE_COLOR).append(';')
          .append("}\n");
    }

    private void structure(StringBuilder sb) {
        sb.append("""
                .fd-root{font-family:var(--font-family);background:var(--bg);color:#2d3748;}
                .fd-root *{box-sizing:border-box;}
                .fd-canvas{position:relative;transform-origin:0 0;background:var(--bg);}
                .fd-canvas .diagram-title{position:absolute;left:0;top:0;margin:0;\
                font-size:var(--title-font-size);font-weight:700;color:var(--title-color);\
                letter-spacing:.02em;}

                /* --- 大枠（ステータス／手続き） --- */
                .fd-canvas .node{position:absolute;z-index:2;border-radius:10px;\
                box-shadow:var(--node-shadow);overflow:hidden;border:1.5px solid transparent;\
                transition:outline-color .15s;outline:3px solid transparent;outline-offset:3px;}
                /* --- 複製参照テキスト（後退辺・自己ループの終端） --- */
                .fd-canvas .node-clone-ref{position:absolute;z-index:2;display:flex;\
                align-items:center;gap:4px;font-size:var(--state-font-size);color:#6b7280;\
                font-style:italic;cursor:default;white-space:nowrap;overflow:hidden;\
                text-overflow:ellipsis;padding:0 6px;}
                .fd-canvas .node-clone-ref::before{content:"↩";font-style:normal;flex-shrink:0;}
                .fd-canvas .node-clone-ref:hover{color:var(--edge-color);}
                .fd-canvas .node-header{min-height:var(--header-height);display:flex;\
                flex-direction:column;justify-content:center;gap:2px;padding:6px 12px;}
                .fd-canvas .kind-badge,.fd-canvas .unconnected-badge{align-self:flex-start;\
                font-size:var(--badge-font-size);font-weight:700;letter-spacing:.03em;\
                padding:1px 7px;border-radius:999px;background:rgba(255,255,255,.55);}
                .fd-canvas .state-label{font-size:var(--state-font-size);font-weight:700;\
                white-space:nowrap;overflow:hidden;text-overflow:ellipsis;}

                /* --- ステータス（薄い青） --- */
                .fd-canvas .node.k-status{background:%STATUS_BODY%;border-color:%STATUS_BORDER%;}
                .fd-canvas .node.k-status .node-header{background:%STATUS_HEADER_BG%;color:%STATUS_HEADER_TEXT%;}
                .fd-canvas .node.k-status .kind-badge{color:%STATUS_HEADER_TEXT%;}

                /* --- 手続き（薄い緑） --- */
                .fd-canvas .node.k-procedure{background:%PROC_BODY%;border-color:%PROC_BORDER%;}
                .fd-canvas .node.k-procedure .node-header{background:%PROC_HEADER_BG%;color:%PROC_HEADER_TEXT%;}
                .fd-canvas .node.k-procedure .kind-badge{color:%PROC_HEADER_TEXT%;}

                /* --- 未接続ノード（どこからも参照されず、遷移先も無い） --- */
                .fd-canvas .node.is-unconnected{border-style:dashed;border-color:%UNCONNECTED_BORDER%;}
                .fd-canvas .unconnected-badge{background:%UNCONNECTED_BADGE_BG%;color:%UNCONNECTED_BADGE_TEXT%;}

                /* --- アクション（ボタン／フロー） --- */
                .fd-canvas .node-actions{display:flex;flex-direction:column;gap:var(--action-gap);\
                padding:var(--node-padding-top) 10px var(--node-padding-bottom);}
                .fd-canvas .action{height:var(--action-height);min-height:var(--action-height);\
                display:flex;align-items:center;justify-content:space-between;gap:8px;\
                padding:0 12px;border-radius:8px;font-size:var(--action-font-size);}
                .fd-canvas .action .action-label{flex:1 1 auto;white-space:nowrap;overflow:hidden;\
                text-overflow:ellipsis;}
                .fd-canvas .action .chev{flex:0 0 auto;font-weight:700;opacity:.7;}

                .fd-canvas .action.a-button{background:%BUTTON_BG%;color:%BUTTON_TEXT%;\
                border:%BUTTON_BORDER_W% solid %BUTTON_BORDER%;box-shadow:%BUTTON_SHADOW%;}
                .fd-canvas .action.a-flow{background:%FLOW_BG%;color:%FLOW_TEXT%;\
                border:%FLOW_BORDER_W% solid %FLOW_BORDER%;}

                /* --- 関係線（すべて薄い青の実線で統一） --- */
                .fd-canvas .edge{position:absolute;left:0;top:0;z-index:1;}
                .fd-canvas .seg{position:absolute;}
                .fd-canvas .seg.h{border-top:var(--edge-w) solid var(--edge-color);}
                .fd-canvas .seg.v{border-left:var(--edge-w) solid var(--edge-color);}
                .fd-canvas .arrow{position:absolute;width:0;height:0;\
                border-left:var(--arrow-size) solid var(--edge-color);\
                border-top:calc(var(--arrow-size) * .6) solid transparent;\
                border-bottom:calc(var(--arrow-size) * .6) solid transparent;}

                /* --- 凡例 --- */
                .fd-canvas .legend{position:absolute;display:flex;flex-wrap:wrap;gap:18px;\
                align-items:center;font-size:var(--action-font-size);color:#4a5568;}
                .fd-canvas .legend .item{display:flex;align-items:center;gap:6px;}
                .fd-canvas .legend .swatch{display:inline-block;width:14px;height:14px;\
                border-radius:4px;border:1px solid rgba(0,0,0,.12);}
                .fd-canvas .legend .swatch.status{background:%STATUS_HEADER_BG%;}
                .fd-canvas .legend .swatch.procedure{background:%PROC_HEADER_BG%;}
                .fd-canvas .legend .swatch.unconnected{background:#fff;\
                border-style:dashed;border-color:%UNCONNECTED_BORDER%;}
                .fd-canvas .legend .chip{display:inline-block;width:26px;height:14px;border-radius:4px;}
                .fd-canvas .legend .chip.button{background:%BUTTON_BG%;border:%BUTTON_BORDER_W% solid %BUTTON_BORDER%;}
                .fd-canvas .legend .chip.flow{background:%FLOW_BG%;border:%FLOW_BORDER_W% solid %FLOW_BORDER%;}
                .fd-canvas .legend .bar{display:inline-block;width:26px;\
                border-top:var(--edge-w) solid var(--edge-color);}
                """
                .replace("%STATUS_BODY%", Palette.STATUS_BODY_BG)
                .replace("%STATUS_BORDER%", Palette.STATUS_BORDER)
                .replace("%STATUS_HEADER_BG%", Palette.STATUS_HEADER_BG)
                .replace("%STATUS_HEADER_TEXT%", Palette.STATUS_HEADER_TEXT)
                .replace("%PROC_BODY%", Palette.PROCEDURE_BODY_BG)
                .replace("%PROC_BORDER%", Palette.PROCEDURE_BORDER)
                .replace("%PROC_HEADER_BG%", Palette.PROCEDURE_HEADER_BG)
                .replace("%PROC_HEADER_TEXT%", Palette.PROCEDURE_HEADER_TEXT)
                .replace("%BUTTON_BG%", Palette.BUTTON_BG)
                .replace("%BUTTON_TEXT%", Palette.BUTTON_TEXT)
                .replace("%BUTTON_BORDER%", Palette.BUTTON_BORDER)
                .replace("%BUTTON_SHADOW%", Palette.BUTTON_SHADOW)
                .replace("%FLOW_BG%", Palette.FLOW_BG)
                .replace("%FLOW_TEXT%", Palette.FLOW_TEXT)
                .replace("%FLOW_BORDER%", Palette.FLOW_BORDER)
                .replace("%BUTTON_BORDER_W%", Palette.BUTTON_BORDER_WIDTH)
                .replace("%FLOW_BORDER_W%", Palette.FLOW_BORDER_WIDTH)
                .replace("%UNCONNECTED_BORDER%", Palette.UNCONNECTED_BORDER)
                .replace("%UNCONNECTED_BADGE_BG%", Palette.UNCONNECTED_BADGE_BG)
                .replace("%UNCONNECTED_BADGE_TEXT%", Palette.UNCONNECTED_BADGE_TEXT));
    }
}
