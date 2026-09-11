package com.example.flowdiagram.core;

/**
 * kind/type に紐付かない、図全体で共通の色定義（basic-design.md 6.7章）。
 * kind/type ごとの色は JSON ルートの {@code kinds}/{@code types} で指定する
 * （{@link com.example.flowdiagram.model.KindStyle}/{@link com.example.flowdiagram.model.ActionTypeStyle}）。
 * ここに残っているのは、どの kind/type にも属さない色のみ。
 */
public final class Palette {

    private Palette() {
    }

    // --- 関係線（すべて薄い青の実線で統一） ---
    public static final String EDGE_COLOR = "#7fb8ee";
}
