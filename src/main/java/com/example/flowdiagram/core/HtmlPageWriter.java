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

    /** ブラウザでそのまま開ける1枚もの HTML。 */
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
                .fd-toolbar{position:sticky;top:0;z-index:10;display:flex;gap:8px;align-items:center;
                padding:10px 16px;background:rgba(255,255,255,.92);border-bottom:1px solid #e2e8f0;
                font:13px/1.4 %FONT%;backdrop-filter:blur(4px);}
                .fd-toolbar button{font:inherit;padding:5px 12px;border:1px solid #cbd5e0;border-radius:6px;
                background:#fff;color:#2d3748;cursor:pointer;}
                .fd-toolbar button:hover{background:#edf2f7;}
                .fd-toolbar .zoom-label{color:#718096;margin-left:4px;min-width:46px;}
                .fd-scroll{overflow:auto;padding:16px;}
                @media print{.fd-toolbar{display:none;}.fd-scroll{overflow:visible;padding:0;}}
                %CSS%
                </style>
                </head>
                <body>
                <div class="fd-toolbar">
                  <button type="button" data-act="out">&minus; 縮小</button>
                  <button type="button" data-act="in">&plus; 拡大</button>
                  <button type="button" data-act="reset">等倍</button>
                  <span class="zoom-label" id="fdZoomLabel">100%</span>
                  <button type="button" data-act="save">HTMLを保存</button>
                </div>
                <div class="fd-scroll">
                <div class="fd-root">
                %CANVAS%
                </div>
                </div>
                <script>
                (function(){
                  var scale = 1;
                  var canvas = document.querySelector('.fd-canvas');
                  var scroll = document.querySelector('.fd-scroll');
                  var label  = document.getElementById('fdZoomLabel');
                  var baseW = canvas.offsetWidth, baseH = canvas.offsetHeight;
                  function apply(){
                    canvas.style.transform = 'scale(' + scale + ')';
                    scroll.style.minHeight = (baseH * scale + 32) + 'px';
                    canvas.style.marginRight = (baseW * (scale - 1)) + 'px';
                    label.textContent = Math.round(scale * 100) + '%';
                  }
                  function save(){
                    var blob = new Blob(['<!DOCTYPE html>\\n' + document.documentElement.outerHTML],
                                        {type:'text/html;charset=utf-8'});
                    var a = document.createElement('a');
                    a.href = URL.createObjectURL(blob);
                    a.download = 'flow-diagram.html';
                    document.body.appendChild(a); a.click(); document.body.removeChild(a);
                    setTimeout(function(){ URL.revokeObjectURL(a.href); }, 1000);
                  }
                  document.querySelector('.fd-toolbar').addEventListener('click', function(ev){
                    var act = ev.target.getAttribute('data-act');
                    if (!act) return;
                    if (act === 'in')    { scale = Math.min(3, scale + 0.1); apply(); }
                    if (act === 'out')   { scale = Math.max(0.3, scale - 0.1); apply(); }
                    if (act === 'reset') { scale = 1; apply(); }
                    if (act === 'save')  { save(); }
                  });
                  apply();
                })();
                </script>
                </body>
                </html>
                """
                .replace("%TITLE%", Html.esc(pageTitle))
                .replace("%BG%", Html.cssValue(theme.background, "#f7f8fa"))
                .replace("%FONT%", Html.cssValue(theme.fontFamily, "sans-serif"))
                .replace("%CSS%", css)
                .replace("%CANVAS%", canvasHtml);
    }
}
