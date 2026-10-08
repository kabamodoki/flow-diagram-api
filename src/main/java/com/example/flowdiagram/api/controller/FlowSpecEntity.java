package com.example.flowdiagram.api.controller;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

/** リクエストentity。 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FlowSpecEntity {

    public String title;
    public ThemeEntity theme;
    public Map<String, KindStyleEntity> kinds;
    public Map<String, ActionTypeStyleEntity> types;
    public List<StateSpecEntity> states;

    /** ステータスentity。 */
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

    /** アクションentity。 */
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

    /** kind見た目entity。 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class KindStyleEntity {
        public String headerBackground;
        public String background;
        public String border;
        public String textColor;
    }

    /** type見た目entity。 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ActionTypeStyleEntity {
        public String background;
        public String border;
        public Double borderWidth;
        public String textColor;
        public String shadow;
    }

    /** テーマentity。 */
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
