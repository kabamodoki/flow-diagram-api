package com.example.flowdiagram.api.controller;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

/** リクエストentity。 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FlowDiagramGeneratorRequest {

    @NotNull
    private String title;
    private ThemeEntity theme;
    @NotNull
    private Map<String, KindStyleEntity> kinds;
    @NotNull
    private Map<String, ActionTypeStyleEntity> types;
    @NotNull
    private List<StateSpecEntity> states;

    /** ステータスentity。 */
    @Data
    public static class StateSpecEntity {

        private String label;
        private String kind;
        private List<ActionSpecEntity> actions;

        public StateSpecEntity() {
        }

        public StateSpecEntity(String label) {
            this.label = label;
        }
    }

    /** アクションentity。 */
    @Data
    public static class ActionSpecEntity {

        private String type;
        private String label;

        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        private List<String> next;

        public ActionSpecEntity() {
        }

        public ActionSpecEntity(String type, String label, List<String> next) {
            this.type = type;
            this.label = label;
            this.next = next;
        }
    }

    /** kind見た目entity。 */
    @Data
    public static class KindStyleEntity {
        private String headerBackground;
        private String background;
        private String border;
        private String textColor;
    }

    /** type見た目entity。 */
    @Data
    public static class ActionTypeStyleEntity {
        private String background;
        private String border;
        private Double borderWidth;
        private String textColor;
        private String shadow;
    }

    /** テーマentity。 */
    @Data
    public static class ThemeEntity {

        private Integer nodeWidth;
        private Integer headerHeight;
        private Integer actionRowHeight;
        private Integer actionGap;
        private Integer nodePaddingTop;
        private Integer nodePaddingBottom;
        private Integer columnGap;
        private Integer rowGap;
        private Integer canvasPadding;

        private String fontFamily;
        private Integer titleFontSize;
        private Integer badgeFontSize;
        private Integer stateFontSize;
        private Integer actionFontSize;
        private String background;
        private String titleColor;
        private String nodeShadow;
        private Integer edgeWidth;
        private Integer arrowSize;
        private Boolean showLegend;
    }
}
