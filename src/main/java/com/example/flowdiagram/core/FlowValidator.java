package com.example.flowdiagram.core;

import com.example.flowdiagram.FlowDiagramException;
import com.example.flowdiagram.model.ActionSpec;
import com.example.flowdiagram.model.FlowSpec;
import com.example.flowdiagram.model.StateSpec;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** 入力検証（basic-design.md 5章）。全違反をまとめて返す。 */
public final class FlowValidator {

    private FlowValidator() {
    }

    public static void validate(FlowSpec spec) {
        List<String> errors = new ArrayList<>();

        if (spec == null || spec.states == null || spec.states.isEmpty()) {
            throw new FlowDiagramException(List.of("states は1件以上必要です")); // V-01
        }

        Set<String> labels = new LinkedHashSet<>();
        for (int i = 0; i < spec.states.size(); i++) {
            StateSpec s = spec.states.get(i);
            if (s == null || s.label == null || s.label.isBlank()) {
                errors.add("states[" + i + "].label は必須です"); // V-02
                continue;
            }
            if (!labels.add(s.label)) {
                errors.add("ステータス/手続きのlabelが重複しています: " + s.label); // V-03
            }
        }

        for (int i = 0; i < spec.states.size(); i++) {
            StateSpec s = spec.states.get(i);
            if (s == null) {
                continue;
            }
            List<ActionSpec> actions = s.safeActions();
            for (int j = 0; j < actions.size(); j++) {
                ActionSpec a = actions.get(j);
                if (a == null || a.label == null || a.label.isBlank()) {
                    errors.add("states[" + i + "].actions[" + j + "].label は必須です"); // V-04
                    continue;
                }
                for (String next : a.effectiveNextTargets()) {
                    if (!labels.contains(next)) {
                        errors.add("states[" + i + "].actions[" + j + "].next が存在しないlabelを指しています: "
                                + next); // V-05
                    }
                }
            }
        }

        if (!errors.isEmpty()) {
            throw new FlowDiagramException(errors);
        }
    }
}
