package com.knoxhack.echoscreencore.client.reference;

import com.knoxhack.echoscreencore.api.EchoDataContext;
import com.knoxhack.echoscreencore.api.EchoScreenRegistry;
import com.knoxhack.echoscreencore.api.EchoScreens;
import com.knoxhack.echoscreencore.client.debug.EchoDiagnosticCatalog;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;

public final class ScreenCoreReferenceData {
    private static String selectedCategoryId = "getting_started";
    private static String selectedFeatureId = "start_from_reference";
    private static String selectedRecordId = "record_route";

    private static final List<Map<String, Object>> CATEGORIES = List.of(
            row("id", "getting_started", "title", "Getting Started", "summary", "Start from a proven page and starter shell."),
            row("id", "pages_manifest", "title", "Pages And Manifest", "summary", "Resource ids, manifest metadata, and discovery."),
            row("id", "styles_tokens", "title", "Styles And Tokens", "summary", "Supported selectors, properties, and tokens."),
            row("id", "layout", "title", "Layout Containers", "summary", "Rows, columns, grids, stacks, panels, and scroll owners."),
            row("id", "responsive", "title", "Responsive Rules", "summary", "stack-below, hide-below, compact, dense, and collapse."),
            row("id", "lists_data", "title", "Lists And Repeated Data", "summary", "Provider-backed rows, empty states, and selection."),
            row("id", "actions", "title", "Actions And Navigation", "summary", "Built-ins, addon actions, and page links."),
            row("id", "inputs", "title", "Inputs And Search", "summary", "Inputs, search boxes, state keys, and validation."),
            row("id", "selects", "title", "Selects And Dropdowns", "summary", "Static and provider-backed option surfaces."),
            row("id", "overlays", "title", "Tooltips And Overlays", "summary", "Tooltips, dropdowns, and clamped overlay behavior."),
            row("id", "modals", "title", "Dialogs And Modals", "summary", "Modal structure, actions, and focus trap behavior."),
            row("id", "focus", "title", "Focus And Keyboard", "summary", "Keyboard traversal and activation rules."),
            row("id", "accessibility", "title", "Accessibility Modes", "summary", "Large text, contrast, reduced clutter, and density."),
            row("id", "diagnostics", "title", "Diagnostics And Debugging", "summary", "Debug overlay, inspect commands, and fix hints."),
            row("id", "patterns", "title", "Reference Patterns", "summary", "Copyable dashboard, list/detail, settings, and dense-list pages."),
            row("id", "bad_fixes", "title", "Bad Layout Fixes", "summary", "Broken examples paired with corrected patterns.")
    );

    private static final List<Map<String, Object>> FEATURES = List.of(
            feature("start_from_reference", "getting_started", "Start From A Reference", "Copy a real reference page before inventing a layout.", "echoscreencore:reference_dashboard", "reference_page_failed_contract"),
            feature("manifest_entries", "pages_manifest", "Manifest Entries", "Register pages, components, starter resources, and feature metadata.", "echoscreencore:reference_feature_hub", "unknown_reference_page"),
            feature("style_contract", "styles_tokens", "Style Contract", "Use ScreenCore-supported properties and ThemeCore tokens only.", "echoscreencore:reference_settings", "unknown_style_property"),
            feature("safe_grids", "layout", "Safe Grids", "Every multi-column grid needs stack-below.", "echoscreencore:reference_three_column", "grid_missing_stack_below"),
            feature("single_scroll_owner", "layout", "Single Scroll Owner", "Dense content should have exactly one scroll owner.", "echoscreencore:reference_dense_list", "nested_scroll_region"),
            feature("responsive_hooks", "responsive", "Responsive Hooks", "Use stack, hide, compact, dense, and collapse breakpoints deliberately.", "echoscreencore:reference_three_column", "grid_missing_stack_below"),
            feature("provider_lists", "lists_data", "Provider Lists", "Bind lists to provider data and include empty states.", "echoscreencore:reference_dense_list", "missing_list_empty_state"),
            feature("list_detail", "lists_data", "List Detail", "Use fixed badges and a flexible copy column.", "echoscreencore:reference_list_detail", "unbounded_row_text"),
            feature("actions_navigation", "actions", "Actions And Navigation", "Use built-ins or registered addon actions.", "echoscreencore:reference_feature_hub", "action_not_registered"),
            feature("inputs_search", "inputs", "Inputs And Search", "Use state keys and provider actions for filtering.", "echoscreencore:reference_inputs", "text_overflow_without_scroll"),
            feature("select_controls", "selects", "Select Controls", "Use static or provider-backed options.", "echoscreencore:reference_selects_dropdowns", "missing_list_empty_state"),
            feature("tooltip_overlays", "overlays", "Tooltips", "Keep overlay content short and edge-clamped.", "echoscreencore:reference_modal_overlay", "overflow_without_scroll"),
            feature("modal_states", "modals", "Dialog States", "Use dialog body/actions and close actions.", "echoscreencore:reference_modal_overlay", "modal_missing"),
            feature("focus_keyboard", "focus", "Focus And Keyboard", "Focusable rows and controls should be reachable with keyboard.", "echoscreencore:reference_accessibility", "component_outside_parent_bounds"),
            feature("accessibility_modes", "accessibility", "Accessibility Modes", "Design for large text, high contrast, and compact density.", "echoscreencore:reference_accessibility", "large_fixed_height"),
            feature("inspect_debug", "diagnostics", "Inspect And Debug", "Use inspect commands, workbench, and debug overlay fix hints.", "echoscreencore:reference_workbench", "reference_page_failed_contract"),
            feature("copy_patterns", "patterns", "Copy Patterns", "Reference pages are the source of truth for snippets.", "echoscreencore:reference_list_detail", "reference_page_failed_contract"),
            feature("bad_to_good", "bad_fixes", "Bad To Good", "Broken layouts exist to teach diagnostics.", "echoscreencore:reference_bad_layouts", "grid_missing_stack_below")
    );

