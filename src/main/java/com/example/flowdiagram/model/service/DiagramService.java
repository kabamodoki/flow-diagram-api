package com.example.flowdiagram.model.service;

import com.example.flowdiagram.FlowDiagramException;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 図の生成に関する処理を全て行う model 層のサービス（検証・レイアウト計算・スタイル解決・
 * HTML/CSS描画・ページ組み立て）。API層（{@code com.example.flowdiagram.api}）はこのクラスを
 * 呼び出すだけで、ここに書かれた処理の内容を知らない。
 *
 * <p>ユーザー指示（2026-09-30リファクタ）により、model層は「serviceクラスが処理の全般を全て行う」
 * 構成にするため、内部の入力entity・処理ロジックは全てこのクラスのネストクラスとしてまとめている
 * （api層に渡す入出力の entity のみ {@code api.controller} パッケージに分離）。</p>
 */
@Service
public class DiagramService {

    // ============================================================
    // 公開API（api層から呼ばれる窓口）
    // ============================================================

    /** ブラウザでそのまま開ける完結した HTML を返す。 */
    public String renderPage(FlowSpec spec) {
        FlowValidator.validate(spec);
        Theme theme = spec.resolvedTheme();
        Layout layout = new LayoutEngine(theme).build(spec);
        Map<String, KindStyle> kindStyles = StyleRegistry.resolveKindStyles(spec);
        Map<String, ActionTypeStyle> typeStyles = StyleRegistry.resolveTypeStyles(spec);
        String css = new CssBuilder(theme).build();
        String canvas = new HtmlDiagramRenderer(theme, kindStyles, typeStyles).render(layout);
        return new HtmlPageWriter(theme).page(spec.displayTitle(), css, canvas);
    }

    // ============================================================
    // 内部entity（basic-design.md 3章）
    // コントローラの入り口 entity（api.controller パッケージ）とは別クラス。
    // JSONの入出力は持たず、model層の処理だけに使う。
    // ============================================================

    /** 図の内部表現のルート（basic-design.md 3.1）。 */
    public static class FlowSpec {

        public String title;

        /** 見た目の部分上書き。null 可。 */
        public Theme theme;

        /** kind ごとの見た目定義。null 可（basic-design.md 3.5）。 */
        public Map<String, KindStyle> kinds;

        /** type ごとの見た目定義。null 可（basic-design.md 3.6）。 */
        public Map<String, ActionTypeStyle> types;

        public List<StateSpec> states = new ArrayList<>();

        public String displayTitle() {
            return (title == null || title.isBlank()) ? "" : title;
        }

        /** 既定値とマージ済みのテーマを返す。 */
        public Theme resolvedTheme() {
            return Theme.defaults().mergeWith(theme);
        }
    }

    /**
     * ステータス／手続き＝大枠（basic-design.md 3.2）。
     * {@code id} は持たない。{@link #label} が識別子を兼ねる（states[] 内で一意）。
     * {@code kind} は固定enumではなく任意の文字列（basic-design.md v3.0）。
     * 見た目は JSON ルートの {@code kinds} 定義で決まり、このクラスは意味を持たない。
     */
    public static class StateSpec {

        /** 未指定時に使う内部キー（basic-design.md 3.7）。 */
        public static final String DEFAULT_KIND = "default";

        /** 表示名。必須。states[] 内で一意である必要があり、識別子としても使われる。 */
        public String label;

        /** 任意の文字列。JSON ルートの {@code kinds} のキーと対応させる。省略可。 */
        public String kind;

        /** アクション。省略・空なら終端。 */
        public List<ActionSpec> actions = new ArrayList<>();

        public StateSpec() {
        }

        public StateSpec(String label) {
            this.label = label;
        }

        public List<ActionSpec> safeActions() {
            return actions == null ? List.of() : actions;
        }

        /** kind が省略されている場合の解決済みキー。 */
        public String effectiveKind() {
            return (kind == null || kind.isBlank()) ? DEFAULT_KIND : kind;
        }
    }

    /**
     * アクション（basic-design.md 3.3）。
     * {@code type} は固定enumではなく任意の文字列（basic-design.md v3.0）。
     * 見た目は JSON ルートの {@code types} 定義で決まり、このクラスは意味を持たない。
     */
    public static class ActionSpec {

        /** 未指定時に使う内部キー（basic-design.md 3.7）。 */
        public static final String DEFAULT_TYPE = "default";

        /** 任意の文字列。JSON ルートの {@code types} のキーと対応させる。省略可。 */
        public String type;

        /** アクション名。必須。 */
        public String label;

        /**
         * 遷移先の label 一覧（basic-design.md 3.3, v3.1）。
         * 1つのアクションから複数の矢印を出したい場合は複数指定する。
         */
        public List<String> next;

        public ActionSpec() {
        }

        /** 単一遷移先向けの簡易コンストラクタ。next が null/空なら遷移無しとして扱う。 */
        public ActionSpec(String type, String label, String next) {
            this.type = type;
            this.label = label;
            this.next = (next == null || next.isBlank()) ? null : List.of(next);
        }

        /** 複数遷移先向けのコンストラクタ（v3.1）。 */
        public ActionSpec(String type, String label, List<String> next) {
            this.type = type;
            this.label = label;
            this.next = next;
        }

        /** type が省略されている場合の解決済みキー。 */
        public String effectiveType() {
            return (type == null || type.isBlank()) ? DEFAULT_TYPE : type;
        }

        /** next のうち空でないものだけを返す（null/空文字を除外）。次が無ければ空リスト。 */
        public List<String> effectiveNextTargets() {
            if (next == null) {
                return List.of();
            }
            List<String> out = new ArrayList<>(next.size());
            for (String n : next) {
                if (n != null && !n.isBlank()) {
                    out.add(n);
                }
            }
            return out;
        }
    }

