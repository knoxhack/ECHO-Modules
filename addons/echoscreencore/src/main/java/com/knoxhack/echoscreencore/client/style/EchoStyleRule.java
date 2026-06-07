package com.knoxhack.echoscreencore.client.style;

import com.knoxhack.echoscreencore.client.parser.EchoNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class EchoStyleRule {
    private final String selector;
    private final List<EchoStyleDeclaration> declarations;
    private final int order;
    private final ParsedSelector parsed;

    public EchoStyleRule(String selector, List<EchoStyleDeclaration> declarations, int order) {
        this.selector = selector == null ? "" : selector.strip();
        this.declarations = declarations == null ? List.of() : List.copyOf(declarations);
        this.order = order;
        this.parsed = ParsedSelector.parse(this.selector);
    }

    public String selector() {
        return selector;
    }

    public List<EchoStyleDeclaration> declarations() {
        return declarations;
    }

    public int order() {
        return order;
    }

    public int specificity() {
        return parsed.specificity();
    }

    public boolean matches(EchoNode node) {
        return parsed.matches(node, List.of(), EchoStyleState.NONE);
    }

    public boolean matches(EchoNode node, List<EchoNode> ancestors, EchoStyleState state) {
        return parsed.matches(node, ancestors, state == null ? EchoStyleState.NONE : state);
    }

    private record ParsedSelector(
        List<SimpleSelector> chain,
        int specificity
    ) {
        private static ParsedSelector parse(String selector) {
            String value = selector == null ? "" : selector.strip().toLowerCase(Locale.ROOT);
            ArrayList<SimpleSelector> chain = new ArrayList<>();
            int specificity = 0;
            for (String part : value.split("\\s+")) {
                String token = part.strip();
                if (token.isBlank()) {
                    continue;
                }
                SimpleSelector simple = SimpleSelector.parse(token);
                chain.add(simple);
                specificity += simple.specificity();
            }
            if (chain.isEmpty()) {
                SimpleSelector simple = SimpleSelector.parse(value);
                chain.add(simple);
                specificity = simple.specificity();
            }
            return new ParsedSelector(List.copyOf(chain), specificity);
        }

        private boolean matches(EchoNode node, List<EchoNode> ancestors, EchoStyleState state) {
            if (node == null || chain.isEmpty()) {
                return false;
            }
            int current = chain.size() - 1;
            if (!chain.get(current).matches(node, state)) {
                return false;
            }
            current--;
            if (current < 0) {
                return true;
            }
            List<EchoNode> safeAncestors = ancestors == null ? List.of() : ancestors;
            int ancestorIndex = safeAncestors.size() - 1;
            while (current >= 0 && ancestorIndex >= 0) {
                if (chain.get(current).matches(safeAncestors.get(ancestorIndex), EchoStyleState.NONE)) {
                    current--;
                }
                ancestorIndex--;
            }
            return current < 0;
        }
    }

    private record SimpleSelector(
        String tag,
        String id,
        List<String> classes,
        List<AttributeSelector> attributes,
        int specificity
    ) {
        private static SimpleSelector parse(String selector) {
            String value = selector == null ? "" : selector.strip().toLowerCase(Locale.ROOT);
            ArrayList<AttributeSelector> attributes = new ArrayList<>();
            int attrStart = value.indexOf('[');
            while (attrStart >= 0) {
                int attrEnd = value.indexOf(']', attrStart);
                if (attrEnd <= attrStart) {
                    break;
                }
                String raw = value.substring(attrStart + 1, attrEnd).strip();
                value = (value.substring(0, attrStart) + value.substring(attrEnd + 1)).strip();
                int equals = raw.indexOf('=');
                if (equals >= 0) {
                    attributes.add(new AttributeSelector(raw.substring(0, equals).strip(),
                            stripQuotes(raw.substring(equals + 1).strip()), false));
                } else if (!raw.isBlank()) {
                    attributes.add(new AttributeSelector(raw, "", true));
                }
                attrStart = value.indexOf('[');
            }
            int pseudoStart = value.indexOf(':');
            while (pseudoStart >= 0) {
                int pseudoEnd = pseudoStart + 1;
                while (pseudoEnd < value.length()) {
                    char c = value.charAt(pseudoEnd);
                    if (!Character.isLetterOrDigit(c) && c != '-' && c != '_') {
                        break;
                    }
                    pseudoEnd++;
                }
                String pseudo = value.substring(pseudoStart + 1, pseudoEnd).strip();
                attributes.add(pseudoAttribute(pseudo));
                value = (value.substring(0, pseudoStart) + value.substring(pseudoEnd)).strip();
                pseudoStart = value.indexOf(':');
            }

            String tag = "";
            String id = "";
            ArrayList<String> classes = new ArrayList<>();
            StringBuilder token = new StringBuilder();
            char mode = 't';
            for (int i = 0; i <= value.length(); i++) {
                char c = i == value.length() ? '\0' : value.charAt(i);
                if (c == '#' || c == '.' || c == '\0') {
                    applyToken(mode, token.toString(), classes);
                    if (mode == 't' && !token.isEmpty()) {
                        tag = token.toString();
                    } else if (mode == '#') {
                        id = token.toString();
                    }
                    token.setLength(0);
                    mode = c;
                } else {
                    token.append(c);
                }
            }
            int specificity = (id.isBlank() ? 0 : 100) + classes.size() * 10 + attributes.size() * 10 + (tag.isBlank() ? 0 : 1);
            return new SimpleSelector(tag, id, List.copyOf(classes), List.copyOf(attributes), specificity);
        }

        private static AttributeSelector pseudoAttribute(String pseudo) {
            return switch (pseudo == null ? "" : pseudo.strip().toLowerCase(Locale.ROOT)) {
                case "hover", "hovered" -> new AttributeSelector("hovered", "", true);
                case "focus", "focused" -> new AttributeSelector("focused", "", true);
                case "disabled" -> new AttributeSelector("disabled", "", true);
                case "selected" -> new AttributeSelector("selected", "", true);
                case "active" -> new AttributeSelector("active", "", true);
                default -> new AttributeSelector("__unsupported_pseudo_" + pseudo, "", true);
            };
        }

        private static void applyToken(char mode, String raw, List<String> classes) {
            String token = raw == null ? "" : raw.strip();
            if (token.isBlank()) {
                return;
            }
            if (mode == '.') {
                classes.add(token);
            }
        }

        private boolean matches(EchoNode node, EchoStyleState state) {
            if (node == null) {
                return false;
            }
            if (!tag.isBlank() && !tag.equals(node.tagName())) {
                return false;
            }
            if (!id.isBlank() && !id.equals(node.id())) {
                return false;
            }
            for (String cls : classes) {
                if (!node.classes().contains(cls)) {
                    return false;
                }
            }
            for (AttributeSelector attribute : attributes) {
                if (stateAttribute(attribute.name())) {
                    if (stateAttributeMatches(attribute.name(), attribute.value(), attribute.presence(), state)
                            || nodeStateAttributeMatches(node, attribute)) {
                        continue;
                    }
                    return false;
                } else {
                    if (!node.hasAttribute(attribute.name())) {
                        return false;
                    }
                    if (!attribute.presence() && !attribute.value().equalsIgnoreCase(node.attribute(attribute.name(), ""))) {
                        return false;
                    }
                }
            }
            return true;
        }

        private static boolean stateAttribute(String attribute) {
            return switch (attribute) {
                case "hovered", "focused", "disabled", "selected", "active" -> true;
                default -> false;
            };
        }

        private static boolean stateAttributeMatches(String attribute, String attributeValue,
                boolean attributePresence, EchoStyleState state) {
            boolean matched = switch (attribute) {
                case "hovered" -> state.hovered();
                case "focused" -> state.focused();
                case "disabled" -> state.disabled();
                case "selected" -> state.selected();
                case "active" -> state.active();
                default -> false;
            };
            if (!matched) {
                return false;
            }
            return attributePresence || truthy(attributeValue);
        }

        private static boolean nodeStateAttributeMatches(EchoNode node, AttributeSelector attribute) {
            if (!node.hasAttribute(attribute.name())) {
                return false;
            }
            String raw = node.attribute(attribute.name(), "");
            if (raw.contains("{") || raw.contains("}")) {
                return false;
            }
            return attribute.presence()
                    ? truthy(raw)
                    : attribute.value().equalsIgnoreCase(raw);
        }

        private static boolean truthy(String value) {
            String normalized = value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
            return normalized.isBlank()
                    || (!"false".equals(normalized) && !"0".equals(normalized)
                            && !"no".equals(normalized) && !"off".equals(normalized));
        }

        private static String stripQuotes(String value) {
            if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                return value.substring(1, value.length() - 1);
            }
            return value;
        }
    }

    private record AttributeSelector(String name, String value, boolean presence) {
    }
}
