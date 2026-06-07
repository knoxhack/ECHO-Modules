package com.knoxhack.echoscreencore.client.style;

import com.knoxhack.echoscreencore.client.debug.EchoScreenDiagnostics;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;

public final class EchoStyleParser {
    public EchoStyleSheet parse(Identifier id, String css, EchoScreenDiagnostics diagnostics) {
        String clean = stripComments(css == null ? "" : css);
        ArrayList<EchoStyleRule> rules = new ArrayList<>();
        int cursor = 0;
        int order = 0;
        while (cursor < clean.length()) {
            int open = clean.indexOf('{', cursor);
            if (open < 0) {
                break;
            }
            int close = clean.indexOf('}', open + 1);
            if (close < 0) {
                if (diagnostics != null) {
                    diagnostics.warn("style_parse_failed", id + ": missing closing brace near " + clean.substring(Math.max(0, open - 16), open));
                }
                break;
            }
            String selectorList = clean.substring(cursor, open).strip();
            String body = clean.substring(open + 1, close);
            List<EchoStyleDeclaration> declarations = declarations(body);
            for (String selector : selectorList.split(",")) {
                if (!selector.isBlank()) {
                    String normalized = normalizePseudo(selector.strip());
                    if (normalized.contains(":") && diagnostics != null) {
                        diagnostics.warnOnce("unsupported_selector", id + " -> " + normalized);
                    }
                    rules.add(new EchoStyleRule(normalized, declarations, order++));
                }
            }
            cursor = close + 1;
        }
        return new EchoStyleSheet(id, List.copyOf(rules));
    }

    private static List<EchoStyleDeclaration> declarations(String body) {
        ArrayList<EchoStyleDeclaration> declarations = new ArrayList<>();
        for (String raw : body.split(";")) {
            int colon = raw.indexOf(':');
            if (colon <= 0) {
                continue;
            }
            String property = raw.substring(0, colon).strip();
            String value = raw.substring(colon + 1).strip();
            if (!property.isBlank() && !value.isBlank()) {
                declarations.add(new EchoStyleDeclaration(property, value));
            }
        }
        return List.copyOf(declarations);
    }

    private static String stripComments(String css) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < css.length()) {
            if (i + 1 < css.length() && css.charAt(i) == '/' && css.charAt(i + 1) == '*') {
                int end = css.indexOf("*/", i + 2);
                i = end < 0 ? css.length() : end + 2;
            } else {
                out.append(css.charAt(i++));
            }
        }
        return out.toString();
    }

    private static String normalizePseudo(String selector) {
        String value = selector == null ? "" : selector.strip();
        for (String state : List.of("hover", "hovered", "active", "focus", "focused", "selected", "disabled", "checked", "open")) {
            value = value.replace(":" + state, "[" + switch (state) {
                case "hover" -> "hovered";
                case "focus" -> "focused";
                default -> state;
            } + "]");
        }
        return value;
    }
}
