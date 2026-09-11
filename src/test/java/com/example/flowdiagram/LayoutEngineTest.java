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
        assertEquals(LayoutEngine.CLONE_TEXT_HEIGHT, clone.height, "複製はテキスト表示のみの高さ");
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
        assertEquals(LayoutEngine.CLONE_TEXT_HEIGHT, cloneA.height, "複製は元のアクション数に関わらずテキスト高さのみ");
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
    void bendPointIsNearTargetNotNearSource() {
        // basic-design.md 6.4 v3.1: 折れ位置は終点寄り（tx - max(16, columnGap/4) 付近）
        Layout l = buildLayout(spec(state("a", go("x", "b")), state("b")));
        Layout.EdgeRoute e = l.edges.get(0);
        Layout.NodeBox a = l.nodeByLabel().get("a");
        Layout.NodeBox b = l.nodeByLabel().get("b");
        assertEquals(3, e.segments.size());
        // 中間の垂直セグメントのxが、起点右端付近ではなく終点左端付近にあること
        Layout.Segment vertical = e.segments.stream().filter(s -> !s.horizontal).findFirst().orElseThrow();
        int distFromSource = vertical.x - a.right();
        int distFromTarget = b.x - vertical.x;
        assertTrue(distFromTarget < distFromSource, "折れ位置は起点よりも終点に近いこと");
    }

    @Test
    void oneActionWithMultipleNextTargetsProducesMultipleEdges() {
        // basic-design.md 3.3/6.4 v3.1: 1つのアクションから複数の矢印を出せる
        Layout l = buildLayout(spec(
                state("a", new ActionSpec("button", "go", List.of("b", "c"))),
                state("b"),
                state("c")));
        long fromA = l.edges.stream().filter(e -> e.fromStateLabel.equals("a")).count();
        assertEquals(2, fromA, "1つのアクションのnextが2件なら2本のエッジになる");
    }

    @Test
    void multipleNextTargetsCanMixForwardAndBackward() {
        // 1つのアクションの複数next のうち、一方は前進・もう一方は後退（複製化）でもよい
        Layout l = buildLayout(spec(
                state("a", new ActionSpec("button", "go", List.of("a", "b"))), // 自己ループ + 前進
                state("b")));
        long cloneCount = l.nodes.stream().filter(n -> n.clone).count();
        assertEquals(1, cloneCount, "自己ループ側だけ複製ノードになる");
        long fromA = l.edges.stream().filter(e -> e.fromStateLabel.equals("a")).count();
        assertEquals(2, fromA);
    }

    @Test
    void edgeSkippingTwoOrMoreColumnsUsesDetourBelowAllNodes() {
        // basic-design.md 6.4.1 v3.2: a→d は a(col0)→b(col1)→c(col2)→d(col3) を飛び越すので、
        // 中間列(b,c)の高さを通らない迂回経路（実ノード最下端より下の5セグメント）になる
        Layout l = buildLayout(spec(
                state("a", go("skip", "d"), go("x", "b")),
                state("b", go("y", "c")),
                state("c", go("z", "d")),
                state("d")));
        Layout.EdgeRoute skip = l.edges.stream()
                .filter(e -> e.fromStateLabel.equals("a") && e.segments.size() == 5)
                .findFirst().orElseThrow();

        int realNodesMaxBottom = l.nodes.stream()
                .filter(n -> !n.clone).mapToInt(Layout.NodeBox::bottom).max().orElseThrow();
        Layout.Segment detourLane = skip.segments.stream()
                .filter(s -> s.horizontal).skip(1).findFirst().orElseThrow();
        assertTrue(detourLane.y > realNodesMaxBottom, "迂回レーンはどの実ノードよりも下を通ること");
    }

    @Test
    void laneOrderIsNonCrossingByTargetY() {
        // basic-design.md 6.4 v3.3: 非交差マッチングの定石により、終点yが小さい（上にある）ほど
        // レーンは終点寄り（mxが大きい）、終点yが大きい（下にある）ほど起点寄り（mxが小さい）になる
        Layout l = buildLayout(spec(
                state("a", go("toLower", "g"), go("toUpper", "e")),
                state("b"), state("c"), state("d"),
                state("e"), state("f"), state("g")));
        Layout.NodeBox e = l.nodeByLabel().get("e");
        Layout.NodeBox g = l.nodeByLabel().get("g");
        assertTrue(e.y < g.y, "テスト前提: eはgより上に積まれる");

        Layout.EdgeRoute toE = l.edges.stream().filter(r -> r.arrowY == e.y + theme.headerHeight / 2)
                .findFirst().orElseThrow();
        Layout.EdgeRoute toG = l.edges.stream().filter(r -> r.arrowY == g.y + theme.headerHeight / 2)
                .findFirst().orElseThrow();
        Layout.Segment mxE = toE.segments.stream().filter(s -> !s.horizontal).findFirst().orElseThrow();
        Layout.Segment mxG = toG.segments.stream().filter(s -> !s.horizontal).findFirst().orElseThrow();
        assertTrue(mxE.x > mxG.x, "上にある終点(e)の方がレーンのxは大きい（終点寄り）こと。逆だと交差する");
    }

    @Test
    void adjacentColumnEdgeDoesNotUseDetour() {
        // 隣の列（1列だけ）へのエッジは通常の3セグメント経路のまま
        Layout l = buildLayout(spec(state("a", go("x", "b")), state("b")));
        assertTrue(l.edges.stream().noneMatch(e -> e.segments.size() == 5));
    }
}
