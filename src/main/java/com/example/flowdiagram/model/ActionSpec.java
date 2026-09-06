package com.example.flowdiagram.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** アクション（basic-design.md 3.3）。 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ActionSpec {

    public static final String TYPE_BUTTON = "button";
    public static final String TYPE_FLOW = "flow";

    /** "button" または "flow"。省略・不明値は button 扱い。 */
    public String type;

    /** アクション名。必須。 */
    public String label;

    /** 遷移先ステータス id。省略時は矢印を描かない。 */
    public String next;

    public ActionSpec() {
    }

    public ActionSpec(String type, String label, String next) {
        this.type = type;
        this.label = label;
        this.next = next;
    }

    @JsonIgnore
    public boolean isFlow() {
        return TYPE_FLOW.equalsIgnoreCase(type);
    }

    public String resolvedType() {
        return isFlow() ? TYPE_FLOW : TYPE_BUTTON;
    }
}