    private static final List<Map<String, Object>> REFERENCE_PAGES = List.of(
            reference("echoscreencore:reference_feature_hub", "Feature Hub", "Central ScreenCore wiki and category browser.", "hub"),
            reference("echoscreencore:reference_workbench", "Workbench", "Preview and inspect reference pages.", "debug"),
            reference("echoscreencore:reference_dashboard", "Dashboard", "Summary cards, action rows, and bounded panels.", "layout"),
            reference("echoscreencore:reference_list_detail", "List Detail", "Safe list/detail layout with one scroll owner.", "list"),
            reference("echoscreencore:reference_three_column", "Three Column", "Responsive left-center-right screen pattern.", "layout"),
            reference("echoscreencore:reference_dense_list", "Dense List", "Provider rows, empty states, and scroll ownership.", "list"),
            reference("echoscreencore:reference_settings", "Settings", "Toggles, selects, and compact form rows.", "input"),
            reference("echoscreencore:reference_inputs", "Inputs", "Search/input controls and state keys.", "input"),
            reference("echoscreencore:reference_selects_dropdowns", "Selects", "Select and dropdown menu patterns.", "input"),
            reference("echoscreencore:reference_modal_overlay", "Modal Overlay", "Tooltips, dialogs, and overlay actions.", "overlay"),
            reference("echoscreencore:reference_accessibility", "Accessibility", "Large text, contrast, reduced clutter, density.", "access"),
            reference("echoscreencore:reference_bad_layouts", "Bad Layout Fixes", "Intentional diagnostics and corrected patterns.", "warn")
    );

    private static final List<Map<String, Object>> RECORDS = List.of(
            record("record_route", "Route Browser", "A list/detail screen with bounded rows and a single scroll owner.", "ready", "READY", "Use this when a screen has selectable records and a detail panel.", "echoscreencore:reference_list_detail"),
            record("record_dashboard", "Operational Dashboard", "Summary metrics and action cards that stack on small screens.", "active", "ACTIVE", "Use this for overview screens with cards and quick actions.", "echoscreencore:reference_dashboard"),
            record("record_settings", "Settings Surface", "Compact toggles, selects, and reset actions.", "info", "INFO", "Use this for options screens that need predictable row heights.", "echoscreencore:reference_settings"),
            record("record_overlays", "Overlay States", "Tooltip, dropdown, and modal examples.", "warning", "WARN", "Use this when a screen needs temporary UI layers.", "echoscreencore:reference_modal_overlay")
    );

    private ScreenCoreReferenceData() {
    }

    public static void register() {
        EchoScreenRegistry.registerDataProvider("screencore", ScreenCoreReferenceData::resolve);
        EchoScreenRegistry.registerDataProvider("screencore.reference", ScreenCoreReferenceData::resolve);
        EchoScreenRegistry.registerAction("screencore.reference.select_category", context -> {
            selectedCategoryId = safeValue(context.actionValue(), selectedCategoryId);
            selectedFeatureId = firstFeatureForCategory(selectedCategoryId).orElse(selectedFeatureId);
            EchoScreens.invalidateData();
            return true;
        });
        EchoScreenRegistry.registerAction("screencore.reference.select_feature", context -> {
            selectedFeatureId = safeValue(context.actionValue(), selectedFeatureId);
            feature(selectedFeatureId).ifPresent(feature -> selectedCategoryId = String.valueOf(feature.get("category")));
            EchoScreens.invalidateData();
            return true;
        });
        EchoScreenRegistry.registerAction("screencore.reference.select_record", context -> {
            selectedRecordId = safeValue(context.actionValue(), selectedRecordId);
            EchoScreens.invalidateData();
            return true;
        });
        EchoScreenRegistry.registerAction("screencore.reference.open_reference", context -> openReference(context.actionValue(), context::open));
        EchoScreenRegistry.registerAction("screencore.reference.open_selected", context -> {
            Object page = selectedRecord().get("page");
            return openReference(String.valueOf(page), context::open);
        });
    }

