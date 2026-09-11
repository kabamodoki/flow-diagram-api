package com.example.flowdiagram.core;

import com.example.flowdiagram.model.ActionSpec;
import com.example.flowdiagram.model.ActionTypeStyle;
import com.example.flowdiagram.model.FlowSpec;
import com.example.flowdiagram.model.KindStyle;
import com.example.flowdiagram.model.StateSpec;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JSON の {@code kinds}/{@code types} と、実際に使われている kind/type キーの集合から、
 * このリクエスト1回分だけで使う最終的なスタイル一覧を作る（basic-design.md 7章）。
 * プログラムは kind/type の意味を一切知らない。渡された定義をそのまま解決するだけ。
 */
public final class StyleRegistry {

    private StyleRegistry() {
    }

    /** 実際に使われている kind ごとに、既定値へ spec.kinds を上書きしたスタイルを返す。 */
    public static Map<String, KindStyle> resolveKindStyles(FlowSpec spec) {
        Map<String, KindStyle> result = new LinkedHashMap<>();
        KindStyle base = KindStyle.defaults();
        for (StateSpec s : spec.states) {
            String key = s.effectiveKind();
            if (result.containsKey(key)) {
                continue;
            }
            KindStyle override = spec.kinds == null ? null : spec.kinds.get(key);
            result.put(key, base.mergeWith(override));
        }
        return result;
    }

    /** 実際に使われている type ごとに、既定値へ spec.types を上書きしたスタイルを返す。 */
    public static Map<String, ActionTypeStyle> resolveTypeStyles(FlowSpec spec) {
        Map<String, ActionTypeStyle> result = new LinkedHashMap<>();
        ActionTypeStyle base = ActionTypeStyle.defaults();
        for (StateSpec s : spec.states) {
            for (ActionSpec a : s.safeActions()) {
                String key = a.effectiveType();
                if (result.containsKey(key)) {
                    continue;
                }
                ActionTypeStyle override = spec.types == null ? null : spec.types.get(key);
                result.put(key, base.mergeWith(override));
            }
        }
        return result;
    }
}