    /**
     * 大枠（kind）の見た目定義（basic-design.md 3.5章）。
     * 全フィールド任意。プログラム側は kind の意味を一切知らず、ここに書かれた色をそのまま使うだけ。
     */
    public static class KindStyle {

        public String headerBackground;
        public String background;
        public String border;
        public String textColor;

        public KindStyle() {
        }

        /** 中立フォールバック（basic-design.md 3.8章）。特定の kind の意味には偏らない灰色系。 */
        public static KindStyle defaults() {
            KindStyle s = new KindStyle();
            s.headerBackground = "#eef0f2";
            s.background = "#fbfcfd";
            s.border = "#c7ccd3";
            s.textColor = "#4a5057";
            return s;
        }

        /** this をベースに、override の非 null フィールドだけを反映した新インスタンスを返す。 */
        public KindStyle mergeWith(KindStyle o) {
            KindStyle r = new KindStyle();
            r.headerBackground = pick(o == null ? null : o.headerBackground, headerBackground);
            r.background = pick(o == null ? null : o.background, background);
            r.border = pick(o == null ? null : o.border, border);
            r.textColor = pick(o == null ? null : o.textColor, textColor);
            return r;
        }

        private static String pick(String override, String base) {
            return (override != null && !override.isBlank()) ? override : base;
        }
    }

    /**
     * アクション（type）の見た目定義（basic-design.md 3.6章）。
     * 全フィールド任意。
     */
    public static class ActionTypeStyle {

        public String background;
        public String border;
        public Double borderWidth;
        public String textColor;
        public String shadow;

        public ActionTypeStyle() {
        }

        /** 中立フォールバック（basic-design.md 3.8章）。 */
        public static ActionTypeStyle defaults() {
            ActionTypeStyle s = new ActionTypeStyle();
            s.background = "#ffffff";
            s.border = "#c7ccd3";
            s.borderWidth = 1.5;
            s.textColor = "#4a5057";
            s.shadow = null;
            return s;
        }

        /** this をベースに、override の非 null フィールドだけを反映した新インスタンスを返す。 */
        public ActionTypeStyle mergeWith(ActionTypeStyle o) {
            ActionTypeStyle r = new ActionTypeStyle();
            r.background = pick(o == null ? null : o.background, background);
            r.border = pick(o == null ? null : o.border, border);
            r.borderWidth = o != null && o.borderWidth != null ? o.borderWidth : borderWidth;
            r.textColor = pick(o == null ? null : o.textColor, textColor);
            r.shadow = o != null && o.shadow != null ? o.shadow : shadow;
            return r;
        }

        private static String pick(String override, String base) {
            return (override != null && !override.isBlank()) ? override : base;
        }
    }

    /**
     * レイアウト・タイポグラフィの設定（basic-design.md 4章）。
     * 色はここに含まれない。配色は {@link Palette} のソースコード上の定数が唯一の情報源であり、
     * JSON からは変更できない（ユーザー指示）。
     */
    public static class Theme {

        // --- 4.1 レイアウト系 ---
        public Integer nodeWidth;
        public Integer headerHeight;
        public Integer actionRowHeight;
        public Integer actionGap;
        public Integer nodePaddingTop;
        public Integer nodePaddingBottom;
        public Integer columnGap;
        public Integer rowGap;
        public Integer canvasPadding;

        // --- 4.2 表示系 ---
        public String fontFamily;
        public Integer titleFontSize;
        public Integer badgeFontSize;
        public Integer stateFontSize;
        public Integer actionFontSize;
        public String background;
        public String titleColor;
        public String nodeShadow;
        public Integer edgeWidth;
        public Integer arrowSize;
        public Boolean showLegend;

        public static Theme defaults() {
            Theme t = new Theme();
            t.nodeWidth = 240;
            t.headerHeight = 50;
            t.actionRowHeight = 34;
            t.actionGap = 10;
            t.nodePaddingTop = 14;
            t.nodePaddingBottom = 20;
            t.columnGap = 150;
            t.rowGap = 40;
            t.canvasPadding = 40;

            t.fontFamily = "\"Meiryo UI\",\"Yu Gothic UI\",\"Segoe UI\",Meiryo,\"Hiragino Kaku Gothic ProN\",sans-serif";
            t.titleFontSize = 20;
            t.badgeFontSize = 10;
            t.stateFontSize = 15;
            t.actionFontSize = 13;
            t.background = "#f7f8fa";
            t.titleColor = "#1f2933";
            t.nodeShadow = "0 2px 6px rgba(0,0,0,.10)";
            t.edgeWidth = 3;
            t.arrowSize = 9;
            t.showLegend = Boolean.TRUE;
            return t;
        }

        /** this（＝既定値）をベースに、override の非 null フィールドだけを反映した新インスタンスを返す。 */
        public Theme mergeWith(Theme o) {
            Theme r = new Theme();
            r.nodeWidth = pick(o == null ? null : o.nodeWidth, nodeWidth);
            r.headerHeight = pick(o == null ? null : o.headerHeight, headerHeight);
            r.actionRowHeight = pick(o == null ? null : o.actionRowHeight, actionRowHeight);
            r.actionGap = pick(o == null ? null : o.actionGap, actionGap);
            r.nodePaddingTop = pick(o == null ? null : o.nodePaddingTop, nodePaddingTop);
            r.nodePaddingBottom = pick(o == null ? null : o.nodePaddingBottom, nodePaddingBottom);
            r.columnGap = pick(o == null ? null : o.columnGap, columnGap);
            r.rowGap = pick(o == null ? null : o.rowGap, rowGap);
            r.canvasPadding = pick(o == null ? null : o.canvasPadding, canvasPadding);

            r.fontFamily = pickStr(o == null ? null : o.fontFamily, fontFamily);
            r.titleFontSize = pick(o == null ? null : o.titleFontSize, titleFontSize);
            r.badgeFontSize = pick(o == null ? null : o.badgeFontSize, badgeFontSize);
            r.stateFontSize = pick(o == null ? null : o.stateFontSize, stateFontSize);
            r.actionFontSize = pick(o == null ? null : o.actionFontSize, actionFontSize);
            r.background = pickStr(o == null ? null : o.background, background);
            r.titleColor = pickStr(o == null ? null : o.titleColor, titleColor);
            r.nodeShadow = pickStr(o == null ? null : o.nodeShadow, nodeShadow);
            r.edgeWidth = pick(o == null ? null : o.edgeWidth, edgeWidth);
            r.arrowSize = pick(o == null ? null : o.arrowSize, arrowSize);
            r.showLegend = o != null && o.showLegend != null ? o.showLegend : showLegend;
            return r;
        }

