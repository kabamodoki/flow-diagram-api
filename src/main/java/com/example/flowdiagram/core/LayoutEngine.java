package com.example.flowdiagram.core;

import com.example.flowdiagram.model.ActionSpec;
import com.example.flowdiagram.model.FlowSpec;
import com.example.flowdiagram.model.StateSpec;
import com.example.flowdiagram.model.Theme;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 座標計算（basic-design.md 6章）。 */
public class LayoutEngine {

    private static final int LEGEND_HEIGHT = 56;

    private final Theme theme;

    public LayoutEngine(Theme theme) {
        this.theme = theme;
    }

    public Layout build(FlowSpec spec) {
        Layout layout = new Layout();
        layout.title = spec.displayTitle();

        List<StateSpec> states = spec.states;
        Map<String, Integer> indexByLabel = new LinkedHashMap<>();
        for (int i = 0; i < states.size(); i++) {
            indexByLabel.put(states.get(i).label, i);
        }

        int[] columns = assignColumns(states, indexByLabel);
        Set<String> referencedLabels = referencedLabels(states);

        // --- 実ノード配置 ---
        int titleAreaHeight = layout.title.isEmpty() ? 0 : 52;
        int topY = theme.canvasPadding + titleAreaHeight;
        Map<Integer, Integer> nextYByColumn = new HashMap<>();

        Map<String, Layout.NodeBox> boxes = new LinkedHashMap<>();
        for (int i = 0; i < states.size(); i++) {
            StateSpec s = states.get(i);
            Layout.NodeBox box = new Layout.NodeBox();
            box.state = s;
            box.column = columns[i];
            box.width = theme.nodeWidth;
            box.height = nodeHeight(s.safeActions().size());
            // basic-design.md 6.6: 出次数0かつ入次数0（どこからも参照されない）ノードのみ未接続扱い
            box.isolated = s.safeActions().isEmpty() && !referencedLabels.contains(s.label);
            placeInColumn(box, nextYByColumn, topY);
            layout.nodes.add(box);
            boxes.put(s.label, box);
        }

        // --- エッジ（前進のみ）＋ 後退辺・自己ループの複製ノード ---
        for (StateSpec s : states) {
            Layout.NodeBox from = boxes.get(s.label);
            List<ActionSpec> actions = s.safeActions();

            // 同じノードから出るエッジが縦の幹線で重なって描かれないよう、レーンをずらす（basic-design.md 6.4）
            int edgeCount = (int) actions.stream().filter(a -> a.next != null && !a.next.isBlank()).count();
            int lane = 0;

            for (int j = 0; j < actions.size(); j++) {
                ActionSpec a = actions.get(j);
                if (a.next == null || a.next.isBlank()) {
                    continue;
                }
                Layout.NodeBox target = boxes.get(a.next);
                int sx = from.right();
                int sy = actionAnchorY(from, j);

                Layout.NodeBox to;
                if (target.column > from.column) {
                    to = target; // 前進: 実ボックスへ
                } else {
                    // 後退辺・自己ループ（basic-design.md 6.5）: 複製ノードを新規に右側へ配置
                    to = cloneNode(target.state, from.column + 1, nextYByColumn, topY);
                    layout.nodes.add(to);
                }

                int ty = to.clone ? to.y + CLONE_TEXT_HEIGHT / 2 : to.y + theme.headerHeight / 2;
                Layout.EdgeRoute e = new Layout.EdgeRoute();
                e.fromStateLabel = s.label;
                routeForward(e, sx, sy, to.x, ty, lane, edgeCount);
                lane++;
                e.arrowX = to.x;
                e.arrowY = ty;
                layout.edges.add(e);
            }
        }

        int maxRight = 0;
        int maxBottom = 0;
        for (Layout.NodeBox n : layout.nodes) {
            maxRight = Math.max(maxRight, n.right());
            maxBottom = Math.max(maxBottom, n.bottom());
        }

        int legend = Boolean.TRUE.equals(theme.showLegend) ? LEGEND_HEIGHT : 0;
        layout.canvasWidth = maxRight + theme.canvasPadding;
        layout.canvasHeight = maxBottom + legend + theme.canvasPadding;
        return layout;
    }

    /** 後退辺・自己ループの終端として使う複製ノードを作る（basic-design.md 6.5）。 */
    private Layout.NodeBox cloneNode(StateSpec target, int column, Map<Integer, Integer> nextYByColumn, int topY) {
        Layout.NodeBox box = new Layout.NodeBox();
        box.state = target;
        box.column = column;
        box.width = theme.nodeWidth;
        box.height = CLONE_TEXT_HEIGHT; // テキスト表示のみ
        box.clone = true;
        placeInColumn(box, nextYByColumn, topY);
        return box;
    }

    private void placeInColumn(Layout.NodeBox box, Map<Integer, Integer> nextYByColumn, int topY) {
        box.x = theme.canvasPadding + box.column * (theme.nodeWidth + theme.columnGap);
        int y = nextYByColumn.getOrDefault(box.column, topY);
        box.y = y;
        nextYByColumn.put(box.column, y + box.height + theme.rowGap);
    }

    /**
     * 全 state の全 actions のうち、next で参照されている label の集合（basic-design.md 6.6）。
     * 自己ループも含む（next == 自分自身でも「参照されている」扱い）。
     */
    private Set<String> referencedLabels(List<StateSpec> states) {
        Set<String> refs = new HashSet<>();
        for (StateSpec s : states) {
            for (ActionSpec a : s.safeActions()) {
                if (a.next != null && !a.next.isBlank()) {
                    refs.add(a.next);
                }
            }
        }
        return refs;
    }

