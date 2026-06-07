package com.knoxhack.echoscreencore.client.input;

import com.knoxhack.echoscreencore.client.component.EchoComponent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class EchoFocusManager {
    private final ArrayList<EchoComponent> focusOrder = new ArrayList<>();
    private EchoComponent focused;

    public void rebuild(EchoComponent root) {
        focusOrder.clear();
        collectSorted(root, false);
        if (focused != null && !focusOrder.contains(focused)) {
            focused.setFocused(false);
            focused = null;
        }
        if (focused == null) {
            focus(autofocusOrFirst());
        }
    }

    public void trap(EchoComponent root) {
        focusOrder.clear();
        collectSorted(root, true);
        if (focused != null && !focusOrder.contains(focused)) {
            focused.setFocused(false);
            focused = null;
        }
        if (focused == null) {
            focus(autofocusOrFirst());
        }
    }

    public List<EchoComponent> focusOrder() {
        return List.copyOf(focusOrder);
    }

    public EchoComponent focused() {
        return focused;
    }

    public void focus(EchoComponent component) {
        if (focused == component) {
            return;
        }
        if (focused != null) {
            focused.setFocused(false);
        }
        focused = component != null && component.focusable() && !component.disabled() ? component : null;
        if (focused != null) {
            focused.setFocused(true);
        }
    }

    public void clearFocus() {
        if (focused != null) {
            focused.setFocused(false);
        }
        focused = null;
    }

    public boolean focusFirst() {
        if (focusOrder.isEmpty()) {
            return false;
        }
        focus(focusOrder.get(0));
        return true;
    }

    public boolean focusNext() {
        if (focusOrder.isEmpty()) {
            return false;
        }
        int index = focused == null ? -1 : focusOrder.indexOf(focused);
        focus(focusOrder.get((index + 1 + focusOrder.size()) % focusOrder.size()));
        return true;
    }

    public boolean focusPrevious() {
        if (focusOrder.isEmpty()) {
            return false;
        }
        int index = focused == null ? 0 : focusOrder.indexOf(focused);
        focus(focusOrder.get((index - 1 + focusOrder.size()) % focusOrder.size()));
        return true;
    }

    private EchoComponent autofocusOrFirst() {
        for (EchoComponent component : focusOrder) {
            if (truthy(component.node().attribute("autofocus", ""))) {
                return component;
            }
        }
        return focusOrder.isEmpty() ? null : focusOrder.get(0);
    }

    private void collectSorted(EchoComponent root, boolean forcedRoot) {
        ArrayList<EchoFocusNode> nodes = new ArrayList<>();
        collect(root, forcedRoot, nodes);
        nodes.sort(Comparator.comparingInt(EchoFocusNode::order));
        for (EchoFocusNode node : nodes) {
            focusOrder.add(node.component());
        }
    }

    private void collect(EchoComponent component, boolean forcedRoot, List<EchoFocusNode> nodes) {
        if (component == null) {
            return;
        }
        if (!forcedRoot && !component.participatesInFocus()) {
            return;
        }
        if (component.focusable() && !component.disabled() && !"false".equalsIgnoreCase(component.node().attribute("focusable", "true"))) {
            nodes.add(new EchoFocusNode(component, order(component), truthy(component.node().attribute("autofocus", ""))));
        }
        for (EchoComponent child : component.children()) {
            collect(child, false, nodes);
        }
    }

    private static int order(EchoComponent component) {
        try {
            return Integer.parseInt(component.node().attribute("focus-order", "10000").strip());
        } catch (NumberFormatException exception) {
            return 10000;
        }
    }

    private static boolean truthy(String raw) {
        return switch (raw == null ? "" : raw.toLowerCase(java.util.Locale.ROOT)) {
            case "true", "yes", "1", "on", "autofocus" -> true;
            default -> false;
        };
    }
}
