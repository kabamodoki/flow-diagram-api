package com.example.flowdiagram.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * ステータス／手続き＝大枠（basic-design.md 3.2）。
 * v2.5 で {@code id} を廃止。{@link #label} が識別子を兼ねる（states[] 内で一意）。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class StateSpec {

    public static final String KIND_STATUS = "status";
    public static final String KIND_PROCEDURE = "procedure";

    /** 表示名。必須。states[] 内で一意である必要があり、識別子としても使われる。 */
    public String label;

    /** "status" または "procedure"。省略・不明値は status 扱い。 */
    public String kind;

    /** アクション。省略・空なら終端。 */
    public List<ActionSpec> actions = new ArrayList<>();

    public StateSpec() {
    }

    public StateSpec(String label) {
        this.label = label;
    }

    public List<ActionSpec> safeActions() {
        return actions == null ? List.of() : actions;
    }

    @JsonIgnore
    public boolean isProcedure() {
        return KIND_PROCEDURE.equalsIgnoreCase(kind);
    }

    public String resolvedKind() {
        return isProcedure() ? KIND_PROCEDURE : KIND_STATUS;
    }
}
