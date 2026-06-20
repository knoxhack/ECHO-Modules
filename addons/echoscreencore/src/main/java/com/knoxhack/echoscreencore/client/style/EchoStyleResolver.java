package com.knoxhack.echoscreencore.client.style;

import com.knoxhack.echoscreencore.api.style.EchoStyle;
import com.knoxhack.echoscreencore.api.theme.EchoAccessibilitySettings;
import com.knoxhack.echoscreencore.client.debug.EchoScreenDiagnostics;
import com.knoxhack.echoscreencore.client.parser.EchoNode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class EchoStyleResolver {
    private static final Set<String> KNOWN_PROPERTIES = Set.of(
        "width", "height", "min-width", "min-height", "max-width", "max-height",
        "padding", "padding-top", "padding-right", "padding-bottom", "padding-left",
        "margin", "margin-top", "margin-right", "margin-bottom", "margin-left",
        "gap", "layout", "direction", "columns", "rows",
        "align", "justify", "background", "border-color", "border-width", "border-radius",
        "color", "font-size", "opacity", "overflow", "wrap", "text-align", "visibility",
        "glow", "shadow", "z-index", "line-height", "max-lines", "tab-height",
        "title-color", "detail-color", "title-line-height", "detail-line-height",
        "text-gap", "content-height",
        "fit", "surface", "surface-depth", "background-texture", "background-texture-2",
        "background-texture-3", "texture-alpha", "texture-alpha-2", "texture-alpha-3",
        "texture-inset", "texture-inset-2", "texture-inset-3",
        "texture-fit", "texture-fit-2", "texture-fit-3",
        "texture-region", "texture-region-2", "texture-region-3",
        "glow-strength", "shadow-strength", "inner-highlight", "corner-treatment",
        "accent-color", "track-color", "fill-color", "segmented", "segment-size",
        "collapse-below", "hide-below", "stack-below"
        , "compact-below", "dense-below", "sidebar-collapse-below", "detail-collapse-below"
    );

    public EchoStyle resolve(EchoNode node, List<EchoStyleSheet> styleSheets, EchoAccessibilitySettings accessibility,
            EchoScreenDiagnostics diagnostics) {
        return resolve(node, List.of(), styleSheets, accessibility, diagnostics, EchoStyleState.NONE);
    }

    public EchoStyle resolve(EchoNode node, List<EchoNode> ancestors, List<EchoStyleSheet> styleSheets,
            EchoAccessibilitySettings accessibility, EchoScreenDiagnostics diagnostics, EchoStyleState state) {
        LinkedHashMap<String, String> values = new LinkedHashMap<>(defaults(node));
        ArrayList<EchoStyleRule> matches = new ArrayList<>();
        for (EchoStyleSheet sheet : styleSheets == null ? List.<EchoStyleSheet>of() : styleSheets) {
            for (EchoStyleRule rule : sheet.rules()) {
                if (rule.matches(node, ancestors, state)) {
                    matches.add(rule);
                }
            }
        }
        matches.sort(Comparator.comparingInt(EchoStyleRule::specificity).thenComparingInt(EchoStyleRule::order));
        for (EchoStyleRule rule : matches) {
            for (EchoStyleDeclaration declaration : rule.declarations()) {
                String property = EchoStyle.normalize(declaration.property());
                if (!KNOWN_PROPERTIES.contains(property) && !property.startsWith("--echo-")) {
                    if (diagnostics != null) {
                        diagnostics.warn("unknown_style_property", rule.selector() + " -> " + property);
                    }
                    continue;
                }
                values.put(property, declaration.value());
            }
        }
        for (Map.Entry<String, String> attribute : node.attributes().entrySet()) {
            String property = EchoStyle.normalize(attribute.getKey());
            if (KNOWN_PROPERTIES.contains(property)) {
                values.put(property, attribute.getValue());
            }
        }
        applyAccessibility(values, accessibility == null ? EchoAccessibilitySettings.DEFAULT : accessibility);
        return new EchoStyle(values);
    }

    private static Map<String, String> defaults(EchoNode node) {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        String tag = node == null ? "" : node.tagName();
        values.put("layout", switch (tag) {
            case "grid" -> "grid";
            case "row", "list-row", "dialog-actions", "status-chip-row" -> "row";
            default -> "column";
        });
        values.put("visibility", "visible");
        values.put("wrap", "false");
        values.put("gap", "0");
        if (tag.equals("button")) {
            values.put("height", "32px");
            values.put("padding", "8px 12px");
        } else if (tag.equals("title")) {
            values.put("font-size", "font(title)");
        } else if (tag.equals("label")) {
            values.put("font-size", "font(small)");
        } else if (tag.equals("text")) {
            values.put("font-size", "font(body)");
            values.put("wrap", "true");
        } else if (tag.equals("status-chip")) {
            values.put("height", "18px");
            values.put("padding", "3px 8px");
        } else if (tag.equals("icon") || tag.equals("item-icon") || tag.equals("item-stack")) {
            values.put("width", "20px");
            values.put("height", "20px");
        } else if (tag.equals("image")) {
            values.put("height", "64px");
        } else if (tag.equals("list")) {
            values.put("gap", "space(xs)");
        } else if (tag.equals("list-row")) {
            values.put("min-height", "48px");
            values.put("padding", "space(sm)");
            values.put("gap", "space(sm)");
            values.put("border-width", "1");
        } else if (tag.equals("tabs")) {
            values.put("min-height", "120px");
            values.put("padding", "space(sm)");
            values.put("border-width", "1");
        } else if (tag.equals("input") || tag.equals("search-box") || tag.equals("select") || tag.equals("dropdown")) {
            values.put("height", "30px");
            values.put("padding", "6px 10px");
            values.put("border-width", "1");
        } else if (tag.equals("dialog") || tag.equals("modal")) {
            values.put("width", "440px");
            values.put("height", "240px");
            values.put("padding", "space(lg)");
            values.put("gap", "space(sm)");
            values.put("background", "theme(panel)");
            values.put("border-color", "theme(borderStrong)");
            values.put("border-width", "1");
        } else if (tag.equals("dialog-title")) {
            values.put("font-size", "font(title)");
            values.put("color", "theme(textPrimary)");
        } else if (tag.equals("dialog-actions")) {
            values.put("gap", "space(sm)");
        } else if (tag.equals("tooltip")) {
            values.put("padding", "space(xs)");
        } else if (tag.equals("app-shell")) {
            values.put("padding", "space(lg)");
            values.put("gap", "space(md)");
        } else if (tag.equals("app-sidebar") || tag.equals("detail-panel") || tag.equals("inspector-panel")) {
            values.put("padding", "space(md)");
            values.put("gap", "space(sm)");
            values.put("border-width", "1");
            values.put("background", "theme(panel)");
        } else if (tag.equals("nav-list")) {
            values.put("gap", "space(xs)");
        } else if (tag.equals("nav-item")) {
            values.put("min-height", "36px");
            values.put("padding", "space(sm)");
            values.put("border-width", "1");
        } else if (tag.equals("command-card")) {
            values.put("padding", "space(md)");
            values.put("gap", "space(sm)");
            values.put("border-width", "1");
        } else if (tag.equals("toggle") || tag.equals("checkbox")) {
            values.put("height", "24px");
            values.put("border-width", "1");
        } else if (tag.equals("repeat")) {
            values.put("gap", "space(sm)");
        }
        return values;
    }

    private static void applyAccessibility(Map<String, String> values, EchoAccessibilitySettings accessibility) {
        if (accessibility.largeText()) {
            values.computeIfPresent("font-size", (key, value) -> "scale(" + value + ",1.25)");
            if (values.containsKey("height")) {
                values.put("min-height", "36px");
            }
        }
        if (accessibility.highContrast()) {
            values.putIfAbsent("border-width", "1");
            values.put("color", "theme(textPrimary)");
            values.put("border-color", "theme(borderStrong)");
        }
        if (accessibility.quietVisuals()) {
            values.put("glow", "false");
            values.put("shadow", "false");
        }
    }
}
