package com.example.flowdiagram;

import com.example.flowdiagram.core.StyleRegistry;
import com.example.flowdiagram.model.ActionSpec;
import com.example.flowdiagram.model.ActionTypeStyle;
import com.example.flowdiagram.model.FlowSpec;
import com.example.flowdiagram.model.KindStyle;
import com.example.flowdiagram.model.StateSpec;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StyleRegistryTest {

    private static FlowSpec spec(StateSpec... states) {
        FlowSpec s = new FlowSpec();
        s.states = new ArrayList<>(List.of(states));
        return s;
    }

    private static StateSpec state(String label, String kind, ActionSpec... actions) {
        StateSpec s = new StateSpec(label);
        s.kind = kind;
        s.actions = new ArrayList<>(List.of(actions));
        return s;
    }

    @Test
    void undefinedKindsFallsBackToDefaults() {
        FlowSpec spec = spec(state("a", "何か"));
        Map<String, KindStyle> resolved = StyleRegistry.resolveKindStyles(spec);
        assertEquals(1, resolved.size());
        KindStyle style = resolved.get("何か");
        KindStyle defaults = KindStyle.defaults();
        assertEquals(defaults.background, style.background);
        assertEquals(defaults.headerBackground, style.headerBackground);
    }

    @Test
    void jsonKindsPartiallyOverridesDefaults() {
        FlowSpec spec = spec(state("a", "status"));
        Map<String, KindStyle> kinds = new LinkedHashMap<>();
        KindStyle override = new KindStyle();
        override.background = "#123456"; // headerBackground 等は未指定のまま
        kinds.put("status", override);
        spec.kinds = kinds;

        Map<String, KindStyle> resolved = StyleRegistry.resolveKindStyles(spec);
        KindStyle style = resolved.get("status");
        assertEquals("#123456", style.background);
        assertEquals(KindStyle.defaults().headerBackground, style.headerBackground,
                "未指定フィールドは既定値のまま");
    }

    @Test
    void unusedKindsDefinitionIsNotIncludedInResolution() {
        FlowSpec spec = spec(state("a", "status"));
        Map<String, KindStyle> kinds = new LinkedHashMap<>();
        kinds.put("status", new KindStyle());
        kinds.put("procedure", new KindStyle()); // どの state にも使われていない
        spec.kinds = kinds;

        Map<String, KindStyle> resolved = StyleRegistry.resolveKindStyles(spec);
        assertTrue(resolved.containsKey("status"));
        assertFalse(resolved.containsKey("procedure"), "使われていないkindはCSSに出さない");
    }

    @Test
    void blankKindResolvesToDefaultKey() {
        FlowSpec spec = spec(state("a", null));
        Map<String, KindStyle> resolved = StyleRegistry.resolveKindStyles(spec);
        assertTrue(resolved.containsKey(StateSpec.DEFAULT_KIND));
    }

    @Test
    void typesResolveAcrossAllStatesActions() {
        FlowSpec spec = spec(
                state("a", "status", new ActionSpec("button", "go", "b")),
                state("b", "status", new ActionSpec("flow", "go2", (String) null)));
        Map<String, ActionTypeStyle> resolved = StyleRegistry.resolveTypeStyles(spec);
        assertTrue(resolved.containsKey("button"));
        assertTrue(resolved.containsKey("flow"));
    }

    @Test
    void jsonTypesPartiallyOverridesDefaults() {
        FlowSpec spec = spec(state("a", "status", new ActionSpec("button", "go", (String) null)));
        Map<String, ActionTypeStyle> types = new LinkedHashMap<>();
        ActionTypeStyle override = new ActionTypeStyle();
        override.borderWidth = 3.0;
        types.put("button", override);
        spec.types = types;

        Map<String, ActionTypeStyle> resolved = StyleRegistry.resolveTypeStyles(spec);
        ActionTypeStyle style = resolved.get("button");
        assertEquals(3.0, style.borderWidth);
        assertEquals(ActionTypeStyle.defaults().background, style.background, "未指定フィールドは既定値のまま");
    }

    @Test
    void blankTypeResolvesToDefaultKey() {
        FlowSpec spec = spec(state("a", "status", new ActionSpec(null, "go", (String) null)));
        Map<String, ActionTypeStyle> resolved = StyleRegistry.resolveTypeStyles(spec);
        assertTrue(resolved.containsKey(ActionSpec.DEFAULT_TYPE));
    }
}