        private static Integer pick(Integer override, Integer base) {
            return override != null ? override : base;
        }

        private static String pickStr(String override, String base) {
            return (override != null && !override.isBlank()) ? override : base;
        }
    }

    /** レイアウト結果（basic-design.md 6章）。座標はすべて px。 */
    public static class Layout {

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

    // ============================================================
    // 内部処理クラス（basic-design.md 5〜9章）
    // ============================================================

    /** 入力検証（basic-design.md 5章）。全違反をまとめて返す。 */
    public static final class FlowValidator {

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

    /**
     * JSON の {@code kinds}/{@code types} と、実際に使われている kind/type キーの集合から、
     * このリクエスト1回分だけで使う最終的なスタイル一覧を作る（basic-design.md 7章）。
     * プログラムは kind/type の意味を一切知らない。渡された定義をそのまま解決するだけ。
     */
    public static final class StyleRegistry {

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

    /** 座標計算（basic-design.md 6章）。 */
    public static class LayoutEngine {

        private static final int LEGEND_HEIGHT = 76;

        /** 確定済みの終点を持つエッジ候補（basic-design.md 6.4のレーン割り当て用）。 */
        private record EdgeCandidate(int sx, int sy, Layout.NodeBox to, int ty) {
        }

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
                List<ActionSpec> boxActions = s.safeActions();
                for (ActionSpec a : boxActions) {
                    box.actionRowHeights.add(actionRowHeight(a));
                }
                box.height = nodeHeight(boxActions, box.actionRowHeights);
                placeInColumn(box, nextYByColumn, topY);
                layout.nodes.add(box);
                boxes.put(s.label, box);
            }

            // 迂回レーン（6.4.1）は実ノードより下を通す。複製ノードを足す前のこの時点の最下端を基準にする
            int realNodesMaxBottom = 0;
            for (Layout.NodeBox n : layout.nodes) {
                realNodesMaxBottom = Math.max(realNodesMaxBottom, n.bottom());
            }
            int skipLaneIndex = 0;

            // --- エッジ（前進のみ）＋ 後退辺・自己ループの複製ノード ---
            for (StateSpec s : states) {
                Layout.NodeBox from = boxes.get(s.label);
                List<ActionSpec> actions = s.safeActions();

                // まず終点（実ボックス／複製ノード）だけ確定させる（複製ノードはここで作る）。
                // レーンの割り当ては後段でまとめて行う（basic-design.md 6.4）
                List<EdgeCandidate> candidates = new ArrayList<>();
                for (int j = 0; j < actions.size(); j++) {
                    ActionSpec a = actions.get(j);
                    int sx = from.right();
                    int sy = actionAnchorY(from, j);
                    for (String next : a.effectiveNextTargets()) {
                        Layout.NodeBox target = boxes.get(next);

                        Layout.NodeBox to;
                        boolean skipsColumns;
                        if (target.column > from.column) {
                            to = target; // 前進: 実ボックスへ
                            skipsColumns = (target.column - from.column) > 1;
                        } else {
                            // 後退辺・自己ループ（basic-design.md 6.5）: 複製ノードを新規に右側へ配置
                            to = cloneNode(target.state, from.column + 1, nextYByColumn, topY);
                            layout.nodes.add(to);
                            skipsColumns = false; // 複製は常に起点の隣の列
                        }
                        int ty = to.clone ? to.y + CLONE_TEXT_HEIGHT / 2 : to.y + theme.headerHeight / 2;

                        if (skipsColumns) {
                            // 6.4.1: 中間列のノードの背後を通らないよう、迂回レーンを経由させる
                            int laneY = realNodesMaxBottom + SKIP_LANE_GAP + skipLaneIndex * SKIP_LANE_PITCH;
                            skipLaneIndex++;
                            Layout.EdgeRoute e = new Layout.EdgeRoute();
                            e.fromStateLabel = s.label;
                            routeSkip(e, sx, sy, to.x, ty, laneY, skipLaneIndex);
                            e.arrowX = to.x;
                            e.arrowY = ty;
                            layout.edges.add(e);
                        } else {
                            candidates.add(new EdgeCandidate(sx, sy, to, ty));
                        }
                    }
                }

                // 同じ起点から出る複数のエッジが縦の幹線で重なって描かれないよう、レーンをずらす（basic-design.md 6.4）。
                // 非交差マッチングの定石: 終点yが大きい（下にある）ものほどレーン0（起点寄り＝内側）、
                // 終点yが小さい（上にある）ものほどレーン番号を大きく（終点寄り＝外側）する。
                // 逆順（yが小さい順にレーン0から）にすると、下のペアの水平区間が上のペアの垂直区間と交差する
                candidates.sort(Comparator.comparingInt(EdgeCandidate::ty).reversed());
                int laneCount = candidates.size();
                for (int lane = 0; lane < laneCount; lane++) {
                    EdgeCandidate c = candidates.get(lane);
                    Layout.EdgeRoute e = new Layout.EdgeRoute();
                    e.fromStateLabel = s.label;
                    routeForward(e, c.sx(), c.sy(), c.to().x, c.ty(), lane, laneCount);
                    e.arrowX = c.to().x;
                    e.arrowY = c.ty();
                    layout.edges.add(e);
                }
            }

