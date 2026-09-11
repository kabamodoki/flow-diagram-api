package com.example.flowdiagram.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 入力 JSON のルート（basic-design.md 3.1）。 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FlowSpec {

    public String title;

    /** 見た目の部分上書き。null 可。 */
    public Theme theme;

    /** kind ごとの見た目定義。null 可（basic-design.md 3.5）。 */
    public Map<String, KindStyle> kinds;

    /** type ごとの見た目定義。null 可（basic-design.md 3.6）。 */
    public Map<String, ActionTypeStyle> types;

    public List<StateSpec> states = new ArrayList<>();

    public String displayTitle() {
        return (title == null || title.isBlank()) ? "" : title;
    }

    /** 既定値とマージ済みのテーマを返す。 */
    public Theme resolvedTheme() {
        return Theme.defaults().mergeWith(theme);
    }
}
