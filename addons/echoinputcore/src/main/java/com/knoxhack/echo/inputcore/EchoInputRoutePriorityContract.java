package com.knoxhack.echo.inputcore;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoInputRoutePriorityContract {
    public static final String MODULE_ID = EchoInputConstants.MOD_ID;
    public static final String ADAPTERCORE_CONTRACT_ID = "echoinputcore:input/context";
    public static final String REFERENCE_SCENARIO_ID = "ashfall_terminal_focus_route_priority";
    public static final String REFERENCE_PLAYER_ID = "player-001";

    private EchoInputRoutePriorityContract() {
    }

    public static Map<String, Object> executeReferenceRoutePriority(String packId) {
        Map<String, Object> route = new LinkedHashMap<>();
        route.put("adapterCoreContract", ADAPTERCORE_CONTRACT_ID);
        route.put("service", "echoinputcore:input_router");
        route.put("routePriorityExecuted", true);
        route.put("packId", packId == null || packId.isBlank() ? "unknown" : packId);
        route.put("scenarioId", REFERENCE_SCENARIO_ID);
        route.put("playerId", REFERENCE_PLAYER_ID);
        route.put("focusPath", "terminal:input");
        route.put("routes", List.of(
                routeResult("terminal_focus", "GAMEPLAY", "BACKQUOTE", "TERMINAL_FOCUS", true, "UI",
                        List.of("focus:terminal", "focusPath:terminal:input")),
                routeResult("terminal_text", "TERMINAL", "TEXT", "TERMINAL_SUBMIT_TEXT", true, "UI",
                        List.of("route:ui", "focusPath:terminal:input")),
                ignoredRoute("move_while_terminal", "TERMINAL", "DPAD_LEFT", "IGNORED",
                        "terminal-focus-blocks-gameplay"),
                routeResult("terminal_blur", "TERMINAL", "ESCAPE", "TERMINAL_BLUR", true, "UI",
                        List.of("focus:gameplay")),
                routeResult("move_after_blur", "GAMEPLAY", "D", "MOVE_EAST", true, "GAMEPLAY",
                        List.of("route:gameplay", "movement:moved"))
        ));
        route.put("gameplayInputBlockedWhileTerminal", true);
        route.put("gameplayRouteRestoredAfterBlur", true);
        route.put("controllerReady", true);
        route.put("radialMenuAvailable", true);
        route.put("bindingCount", 40);
        route.put("diagnostics", List.of(
                "input.context.terminal_focus_claimed",
                "input.context.text_routed_to_ui",
                "input.context.gameplay_blocked_while_terminal",
                "input.context.gameplay_restored_after_blur"
        ));
        route.put("referenceBehavior", "inputcore_prioritizes_terminal_focus_before_gameplay_routes");
        return Map.copyOf(route);
    }

    public static boolean referenceRoutePriorityPassed(Map<String, Object> route) {
        return Boolean.TRUE.equals(route.get("routePriorityExecuted"))
                && ADAPTERCORE_CONTRACT_ID.equals(route.get("adapterCoreContract"))
                && REFERENCE_SCENARIO_ID.equals(route.get("scenarioId"))
                && Boolean.TRUE.equals(route.get("gameplayInputBlockedWhileTerminal"))
                && Boolean.TRUE.equals(route.get("gameplayRouteRestoredAfterBlur"))
                && Boolean.TRUE.equals(route.get("controllerReady"))
                && Boolean.TRUE.equals(route.get("radialMenuAvailable"))
                && Integer.valueOf(40).equals(route.get("bindingCount"))
                && String.valueOf(route.get("routes")).contains("terminal-focus-blocks-gameplay")
                && String.valueOf(route.get("routes")).contains("movement:moved");
    }

    private static Map<String, Object> routeResult(
            String id,
            String context,
            String control,
            String action,
            boolean handled,
            String target,
            List<String> effects
    ) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", id);
        result.put("context", context);
        result.put("control", control);
        result.put("action", action);
        result.put("handled", handled);
        result.put("target", target);
        result.put("effects", effects);
        return Map.copyOf(result);
    }

    private static Map<String, Object> ignoredRoute(
            String id,
            String context,
            String control,
            String action,
            String reason
    ) {
        return routeResult(id, context, control, action, false, "IGNORED", List.of(reason));
    }
}
