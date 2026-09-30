package com.example.flowdiagram.api.controller;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** API入出力用の kind 見た目定義。JSON schema は basic-design.md 3.5 のまま変更しない。 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class KindStyleEntity {

    public String headerBackground;
    public String background;
    public String border;
    public String textColor;
}
