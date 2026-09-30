package com.example.flowdiagram.api;

import com.example.flowdiagram.api.entity.ActionSpecEntity;
import com.example.flowdiagram.api.entity.ActionTypeStyleEntity;
import com.example.flowdiagram.api.entity.FlowSpecEntity;
import com.example.flowdiagram.api.entity.KindStyleEntity;
import com.example.flowdiagram.api.entity.StateSpecEntity;
import com.example.flowdiagram.api.entity.ThemeEntity;
import com.example.flowdiagram.model.ActionSpec;
import com.example.flowdiagram.model.ActionTypeStyle;
import com.example.flowdiagram.model.DiagramService;
import com.example.flowdiagram.model.FlowSpec;
import com.example.flowdiagram.model.KindStyle;
import com.example.flowdiagram.model.StateSpec;
import com.example.flowdiagram.model.Theme;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * コントローラの値を受け取り処理を行うクラス（API層）。
 * コントローラの入り口 entity（{@code api.entity} パッケージ）と model 層の内部 entity
 * （{@code model} パッケージ）は別クラスのため、ここで相互変換したうえで
 * {@link DiagramService}（model層。処理の全般を行う）を呼び出し、結果をコントローラ向けに
 * 加工する。
 */
@Service
public class DiagramApiService {

    private static final String HTML = MediaType.TEXT_HTML_VALUE + ";charset=UTF-8";

    private final DiagramService diagramService;

    public DiagramApiService(DiagramService diagramService) {
        this.diagramService = diagramService;
    }

    public ResponseEntity<String> diagram(FlowSpecEntity request) {
        String html = diagramService.renderPage(toModel(request));
        return ResponseEntity.ok().header("Content-Type", HTML).body(html);
    }

    public ResponseEntity<String> fragment(FlowSpecEntity request) {
        String html = diagramService.renderFragment(toModel(request));
        return ResponseEntity.ok().header("Content-Type", HTML).body(html);
    }

    public FlowSpecEntity sample() {
        return toEntity(diagramService.sampleSpec());
    }

    public FlowSpecEntity sample1() {
        return toEntity(diagramService.sampleSpec());
    }

    public FlowSpecEntity sample2() {
        return toEntity(diagramService.sample2Spec());
    }

    // --- entity(api) -> model 変換 ---

    private static FlowSpec toModel(FlowSpecEntity e) {
        if (e == null) {
            return new FlowSpec();
        }
        FlowSpec m = new FlowSpec();
        m.title = e.title;
        m.theme = toModel(e.theme);
        m.kinds = toModelKinds(e.kinds);
        m.types = toModelTypes(e.types);
        m.states = toModelStates(e.states);
        return m;
    }

    private static Theme toModel(ThemeEntity e) {
        if (e == null) {
            return null;
        }
        Theme m = new Theme();
        m.nodeWidth = e.nodeWidth;
        m.headerHeight = e.headerHeight;
        m.actionRowHeight = e.actionRowHeight;
        m.actionGap = e.actionGap;
        m.nodePaddingTop = e.nodePaddingTop;
        m.nodePaddingBottom = e.nodePaddingBottom;
        m.columnGap = e.columnGap;
        m.rowGap = e.rowGap;
        m.canvasPadding = e.canvasPadding;
        m.fontFamily = e.fontFamily;
        m.titleFontSize = e.titleFontSize;
        m.badgeFontSize = e.badgeFontSize;
        m.stateFontSize = e.stateFontSize;
        m.actionFontSize = e.actionFontSize;
        m.background = e.background;
        m.titleColor = e.titleColor;
        m.nodeShadow = e.nodeShadow;
        m.edgeWidth = e.edgeWidth;
        m.arrowSize = e.arrowSize;
        m.showLegend = e.showLegend;
        return m;
    }

    private static Map<String, KindStyle> toModelKinds(Map<String, KindStyleEntity> kinds) {
        if (kinds == null) {
            return null;
        }
        Map<String, KindStyle> result = new LinkedHashMap<>();
        for (Map.Entry<String, KindStyleEntity> entry : kinds.entrySet()) {
            result.put(entry.getKey(), toModel(entry.getValue()));
        }
        return result;
    }

    private static KindStyle toModel(KindStyleEntity e) {
        if (e == null) {
            return null;
        }
        KindStyle m = new KindStyle();
        m.headerBackground = e.headerBackground;
        m.background = e.background;
        m.border = e.border;
        m.textColor = e.textColor;
        return m;
    }

    private static Map<String, ActionTypeStyle> toModelTypes(Map<String, ActionTypeStyleEntity> types) {
        if (types == null) {
            return null;
        }
        Map<String, ActionTypeStyle> result = new LinkedHashMap<>();
        for (Map.Entry<String, ActionTypeStyleEntity> entry : types.entrySet()) {
            result.put(entry.getKey(), toModel(entry.getValue()));
        }
        return result;
    }

    private static ActionTypeStyle toModel(ActionTypeStyleEntity e) {
        if (e == null) {
            return null;
        }
        ActionTypeStyle m = new ActionTypeStyle();
        m.background = e.background;
        m.border = e.border;
        m.borderWidth = e.borderWidth;
        m.textColor = e.textColor;
        m.shadow = e.shadow;
        return m;
    }

