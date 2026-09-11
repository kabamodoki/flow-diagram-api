package com.example.flowdiagram.core;

import com.example.flowdiagram.model.StateSpec;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** レイアウト結果（basic-design.md 6章）。座標はすべて px。 */
public class Layout {

    /** 配置済みのボックス。実ノードと、後退辺/自己ループ用の複製ノードの両方を表す。 */
    public static class NodeBox {
        public StateSpec state;
        public int column;
        public int x;
        public int y;
        public int width;
        public int height;
        /** true の場合、state.actions に関わらずアクションを描画しない（basic-design.md 6.5）。 */
        public boolean clone;
        /** アクションチップごとの実高さ（basic-design.md 6.1.1）。長いラベルの折り返しを見込んだ px。 */
        public List<Integer> actionRowHeights = new ArrayList<>();

        public int right() {
            return x + width;
        }

        public int bottom() {
            return y + height;
        }
    }

    /** 経路の1区間。水平 or 垂直のみ。 */
    public static class Segment {
        public boolean horizontal;
        public int x;
        public int y;
        public int width;
        public int height;

        public static Segment h(int x1, int x2, int y) {
            Segment s = new Segment();
            s.horizontal = true;
            s.x = Math.min(x1, x2);
            s.y = y;
            s.width = Math.abs(x2 - x1);
            return s;
        }

        public static Segment v(int y1, int y2, int x) {
            Segment s = new Segment();
            s.horizontal = false;
            s.x = x;
            s.y = Math.min(y1, y2);
            s.height = Math.abs(y2 - y1);
            return s;
        }
    }

    /** アクション1件に対応する矢印。常に前進（起点より右）。 */
    public static class EdgeRoute {
        public String fromStateLabel;
        public List<Segment> segments = new ArrayList<>();
        public int arrowX;
        public int arrowY;
    }

    public String title = "";
    public int canvasWidth;
    public int canvasHeight;
    public List<NodeBox> nodes = new ArrayList<>();
    public List<EdgeRoute> edges = new ArrayList<>();

    /** label をキーにした実ノード（複製は含まない）の索引。 */
    public Map<String, NodeBox> nodeByLabel() {
        Map<String, NodeBox> m = new LinkedHashMap<>();
        for (NodeBox n : nodes) {
            m.putIfAbsent(n.state.label, n);
        }
        return m;
    }
}
