package com.example.flowdiagram.api.controller;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

/**
 * リクエストのJSONをそのまま受け取るための入れ物（entity）のクラス。状態遷移図1件分の定義
 * （タイトル・見た目の設定・各ステータスの一覧）をまるごと表す。
 *
 * <p>ここに並んでいるクラス群はJSONの形をそのまま写しただけのデータの入れ物で、中身の意味を
 * 判断したり処理したりすることは一切しない（それはmodel側の役割）。JSONの入れ子構造に合わせて、
 * 関連するクラスをすべてこのファイルの中にネストクラスとしてまとめている。</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FlowSpecEntity {

    public String title;
    public ThemeEntity theme;
    public Map<String, KindStyleEntity> kinds;
    public Map<String, ActionTypeStyleEntity> types;
    public List<StateSpecEntity> states;

    /** 1つのステータス（または手続き）の定義。中に複数のアクションを持てる。 */
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

    /** ステータスの中の1つのボタン（アクション）の定義。押した後どこへ遷移するかを持つ。 */
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

    /** ステータスの種類（kind）ごとの色・見た目の指定。 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class KindStyleEntity {
        public String headerBackground;
        public String background;
        public String border;
        public String textColor;
    }

    /** アクションの種類（type）ごとの色・見た目の指定。 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ActionTypeStyleEntity {
        public String background;
        public String border;
        public Double borderWidth;
        public String textColor;
        public String shadow;
    }

    /**
     * レイアウトや文字サイズなどの見た目設定の上書き指定。全項目が任意で、指定しなければ
     * 既定値が使われる（この既定値の決定処理自体はここでは行わない）。
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
