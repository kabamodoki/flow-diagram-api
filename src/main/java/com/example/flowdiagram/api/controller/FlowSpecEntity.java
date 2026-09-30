package com.example.flowdiagram.api.controller;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

/**
 * API入出力のルート entity（basic-design.md 3.1）。JSON schema は変更しない。
 * コントローラの入り口として使う entity であり、model.FlowSpec（内部処理用の entity）とは
 * 別のクラスとして持つ。相互変換は {@link com.example.flowdiagram.api.DiagramApiService} が行う。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FlowSpecEntity {

    public String title;
    public ThemeEntity theme;
    public Map<String, KindStyleEntity> kinds;
    public Map<String, ActionTypeStyleEntity> types;
    public List<StateSpecEntity> states;
}
