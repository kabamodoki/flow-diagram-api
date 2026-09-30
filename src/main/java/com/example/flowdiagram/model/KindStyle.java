package com.example.flowdiagram.model;

/**
 * 大枠（kind）の見た目定義（basic-design.md 3.5章）。
 * 全フィールド任意。プログラム側は kind の意味を一切知らず、ここに書かれた色をそのまま使うだけ。
 */
public class KindStyle {

    public String headerBackground;
    public String background;
    public String border;
    public String textColor;

    public KindStyle() {
    }

    /** 中立フォールバック（basic-design.md 3.8章）。特定の kind の意味には偏らない灰色系。 */
    public static KindStyle defaults() {
        KindStyle s = new KindStyle();
        s.headerBackground = "#eef0f2";
        s.background = "#fbfcfd";
        s.border = "#c7ccd3";
        s.textColor = "#4a5057";
        return s;
    }

    /** this をベースに、override の非 null フィールドだけを反映した新インスタンスを返す。 */
    public KindStyle mergeWith(KindStyle o) {
        KindStyle r = new KindStyle();
        r.headerBackground = pick(o == null ? null : o.headerBackground, headerBackground);
        r.background = pick(o == null ? null : o.background, background);
        r.border = pick(o == null ? null : o.border, border);
        r.textColor = pick(o == null ? null : o.textColor, textColor);
        return r;
    }

    private static String pick(String override, String base) {
        return (override != null && !override.isBlank()) ? override : base;
    }
}
