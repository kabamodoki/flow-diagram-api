package com.example.flowdiagram.api.controller;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** API入出力用の type 見た目定義。JSON schema は basic-design.md 3.6 のまま変更しない。 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ActionTypeStyleEntity {

    public String background;
    public String border;
    public Double borderWidth;
    public String textColor;
    public String shadow;
}
