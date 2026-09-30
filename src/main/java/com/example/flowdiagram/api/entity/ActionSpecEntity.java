package com.example.flowdiagram.api.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * API入出力用のアクション表現。JSON schema は basic-design.md 3.3 のまま変更しない。
 * ここはJSONの受け渡しのためだけのデータ保持クラスで、意味付け（既定値解決など）は
 * 一切行わない（それは model 側の責務）。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ActionSpecEntity {

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
