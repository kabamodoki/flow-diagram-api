package com.example.flowdiagram;

import com.example.flowdiagram.core.Layout;
import com.example.flowdiagram.core.LayoutEngine;
import com.example.flowdiagram.model.ActionSpec;
import com.example.flowdiagram.model.FlowSpec;
import com.example.flowdiagram.model.StateSpec;
import com.example.flowdiagram.model.Theme;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LayoutEngineTest {

    private final Theme theme = Theme.defaults().mergeWith(null);

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

    private static ActionSpec go(String label, String next) {
        return new ActionSpec("button", label, next);
    }

    private Layout buildLayout(FlowSpec spec) {
        return new LayoutEngine(theme).build(spec);
    }

    private Map<String, Layout.NodeBox> realNodes(FlowSpec spec) {
        return buildLayout(spec).nodeByLabel();
    }

    @Test
    void linearFlowGetsIncreasingColumns() {
        Map<String, Layout.NodeBox> n = realNodes(spec(
                state("a", go("x", "b")),
                state("b", go("y", "c")),
                state("c")));
        assertEquals(0, n.get("a").column);
        assertEquals(1, n.get("b").column);
        assertEquals(2, n.get("c").column);
    }

    @Test
    void branchPutsBothTargetsInSameColumn() {
        Map<String, Layout.NodeBox> n = realNodes(spec(
                state("a", go("x", "b"), go("y", "c")),
                state("b"),
                state("c")));
        assertEquals(1, n.get("b").column);
        assertEquals(1, n.get("c").column);
        assertTrue(n.get("c").y > n.get("b").y, "同一列は縦に積む");
    }

    @Test
    void mergeTakesLongestPathColumn() {
        Map<String, Layout.NodeBox> n = realNodes(spec(
                state("a", go("x", "b"), go("z", "c")),
                state("b", go("y", "c")),
                state("c")));
        assertEquals(2, n.get("c").column);
    }

    @Test
    void isolatedNodeIsPlaced() {
        Map<String, Layout.NodeBox> n = realNodes(spec(
                state("a", go("x", "b")),
                state("b"),
                state("lonely")));
        assertEquals(0, n.get("lonely").column);
        assertTrue(n.get("lonely").y > n.get("a").y, "同じ列なので a の下に積まれる");
    }

    @Test
    void nodeHeightFollowsDesignFormula() {
        LayoutEngine engine = new LayoutEngine(theme);
        int expected = theme.headerHeight + theme.nodePaddingTop + theme.nodePaddingBottom
                + theme.actionRowHeight * 2 + theme.actionGap;
        assertEquals(expected, engine.nodeHeight(2));
    }

    @Test
    void terminalNodeHasHeaderOnlyHeight() {
        LayoutEngine engine = new LayoutEngine(theme);
        assertEquals(theme.headerHeight, engine.nodeHeight(0));
    }

    @Test
    void allSegmentsAreOrthogonal() {
        Layout l = buildLayout(spec(
                state("a", go("x", "b"), go("self", "a")),
                state("b", go("back", "a"))));
        assertFalse(l.edges.isEmpty());
        for (Layout.EdgeRoute e : l.edges) {
            assertFalse(e.segments.isEmpty(), "経路が空: " + e.fromStateLabel);
            for (Layout.Segment s : e.segments) {
                if (s.horizontal) {
                    assertTrue(s.width >= 0);
                } else {
                    assertTrue(s.height >= 0);
                }
            }
        }
    }

    @Test
    void selfLoopBecomesCloneNode() {
        // basic-design.md 6.5: 自己ループは実ボックスへ戻らず、複製ノードを右に出して終端にする
        Layout l = buildLayout(spec(state("a", go("retry", "a"))));
        long cloneCount = l.nodes.stream().filter(n -> n.clone).count();
        assertEquals(1, cloneCount);

        Layout.NodeBox clone = l.nodes.stream().filter(n -> n.clone).findFirst().orElseThrow();
        Layout.NodeBox original = l.nodes.stream().filter(n -> !n.clone).findFirst().orElseThrow();
        assertEquals("a", clone.state.label);
        assertEquals(original.column + 1, clone.column, "複製は起点の右隣の列に置く");
        assertEquals(theme.headerHeight, clone.height, "複製は常に終端＝ヘッダのみの高さ");
    }

    @Test
    void backwardEdgeBecomesCloneNodeNotALineToOriginal() {
        Layout l = buildLayout(spec(
                state("a", go("x", "b")),
                state("b", go("back", "a"))));

        long realACount = l.nodes.stream().filter(n -> !n.clone && n.state.label.equals("a")).count();
        long cloneACount = l.nodes.stream().filter(n -> n.clone && n.state.label.equals("a")).count();
        assertEquals(1, realACount, "実ボックス a は1個のまま");
        assertEquals(1, cloneACount, "差し戻し先の複製 a が1個できる");

        Layout.NodeBox realB = l.nodeByLabel().get("b");
        Layout.NodeBox cloneA = l.nodes.stream()
                .filter(n -> n.clone && n.state.label.equals("a")).findFirst().orElseThrow();
        assertTrue(cloneA.column > realB.column, "複製は起点より右（前進）に置かれる");
    }

    @Test
    void cloneNodeHasNoActionsEvenIfOriginalHasActions() {
        Layout l = buildLayout(spec(
                state("a", go("x", "b"), go("y", "b")), // a には複数アクションがある
                state("b", go("back", "a"))));
        Layout.NodeBox cloneA = l.nodes.stream()
                .filter(n -> n.clone && n.state.label.equals("a")).findFirst().orElseThrow();
        assertEquals(theme.headerHeight, cloneA.height, "複製は元のアクション数に関わらずヘッダのみ");
    }

    @Test
    void cycleWithoutRootStillLaysOut() {
        Layout l = buildLayout(spec(
                state("a", go("x", "b")),
                state("b", go("y", "a"))));
        assertTrue(l.canvasWidth > 0 && l.canvasHeight > 0);
    }

    @Test
    void straightForwardEdgeIsSingleOrThreeSegments() {
        Layout l = buildLayout(spec(state("a", go("x", "b")), state("b")));
        Layout.EdgeRoute e = l.edges.get(0);
        assertTrue(e.segments.size() == 1 || e.segments.size() == 3);
    }

    @Test
    void fullyDisconnectedNodeIsIsolated() {
        // basic-design.md 6.6: どこからも参照されず、自身にもアクションが無いノードのみ isolated
        Map<String, Layout.NodeBox> n = realNodes(spec(
                state("a", go("x", "b")),
                state("b"),
                state("lonely")));
        assertTrue(n.get("lonely").isolated, "誰からも参照されずアクションも無いので未接続");
    }

    @Test
    void terminalNodeReachedByAnEdgeIsNotIsolated() {
        // 通常の終端（矢印で繋がっている）は isolated ではない
        Map<String, Layout.NodeBox> n = realNodes(spec(
                state("a", go("x", "b")),
                state("b")));
        assertFalse(n.get("b").isolated, "aから参照されているので未接続ではない");
        assertFalse(n.get("a").isolated, "bへのアクションを持つので未接続ではない");
    }

    @Test
    void selfLoopingNodeIsNotIsolated() {
        // 自己ループは自分自身を参照している扱いなので isolated にはならない
        Map<String, Layout.NodeBox> n = realNodes(spec(state("a", go("retry", "a"))));
        assertFalse(n.get("a").isolated);
    }

    @Test
    void cloneNodeIsNeverIsolated() {
        Layout l = buildLayout(spec(
                state("a", go("x", "b")),
                state("b", go("back", "a"))));
        Layout.NodeBox clone = l.nodes.stream().filter(box -> box.clone).findFirst().orElseThrow();
        assertFalse(clone.isolated, "複製ノードは何かから参照された結果なので未接続にはならない");
    }
}
