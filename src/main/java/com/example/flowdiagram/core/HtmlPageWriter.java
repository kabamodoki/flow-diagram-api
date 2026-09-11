package com.example.flowdiagram.core;

import com.example.flowdiagram.model.Theme;

/**
 * 完結した HTML ページ / 埋め込み用フラグメントを組み立てる（basic-design.md 8章）。
 * 外部 CDN・外部 CSS・画像を一切参照しない。
 */
public class HtmlPageWriter {

    private final Theme theme;

    public HtmlPageWriter(Theme theme) {
        this.theme = theme;
    }

    /** 既存ページへの埋め込み用。`<style>` + `.fd-root` のみで `<!DOCTYPE>` を含まない。 */
    public String fragment(String css, String canvasHtml) {
        return "<style>\n" + css + "</style>\n"
                + "<div class=\"fd-root\">\n" + canvasHtml + "</div>\n";
    }

    /** ブラウザでそのまま開ける1枚もの HTML。JSは一切含まない（basic-design.md 9章）。 */
    public String page(String title, String css, String canvasHtml) {
        String pageTitle = (title == null || title.isBlank()) ? "Flow Diagram" : title;
        return """
                <!DOCTYPE html>
                <html lang="ja">
                <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width,initial-scale=1">
                <title>%TITLE%</title>
                <style>
                html,body{margin:0;padding:0;background:%BG%;}
                %CSS%
                </style>
                </head>
                <body>
                <div class="fd-root">
                %CANVAS%
                </div>
                </body>
                </html>
                """
                .replace("%TITLE%", Html.esc(pageTitle))
                .replace("%BG%", Html.cssValue(theme.background, "#f7f8fa"))
                .replace("%CSS%", css)
                .replace("%CANVAS%", canvasHtml);
    }
}
