package com.example.flowdiagram.core;

/**
 * 図の配色定義（basic-design.md 6.6章）。
 * ユーザー指示により、色は JSON テーマではなくソースコード上の定数として一元管理する。
 * 配色を変えたいときはこのファイルの値だけを書き換える。
 */
public final class Palette {

    private Palette() {
    }

    // --- ステータス（薄い青。ヘッダー/本体ともかなり淡く） ---
    public static final String STATUS_HEADER_BG = "#eaf4ff";
    public static final String STATUS_HEADER_TEXT = "#2c6291";
    public static final String STATUS_BODY_BG = "#f8fbfe";
    public static final String STATUS_BORDER = "#cfe3f5";

    // --- 手続き（薄い緑。ヘッダー/本体ともかなり淡く） ---
    public static final String PROCEDURE_HEADER_BG = "#eaf8ee";
    public static final String PROCEDURE_HEADER_TEXT = "#2f7a52";
    public static final String PROCEDURE_BODY_BG = "#f8fcf9";
    public static final String PROCEDURE_BORDER = "#cdeada";

    public static final String KIND_BADGE_LABEL_STATUS = "ステータス";
    public static final String KIND_BADGE_LABEL_PROCEDURE = "手続き";

    // --- アクション: ボタン（白っぽい、ボタンらしい見た目） ---
    public static final String BUTTON_BG = "#ffffff";
    public static final String BUTTON_BORDER = "#c4c9d0";
    public static final String BUTTON_BORDER_WIDTH = "1.5px";
    public static final String BUTTON_TEXT = "#2d3748";
    public static final String BUTTON_SHADOW = "0 1px 2px rgba(15,23,42,.10)";

    // --- アクション: フロー（ボタンよりわずかに青みがある程度の、白に近い枠） ---
    public static final String FLOW_BG = "#f8fbfe";
    public static final String FLOW_BORDER = "#7fb8ee";
    public static final String FLOW_BORDER_WIDTH = "1.5px";
    public static final String FLOW_TEXT = "#2c6291";

    // --- 関係線（すべて薄い青の実線で統一） ---
    public static final String EDGE_COLOR = "#7fb8ee";

    // --- 未接続ノード（どこからも参照されず、アクションも無い孤立したノード） ---
    public static final String UNCONNECTED_BORDER = "#9aa1ab";
    public static final String UNCONNECTED_BADGE_BG = "#eef0f2";
    public static final String UNCONNECTED_BADGE_TEXT = "#5b6270";
    public static final String UNCONNECTED_BADGE_LABEL = "未接続";
}
