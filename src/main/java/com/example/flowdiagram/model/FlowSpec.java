package com.example.flowdiagram.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 図の内部表現のルート（basic-design.md 3.1）。
 * JSONの入出力形式は {@link com.example.flowdiagram.api.entity.FlowSpecEntity} が持ち、
 * このクラスは model 層の処理（検証・レイアウト・描画）だけに使う。
 */
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
