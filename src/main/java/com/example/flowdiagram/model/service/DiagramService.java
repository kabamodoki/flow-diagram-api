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
 * 状態遷移図を1枚のHTMLとして組み立てる中心クラス。入力データのチェックから、箱や矢印を
 * どこに置くかの位置計算、色やCSSの組み立て、最終的なHTMLの生成までを一手に引き受ける。
 * 呼び出す側（APIの窓口）はこのクラスの {@link #renderPage} を呼ぶだけでよく、内部で
 * どういう手順で組み立てているかを知る必要はない。
 *
 * <p>処理に必要なデータの入れ物（ステータス・アクション・レイアウト結果など）や、各工程を
 * 担当する小さなクラス（検証・レイアウト計算・CSS生成・HTML生成など）は、すべてこのクラスの
 * 内側にまとめて置いている。</p>
 */
@Service
public class DiagramService {

    // ============================================================
    // 外から呼ばれる唯一の入り口
    // ============================================================

    /**
     * 状態遷移図1件分のデータを受け取り、ブラウザでそのまま開ける完結したHTMLを1枚返す。
     * 処理の流れ: ①入力データのチェック → ②見た目設定の確定 → ③箱や矢印の配置計算 →
     * ④使われている色の解決 → ⑤CSSの組み立て → ⑥HTML本体の組み立て → ⑦1ページにまとめる。
     */
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
    // ここから下は、処理の途中で使うデータの入れ物（クラス）たち。
    // リクエストで受け取るデータの入れ物（api側）とは別物で、こちらはJSONの形を
    // 気にせず処理しやすい形にしたもの。
    // ============================================================

    /** 状態遷移図1件分のデータ全体。タイトル・見た目設定・ステータス一覧を持つ。 */
    public static class FlowSpec {

        public String title;

        /** 見た目の部分上書き。指定しなければ既定の見た目になる。 */
        public Theme theme;

        /** ステータスの種類（kind）ごとの見た目定義。未指定可。 */
        public Map<String, KindStyle> kinds;

        /** アクションの種類（type）ごとの見た目定義。未指定可。 */
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
     * 図に表示する1つの箱（ステータス、または手続き）。IDのような専用の識別子は持たず、
     * {@link #label}（表示名）がそのまま箱を特定するためのキーも兼ねる（図の中で重複不可）。
     * アクションを何も持たなければ、矢印の行き着く先となる終端の箱として扱われる。
     */
    public static class StateSpec {

        /** kindが指定されなかったときに使う種類名。 */
        public static final String DEFAULT_KIND = "default";

        /** 箱に表示する名前。図の中で一意である必要があり、他の箱から参照されるときの名前にもなる。 */
        public String label;

        /** 箱の種類（例: 「ステータス」「手続き」など自由な文字列）。見た目の色分けに使う。省略可。 */
        public String kind;

        /** 箱の中に並ぶボタン（アクション）の一覧。空なら、矢印が到達するだけの終端の箱になる。 */
        public List<ActionSpec> actions = new ArrayList<>();

        public StateSpec() {
        }

        public StateSpec(String label) {
            this.label = label;
        }

        public List<ActionSpec> safeActions() {
            return actions == null ? List.of() : actions;
        }

        /** kindが省略されていれば既定の種類名を、指定されていればそのまま返す。 */
        public String effectiveKind() {
            return (kind == null || kind.isBlank()) ? DEFAULT_KIND : kind;
        }
    }

    /**
     * 箱の中にある1つのボタン（アクション）。押した後にどの箱へ遷移するかを持つ。
     * 種類（type）は自由な文字列で、見た目の色分けに使う以外の意味は持たない。
     */
    public static class ActionSpec {

        /** typeが指定されなかったときに使う種類名。 */
        public static final String DEFAULT_TYPE = "default";

        /** ボタンの種類（例: 「button」「flow」など自由な文字列）。見た目の色分けに使う。省略可。 */
        public String type;

        /** ボタンに表示する名前。必須。 */
        public String label;

        /**
         * 遷移先の箱の名前（label）一覧。1つのボタンから複数の箱へ矢印を出したい場合は
         * ここに複数指定する。
         */
        public List<String> next;

        public ActionSpec() {
        }

        /** 遷移先が1つだけの場合のコンストラクタ。next が空文字/nullなら遷移無しとして扱う。 */
        public ActionSpec(String type, String label, String next) {
            this.type = type;
            this.label = label;
            this.next = (next == null || next.isBlank()) ? null : List.of(next);
        }

        /** 遷移先が複数ある場合のコンストラクタ。 */
        public ActionSpec(String type, String label, List<String> next) {
            this.type = type;
            this.label = label;
            this.next = next;
        }

        /** typeが省略されていれば既定の種類名を、指定されていればそのまま返す。 */
        public String effectiveType() {
            return (type == null || type.isBlank()) ? DEFAULT_TYPE : type;
        }

        /** 遷移先一覧のうち、空文字やnullを取り除いた実際に有効なものだけを返す。 */
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
     * 箱の種類（kind）ごとの色の定義。すべての項目が省略可能で、指定されなかった色は
     * 既定色で補われる。
     */
    public static class KindStyle {

        public String headerBackground;
        public String background;
        public String border;
        public String textColor;

        public KindStyle() {
        }

        /** 何も指定がない種類に使う、特定の意味に偏らない灰色系の既定色一式。 */
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
     * ボタンの種類（type）ごとの色・枠線の定義。すべての項目が省略可能。
     */
    public static class ActionTypeStyle {

        public String background;
        public String border;
        public Double borderWidth;
        public String textColor;
        public String shadow;

        public ActionTypeStyle() {
        }

        /** 何も指定がない種類に使う既定色一式。 */
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
     * 図全体のレイアウト・文字サイズなどの設定。箱や矢印の色はここには含まれない
     * （矢印などの共通色は {@link Palette} に定数として直接持っている）。
     */
    public static class Theme {

        // --- 配置に関する設定 ---
        public Integer nodeWidth;
        public Integer headerHeight;
        public Integer actionRowHeight;
        public Integer actionGap;
        public Integer nodePaddingTop;
        public Integer nodePaddingBottom;
        public Integer columnGap;
        public Integer rowGap;
        public Integer canvasPadding;

        // --- 文字・配色に関する設定 ---
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

    /** 箱や矢印を実際に画面のどこへ置くかを計算した結果。座標はすべてピクセル単位。 */
    public static class Layout {

        /** 画面に配置された1つの箱の情報。通常の箱と、後戻り先を表す複製の箱の両方をこれで表す。 */
        public static class NodeBox {
            public StateSpec state;
            public int column;
            public int x;
            public int y;
            public int width;
            public int height;
            /** trueなら、元のステータスにボタンがあっても何も描かない（後戻り先を示すだけの複製の箱）。 */
            public boolean clone;
            /** ボタン1つ1つの実際の高さ。長い名前は折り返すため、ボタンごとに高さが変わりうる。 */
            public List<Integer> actionRowHeights = new ArrayList<>();
            /** ボタン1つ1つから矢印が出発するy座標。 */
            public List<Integer> actionAnchorYs = new ArrayList<>();

            public int right() {
                return x + width;
            }

            public int bottom() {
                return y + height;
            }
        }

        /** 矢印の経路を構成する1本の線分。水平か垂直のどちらか一方のみ。 */
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

        /** ボタン1つから出る矢印1本分の経路（線分の集まり）。矢印は常に起点より右へ進む。 */
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

        /** 箱の名前（label）から通常の箱（複製は含まない）を引けるようにした一覧。 */
        public Map<String, NodeBox> nodeByLabel() {
            Map<String, NodeBox> m = new LinkedHashMap<>();
            for (NodeBox n : nodes) {
                m.putIfAbsent(n.state.label, n);
            }
            return m;
        }
    }

    // ============================================================
    // ここから下は、実際の処理（検証・配置計算・CSS/HTML組み立て）を担当する部品たち。
    // ============================================================

    /**
     * 受け取ったデータに不備がないかをチェックする。見つかった不備は1つで止めず、
     * すべて集めてからまとめてエラーとして投げる。
     */
    public static final class FlowValidator {

        private FlowValidator() {
        }

        public static void validate(FlowSpec spec) {
            List<String> errors = new ArrayList<>();

            // ステータスが1件も無ければ、他のチェックをするまでもなくここで打ち切る
            if (spec == null || spec.states == null || spec.states.isEmpty()) {
                throw new FlowDiagramException(List.of("states は1件以上必要です"));
            }

            // 名前（label）の必須チェックと重複チェック
            Set<String> labels = new LinkedHashSet<>();
            for (int i = 0; i < spec.states.size(); i++) {
                StateSpec s = spec.states.get(i);
                if (s == null || s.label == null || s.label.isBlank()) {
                    errors.add("states[" + i + "].label は必須です");
                    continue;
                }
                if (!labels.add(s.label)) {
                    errors.add("ステータス/手続きのlabelが重複しています: " + s.label);
                }
            }

            // ボタン名の必須チェックと、遷移先が実在する名前を指しているかのチェック
            for (int i = 0; i < spec.states.size(); i++) {
                StateSpec s = spec.states.get(i);
                if (s == null) {
                    continue;
                }
                List<ActionSpec> actions = s.safeActions();
                for (int j = 0; j < actions.size(); j++) {
                    ActionSpec a = actions.get(j);
                    if (a == null || a.label == null || a.label.isBlank()) {
                        errors.add("states[" + i + "].actions[" + j + "].label は必須です");
                        continue;
                    }
                    for (String next : a.effectiveNextTargets()) {
                        if (!labels.contains(next)) {
                            errors.add("states[" + i + "].actions[" + j + "].next が存在しないlabelを指しています: "
                                    + next);
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
     * 「どの種類（kind/type）がどんな色か」を、実際に使われている種類の分だけ解決して
     * 一覧にするクラス。リクエストで指定された色があればそれを使い、無ければ既定色を使う。
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

    /**
     * 箱と矢印を実際に画面のどこへ置くかを計算するクラス。箱をどの列・どの高さに並べるか、
     * 矢印をどういう経路（直進・折れ線・迂回路）で引くかを、すべてピクセル単位で決める。
     * このファイルの中で一番複雑な計算を担当している部分。
     */
    public static class LayoutEngine {

        private static final int LEGEND_HEIGHT = 76;

        /** 矢印1本分の「行き先が決まった候補」。複数本をまとめてレーン（通り道）を割り振るために使う。 */
        private record EdgeCandidate(int sx, int sy, Layout.NodeBox to, int ty) {
        }

        private final Theme theme;

        public LayoutEngine(Theme theme) {
            this.theme = theme;
        }

        /**
         * 図全体の配置を計算する。流れとしては、①各箱を何列目に置くか決める → ②列と行の位置から
         * 箱のxy座標・大きさを決める → ③各ボタンから出る矢印の経路（まっすぐ進む・折れる・
         * 他の箱を避けて迂回する）を1本ずつ決める、という順番で進む。
         */
        public Layout build(FlowSpec spec) {
            Layout layout = new Layout();
            layout.title = spec.displayTitle();

            List<StateSpec> states = spec.states;
            Map<String, Integer> indexByLabel = new LinkedHashMap<>();
            for (int i = 0; i < states.size(); i++) {
                indexByLabel.put(states.get(i).label, i);
            }

            int[] columns = assignColumns(states, indexByLabel);

            // --- ① 各箱を画面上に配置する ---
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
                for (int j = 0; j < boxActions.size(); j++) {
                    box.actionAnchorYs.add(actionAnchorY(box, j));
                }
                layout.nodes.add(box);
                boxes.put(s.label, box);
            }

            // 複数の箱をまたいで遠回りする矢印は、途中の箱の下をくぐる専用の通り道を使う。
            // その通り道の高さを決めるため、ここまでに配置した箱の一番下のy座標を覚えておく
            int realNodesMaxBottom = 0;
            for (Layout.NodeBox n : layout.nodes) {
                realNodesMaxBottom = Math.max(realNodesMaxBottom, n.bottom());
            }
            int skipLaneIndex = 0;

            // --- ② 各ボタンから出る矢印の経路を決める（後戻りする矢印は複製の箱を新しく作る） ---
            for (StateSpec s : states) {
                Layout.NodeBox from = boxes.get(s.label);
                List<ActionSpec> actions = s.safeActions();

                // まず矢印の行き先（実在の箱、または後戻り用の複製の箱）だけを先に確定させる。
                // 同じ箱から出る矢印同士が重ならないよう通り道を振り分けるのは、この後にまとめて行う
                List<EdgeCandidate> candidates = new ArrayList<>();
                for (int j = 0; j < actions.size(); j++) {
                    ActionSpec a = actions.get(j);
                    int sx = from.right();
                    int sy = from.actionAnchorYs.get(j);
                    for (String next : a.effectiveNextTargets()) {
                        Layout.NodeBox target = boxes.get(next);

                        Layout.NodeBox to;
                        boolean skipsColumns;
                        if (target.column > from.column) {
                            to = target; // 前へ進む矢印: 実在の箱がそのまま行き先
                            skipsColumns = (target.column - from.column) > 1;
                        } else {
                            // 自分自身、または手前の列へ戻る矢印: 実際の箱へは線を引かず、
                            // 起点のすぐ右隣に「行き先はここですよ」を示す複製の箱を新しく作る
                            to = cloneNode(target.state, from.column + 1, nextYByColumn, topY);
                            layout.nodes.add(to);
                            skipsColumns = false; // 複製の箱は必ず起点の隣の列に置くので、列を飛び越さない
                        }
                        int ty = to.clone ? to.y + CLONE_TEXT_HEIGHT / 2 : to.y + theme.headerHeight / 2;

                        if (skipsColumns) {
                            // 2列以上先へ直接進む矢印は、途中の箱の背後を線が通り抜けて見えなく
                            // ならないよう、すべての箱より下を通る迂回路を経由させる
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

                // 同じ箱から出る複数の矢印が、途中の折れ曲がり部分で重なって見分けがつかなくなら
                // ないよう、通り道（レーン）を少しずつずらす。ずらし方のコツ: 行き先が下にある
                // 矢印ほど起点寄りの内側の通り道を、行き先が上にある矢印ほど外側の通り道を使うと、
                // 矢印同士が交差しない（逆にすると交差してしまう）
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

        /** 後戻りする矢印の行き先として使う、複製の箱（名前だけのテキスト表示）を新しく作る。 */
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

        /** 箱の高さを計算する。ボタンの名前がすべて1行に収まる前提の簡易版（主にテスト用）。 */
        public int nodeHeight(int actionCount) {
            if (actionCount == 0) {
                return theme.headerHeight;
            }
            return theme.headerHeight + theme.nodePaddingTop
                    + theme.actionRowHeight * actionCount + theme.actionGap * (actionCount - 1)
                    + theme.nodePaddingBottom;
        }

        /** 箱の高さを計算する。ボタンの名前が長くて折り返される場合も考慮し、ボタンごとの実際の高さを積み上げる。 */
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

        /** j番目のボタンの中央のy座標（＝そこから出る矢印の起点）。それより前のボタンの高さを積み上げて求める。 */
        public int actionAnchorY(Layout.NodeBox box, int j) {
            int y = box.y + theme.headerHeight + theme.nodePaddingTop;
            for (int i = 0; i < j; i++) {
                y += box.actionRowHeights.get(i) + theme.actionGap;
            }
            return y + box.actionRowHeights.get(j) / 2;
        }

        /**
         * ボタン1個の実際の高さを見積もる。名前がボタンの横幅に収まらない場合は折り返して
         * 複数行になるため、おおよその行数から高さを逆算する。実際のフォントの文字幅は
         * 測れないので、全角文字は1文字分、半角文字は0.55文字分の幅として概算する。
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

        /** 漢字・ひらがな・カタカナ・全角記号など、横幅が広い文字かどうかの簡易判定。 */
        private static boolean isWideChar(char c) {
            return (c >= 0x1100 && c <= 0x115F)
                    || (c >= 0x2E80 && c <= 0xA4CF)
                    || (c >= 0xAC00 && c <= 0xD7A3)
                    || (c >= 0xF900 && c <= 0xFAFF)
                    || (c >= 0xFF00 && c <= 0xFF60)
                    || (c >= 0xFFE0 && c <= 0xFFE6);
        }

        /**
         * 各箱を何列目に置くかを決める。「AからBへ、BからAへ」のような行き来がある場合でも
         * 無限ループに陥らないよう、まず「後戻りする矢印」を一旦除いて一方通行の関係図にし、
         * そこから各箱までの最長経路の長さで列番号を決める。
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
         * 「後戻りする矢印」（深さ優先で箱をたどっていったときに、今たどっている途中の箱へ
         * 戻ってくるような矢印＝ループを作る矢印）を見つける。戻り値は
         * [箱の番号][その箱から出る何本目の矢印か] で引ける、該当するかどうかのフラグ表。
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
        /** ボタンの名前が収まる横幅を見積もる際に、ボタンの左右の余白分として差し引く長さ。 */
        private static final int ACTION_LABEL_H_RESERVE = 44;
        /**
         * ボタンの右端から箱の右端までをつなぐ短い線（接続スタブ）の長さ。矢印の線はボタンから
         * 続いて見えるように、箱の見た目上の余白をこの線で埋めている。CSSの指定だけだと
         * ボタンの枠線の太さ次第で位置がずれてしまうため、矢印の線と全く同じ座標計算（箱の右端
         * を基準にする）で正確に描く。
         */
        public static final int ACTION_STUB_LENGTH = 12;
        private static final int ACTION_LABEL_V_PADDING = 12;

        /**
         * 前へ進む矢印の経路を決める。折れ曲がる位置はできるだけ終点に近いところにする。
         * これにより、起点を出てすぐは各ボタンの高さのまま横に伸びるだけになり、
         * 同じ箱から出る複数の矢印が序盤で重なって見分けづらくなるのを防げる。
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
         * 2列以上先へ直接進む矢印の経路を決める。途中の列にある箱の背後を線が通り抜けて
         * 見えなくならないよう、すべての箱より下を通る迂回路を経由させる。
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
     * 配置計算の結果をもとに、図の本体（箱・矢印・凡例）をHTMLとして組み立てるクラス。
     * 画像やSVGは使わず、すべて `div` 要素と座標指定のスタイルだけで表現する。
     * 色については {@link StyleRegistry} が解決した結果をそのままCSSに変換するだけで、
     * このクラス自身は「どの種類が何色か」という意味までは判断しない。
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

        /** 図全体を囲む `<div class="fd-canvas">…</div>` のHTML文字列を組み立てて返す。 */
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
         * この図の中で実際に使われている種類（kind/type）ごとの色を、CSSとして出力する。
         * 色の決定自体は {@link #kindStyles}/{@link #typeStyles} が既に済ませているので、
         * ここではそれをCSSの文字列に変換するだけ。種類名をそのままクラス名に使うと日本語などで
         * 壊れるおそれがあるため、{@link Html#labelToken} で安全な文字列に変換したものを使う。
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
         * 複製の箱（後戻り先を示すテキスト）にカーソルを合わせたとき、対応する元の箱も一緒に
         * アウトラインで強調表示するためのCSSを出力する。JavaScriptは使わず、CSSの `:has()` で
         * 「このあとに自分と同じ名前の複製がホバーされているか」を判定して実現している。
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

        /**
         * 1本の矢印（複数の線分のつながり）をHTMLとして描く。線分同士のつなぎ目に隙間ができたり、
         * 逆にはみ出して見えたりしないよう、隣り合う線分とくっつく側の端だけを少し延長して描く。
         * 矢印の先端（三角形）に接続する最後の線分だけは、先端ぎりぎりまで伸ばすと三角形からはみ
         * 出て見えてしまうため、先端の手前（三角形の根元）で止める。
         */
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
                // 一番最初の線分の始まる側は、ボタン側の短い接続線とぴったり繋がるように
                // 常に延長する
                boolean isFirst = i == 0;
                boolean extendStart = isFirst
                        || touchesEndpoint(startX, startY, prev) || touchesEndpoint(startX, startY, next);
                boolean extendEnd = touchesEndpoint(endX, endY, prev) || touchesEndpoint(endX, endY, next);
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
         * (x,y) が隣の線分 {@code other} の端点のどちらかと同じ場所かどうかを判定する。
         * 同じ場所＝線分同士がつながる継ぎ目なので延長してよく、同じ場所でなければ経路全体の
         * 端（起点または先端）なので延長しない、という判断に使う。
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

        /** 後戻り先を示す複製の箱を、名前だけのテキストとして描く（通常の箱のような枠は付けない）。 */
        private void renderCloneRef(StringBuilder sb, Layout.NodeBox n) {
            sb.append("  <div class=\"node-clone-ref lbl-").append(Html.labelToken(n.state.label))
              .append("\" style=\"left:").append(n.x).append("px;top:").append(n.y)
              .append("px;width:").append(n.width).append("px\">")
              .append(Html.esc(n.state.label)).append("</div>\n");
        }

        /** 1つの箱（見出し部分＋中のボタン一覧）をHTMLとして描く。複製の箱の場合は別の描き方に回す。 */
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
                      .append("<span class=\"action-label\">").append(Html.esc(a.label)).append("</span>")
                      .append("</div>\n");
                }
                sb.append("    </div>\n");
            }
            sb.append("  </div>\n");

            if (!actions.isEmpty()) {
                for (int j = 0; j < actions.size(); j++) {
                    if (!actions.get(j).effectiveNextTargets().isEmpty()) {
                        renderActionStub(sb, n, j);
                    }
                }
            }
        }

        /**
         * ボタンの見た目上の右端から箱の右端までをつなぐ短い線（接続スタブ）を描く。箱の
         * 中に入れ子にすると座標の基準がずれてしまうため、図全体と同じ兄弟要素として描く。
         */
        private void renderActionStub(StringBuilder sb, Layout.NodeBox n, int j) {
            int right = n.right();
            int left = right - LayoutEngine.ACTION_STUB_LENGTH;
            int sy = n.actionAnchorYs.get(j);
            sb.append("    <div class=\"action-stub\" style=\"left:").append(left)
              .append("px;top:calc(").append(sy).append("px - var(--edge-w) / 2);width:")
              .append(LayoutEngine.ACTION_STUB_LENGTH).append("px\"></div>\n");
        }

        /** 図の下に出す凡例を描く。実際に使われている種類（kind/type）だけを一覧表示する。 */
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
                "変更を反映するには審査の流れ画面から再度「審査の流れを確認する」"
                        + "または「申請の流れを確認する」ボタンを押してください";
        private static final int LEGEND_HINT_OFFSET = 24;
    }

    /**
     * 組み立てたCSSとHTML本体を、1枚の完結したHTMLページに仕上げるクラス。
     * 外部サイトのCSSや画像などを一切読み込まない、単体で開けるファイルにする。
     */
    public static class HtmlPageWriter {

        private final Theme theme;

        public HtmlPageWriter(Theme theme) {
            this.theme = theme;
        }

        /** ブラウザでそのまま開ける1枚もののHTMLを組み立てる。JavaScriptは一切含まない。 */
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
     * 見た目設定（Theme）から、レイアウト・文字サイズなどに関するCSSを組み立てるクラス。
     * 種類（kind/type）ごとの色はリクエストによって変わるためここでは扱わず、
     * {@link HtmlDiagramRenderer} の方で別途出力する。矢印の線など種類に紐付かない共通色は
     * {@link Palette} の定数をそのまま埋め込む。
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
                    box-shadow:var(--node-shadow);border:1.5px solid transparent;\
                    transition:outline-color .15s;outline:3px solid transparent;outline-offset:3px;}
                    /* --- 複製参照テキスト（後退辺・自己ループの終端） --- */
                    .fd-canvas .node-clone-ref{position:absolute;z-index:2;display:flex;\
                    align-items:center;gap:4px;font-size:var(--state-font-size);color:#6b7280;\
                    font-style:italic;cursor:default;white-space:nowrap;overflow:hidden;\
                    text-overflow:ellipsis;padding:0 6px;}
                    .fd-canvas .node-clone-ref:hover{color:var(--edge-color);}
                    /* 箱（.node）全体には overflow:hidden を付けていないため、見出し部分の
                       背景を箱の角丸に合わせて切り抜くための丸みをここで自前で付けている */
                    .fd-canvas .node-header{min-height:var(--header-height);display:flex;\
                    flex-direction:column;justify-content:center;gap:2px;padding:6px 12px;\
                    border-radius:8.5px 8.5px 0 0;}
                    .fd-canvas .kind-badge{align-self:flex-start;\
                    font-size:var(--badge-font-size);font-weight:700;letter-spacing:.03em;\
                    padding:1px 7px;border-radius:999px;background:rgba(255,255,255,.55);}
                    .fd-canvas .state-label{font-size:var(--state-font-size);font-weight:700;\
                    white-space:nowrap;overflow:hidden;text-overflow:ellipsis;}

                    /* --- アクション。色は type ごとに動的CSS（HtmlDiagramRenderer）で決まる --- */
                    .fd-canvas .node-actions{display:flex;flex-direction:column;gap:var(--action-gap);\
                    padding:var(--node-padding-top) 10px var(--node-padding-bottom);}
                    .fd-canvas .action{min-height:var(--action-height);\
                    display:flex;align-items:center;\
                    padding:4px 12px;border-radius:8px;font-size:var(--action-font-size);}
                    .fd-canvas .action .action-label{flex:1 1 auto;white-space:normal;\
                    overflow-wrap:anywhere;line-height:1.35;}
                    /* ボタンの右端から箱の右端までの隙間を埋める接続スタブ用のスタイル。
                       ボタン自体の余白・丸みは変えず、線がボタンから続いているように見せるためのもの */
                    .fd-canvas .action-stub{position:absolute;z-index:2;\
                    height:var(--edge-w);background:var(--edge-color);}

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
     * 種類（kind/type）に関係なく図全体で共通して使う色の定数置き場。種類ごとの色は
     * リクエスト側（{@link KindStyle}/{@link ActionTypeStyle}）で指定するが、矢印の色のように
     * どの種類にも属さない共通の色だけはここに固定で持つ。
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

        /** 文字列の中の `& < > " '` をHTMLとして安全な表記に置き換える。 */
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
