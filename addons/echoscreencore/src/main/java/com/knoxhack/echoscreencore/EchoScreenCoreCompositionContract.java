package com.knoxhack.echoscreencore;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoScreenCoreCompositionContract {
    public static final String MODULE_ID = "echoscreencore";
    public static final String ADAPTERCORE_CONTRACT_ID = "echoscreencore:screen/field_ops_composition";
    public static final String REFERENCE_SCREEN_ID = "echoterminal:field_ops/first_ten_minutes";
    public static final String REFERENCE_ACTION_ID = "open_index";

    private EchoScreenCoreCompositionContract() {
    }

    public static Map<String, Object> executeReferenceComposition(String screenId, String actionId) {
        Map<String, Object> composition = new LinkedHashMap<>();
        composition.put("adapterCoreContract", ADAPTERCORE_CONTRACT_ID);
        composition.put("service", "echoscreencore:screen_data_service");
        composition.put("compositionExecuted", true);
        composition.put("screenId", screenId);
        composition.put("focusedControl", "field_ops.primary_action");
        composition.put("themeId", "echothemecore:ashfall");
        composition.put("layout", Map.of(
                "root", "sc_app_shell",
                "columns", 2,
                "safeArea", "echohudcore:hud/runtime_snapshot"
        ));
        composition.put("bindings", List.of(
                binding("mission.title", "Build a power cell"),
                binding("hazard.summary", "Ash storm warning"),
                binding("route.target", "echoholomap:layer/field_route")
        ));
        composition.put("components", List.of(
                component("header", "sc_page_header", "top"),
                component("mission", "mission_row", "left"),
                component("route", "sc_detail_panel", "right"),
                component("actions", "sc_action_strip", "bottom")
        ));
        composition.put("actions", List.of(
                action("open_index", "echoindex:recipe_search/index_query"),
                action("scan_target", "echolens:scanner/field_inspection"),
                action("open_route", "echoholomap:layer/field_route")
        ));
        composition.put("selectedAction", actionId);
        composition.put("inputResult", Map.of(
                "handled", true,
                "route", "echoindex:recipe_search/index_query",
                "focusAfter", "field_ops.primary_action"
        ));
        composition.put("diagnostics", List.of("screen.layout.ready", "screen.bindings.resolved"));
        composition.put("referenceBehavior", "screencore_composes_field_ops_dashboard");
        return Map.copyOf(composition);
    }

    public static boolean referenceCompositionPassed(Map<String, Object> composition) {
        return Boolean.TRUE.equals(composition.get("compositionExecuted"))
                && ADAPTERCORE_CONTRACT_ID.equals(composition.get("adapterCoreContract"))
                && REFERENCE_SCREEN_ID.equals(composition.get("screenId"))
                && REFERENCE_ACTION_ID.equals(composition.get("selectedAction"))
                && String.valueOf(composition.get("bindings")).contains("echoholomap:layer/field_route")
                && String.valueOf(composition.get("actions")).contains("echoindex:recipe_search/index_query")
                && String.valueOf(composition.get("inputResult")).contains("handled=true");
    }

    private static Map<String, String> binding(String key, String value) {
        Map<String, String> binding = new LinkedHashMap<>();
        binding.put("key", key);
        binding.put("value", value);
        return Map.copyOf(binding);
    }

    private static Map<String, String> component(String id, String type, String slot) {
        Map<String, String> component = new LinkedHashMap<>();
        component.put("id", id);
        component.put("type", type);
        component.put("slot", slot);
        return Map.copyOf(component);
    }

    private static Map<String, String> action(String id, String target) {
        Map<String, String> action = new LinkedHashMap<>();
        action.put("id", id);
        action.put("target", target);
        return Map.copyOf(action);
    }
}