            int maxRight = 0;
            int maxBottom = 0;
            for (Layout.NodeBox n : layout.nodes) {
                maxRight = Math.max(maxRight, n.right());
                maxBottom = Math.max(maxBottom, n.bottom());
            }
            if (skipLaneIndex > 0) {
                int lastLaneY = realNodesMaxBottom + SKIP_LANE_GAP + (skipLaneIndex - 1) * SKIP_LANE_PITCH;
                maxBottom = Math.max(maxBottom, lastLaneY + SKIP_LANE_GAP);
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

        /** ノード高さ（basic-design.md 6.1）。全チップが1行に収まる前提の単純式（テスト・既定計算用）。 */
        public int nodeHeight(int actionCount) {
            if (actionCount == 0) {
                return theme.headerHeight;
            }
            return theme.headerHeight + theme.nodePaddingTop
                    + theme.actionRowHeight * actionCount + theme.actionGap * (actionCount - 1)
                    + theme.nodePaddingBottom;
        }

        /** ノード高さ（basic-design.md 6.1）。チップごとの実高さ（折り返し考慮）を積み上げる版。 */
        private int nodeHeight(List<ActionSpec> actions, List<Integer> rowHeights) {
            if (actions.isEmpty()) {
                return theme.headerHeight;
            }
            int sum = 0;
            for (int h : rowHeights) {
                sum += h;
            }
            return theme.headerHeight + theme.nodePaddingTop
                    + sum + theme.actionGap * (actions.size() - 1)
                    + theme.nodePaddingBottom;
        }

        /** アクションチップ j の中央 y（矢印の起点）。それより前のチップの実高さを積み上げる（basic-design.md 6.4）。 */
        public int actionAnchorY(Layout.NodeBox box, int j) {
            int y = box.y + theme.headerHeight + theme.nodePaddingTop;
            for (int i = 0; i < j; i++) {
                y += box.actionRowHeights.get(i) + theme.actionGap;
            }
            return y + box.actionRowHeights.get(j) / 2;
        }

        /**
         * アクションチップ1件の実高さ（basic-design.md 6.1.1）。ラベルがチップ幅に収まらない場合は
         * 折り返し表示になるため、概算の行数から高さを見積もる。フォントメトリクスは使わず、
         * 全角相当の文字を1.0em・それ以外を0.55emとして幅を積算する簡易推定。
         */
        private int actionRowHeight(ActionSpec a) {
            int availableWidth = theme.nodeWidth - ACTION_LABEL_H_RESERVE;
            if (availableWidth <= 0) {
                return theme.actionRowHeight;
            }
            int estimatedTextWidth = estimateTextWidthPx(a.label, theme.actionFontSize);
            int lines = Math.max(1, (int) Math.ceil(estimatedTextWidth / (double) availableWidth));
            if (lines <= 1) {
                return theme.actionRowHeight;
            }
            int lineHeight = (int) Math.round(theme.actionFontSize * 1.35);
            return Math.max(theme.actionRowHeight, lines * lineHeight + ACTION_LABEL_V_PADDING);
        }

        private static int estimateTextWidthPx(String text, int fontSizePx) {
            double widthEm = 0;
            for (int i = 0; i < text.length(); i++) {
                widthEm += isWideChar(text.charAt(i)) ? 1.0 : 0.55;
            }
            return (int) Math.ceil(widthEm * fontSizePx);
        }

        /** 全角相当（CJK統合漢字・ひらがな・カタカナ・全角記号など）かどうかの簡易判定。 */
        private static boolean isWideChar(char c) {
            return (c >= 0x1100 && c <= 0x115F)
                    || (c >= 0x2E80 && c <= 0xA4CF)
                    || (c >= 0xAC00 && c <= 0xD7A3)
                    || (c >= 0xF900 && c <= 0xFAFF)
                    || (c >= 0xFF00 && c <= 0xFF60)
                    || (c >= 0xFFE0 && c <= 0xFFE6);
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
                    for (String next : a.effectiveNextTargets()) {
                        if (next.equals(states.get(i).label)) {
                            continue; // 自己ループは列割り当てに使わない
                        }
                        Integer ti = indexByLabel.get(next);
                        if (ti != null && ti != i) {
                            adj.get(i).add(ti);
                        }
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
        private static final int SKIP_LANE_GAP = 20;
        private static final int SKIP_LANE_PITCH = 14;
        private static final int SKIP_LANE_STAGGER = 8;
        public static final int CLONE_TEXT_HEIGHT = 28;
        /** アクションラベル用の利用可能幅を見積もる際に引く分（node-actions左右padding20 + action左右padding24 + chevron予約20）。 */
        private static final int ACTION_LABEL_H_RESERVE = 64;
        private static final int ACTION_LABEL_V_PADDING = 12;

        /**
         * 前進エッジの経路（basic-design.md 6.4）。折れ位置は終点寄り（v3.1）。
         * 起点直後は各アクションの行の高さのまま水平に伸び、終点の手前で初めて縦に曲がることで、
         * 同じ起点から出る複数のエッジが長い区間で重なるのを防ぐ。
         */
        private void routeForward(Layout.EdgeRoute e, int sx, int sy, int tx, int ty, int lane, int laneCount) {
            if (sy == ty) {
                e.segments.add(Layout.Segment.h(sx, tx, sy));
                return;
            }
            int bendFromTarget = Math.max(16, theme.columnGap / 4);
            double center = (laneCount - 1) / 2.0;
            int mx = tx - bendFromTarget + (int) Math.round((lane - center) * EDGE_LANE_STEP);
            mx = Math.max(sx + 12, Math.min(mx, tx - 12));
            e.segments.add(Layout.Segment.h(sx, mx, sy));
            e.segments.add(Layout.Segment.v(sy, ty, mx));
            e.segments.add(Layout.Segment.h(mx, tx, ty));
        }

        /**
         * 列を2つ以上飛び越すエッジの迂回経路（basic-design.md 6.4.1）。
         * 中間列にあるノードの背後を通らないよう、すべての実ノードより下の迂回レーンを経由する。
         */
        private void routeSkip(Layout.EdgeRoute e, int sx, int sy, int tx, int ty, int laneY, int laneOrdinal) {
            int outX = sx + 20 + laneOrdinal * SKIP_LANE_STAGGER;
            int inX = tx - 20 - laneOrdinal * SKIP_LANE_STAGGER;
            e.segments.add(Layout.Segment.h(sx, outX, sy));
            e.segments.add(Layout.Segment.v(sy, laneY, outX));
            e.segments.add(Layout.Segment.h(outX, inX, laneY));
            e.segments.add(Layout.Segment.v(laneY, ty, inX));
            e.segments.add(Layout.Segment.h(inX, tx, ty));
        }
    }

    /**
     * 図本体の HTML（div 構造）を組み立てる（basic-design.md 8章）。
     * 画像（SVG / PNG / canvas）は一切使わない。
     * kind/type の色は {@link StyleRegistry} が解決した結果をそのままCSS化するだけで、
     * このクラスは kind/type の意味を一切知らない。
     */
    public static class HtmlDiagramRenderer {

        private final Theme theme;
        private final Map<String, KindStyle> kindStyles;
        private final Map<String, ActionTypeStyle> typeStyles;

        public HtmlDiagramRenderer(Theme theme, Map<String, KindStyle> kindStyles,
                Map<String, ActionTypeStyle> typeStyles) {
            this.theme = theme;
            this.kindStyles = kindStyles;
            this.typeStyles = typeStyles;
        }

        /** `<div class="fd-canvas">…</div>` を返す。 */
        public String render(Layout layout) {
            StringBuilder sb = new StringBuilder(8192);
            sb.append("<div class=\"fd-canvas\" style=\"width:").append(layout.canvasWidth)
              .append("px;height:").append(layout.canvasHeight).append("px\">\n");

            renderKindTypeCss(sb);

            if (!layout.title.isEmpty()) {
                sb.append("  <h1 class=\"diagram-title\" style=\"left:").append(theme.canvasPadding)
                  .append("px;top:").append(theme.canvasPadding).append("px\">")
                  .append(Html.esc(layout.title)).append("</h1>\n");
            }

            for (Layout.EdgeRoute e : layout.edges) {
                renderEdge(sb, e);
            }
            for (Layout.NodeBox n : layout.nodes) {
                renderNode(sb, n);
            }
            renderLegend(sb, layout);
            renderCloneHighlightCss(sb, layout);

            sb.append("</div>\n");
            return sb.toString();
        }

        /**
         * このリクエストで実際に使われている kind/type ごとに、色のCSSを出力する（basic-design.md 7章）。
         * JSON の {@code kinds}/{@code types} と既定値をマージ済みの {@link #kindStyles}/{@link #typeStyles}
         * をそのままCSS化する。キー文字列は {@link Html#labelToken} でハッシュ化してクラス名にする。
         */
        private void renderKindTypeCss(StringBuilder sb) {
            sb.append("  <style>\n");
            for (Map.Entry<String, KindStyle> e : kindStyles.entrySet()) {
                String token = Html.labelToken(e.getKey());
                KindStyle s = e.getValue();
                sb.append("    .fd-canvas .node.kind-").append(token).append("{background:")
                  .append(Html.cssValue(s.background, "#fbfcfd")).append(";border-color:")
                  .append(Html.cssValue(s.border, "#c7ccd3")).append(";}\n");
                sb.append("    .fd-canvas .node.kind-").append(token).append(" .node-header{background:")
                  .append(Html.cssValue(s.headerBackground, "#eef0f2")).append(";color:")
                  .append(Html.cssValue(s.textColor, "#4a5057")).append(";}\n");
                sb.append("    .fd-canvas .node.kind-").append(token).append(" .kind-badge{color:")
                  .append(Html.cssValue(s.textColor, "#4a5057")).append(";}\n");
            }
            for (Map.Entry<String, ActionTypeStyle> e : typeStyles.entrySet()) {
                String token = Html.labelToken(e.getKey());
                ActionTypeStyle s = e.getValue();
                sb.append("    .fd-canvas .action.type-").append(token).append("{background:")
                  .append(Html.cssValue(s.background, "#ffffff")).append(";color:")
                  .append(Html.cssValue(s.textColor, "#4a5057")).append(";border:")
                  .append(Html.px(s.borderWidth == null ? 1.5 : s.borderWidth)).append(" solid ")
                  .append(Html.cssValue(s.border, "#c7ccd3")).append(";");
                if (s.shadow != null && !s.shadow.isBlank()) {
                    sb.append("box-shadow:").append(Html.cssValue(s.shadow, "none")).append(";");
                }
                sb.append("}\n");
            }
            sb.append("  </style>\n");
        }

        /**
         * 複製ボックスにカーソルを合わせたとき、対応する元のボックスも一緒に強調するための
         * 図固有の CSS（basic-design.md 9章）。JS は使わず `:has()` の前方兄弟参照で実現する。
         * label をそのまま CSS 属性値に埋め込むと壊れうるため、{@link Html#labelToken} で
         * ハッシュ化したトークンをクラス名として使い、実際の label 文字列は CSS に出さない。
         */
        private void renderCloneHighlightCss(StringBuilder sb, Layout layout) {
            Set<String> labelsWithClone = new LinkedHashSet<>();
            for (Layout.NodeBox n : layout.nodes) {
                if (n.clone) {
                    labelsWithClone.add(Html.labelToken(n.state.label));
                }
            }
            if (labelsWithClone.isEmpty()) {
                return;
            }
            sb.append("  <style>\n");
            for (String token : labelsWithClone) {
                sb.append("    .fd-canvas .node.lbl-").append(token)
                  .append(":has(~ .node-clone-ref.lbl-").append(token)
                  .append(":hover){outline-color:var(--edge-color);}\n");
            }
            sb.append("  </style>\n");
        }

        private void renderEdge(StringBuilder sb, Layout.EdgeRoute e) {
            sb.append("  <div class=\"edge\" data-from=\"").append(Html.esc(e.fromStateLabel)).append("\">\n");
            List<Layout.Segment> segs = e.segments;
            for (int i = 0; i < segs.size(); i++) {
                Layout.Segment s = segs.get(i);
                Layout.Segment prev = i > 0 ? segs.get(i - 1) : null;
                Layout.Segment next = i < segs.size() - 1 ? segs.get(i + 1) : null;
                int startX = s.x;
                int startY = s.y;
                int endX = s.horizontal ? s.x + s.width : s.x;
                int endY = s.horizontal ? s.y : s.y + s.height;
                boolean extendStart = touchesEndpoint(startX, startY, prev) || touchesEndpoint(startX, startY, next);
                boolean extendEnd = touchesEndpoint(endX, endY, prev) || touchesEndpoint(endX, endY, next);
                // 矢印に接続する最後のセグメントは、矢印の先端（arrowX/arrowY）ではなく根元で止める
                // （basic-design.md 9章）。三角形は先端に近づくほど細くなるため、線をそのまま先端まで
                // 伸ばすと先端付近で三角形の斜辺からはみ出して見える。
                boolean isLast = i == segs.size() - 1;

                if (s.horizontal) {
                    int drawWidth = isLast ? Math.max(0, s.width - theme.arrowSize) : s.width;
                    String left = extendStart ? "calc(" + s.x + "px - var(--edge-w) / 2)" : s.x + "px";
                    String width = extendWidth(drawWidth, extendStart, extendEnd);
                    sb.append("    <div class=\"seg h\" style=\"left:").append(left)
                      .append(";top:calc(").append(s.y).append("px - var(--edge-w) / 2);width:").append(width)
                      .append("\"></div>\n");
                } else {
                    int drawHeight = isLast ? Math.max(0, s.height - theme.arrowSize) : s.height;
                    String top = extendStart ? "calc(" + s.y + "px - var(--edge-w) / 2)" : s.y + "px";
                    String height = extendWidth(drawHeight, extendStart, extendEnd);
                    sb.append("    <div class=\"seg v\" style=\"left:calc(").append(s.x)
                      .append("px - var(--edge-w) / 2);top:").append(top).append(";height:").append(height)
                      .append("\"></div>\n");
                }
            }
            sb.append("    <div class=\"arrow\" style=\"left:calc(").append(e.arrowX)
              .append("px - var(--arrow-size));top:calc(").append(e.arrowY)
              .append("px - var(--arrow-size) * 0.6)\"></div>\n");
            sb.append("  </div>\n");
        }

        /**
         * (x,y) が隣接セグメント {@code other} のどちらかの端点と一致するか（basic-design.md 9章）。
         * 一致する＝経路内部の継ぎ目なので、その端は `--edge-w` 分延長してよい。
         * 一致しない＝経路全体の起点（発火元ボックス側）または終点（矢印の先端）なので延長しない。
         */
        private static boolean touchesEndpoint(int x, int y, Layout.Segment other) {
            if (other == null) {
                return false;
            }
            int oStartX = other.x;
            int oStartY = other.y;
            int oEndX = other.horizontal ? other.x + other.width : other.x;
            int oEndY = other.horizontal ? other.y : other.y + other.height;
            return (x == oStartX && y == oStartY) || (x == oEndX && y == oEndY);
        }

        private static String extendWidth(int base, boolean extendStart, boolean extendEnd) {
            if (extendStart && extendEnd) {
                return "calc(" + base + "px + var(--edge-w))";
            }
            if (extendStart || extendEnd) {
                return "calc(" + base + "px + var(--edge-w) / 2)";
            }
            return base + "px";
        }

        private void renderCloneRef(StringBuilder sb, Layout.NodeBox n) {
            sb.append("  <div class=\"node-clone-ref lbl-").append(Html.labelToken(n.state.label))
              .append("\" style=\"left:").append(n.x).append("px;top:").append(n.y)
              .append("px;width:").append(n.width).append("px\">")
              .append(Html.esc(n.state.label)).append("</div>\n");
        }

        private void renderNode(StringBuilder sb, Layout.NodeBox n) {
            if (n.clone) {
                renderCloneRef(sb, n);
                return;
            }
            StateSpec s = n.state;
            String kindClass = "kind-" + Html.labelToken(s.effectiveKind());
            String badge = s.effectiveKind();

            sb.append("  <div class=\"node ").append(kindClass)
              .append(" lbl-").append(Html.labelToken(s.label))
              .append("\" data-label=\"").append(Html.esc(s.label)).append('"');
            sb.append(" style=\"left:").append(n.x).append("px;top:").append(n.y)
              .append("px;width:").append(n.width).append("px;height:").append(n.height)
              .append("px\">\n");

            sb.append("    <div class=\"node-header\">")
              .append("<span class=\"kind-badge\">").append(Html.esc(badge)).append("</span>")
              .append("<span class=\"state-label\">").append(Html.esc(s.label)).append("</span>")
              .append("</div>\n");

            List<ActionSpec> actions = s.safeActions();
            if (!actions.isEmpty()) {
                sb.append("    <div class=\"node-actions\">\n");
                for (int j = 0; j < actions.size(); j++) {
                    ActionSpec a = actions.get(j);
                    String typeClass = "type-" + Html.labelToken(a.effectiveType());
                    sb.append("      <div class=\"action ").append(typeClass)
                      .append("\" style=\"min-height:").append(n.actionRowHeights.get(j)).append("px\">")
                      .append("<span class=\"action-label\">").append(Html.esc(a.label)).append("</span>");
                    if (!a.effectiveNextTargets().isEmpty()) {
                        sb.append("<span class=\"chev\">&rsaquo;</span>");
                    }
                    sb.append("</div>\n");
                }
                sb.append("    </div>\n");
            }
            sb.append("  </div>\n");
        }

        /** 実際に使われている kind/type を動的に列挙する（basic-design.md 8章）。 */
        private void renderLegend(StringBuilder sb, Layout layout) {
            if (!Boolean.TRUE.equals(theme.showLegend)) {
                return;
            }
            int top = layout.canvasHeight - theme.canvasPadding - 20;
            sb.append("  <div class=\"legend\" style=\"left:").append(theme.canvasPadding)
              .append("px;top:").append(top).append("px\">\n");

            for (Map.Entry<String, KindStyle> e : kindStyles.entrySet()) {
                KindStyle s = e.getValue();
                sb.append("    <span class=\"item\"><span class=\"swatch\" style=\"background:")
                  .append(Html.cssValue(s.headerBackground, "#eef0f2")).append("\"></span>")
                  .append(Html.esc(e.getKey())).append("</span>\n");
            }
            for (Map.Entry<String, ActionTypeStyle> e : typeStyles.entrySet()) {
                ActionTypeStyle s = e.getValue();
                sb.append("    <span class=\"item\"><span class=\"chip\" style=\"background:")
                  .append(Html.cssValue(s.background, "#ffffff")).append(";border-color:")
                  .append(Html.cssValue(s.border, "#c7ccd3")).append("\"></span>")
                  .append(Html.esc(e.getKey())).append("</span>\n");
            }
            sb.append("    <span class=\"item\"><span class=\"bar\"></span>遷移</span>\n");
            sb.append("  </div>\n");

            int hintTop = top + LEGEND_HINT_OFFSET;
            sb.append("  <div class=\"legend-hint\" style=\"left:").append(theme.canvasPadding)
              .append("px;top:").append(hintTop).append("px\">")
              .append(Html.esc(LEGEND_HINT_TEXT)).append("</div>\n");
        }

        private static final String LEGEND_HINT_TEXT =
                "変更を反映するには再度プレビューボタンを押してプレビューを生成してください";
        private static final int LEGEND_HINT_OFFSET = 24;
    }

    /**
     * 完結した HTML ページ / 埋め込み用フラグメントを組み立てる（basic-design.md 8章）。
     * 外部 CDN・外部 CSS・画像を一切参照しない。
     */
    public static class HtmlPageWriter {

        private final Theme theme;

        public HtmlPageWriter(Theme theme) {
            this.theme = theme;
        }

        /** ブラウザでそのまま開ける1枚もの HTML。JSは一切含まない（basic-design.md 9章）。 */
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
                    %CSS%
                    </style>
                    </head>
                    <body>
                    <div class="fd-root">
                    %CANVAS%
                    </div>
                    </body>
                    </html>
                    """
                    .replace("%TITLE%", Html.esc(pageTitle))
                    .replace("%BG%", Html.cssValue(theme.background, "#f7f8fa"))
                    .replace("%CSS%", css)
                    .replace("%CANVAS%", canvasHtml);
        }
    }

    /**
     * テーマから CSS を組み立てる（basic-design.md 4章・8章）。
     * レイアウト・タイポグラフィは Theme（CSS カスタムプロパティ）から、
     * kind/type に紐付かない色（関係線）は {@link Palette} のソース定数から直接埋め込む。
     * **kind/type ごとの色はここでは出さない**（リクエストごとに可変のため、
     * {@link HtmlDiagramRenderer} が {@link StyleRegistry} の解決結果から動的に出力する。basic-design.md 7章）。
     */
    public static class CssBuilder {

        private final Theme theme;

        public CssBuilder(Theme theme) {
            this.theme = theme;
        }

        public String build() {
            StringBuilder sb = new StringBuilder(4096);
            rootVariables(sb);
            structure(sb);
            return sb.toString();
        }

        private void rootVariables(StringBuilder sb) {
            sb.append(".fd-root{")
              .append("--node-width:").append(theme.nodeWidth).append("px;")
              .append("--header-height:").append(theme.headerHeight).append("px;")
              .append("--action-height:").append(theme.actionRowHeight).append("px;")
              .append("--action-gap:").append(theme.actionGap).append("px;")
              .append("--node-padding-top:").append(theme.nodePaddingTop).append("px;")
              .append("--node-padding-bottom:").append(theme.nodePaddingBottom).append("px;")
              .append("--font-family:").append(Html.cssValue(theme.fontFamily, "sans-serif")).append(';')
              .append("--title-font-size:").append(theme.titleFontSize).append("px;")
              .append("--badge-font-size:").append(theme.badgeFontSize).append("px;")
              .append("--state-font-size:").append(theme.stateFontSize).append("px;")
              .append("--action-font-size:").append(theme.actionFontSize).append("px;")
              .append("--bg:").append(Html.cssValue(theme.background, "#f7f8fa")).append(';')
              .append("--title-color:").append(Html.cssValue(theme.titleColor, "#1f2933")).append(';')
              .append("--node-shadow:").append(Html.cssValue(theme.nodeShadow, "none")).append(';')
              .append("--edge-w:").append(theme.edgeWidth).append("px;")
              .append("--arrow-size:").append(theme.arrowSize).append("px;")
              .append("--edge-color:").append(Palette.EDGE_COLOR).append(';')
              .append("}\n");
        }

        private void structure(StringBuilder sb) {
            sb.append("""
                    .fd-root{font-family:var(--font-family);background:var(--bg);color:#2d3748;}
                    .fd-root *{box-sizing:border-box;}
                    .fd-canvas{position:relative;transform-origin:0 0;background:var(--bg);}
                    .fd-canvas .diagram-title{position:absolute;left:0;top:0;margin:0;\
                    font-size:var(--title-font-size);font-weight:700;color:var(--title-color);\
                    letter-spacing:.02em;}

                    /* --- 大枠。色は kind ごとに動的CSS（HtmlDiagramRenderer）で決まる --- */
                    .fd-canvas .node{position:absolute;z-index:2;border-radius:10px;\
                    box-shadow:var(--node-shadow);overflow:hidden;border:1.5px solid transparent;\
                    transition:outline-color .15s;outline:3px solid transparent;outline-offset:3px;}
                    /* --- 複製参照テキスト（後退辺・自己ループの終端） --- */
                    .fd-canvas .node-clone-ref{position:absolute;z-index:2;display:flex;\
                    align-items:center;gap:4px;font-size:var(--state-font-size);color:#6b7280;\
                    font-style:italic;cursor:default;white-space:nowrap;overflow:hidden;\
                    text-overflow:ellipsis;padding:0 6px;}
                    .fd-canvas .node-clone-ref::before{content:"↩";font-style:normal;flex-shrink:0;}
                    .fd-canvas .node-clone-ref:hover{color:var(--edge-color);}
                    .fd-canvas .node-header{min-height:var(--header-height);display:flex;\
                    flex-direction:column;justify-content:center;gap:2px;padding:6px 12px;}
                    .fd-canvas .kind-badge{align-self:flex-start;\
                    font-size:var(--badge-font-size);font-weight:700;letter-spacing:.03em;\
                    padding:1px 7px;border-radius:999px;background:rgba(255,255,255,.55);}
                    .fd-canvas .state-label{font-size:var(--state-font-size);font-weight:700;\
                    white-space:nowrap;overflow:hidden;text-overflow:ellipsis;}

                    /* --- アクション。色は type ごとに動的CSS（HtmlDiagramRenderer）で決まる --- */
                    .fd-canvas .node-actions{display:flex;flex-direction:column;gap:var(--action-gap);\
                    padding:var(--node-padding-top) 10px var(--node-padding-bottom);}
                    .fd-canvas .action{min-height:var(--action-height);\
                    display:flex;align-items:center;justify-content:space-between;gap:8px;\
                    padding:4px 12px;border-radius:8px;font-size:var(--action-font-size);}
                    .fd-canvas .action .action-label{flex:1 1 auto;white-space:normal;\
                    overflow-wrap:anywhere;line-height:1.35;}
                    .fd-canvas .action .chev{flex:0 0 auto;font-weight:700;opacity:.7;}

                    /* --- 関係線（すべて薄い青の実線で統一） --- */
                    .fd-canvas .edge{position:absolute;left:0;top:0;z-index:1;}
                    .fd-canvas .seg{position:absolute;}
                    .fd-canvas .seg.h{border-top:var(--edge-w) solid var(--edge-color);}
                    .fd-canvas .seg.v{border-left:var(--edge-w) solid var(--edge-color);}
                    .fd-canvas .arrow{position:absolute;width:0;height:0;\
                    border-left:var(--arrow-size) solid var(--edge-color);\
                    border-top:calc(var(--arrow-size) * .6) solid transparent;\
                    border-bottom:calc(var(--arrow-size) * .6) solid transparent;}

                    /* --- 凡例。kind/typeの色は各項目のinline styleで指定する --- */
                    .fd-canvas .legend{position:absolute;display:flex;flex-wrap:wrap;gap:18px;\
                    align-items:center;font-size:var(--action-font-size);color:#4a5568;}
                    .fd-canvas .legend .item{display:flex;align-items:center;gap:6px;}
                    .fd-canvas .legend .swatch{display:inline-block;width:14px;height:14px;\
                    border-radius:4px;border:1px solid rgba(0,0,0,.12);}
                    .fd-canvas .legend .chip{display:inline-block;width:26px;height:14px;border-radius:4px;\
                    border:1.5px solid rgba(0,0,0,.12);}
                    .fd-canvas .legend .bar{display:inline-block;width:26px;\
                    border-top:var(--edge-w) solid var(--edge-color);}
                    .fd-canvas .legend-hint{position:absolute;font-size:var(--action-font-size);\
                    color:#718096;font-style:italic;}
                    """);
        }
    }

    /**
     * kind/type に紐付かない、図全体で共通の色定義（basic-design.md 6.7章）。
     * kind/type ごとの色は JSON ルートの {@code kinds}/{@code types} で指定する
     * （{@link KindStyle}/{@link ActionTypeStyle}）。
     * ここに残っているのは、どの kind/type にも属さない色のみ。
     */
    public static final class Palette {

        private Palette() {
        }

        // --- 関係線（すべて薄い青の実線で統一） ---
        public static final String EDGE_COLOR = "#7fb8ee";
    }

    /** HTML/CSS 出力ユーティリティ。 */
    public static final class Html {

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
}
