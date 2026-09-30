package com.example.flowdiagram.api.controller;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** API入出力用のステータス/手続き表現。JSON schema は basic-design.md 3.2 のまま変更しない。 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class StateSpecEntity {

    public String label;
    public String kind;
    public List<ActionSpecEntity> actions;

    public StateSpecEntity() {
    }

    public StateSpecEntity(String label) {
        this.label = label;
    }
}