    /** ノード高さ（basic-design.md 6.1）。 */
    public int nodeHeight(int actionCount) {
        if (actionCount == 0) {
            return theme.headerHeight;
        }
        return theme.headerHeight + theme.nodePaddingTop
                + theme.actionRowHeight * actionCount + theme.actionGap * (actionCount - 1)
                + theme.nodePaddingBottom;
    }

    /** アクションチップ j の中央 y（矢印の起点）。 */
    public int actionAnchorY(Layout.NodeBox box, int j) {
        int y = box.y + theme.headerHeight + theme.nodePaddingTop;
        y += j * (theme.actionRowHeight + theme.actionGap);
        return y + theme.actionRowHeight / 2;
    }

    /**
     * 列（レイヤー）割り当て（basic-design.md 6.2）。
     * 循環しても発散しないよう、DFS で後退辺を除いて DAG 化してから最長経路で決める。
     */
    private int[] assignColumns(List<StateSpec> states, Map<String, Integer> indexByLabel) {
        int n = states.size();

        List<List<Integer>> adj = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            adj.add(new ArrayList<>());
        }
        for (int i = 0; i < n; i++) {
            for (ActionSpec a : states.get(i).safeActions()) {
                if (a.next == null || a.next.isBlank() || a.next.equals(states.get(i).label)) {
                    continue;
                }
                Integer ti = indexByLabel.get(a.next);
                if (ti != null && ti != i) {
                    adj.get(i).add(ti);
                }
            }
        }

        boolean[][] isBackEdge = markBackEdges(n, adj, rootsOf(n, adj));

        int[] dagInDegree = new int[n];
        for (int u = 0; u < n; u++) {
            List<Integer> targets = adj.get(u);
            for (int k = 0; k < targets.size(); k++) {
                if (!isBackEdge[u][k]) {
                    dagInDegree[targets.get(k)]++;
                }
            }
        }

        int[] columns = new int[n];
        boolean[] settled = new boolean[n];
        Deque<Integer> queue = new ArrayDeque<>();
        for (int i = 0; i < n; i++) {
            if (dagInDegree[i] == 0) {
                queue.add(i);
                settled[i] = true;
            }
        }
        while (!queue.isEmpty()) {
            int u = queue.poll();
            List<Integer> targets = adj.get(u);
            for (int k = 0; k < targets.size(); k++) {
                if (isBackEdge[u][k]) {
                    continue;
                }
                int v = targets.get(k);
                columns[v] = Math.max(columns[v], columns[u] + 1);
                if (--dagInDegree[v] == 0) {
                    queue.add(v);
                    settled[v] = true;
                }
            }
        }

        for (int i = 0; i < n; i++) {
            if (!settled[i]) {
                columns[i] = 0;
            }
        }
        return columns;
    }

    private List<Integer> rootsOf(int n, List<List<Integer>> adj) {
        int[] inDegree = new int[n];
        for (List<Integer> targets : adj) {
            for (int v : targets) {
                inDegree[v]++;
            }
        }
        List<Integer> roots = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (inDegree[i] == 0) {
                roots.add(i);
            }
        }
        if (roots.isEmpty() && n > 0) {
            roots.add(0);
        }
        return roots;
    }

    /**
     * DFS で後退辺（探索中スタック上のノードへ戻る辺）を検出する。
     * 戻り値は [ノード][そのノードの何番目の辺か] を添字とするフラグ配列。
     */
    private boolean[][] markBackEdges(int n, List<List<Integer>> adj, List<Integer> roots) {
        boolean[][] isBackEdge = new boolean[n][];
        for (int i = 0; i < n; i++) {
            isBackEdge[i] = new boolean[adj.get(i).size()];
        }
        int[] color = new int[n]; // 0=未訪問 1=探索中 2=完了

        List<Integer> starts = new ArrayList<>(roots);
        for (int i = 0; i < n; i++) {
            starts.add(i); // 未到達ノードも順に起点として拾う
        }

        for (int start : starts) {
            if (color[start] != 0) {
                continue;
            }
            Deque<int[]> stack = new ArrayDeque<>(); // {node, 次に見る子の添字}
            stack.push(new int[]{start, 0});
            color[start] = 1;
            while (!stack.isEmpty()) {
                int[] frame = stack.peek();
                int u = frame[0];
                List<Integer> targets = adj.get(u);
                if (frame[1] >= targets.size()) {
                    color[u] = 2;
                    stack.pop();
                    continue;
                }
                int k = frame[1]++;
                int v = targets.get(k);
                if (color[v] == 1) {
                    isBackEdge[u][k] = true; // 循環を作る辺
                } else if (color[v] == 0) {
                    color[v] = 1;
                    stack.push(new int[]{v, 0});
                }
            }
        }
        return isBackEdge;
    }

    private static final int EDGE_LANE_STEP = 16;
    public static final int CLONE_TEXT_HEIGHT = 28;

    private void routeForward(Layout.EdgeRoute e, int sx, int sy, int tx, int ty, int lane, int laneCount) {
        if (sy == ty) {
            e.segments.add(Layout.Segment.h(sx, tx, sy));
            return;
        }
        // 同じ起点から出る複数のエッジが縦の幹線で重ならないよう、レーンごとに x をずらす
        double center = (laneCount - 1) / 2.0;
        int mx = sx + theme.columnGap / 2 + (int) Math.round((lane - center) * EDGE_LANE_STEP);
        mx = Math.max(sx + 12, Math.min(mx, tx - 12));
        e.segments.add(Layout.Segment.h(sx, mx, sy));
        e.segments.add(Layout.Segment.v(sy, ty, mx));
        e.segments.add(Layout.Segment.h(mx, tx, ty));
    }
}
