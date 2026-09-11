package com.example.flowdiagram.core;

import com.example.flowdiagram.model.Theme;

/**
 * テーマから CSS を組み立てる（basic-design.md 4章・8章）。
 * レイアウト・タイポグラフィは Theme（CSS カスタムプロパティ）から、
 * kind/type に紐付かない色（関係線）は {@link Palette} のソース定数から直接埋め込む。
 * **kind/type ごとの色はここでは出さない**（リクエストごとに可変のため、
 * {@link HtmlDiagramRenderer} が {@link StyleRegistry} の解決結果から動的に出力する。basic-design.md 7章）。
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

                /* --- 大枠。色は kind ごとに動的CSS（HtmlDiagramRenderer）で決まる --- */
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
                .fd-canvas .kind-badge{align-self:flex-start;\
                font-size:var(--badge-font-size);font-weight:700;letter-spacing:.03em;\
                padding:1px 7px;border-radius:999px;background:rgba(255,255,255,.55);}
                .fd-canvas .state-label{font-size:var(--state-font-size);font-weight:700;\
                white-space:nowrap;overflow:hidden;text-overflow:ellipsis;}

                /* --- アクション。色は type ごとに動的CSS（HtmlDiagramRenderer）で決まる --- */
                .fd-canvas .node-actions{display:flex;flex-direction:column;gap:var(--action-gap);\
                padding:var(--node-padding-top) 10px var(--node-padding-bottom);}
                .fd-canvas .action{min-height:var(--action-height);\
                display:flex;align-items:center;justify-content:space-between;gap:8px;\
                padding:4px 12px;border-radius:8px;font-size:var(--action-font-size);}
                .fd-canvas .action .action-label{flex:1 1 auto;white-space:normal;\
                overflow-wrap:anywhere;line-height:1.35;}
                .fd-canvas .action .chev{flex:0 0 auto;font-weight:700;opacity:.7;}

                /* --- 関係線（すべて薄い青の実線で統一） --- */
                .fd-canvas .edge{position:absolute;left:0;top:0;z-index:1;}
                .fd-canvas .seg{position:absolute;}
                .fd-canvas .seg.h{border-top:var(--edge-w) solid var(--edge-color);}
                .fd-canvas .seg.v{border-left:var(--edge-w) solid var(--edge-color);}
                .fd-canvas .arrow{position:absolute;width:0;height:0;\
                border-left:var(--arrow-size) solid var(--edge-color);\
                border-top:calc(var(--arrow-size) * .6) solid transparent;\
                border-bottom:calc(var(--arrow-size) * .6) solid transparent;}

                /* --- 凡例。kind/typeの色は各項目のinline styleで指定する --- */
                .fd-canvas .legend{position:absolute;display:flex;flex-wrap:wrap;gap:18px;\
                align-items:center;font-size:var(--action-font-size);color:#4a5568;}
                .fd-canvas .legend .item{display:flex;align-items:center;gap:6px;}
                .fd-canvas .legend .swatch{display:inline-block;width:14px;height:14px;\
                border-radius:4px;border:1px solid rgba(0,0,0,.12);}
                .fd-canvas .legend .chip{display:inline-block;width:26px;height:14px;border-radius:4px;\
                border:1.5px solid rgba(0,0,0,.12);}
                .fd-canvas .legend .bar{display:inline-block;width:26px;\
                border-top:var(--edge-w) solid var(--edge-color);}
                """);
    }
}
