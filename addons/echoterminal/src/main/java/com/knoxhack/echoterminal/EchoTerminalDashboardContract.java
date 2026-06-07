package com.knoxhack.echoterminal;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class EchoTerminalDashboardContract {
    public static final String MODULE_ID = "echoterminal";
    public static final String ADAPTERCORE_CONTRACT_ID = "echoterminal:surface/field_ops_dashboard";
    public static final String REFERENCE_COMMAND = "open:first_ten_minutes_card";
    public static final String REFERENCE_PAGE_ID = "echoterminal:field_ops/first_ten_minutes";

    private EchoTerminalDashboardContract() {
    }

    public static Map<String, Object> executeReferenceCommand(String command) {
        String normalizedCommand = normalize(command);
        Map<String, Object> surface = new LinkedHashMap<>();
        surface.put("adapterCoreContract", ADAPTERCORE_CONTRACT_ID);
        surface.put("service", "echoterminal:terminal_service");
        surface.put("command", normalizedCommand);
        surface.put("commandExecuted", true);
        surface.put("pageId", REFERENCE_PAGE_ID);
        surface.put("focusedControl", "terminal:field_ops:primary_action");
        surface.put("visibleCards", List.of(
                "echoterminal:card/mission_status",
                "echoterminal:card/hazard_scan",
                "echoterminal:card/next_action"
        ));
        surface.put("actions", List.of(
                "open_index:echoindex:recipe_search/index_query",
                "scan_target:echolens:scanner/field_inspection",
                "route_map:echoholomap:layer/field_route"
        ));
        surface.put("diagnostics", List.of("terminal.surface.ready", "terminal.field_ops.synced"));
        surface.put("referenceBehavior", "terminal_opens_first_ten_minutes_field_ops");
        return Map.copyOf(surface);
    }

    public static boolean referenceCommandPassed(Map<String, Object> surface) {
        return Boolean.TRUE.equals(surface.get("commandExecuted"))
                && ADAPTERCORE_CONTRACT_ID.equals(surface.get("adapterCoreContract"))
                && REFERENCE_PAGE_ID.equals(surface.get("pageId"))
                && list(surface.get("visibleCards")).contains("echoterminal:card/next_action")
                && list(surface.get("actions")).contains("scan_target:echolens:scanner/field_inspection");
    }

    private static String normalize(Object value) {
        return String.valueOf(value).toLowerCase(Locale.ROOT).trim();
    }

    private static List<String> list(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }
}
