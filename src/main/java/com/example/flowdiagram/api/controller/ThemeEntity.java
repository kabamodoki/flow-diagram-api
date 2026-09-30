package com.example.flowdiagram.api.controller;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * API入出力用のテーマ上書き表現（basic-design.md 4章）。
 * 全フィールド任意。ここでは既定値・マージロジックは持たない（それは model.Theme の責務）。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ThemeEntity {

    public Integer nodeWidth;
    public Integer headerHeight;
    public Integer actionRowHeight;
    public Integer actionGap;
    public Integer nodePaddingTop;
    public Integer nodePaddingBottom;
    public Integer columnGap;
    public Integer rowGap;
    public Integer canvasPadding;

    public String fontFamily;
    public Integer titleFontSize;
    public Integer badgeFontSize;
    public Integer stateFontSize;
    public Integer actionFontSize;
    public String background;
    public String titleColor;
    public String nodeShadow;
    public Integer edgeWidth;
    public Integer arrowSize;
    public Boolean showLegend;
}
