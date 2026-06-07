package com.knoxhack.echo.scriptcore.client.screencore;

import com.knoxhack.echo.scriptcore.network.ScriptCoreUiResultPacket;
import com.knoxhack.echoscreencore.api.EchoDataContext;
import com.knoxhack.echoscreencore.api.EchoScreens;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;

public final class ScriptCoreScreenCoreClientState {
    private static final String DEFAULT_COMPONENT_KEY = "_page";
    private static volatile boolean bridgeRegistered;
    private static volatile ResultView last = ResultView.empty();
    private static final Map<String, ResultView> RESULTS = new LinkedHashMap<>();

    private ScriptCoreScreenCoreClientState() {
    }

    public static void markBridgeRegistered() {
        bridgeRegistered = true;
    }

    public static synchronized void apply(ScriptCoreUiResultPacket packet) {
        if (packet == null) {
            return;
        }
        ResultView view = ResultView.from(packet);
        last = view;
        RESULTS.put(componentKey(view.componentId()), view);
        Identifier pageId = Identifier.tryParse(view.pageId());
        if (pageId == null) {
            EchoScreens.invalidateData();
        } else {
            EchoScreens.invalidatePage(pageId);
        }
    }

    public static synchronized void resetForTests() {
        last = ResultView.empty();
        RESULTS.clear();
        bridgeRegistered = false;
    }

    public static Object resolveForTests(String path) {
        return resolve(EchoDataContext.empty(), EchoDataContext.splitPath(path));
    }

    public static Object resolve(EchoDataContext context, List<String> path) {
        if (path == null || path.isEmpty()) {
            return root();
        }
        return switch (path.get(0)) {
            case "bridge" -> bridge(path);
            case "last" -> nested(last.asMap(), path, 1);
            case "results" -> result(path);
            default -> null;
        };
    }

    private static Map<String, Object> root() {
        return row(
                "bridge", row("clientRegistered", bridgeRegistered),
                "last", last.asMap(),
                "results", resultMap());
    }

    private static Object bridge(List<String> path) {
        Map<String, Object> bridge = row("clientRegistered", bridgeRegistered);
        return path.size() == 1 ? bridge : nested(bridge, path, 1);
    }

    private static Object result(List<String> path) {
        if (path.size() == 1) {
            return resultMap();
        }
        ResultView view = RESULTS.get(componentKey(path.get(1)));
        if (view == null) {
            return null;
        }
        return path.size() == 2 ? view.asMap() : nested(view.asMap(), path, 2);
    }

    private static Map<String, Object> resultMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        RESULTS.forEach((key, value) -> out.put(key, value.asMap()));
        return Map.copyOf(out);
    }

    private static String componentKey(String componentId) {
        String clean = componentId == null ? "" : componentId.trim();
        return clean.isBlank() ? DEFAULT_COMPONENT_KEY : clean;
    }

    private static Object nested(Object value, List<String> path, int start) {
        Object current = value;
        for (int i = start; i < path.size(); i++) {
            if (current instanceof Map<?, ?> map) {
                current = map.get(path.get(i));
            } else {
                return null;
            }
        }
        return current;
    }

    private static Map<String, Object> row(Object... values) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        for (int i = 0; i + 1 < values.length; i += 2) {
            out.put(String.valueOf(values[i]), values[i + 1]);
        }
        return Map.copyOf(out);
    }

    private record ResultView(
            String mode,
            String definitionId,
            String slot,
            String pageId,
            String componentId,
            boolean success,
            String code,
            String message,
            int actionCount,
            int executedActions) {
        static ResultView empty() {
            return new ResultView("", "", "", "", "", false, "none", "", 0, 0);
        }

        static ResultView from(ScriptCoreUiResultPacket packet) {
            return new ResultView(
                    packet.mode().wireName(),
                    packet.definitionId().toString(),
                    packet.slot(),
                    packet.pageId(),
                    packet.componentId(),
                    packet.success(),
                    packet.code(),
                    packet.message(),
                    packet.actionCount(),
                    packet.executedActions());
        }

        Map<String, Object> asMap() {
            return row(
                    "mode", mode,
                    "definitionId", definitionId,
                    "slot", slot,
                    "pageId", pageId,
                    "componentId", componentId,
                    "success", success,
                    "status", status(),
                    "code", code,
                    "message", message,
                    "actionCount", actionCount,
                    "executedActions", executedActions);
        }

        private String status() {
            if (code == null || code.isBlank() || "none".equals(code)) {
                return "locked";
            }
            return success ? "ready" : "danger";
        }
    }
}