    private static List<StateSpec> toModelStates(List<StateSpecEntity> states) {
        if (states == null) {
            return new ArrayList<>();
        }
        List<StateSpec> result = new ArrayList<>(states.size());
        for (StateSpecEntity e : states) {
            result.add(toModel(e));
        }
        return result;
    }

    private static StateSpec toModel(StateSpecEntity e) {
        if (e == null) {
            return new StateSpec();
        }
        StateSpec m = new StateSpec(e.label);
        m.kind = e.kind;
        m.actions = toModelActions(e.actions);
        return m;
    }

    private static List<ActionSpec> toModelActions(List<ActionSpecEntity> actions) {
        if (actions == null) {
            return new ArrayList<>();
        }
        List<ActionSpec> result = new ArrayList<>(actions.size());
        for (ActionSpecEntity e : actions) {
            result.add(toModel(e));
        }
        return result;
    }

    private static ActionSpec toModel(ActionSpecEntity e) {
        if (e == null) {
            return new ActionSpec(null, null, (List<String>) null);
        }
        return new ActionSpec(e.type, e.label, e.next);
    }

    // --- model -> entity(api) 変換（GET /api/sample 系のレスポンス用） ---

    private static FlowSpecEntity toEntity(FlowSpec m) {
        FlowSpecEntity e = new FlowSpecEntity();
        e.title = m.title;
        e.theme = toEntity(m.theme);
        e.kinds = toEntityKinds(m.kinds);
        e.types = toEntityTypes(m.types);
        e.states = toEntityStates(m.states);
        return e;
    }

    private static ThemeEntity toEntity(Theme m) {
        if (m == null) {
            return null;
        }
        ThemeEntity e = new ThemeEntity();
        e.nodeWidth = m.nodeWidth;
        e.headerHeight = m.headerHeight;
        e.actionRowHeight = m.actionRowHeight;
        e.actionGap = m.actionGap;
        e.nodePaddingTop = m.nodePaddingTop;
        e.nodePaddingBottom = m.nodePaddingBottom;
        e.columnGap = m.columnGap;
        e.rowGap = m.rowGap;
        e.canvasPadding = m.canvasPadding;
        e.fontFamily = m.fontFamily;
        e.titleFontSize = m.titleFontSize;
        e.badgeFontSize = m.badgeFontSize;
        e.stateFontSize = m.stateFontSize;
        e.actionFontSize = m.actionFontSize;
        e.background = m.background;
        e.titleColor = m.titleColor;
        e.nodeShadow = m.nodeShadow;
        e.edgeWidth = m.edgeWidth;
        e.arrowSize = m.arrowSize;
        e.showLegend = m.showLegend;
        return e;
    }

    private static Map<String, KindStyleEntity> toEntityKinds(Map<String, KindStyle> kinds) {
        if (kinds == null) {
            return null;
        }
        Map<String, KindStyleEntity> result = new LinkedHashMap<>();
        for (Map.Entry<String, KindStyle> entry : kinds.entrySet()) {
            result.put(entry.getKey(), toEntity(entry.getValue()));
        }
        return result;
    }

    private static KindStyleEntity toEntity(KindStyle m) {
        if (m == null) {
            return null;
        }
        KindStyleEntity e = new KindStyleEntity();
        e.headerBackground = m.headerBackground;
        e.background = m.background;
        e.border = m.border;
        e.textColor = m.textColor;
        return e;
    }

    private static Map<String, ActionTypeStyleEntity> toEntityTypes(Map<String, ActionTypeStyle> types) {
        if (types == null) {
            return null;
        }
        Map<String, ActionTypeStyleEntity> result = new LinkedHashMap<>();
        for (Map.Entry<String, ActionTypeStyle> entry : types.entrySet()) {
            result.put(entry.getKey(), toEntity(entry.getValue()));
        }
        return result;
    }

    private static ActionTypeStyleEntity toEntity(ActionTypeStyle m) {
        if (m == null) {
            return null;
        }
        ActionTypeStyleEntity e = new ActionTypeStyleEntity();
        e.background = m.background;
        e.border = m.border;
        e.borderWidth = m.borderWidth;
        e.textColor = m.textColor;
        e.shadow = m.shadow;
        return e;
    }

    private static List<StateSpecEntity> toEntityStates(List<StateSpec> states) {
        if (states == null) {
            return null;
        }
        List<StateSpecEntity> result = new ArrayList<>(states.size());
        for (StateSpec m : states) {
            result.add(toEntity(m));
        }
        return result;
    }

    private static StateSpecEntity toEntity(StateSpec m) {
        StateSpecEntity e = new StateSpecEntity(m.label);
        e.kind = m.kind;
        e.actions = toEntityActions(m.actions);
        return e;
    }

    private static List<ActionSpecEntity> toEntityActions(List<ActionSpec> actions) {
        if (actions == null) {
            return null;
        }
        List<ActionSpecEntity> result = new ArrayList<>(actions.size());
        for (ActionSpec m : actions) {
            result.add(toEntity(m));
        }
        return result;
    }

    private static ActionSpecEntity toEntity(ActionSpec m) {
        return new ActionSpecEntity(m.type, m.label, m.next);
    }
}
