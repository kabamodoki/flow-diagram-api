package com.example.flowdiagram.core;

import java.util.Locale;

/** HTML/CSS 出力ユーティリティ。 */
public final class Html {

    private Html() {
    }

    /** HTML エスケープ（basic-design.md 7章）。 */
    public static String esc(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&' -> sb.append("&amp;");
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '"' -> sb.append("&quot;");
                case '\'' -> sb.append("&#39;");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * CSS の値として安全な文字列だけを通す。
     * `}` や `<` などを含む値は CSS/HTML を壊すため、既定色に落とす。
     */
    public static String cssValue(String raw, String fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '{' || c == '}' || c == ';' || c == '<' || c == '>' || c == '\\') {
                return fallback;
            }
        }
        return raw;
    }

    /**
     * label を CSS クラス名の一部として使える安全なトークンに変換する。
     * label は日本語などの任意の文字列になりうるため、文字を残そうとせず
     * {@link String#hashCode()}（Java仕様で計算式が固定されており実行間で安定）を
     * 16進数化するだけにする。これにより非ASCII文字だけの label 同士が
     * 同じトークンに潰れて衝突する事故を避ける。
     */
    public static String labelToken(String raw) {
        String s = raw == null ? "" : raw;
        return "l" + Integer.toHexString(s.hashCode());
    }

    /** 小数の余分な .0 を落として px 値にする。 */
    public static String px(double v) {
        if (v == Math.rint(v)) {
            return ((long) v) + "px";
        }
        return String.format(Locale.ROOT, "%.2fpx", v);
    }
}
