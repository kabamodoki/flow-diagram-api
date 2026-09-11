package com.example.flowdiagram.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * アクション（basic-design.md 3.3）。
 * {@code type} は固定enumではなく任意の文字列（basic-design.md v3.0）。
 * 見た目は JSON ルートの {@code types} 定義で決まり、このクラスは意味を持たない。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ActionSpec {

    /** 未指定時に使う内部キー（basic-design.md 3.7）。 */
    public static final String DEFAULT_TYPE = "default";

    /** 任意の文字列。JSON ルートの {@code types} のキーと対応させる。省略可。 */
    public String type;

    /** アクション名。必須。 */
    public String label;

    /**
     * 遷移先の label 一覧（basic-design.md 3.3, v3.1）。
     * JSON では単一の文字列でも配列でも受け付ける（単一値は自動的に1件の配列になる）。
     * 1つのアクションから複数の矢印を出したい場合は配列で複数指定する。
     */
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    public List<String> next;

    public ActionSpec() {
    }

    /** 単一遷移先向けの簡易コンストラクタ。next が null/空なら遷移無しとして扱う。 */
    public ActionSpec(String type, String label, String next) {
        this.type = type;
        this.label = label;
        this.next = (next == null || next.isBlank()) ? null : List.of(next);
    }

    /** 複数遷移先向けのコンストラクタ（v3.1）。 */
    public ActionSpec(String type, String label, List<String> next) {
        this.type = type;
        this.label = label;
        this.next = next;
    }

    /** type が省略されている場合の解決済みキー。 */
    public String effectiveType() {
        return (type == null || type.isBlank()) ? DEFAULT_TYPE : type;
    }

    /** next のうち空でないものだけを返す（null/空文字を除外）。次が無ければ空リスト。 */
    public List<String> effectiveNextTargets() {
        if (next == null) {
            return List.of();
        }
        List<String> out = new ArrayList<>(next.size());
        for (String n : next) {
            if (n != null && !n.isBlank()) {
                out.add(n);
            }
        }
        return out;
    }
}
