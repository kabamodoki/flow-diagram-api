package com.example.flowdiagram.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * ステータス／手続き＝大枠（basic-design.md 3.2）。
 * {@code id} は持たない。{@link #label} が識別子を兼ねる（states[] 内で一意）。
 * {@code kind} は固定enumではなく任意の文字列（basic-design.md v3.0）。
 * 見た目は JSON ルートの {@code kinds} 定義で決まり、このクラスは意味を持たない。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class StateSpec {

    /** 未指定時に使う内部キー（basic-design.md 3.7）。 */
    public static final String DEFAULT_KIND = "default";

    /** 表示名。必須。states[] 内で一意である必要があり、識別子としても使われる。 */
    public String label;

    /** 任意の文字列。JSON ルートの {@code kinds} のキーと対応させる。省略可。 */
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

    /** kind が省略されている場合の解決済みキー。 */
    public String effectiveKind() {
        return (kind == null || kind.isBlank()) ? DEFAULT_KIND : kind;
    }
}