    public static Object resolve(EchoDataContext context, List<String> path) {
        List<String> cleanPath = normalize(path);
        if (cleanPath.isEmpty()) {
            return Map.of("title", "ScreenCore Reference");
        }
        return switch (cleanPath.get(0)) {
            case "categories" -> CATEGORIES;
            case "selectedCategoryId" -> selectedCategoryId;
            case "selectedCategory" -> category(selectedCategoryId).orElse(CATEGORIES.get(0));
            case "features" -> featuresForSelectedCategory();
            case "allFeatures" -> FEATURES;
            case "selectedFeatureId" -> selectedFeatureId;
            case "selectedFeature" -> feature(selectedFeatureId).orElse(featuresForSelectedCategory().get(0));
            case "referencePages" -> REFERENCE_PAGES;
            case "diagnostics" -> EchoDiagnosticCatalog.rows();
            case "viewports" -> viewports();
            case "records" -> RECORDS;
            case "selectedRecordId" -> selectedRecordId;
            case "selectedRecord" -> selectedRecord();
            case "workbench" -> ScreenCoreWorkbenchState.data();
            default -> null;
        };
    }

    public static List<Map<String, Object>> referencePages() {
        return REFERENCE_PAGES;
    }

    public static List<String> referencePageIds() {
        return REFERENCE_PAGES.stream().map(row -> String.valueOf(row.get("id"))).toList();
    }

    private static List<String> normalize(List<String> path) {
        if (path == null || path.isEmpty()) {
            return List.of();
        }
        if ("reference".equals(path.get(0))) {
            return List.copyOf(path.subList(1, path.size()));
        }
        return List.copyOf(path);
    }

    private static List<Map<String, Object>> featuresForSelectedCategory() {
        List<Map<String, Object>> rows = FEATURES.stream()
                .filter(row -> selectedCategoryId.equals(row.get("category")))
                .toList();
        return rows.isEmpty() ? FEATURES.subList(0, 1) : rows;
    }

    private static Optional<Map<String, Object>> category(String id) {
        return CATEGORIES.stream().filter(row -> id.equals(row.get("id"))).findFirst();
    }

    private static Optional<Map<String, Object>> feature(String id) {
        return FEATURES.stream().filter(row -> id.equals(row.get("id"))).findFirst();
    }

    private static Optional<String> firstFeatureForCategory(String categoryId) {
        return FEATURES.stream()
                .filter(row -> categoryId.equals(row.get("category")))
                .map(row -> String.valueOf(row.get("id")))
                .findFirst();
    }

    private static Map<String, Object> selectedRecord() {
        return RECORDS.stream()
                .filter(row -> selectedRecordId.equals(row.get("id")))
                .findFirst()
                .orElse(RECORDS.get(0));
    }

    private static boolean openReference(String raw, java.util.function.Function<Identifier, Boolean> opener) {
        if (raw == null || raw.isBlank() || opener == null) {
            return false;
        }
        try {
            return opener.apply(Identifier.parse(raw.strip()));
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private static String safeValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.strip();
    }

    private static Map<String, Object> feature(String id, String category, String title, String body, String page, String diagnostic) {
        return row("id", id, "category", category, "title", title, "body", body,
                "page", page, "diagnostic", diagnostic, "badge", categoryBadge(category));
    }

    private static Map<String, Object> reference(String id, String title, String summary, String badge) {
        return row("id", id, "title", title, "summary", summary, "badge", badge.toUpperCase(Locale.ROOT), "status", "ready");
    }

    private static Map<String, Object> record(String id, String title, String summary, String status, String badge, String body, String page) {
        return row("id", id, "title", title, "summary", summary, "status", status, "badge", badge, "body", body, "page", page);
    }

    private static List<Map<String, Object>> viewports() {
        return List.of(
                row("id", "small", "label", "360x240", "width", 360, "height", 240),
                row("id", "default", "label", "854x480", "width", 854, "height", 480),
                row("id", "large", "label", "1280x720", "width", 1280, "height", 720));
    }

    private static String categoryBadge(String category) {
        String[] parts = category.split("_");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (!part.isBlank()) {
                out.append(Character.toUpperCase(part.charAt(0)));
            }
        }
        return out.isEmpty() ? "SC" : out.toString();
    }

    private static Map<String, Object> row(Object... values) {
        LinkedHashMapBuilder builder = new LinkedHashMapBuilder();
        for (int i = 0; i + 1 < values.length; i += 2) {
            builder.put(String.valueOf(values[i]), values[i + 1]);
        }
        return builder.build();
    }

    private static final class LinkedHashMapBuilder {
        private final java.util.LinkedHashMap<String, Object> values = new java.util.LinkedHashMap<>();

        void put(String key, Object value) {
            values.put(key, value);
        }

        Map<String, Object> build() {
            return Map.copyOf(values);
        }
    }
}
