package com.example.flowdiagram.api.service;

import com.example.flowdiagram.api.controller.FlowSpecEntity;
import com.example.flowdiagram.api.controller.FlowSpecEntity.ActionSpecEntity;
import com.example.flowdiagram.api.controller.FlowSpecEntity.ActionTypeStyleEntity;
import com.example.flowdiagram.api.controller.FlowSpecEntity.KindStyleEntity;
import com.example.flowdiagram.api.controller.FlowSpecEntity.StateSpecEntity;
import com.example.flowdiagram.api.controller.FlowSpecEntity.ThemeEntity;
import com.example.flowdiagram.model.service.DiagramService;
import com.example.flowdiagram.model.service.DiagramService.ActionSpec;
import com.example.flowdiagram.model.service.DiagramService.ActionTypeStyle;
import com.example.flowdiagram.model.service.DiagramService.FlowSpec;
import com.example.flowdiagram.model.service.DiagramService.KindStyle;
import com.example.flowdiagram.model.service.DiagramService.StateSpec;
import com.example.flowdiagram.model.service.DiagramService.Theme;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** APIサービス。 */
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

    // --- entity→model変換処理 ---

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
}
