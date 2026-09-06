package com.example.flowdiagram;

import com.example.flowdiagram.core.FlowValidator;
import com.example.flowdiagram.model.ActionSpec;
import com.example.flowdiagram.model.FlowSpec;
import com.example.flowdiagram.model.StateSpec;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlowValidatorTest {

    private static FlowSpec spec(StateSpec... states) {
        FlowSpec s = new FlowSpec();
        s.states = new ArrayList<>(List.of(states));
        return s;
    }

    private static StateSpec state(String label, ActionSpec... actions) {
        StateSpec s = new StateSpec(label);
        s.actions = new ArrayList<>(List.of(actions));
        return s;
    }

    @Test
    void v01_statesEmpty() {
        FlowSpec s = new FlowSpec();
        s.states = new ArrayList<>();
        FlowDiagramException e = assertThrows(FlowDiagramException.class, () -> FlowValidator.validate(s));
        assertEquals(List.of("states は1件以上必要です"), e.getMessages());
    }

    @Test
    void v01_specNull() {
        assertThrows(FlowDiagramException.class, () -> FlowValidator.validate(null));
    }

    @Test
    void v02_labelBlank() {
        FlowSpec s = spec(new StateSpec(""));
        FlowDiagramException e = assertThrows(FlowDiagramException.class, () -> FlowValidator.validate(s));
        assertTrue(e.getMessages().contains("states[0].label は必須です"));
    }

    @Test
    void v03_duplicatedLabel() {
        FlowSpec s = spec(state("a"), state("a"));
        FlowDiagramException e = assertThrows(FlowDiagramException.class, () -> FlowValidator.validate(s));
        assertTrue(e.getMessages().contains("ステータス/手続きのlabelが重複しています: a"));
    }

    @Test
    void v04_actionLabelBlank() {
        FlowSpec s = spec(state("a", new ActionSpec("button", "  ", null)));
        FlowDiagramException e = assertThrows(FlowDiagramException.class, () -> FlowValidator.validate(s));
        assertTrue(e.getMessages().contains("states[0].actions[0].label は必須です"));
    }

    @Test
    void v05_unknownNext() {
        FlowSpec s = spec(state("a", new ActionSpec("button", "go", "zzz")));
        FlowDiagramException e = assertThrows(FlowDiagramException.class, () -> FlowValidator.validate(s));
        assertTrue(e.getMessages().get(0)
                .contains("states[0].actions[0].next が存在しないlabelを指しています: zzz"));
    }

    @Test
    void collectsAllViolationsAtOnce() {
        FlowSpec s = spec(
                state("a", new ActionSpec("button", "go", "nope")),
                state("a", new ActionSpec("button", null, null)));
        FlowDiagramException e = assertThrows(FlowDiagramException.class, () -> FlowValidator.validate(s));
        assertEquals(3, e.getMessages().size(), () -> "実際: " + e.getMessages());
    }

    @Test
    void validSpecPasses() {
        FlowSpec s = spec(
                state("a", new ActionSpec("button", "go", "b")),
                state("b"));
        assertDoesNotThrow(() -> FlowValidator.validate(s));
    }

    @Test
    void selfLoopIsValid() {
        FlowSpec s = spec(state("a", new ActionSpec("flow", "retry", "a")));
        assertDoesNotThrow(() -> FlowValidator.validate(s));
    }
}
