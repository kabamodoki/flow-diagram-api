package com.example.flowdiagram.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * レイアウト・タイポグラフィの設定（basic-design.md 4章）。
 * 色はここに含まれない。配色は {@link com.example.flowdiagram.core.Palette} の
 * ソースコード上の定数が唯一の情報源であり、JSON からは変更できない（ユーザー指示）。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Theme {

    // --- 4.1 レイアウト系 ---
    public Integer nodeWidth;
    public Integer headerHeight;
    public Integer actionRowHeight;
    public Integer actionGap;
    public Integer nodePaddingTop;
    public Integer nodePaddingBottom;
    public Integer columnGap;
    public Integer rowGap;
    public Integer canvasPadding;

    // --- 4.2 表示系 ---
    public String fontFamily;
    public Integer titleFontSize;
    public Integer badgeFontSize;
    public Integer stateFontSize;
    public Integer actionFontSize;
    public String background;
    public String titleColor;
    public String nodeShadow;
    public Integer edgeWidth;
    public Integer arrowSize;
    public Boolean showLegend;

    public static Theme defaults() {
        Theme t = new Theme();
        t.nodeWidth = 240;
        t.headerHeight = 50;
        t.actionRowHeight = 34;
        t.actionGap = 10;
        t.nodePaddingTop = 14;
        t.nodePaddingBottom = 20;
        t.columnGap = 150;
        t.rowGap = 40;
        t.canvasPadding = 40;

        t.fontFamily = "\"Meiryo UI\",\"Yu Gothic UI\",\"Segoe UI\",Meiryo,\"Hiragino Kaku Gothic ProN\",sans-serif";
        t.titleFontSize = 20;
        t.badgeFontSize = 10;
        t.stateFontSize = 15;
        t.actionFontSize = 13;
        t.background = "#f7f8fa";
        t.titleColor = "#1f2933";
        t.nodeShadow = "0 2px 6px rgba(0,0,0,.10)";
        t.edgeWidth = 3;
        t.arrowSize = 9;
        t.showLegend = Boolean.TRUE;
        return t;
    }

    /** this（＝既定値）をベースに、override の非 null フィールドだけを反映した新インスタンスを返す。 */
    public Theme mergeWith(Theme o) {
        Theme r = new Theme();
        r.nodeWidth = pick(o == null ? null : o.nodeWidth, nodeWidth);
        r.headerHeight = pick(o == null ? null : o.headerHeight, headerHeight);
        r.actionRowHeight = pick(o == null ? null : o.actionRowHeight, actionRowHeight);
        r.actionGap = pick(o == null ? null : o.actionGap, actionGap);
        r.nodePaddingTop = pick(o == null ? null : o.nodePaddingTop, nodePaddingTop);
        r.nodePaddingBottom = pick(o == null ? null : o.nodePaddingBottom, nodePaddingBottom);
        r.columnGap = pick(o == null ? null : o.columnGap, columnGap);
        r.rowGap = pick(o == null ? null : o.rowGap, rowGap);
        r.canvasPadding = pick(o == null ? null : o.canvasPadding, canvasPadding);

        r.fontFamily = pickStr(o == null ? null : o.fontFamily, fontFamily);
        r.titleFontSize = pick(o == null ? null : o.titleFontSize, titleFontSize);
        r.badgeFontSize = pick(o == null ? null : o.badgeFontSize, badgeFontSize);
        r.stateFontSize = pick(o == null ? null : o.stateFontSize, stateFontSize);
        r.actionFontSize = pick(o == null ? null : o.actionFontSize, actionFontSize);
        r.background = pickStr(o == null ? null : o.background, background);
        r.titleColor = pickStr(o == null ? null : o.titleColor, titleColor);
        r.nodeShadow = pickStr(o == null ? null : o.nodeShadow, nodeShadow);
        r.edgeWidth = pick(o == null ? null : o.edgeWidth, edgeWidth);
        r.arrowSize = pick(o == null ? null : o.arrowSize, arrowSize);
        r.showLegend = o != null && o.showLegend != null ? o.showLegend : showLegend;
        return r;
    }

    private static Integer pick(Integer override, Integer base) {
        return override != null ? override : base;
    }

    private static String pickStr(String override, String base) {
        return (override != null && !override.isBlank()) ? override : base;
    }
}
