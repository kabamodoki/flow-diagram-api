package com.example.flowdiagram.api.controller;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

/**
 * コントローラがリクエストを受け取るための入り口 entity（basic-design.md 3章）。JSON schema は
 * 変更しない。model 層の内部処理用 entity（{@code model.service.DiagramService} のネストクラス）
 * とは別クラスで、意味付け（既定値解決など）は一切行わないデータ保持だけの役割。相互変換は
 * {@link com.example.flowdiagram.api.service.DiagramApiService} が行う。
 *
 * <p>コントローラが受け取る entity 一式なので、このクラスの下にネストクラスとして1ファイルに
 * まとめている。</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FlowSpecEntity {

    public String title;
    public ThemeEntity theme;
    public Map<String, KindStyleEntity> kinds;
    public Map<String, ActionTypeStyleEntity> types;
    public List<StateSpecEntity> states;

    /** ステータス/手続き表現（basic-design.md 3.2）。 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StateSpecEntity {

        public String label;
        public String kind;
        public List<ActionSpecEntity> actions;

        public StateSpecEntity() {
        }

        public StateSpecEntity(String label) {
            this.label = label;
        }
    }

    /** アクション表現（basic-design.md 3.3）。 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ActionSpecEntity {

        public String type;
        public String label;

        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        public List<String> next;

        public ActionSpecEntity() {
        }

        public ActionSpecEntity(String type, String label, List<String> next) {
            this.type = type;
            this.label = label;
            this.next = next;
        }
    }

    /** kind の見た目定義（basic-design.md 3.5）。 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class KindStyleEntity {
        public String headerBackground;
        public String background;
        public String border;
        public String textColor;
    }

    /** type の見た目定義（basic-design.md 3.6）。 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ActionTypeStyleEntity {
        public String background;
        public String border;
        public Double borderWidth;
        public String textColor;
        public String shadow;
    }

    /**
     * テーマ上書き表現（basic-design.md 4章）。全フィールド任意。
     * 既定値・マージロジックは持たない（それは model.service.DiagramService.Theme の責務）。
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ThemeEntity {

        public Integer nodeWidth;
        public Integer headerHeight;
        public Integer actionRowHeight;
        public Integer actionGap;
        public Integer nodePaddingTop;
        public Integer nodePaddingBottom;
        public Integer columnGap;
        public Integer rowGap;
        public Integer canvasPadding;

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
    }
}
