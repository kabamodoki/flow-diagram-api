package com.example.flowdiagram.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * アクション（type）の見た目定義（basic-design.md 3.6章）。
 * 全フィールド任意。JSON の {@code types} オブジェクトの値として渡される。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ActionTypeStyle {

    public String background;
    public String border;
    public Double borderWidth;
    public String textColor;
    public String shadow;

    public ActionTypeStyle() {
    }

    /** 中立フォールバック（basic-design.md 3.8章）。 */
    public static ActionTypeStyle defaults() {
        ActionTypeStyle s = new ActionTypeStyle();
        s.background = "#ffffff";
        s.border = "#c7ccd3";
        s.borderWidth = 1.5;
        s.textColor = "#4a5057";
        s.shadow = null;
        return s;
    }

    /** this をベースに、override の非 null フィールドだけを反映した新インスタンスを返す。 */
    public ActionTypeStyle mergeWith(ActionTypeStyle o) {
        ActionTypeStyle r = new ActionTypeStyle();
        r.background = pick(o == null ? null : o.background, background);
        r.border = pick(o == null ? null : o.border, border);
        r.borderWidth = o != null && o.borderWidth != null ? o.borderWidth : borderWidth;
        r.textColor = pick(o == null ? null : o.textColor, textColor);
        r.shadow = o != null && o.shadow != null ? o.shadow : shadow;
        return r;
    }

    private static String pick(String override, String base) {
        return (override != null && !override.isBlank()) ? override : base;
    }
}
