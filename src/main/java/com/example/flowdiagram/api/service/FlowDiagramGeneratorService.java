package com.example.flowdiagram.api.service;

import com.example.flowdiagram.api.controller.FlowDiagramGeneratorRequest;
import com.example.flowdiagram.api.controller.FlowDiagramGeneratorRequest.ActionSpecEntity;
import com.example.flowdiagram.api.controller.FlowDiagramGeneratorRequest.ActionTypeStyleEntity;
import com.example.flowdiagram.api.controller.FlowDiagramGeneratorRequest.KindStyleEntity;
import com.example.flowdiagram.api.controller.FlowDiagramGeneratorRequest.StateSpecEntity;
import com.example.flowdiagram.api.controller.FlowDiagramGeneratorRequest.ThemeEntity;
import com.example.flowdiagram.model.service.DiagramService;
import com.example.flowdiagram.model.service.DiagramService.ActionSpec;
import com.example.flowdiagram.model.service.DiagramService.ActionTypeStyle;
import com.example.flowdiagram.model.service.DiagramService.FlowSpec;
import com.example.flowdiagram.model.service.DiagramService.KindStyle;
import com.example.flowdiagram.model.service.DiagramService.StateSpec;
import com.example.flowdiagram.model.service.DiagramService.Theme;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** APIサービス。 */
@Service
public class FlowDiagramGeneratorService {

    private static final String HTML = MediaType.TEXT_HTML_VALUE + ";charset=UTF-8";

    @Autowired
    private DiagramService diagramService;

    public ResponseEntity<String> diagram(FlowDiagramGeneratorRequest request) {
        String html = diagramService.renderPage(toModel(request));
        return ResponseEntity.ok().header("Content-Type", HTML).body(html);
    }

    // --- entity→model変換処理 ---

    private static FlowSpec toModel(FlowDiagramGeneratorRequest e) {
        if (e == null) {
            return new FlowSpec();
        }
        FlowSpec m = new FlowSpec();
        m.title = e.getTitle();
        m.theme = toModel(e.getTheme());
        m.kinds = toModelKinds(e.getKinds());
        m.types = toModelTypes(e.getTypes());
        m.states = toModelStates(e.getStates());
        return m;
    }

    private static Theme toModel(ThemeEntity e) {
        if (e == null) {
            return null;
        }
        Theme m = new Theme();
        m.nodeWidth = e.getNodeWidth();
        m.headerHeight = e.getHeaderHeight();
        m.actionRowHeight = e.getActionRowHeight();
        m.actionGap = e.getActionGap();
        m.nodePaddingTop = e.getNodePaddingTop();
        m.nodePaddingBottom = e.getNodePaddingBottom();
        m.columnGap = e.getColumnGap();
        m.rowGap = e.getRowGap();
        m.canvasPadding = e.getCanvasPadding();
        m.fontFamily = e.getFontFamily();
        m.titleFontSize = e.getTitleFontSize();
        m.badgeFontSize = e.getBadgeFontSize();
        m.stateFontSize = e.getStateFontSize();
        m.actionFontSize = e.getActionFontSize();
        m.background = e.getBackground();
        m.titleColor = e.getTitleColor();
        m.nodeShadow = e.getNodeShadow();
        m.edgeWidth = e.getEdgeWidth();
        m.arrowSize = e.getArrowSize();
        m.showLegend = e.getShowLegend();
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
        m.headerBackground = e.getHeaderBackground();
        m.background = e.getBackground();
        m.border = e.getBorder();
        m.textColor = e.getTextColor();
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
        m.background = e.getBackground();
        m.border = e.getBorder();
        m.borderWidth = e.getBorderWidth();
        m.textColor = e.getTextColor();
        m.shadow = e.getShadow();
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
        StateSpec m = new StateSpec(e.getLabel());
        m.kind = e.getKind();
        m.actions = toModelActions(e.getActions());
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
        return new ActionSpec(e.getType(), e.getLabel(), e.getNext());
    }
}
