package com.knoxhack.echoscreencore.client.debug;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;

public final class EchoDiagnosticCatalog {
    private static final LinkedHashMap<String, Entry> ENTRIES = new LinkedHashMap<>();

    static {
        register("root_overflows_viewport", "Root Overflows Viewport",
                "The page is larger than the GUI viewport and has no safe owner for overflow.",
                "Start from sc_app_shell, reduce fixed sizes, or move long content into a scroll region.",
                "echoscreencore:reference_bad_layouts");
        register("large_fixed_height", "Large Fixed Height",
                "Fixed panels above 260px are brittle at high GUI scale and small windows.",
                "Move long content into sc-scroll-region or replace the fixed height with min/max bounds.",
                "echoscreencore:reference_dense_list");
        register("grid_missing_stack_below", "Grid Missing stack-below",
                "A multi-column grid cannot reliably fit narrow GUI-space widths without a stacking rule.",
                "Add stack-below=\"900\" or use sc-list-detail/sc-three-column from the starter kit.",
                "echoscreencore:reference_three_column");
        register("nested_scroll_region", "Nested Scroll Region",
                "Nested scroll panels fight for wheel/focus ownership and make small screens hard to use.",
                "Keep one scroll owner for each content path.",
                "echoscreencore:reference_dense_list");
        register("row_overflow", "Row Overflow",
                "A row child is extending outside the row bounds.",
                "Use fixed-width badges/icons and one flexible sc-row-copy column.",
                "echoscreencore:reference_list_detail");
        register("unbounded_row_text", "Unbounded Row Text",
                "Text in a row can push controls offscreen or become unreadable.",
                "Use sc-row-copy or set max-lines plus wrap/overflow intentionally.",
                "echoscreencore:reference_list_detail");
        register("missing_list_empty_state", "Missing List Empty State",
                "Provider-backed lists need a graceful empty state when filters return no rows.",
                "Add an empty-state child to the list.",
                "echoscreencore:reference_dense_list");
        register("unknown_reference_page", "Unknown Reference Page",
                "A reference link points at a page ScreenCore cannot discover.",
                "Fix the manifest id or add the missing reference page resource.",
                "echoscreencore:reference_feature_hub");
        register("reference_page_failed_contract", "Reference Page Failed Contract",
                "A page advertised as a reference pattern violates the ScreenCore authoring contract.",
                "Open it in the workbench and resolve the listed diagnostics at every required viewport.",
                "echoscreencore:reference_workbench");
        register("component_outside_parent_bounds", "Component Outside Parent Bounds",
                "A child component was laid out beyond its parent.",
                "Reduce fixed sizes, add stack-below, or make the parent a scroll owner.",
                "echoscreencore:reference_bad_layouts");
        register("overflow_without_scroll", "Overflow Without Scroll",
                "Content exceeds a non-scroll parent.",
                "Wrap the long content in a single scroll region.",
                "echoscreencore:reference_dense_list");
        register("grid_columns_overflow", "Grid Columns Overflow",
                "Fixed grid columns exceed the available width.",
                "Use fr tracks, smaller fixed tracks, or stack-below.",
                "echoscreencore:reference_three_column");
    }

    private EchoDiagnosticCatalog() {
    }

    public static Entry get(String code) {
        return ENTRIES.getOrDefault(code, new Entry(
                code == null ? "" : code,
                "ScreenCore Diagnostic",
                "ScreenCore reported a layout, binding, or resource issue.",
                "Inspect the page in the ScreenCore workbench and fix the reported component.",
                "echoscreencore:reference_workbench"));
    }

    public static List<Entry> entries() {
        return List.copyOf(ENTRIES.values());
    }

    public static List<Map<String, Object>> rows() {
        ArrayList<Map<String, Object>> rows = new ArrayList<>();
        for (Entry entry : ENTRIES.values()) {
            rows.add(Map.of(
                    "code", entry.code(),
                    "title", entry.title(),
                    "why", entry.whyItMatters(),
                    "fix", entry.fixHint(),
                    "page", entry.referencePage()));
        }
        return List.copyOf(rows);
    }

    public static String fixHint(String code) {
        return get(code).fixHint();
    }

    public static Identifier referencePage(String code) {
        try {
            return Identifier.parse(get(code).referencePage());
        } catch (RuntimeException exception) {
            return Identifier.fromNamespaceAndPath("echoscreencore", "reference_workbench");
        }
    }

    private static void register(String code, String title, String whyItMatters, String fixHint, String referencePage) {
        ENTRIES.put(code, new Entry(code, title, whyItMatters, fixHint, referencePage));
    }

    public record Entry(
            String code,
            String title,
            String whyItMatters,
            String fixHint,
            String referencePage) {
    }
}
