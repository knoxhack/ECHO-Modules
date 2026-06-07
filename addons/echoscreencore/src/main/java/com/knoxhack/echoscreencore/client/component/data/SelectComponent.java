package com.knoxhack.echoscreencore.client.component.data;

import com.knoxhack.echoscreencore.api.EchoDataContext;
import com.knoxhack.echoscreencore.api.component.EchoComponentFactory;
import com.knoxhack.echoscreencore.api.layout.EchoMeasureResult;
import com.knoxhack.echoscreencore.api.layout.EchoRect;
import com.knoxhack.echoscreencore.client.component.AbstractEchoComponent;
import com.knoxhack.echoscreencore.client.component.EchoComponent;
import com.knoxhack.echoscreencore.client.component.EchoComponentSupport;
import com.knoxhack.echoscreencore.client.input.EchoInputRouter;
import com.knoxhack.echoscreencore.client.render.EchoRenderBridge;
import com.knoxhack.echoscreencore.client.render.EchoRenderContext;
import com.knoxhack.echoscreencore.client.state.EchoPageStateStore;
import com.knoxhack.echoscreencore.client.style.EchoStyleValues;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import org.lwjgl.glfw.GLFW;

public final class SelectComponent extends AbstractEchoComponent {
    private String value;
    private LinkedHashSet<String> selectedValues;
    private int highlightedIndex;
    private String query = "";
    private EchoRenderContext lastContext;

    public SelectComponent(EchoComponentFactory.Context context) {
        super(EchoComponentSupport.node(context), EchoComponentSupport.children(context));
    }

    @Override
    public boolean focusable() {
        return !disabled();
    }

    @Override
    public String currentValue() {
        if (multi()) {
            ensureMultiValues(lastContext);
            return String.join(",", selectedValues == null ? List.<String>of() : selectedValues);
        }
        return value == null ? "" : value;
    }

    @Override
    public EchoMeasureResult measure(EchoRenderContext context, int availableWidth, int availableHeight) {
        int height = EchoStyleValues.length(style(), "height", availableHeight, 30, context.theme(), context.diagnostics());
        int width = EchoStyleValues.length(style(), "width", availableWidth, Math.max(120, Math.min(availableWidth, 180)), context.theme(), context.diagnostics());
        return new EchoMeasureResult(Math.max(80, width), Math.max(24, height));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button, EchoInputRouter.ActionRunner actions) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT || disabled()) {
            return false;
        }
        if (clearable() && !currentValue().isBlank() && mouseX >= bounds().right() - 28) {
            clear(actions);
            return true;
        }
        if (lastContext != null && lastContext.overlays() != null) {
            if (lastContext.overlays().isSelectOpen(this)) {
                lastContext.overlays().closeSelect();
            } else {
                lastContext.overlays().openSelect(this);
            }
        }
        return true;
    }

    public boolean overlayMouseClicked(double mouseX, double mouseY, int button, EchoInputRouter.ActionRunner actions) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT || lastContext == null) {
            return false;
        }
        List<Option> options = filteredOptions(lastContext);
        for (int i = 0; i < options.size(); i++) {
            if (optionRect(i).contains(mouseX, mouseY)) {
                choose(options.get(i), actions);
                if (!multi() && lastContext.overlays() != null) {
                    lastContext.overlays().closeSelect();
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(int key, EchoInputRouter.ActionRunner actions) {
        if (lastContext == null || disabled()) {
            return false;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE && clearable() && !currentValue().isBlank()) {
            clear(actions);
            return true;
        }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_SPACE || key == GLFW.GLFW_KEY_DOWN) {
            lastContext.overlays().openSelect(this);
            return true;
        }
        return false;
    }

    public boolean overlayKeyPressed(int key, EchoInputRouter.ActionRunner actions) {
        if (lastContext == null) {
            return false;
        }
        List<Option> options = filteredOptions(lastContext);
        if (options.isEmpty()) {
            if (key == GLFW.GLFW_KEY_BACKSPACE && !query.isBlank()) {
                query = query.substring(0, query.length() - 1);
                return true;
            }
            return key == GLFW.GLFW_KEY_ESCAPE;
        }
        if (key == GLFW.GLFW_KEY_HOME) {
            highlightedIndex = 0;
            return true;
        }
        if (key == GLFW.GLFW_KEY_END) {
            highlightedIndex = Math.max(0, options.size() - 1);
            return true;
        }
        if (key == GLFW.GLFW_KEY_BACKSPACE && !query.isBlank()) {
            query = query.substring(0, query.length() - 1);
            highlightedIndex = 0;
            return true;
        }
        if (key == GLFW.GLFW_KEY_DOWN) {
            highlightedIndex = Math.min(options.size() - 1, highlightedIndex + 1);
            return true;
        }
        if (key == GLFW.GLFW_KEY_UP) {
            highlightedIndex = Math.max(0, highlightedIndex - 1);
            return true;
        }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_SPACE) {
            choose(options.get(Math.max(0, Math.min(options.size() - 1, highlightedIndex))), actions);
            if (!multi()) {
                lastContext.overlays().closeSelect();
            }
            return true;
        }
        return key == GLFW.GLFW_KEY_ESCAPE;
    }

    @Override
    public boolean charTyped(String typed, EchoInputRouter.ActionRunner actions) {
        if (lastContext == null || typed == null || typed.isBlank() || disabled()) {
            return false;
        }
        String clean = typed.replaceAll("[\\p{Cntrl}]", "");
        if (clean.isBlank()) {
            return false;
        }
        query = (query + clean).strip();
        highlightedIndex = 0;
        if (lastContext.overlays() != null) {
            lastContext.overlays().openSelect(this);
        }
        return true;
    }

    @Override
    protected void renderSelf(EchoRenderContext context) {
        lastContext = context;
        ensureValue(context);
        boolean open = context.overlays() != null && context.overlays().isSelectOpen(this);
        int bg = EchoStyleValues.color(style(), "background", context.theme(), context.theme().color("buttonBg", 0xCC10243A), context.diagnostics());
        int border = open || focused()
            ? context.theme().color("accent", 0xFF00E5FF)
            : EchoStyleValues.color(style(), "border-color", context.theme(), context.theme().color("borderMuted", 0xFF1A6F8A), context.diagnostics());
        if (disabled()) {
            bg = EchoRenderBridge.withAlpha(context.theme().color("disabled", 0xFF3B4652), 110);
            border = context.theme().color("disabled", 0xFF3B4652);
        }
        context.render().panel(context.graphics(), context.font(), bounds().x(), bounds().y(), bounds().width(), bounds().height(), bg, border, true);
        String label = selectedLabel(context);
        if (label.isBlank()) {
            label = attr(context, "placeholder", "Select...");
        }
        label = context.font().plainSubstrByWidth(label, Math.max(0, bounds().width() - 24));
        context.graphics().text(context.font(), label, bounds().x() + 8, bounds().y() + Math.max(6, (bounds().height() - 8) / 2),
            disabled() ? context.theme().color("textMuted", 0xFF8AAFC2) : context.theme().color("textPrimary", 0xFFEAFBFF), false);
        if (clearable() && !currentValue().isBlank()) {
            context.graphics().text(context.font(), "x", bounds().right() - 25, bounds().y() + Math.max(6, (bounds().height() - 8) / 2),
                context.theme().color("textMuted", 0xFF8AAFC2), false);
        }
        int arrowX = bounds().right() - 14;
        int arrowY = bounds().y() + bounds().height() / 2 - 2;
        context.render().fill(context.graphics(), arrowX, arrowY, 7, 1, border);
        context.render().fill(context.graphics(), arrowX + 1, arrowY + 2, 5, 1, border);
        context.render().fill(context.graphics(), arrowX + 2, arrowY + 4, 3, 1, border);
        if (open && context.overlays() != null) {
            context.overlays().openSelect(this);
        }
    }

    public void renderOverlay(EchoRenderContext context) {
        List<Option> options = filteredOptions(context);
        int searchHeight = searchable() ? optionHeight() : 0;
        int visibleCount = Math.max(1, Math.min(options.size(), maxVisibleOptions()));
        int height = Math.max(optionHeight(), searchHeight + visibleCount * optionHeight());
        int y = bounds().bottom() + 2;
        if (y + height > context.screenHeight() - 4) {
            y = Math.max(4, bounds().y() - height - 2);
        }
        int x = Math.min(bounds().x(), Math.max(4, context.screenWidth() - bounds().width() - 4));
        EchoRect listBounds = new EchoRect(x, y, bounds().width(), height);
        context.render().panel(context.graphics(), context.font(), listBounds.x(), listBounds.y(), listBounds.width(), listBounds.height(),
            context.theme().color("panel", 0xF008111F), context.theme().color("borderStrong", 0xFF5BC0EB), true);
        if (options.isEmpty()) {
            if (searchable()) {
                renderSearchRow(context, listBounds);
            }
            context.graphics().text(context.font(), attr(context, "empty-text", "No options"), listBounds.x() + 8, listBounds.y() + 7 + searchHeight,
                context.theme().color("textMuted", 0xFF8AAFC2), false);
            return;
        }
        if (searchable()) {
            renderSearchRow(context, listBounds);
        }
        context.render().enableScissor(context.graphics(), listBounds.x(), listBounds.y() + searchHeight, listBounds.width(), Math.max(0, listBounds.height() - searchHeight));
        try {
            for (int i = 0; i < Math.min(options.size(), visibleCount); i++) {
                EchoRect row = optionRect(i);
                boolean selected = selected(options.get(i));
                boolean highlighted = i == highlightedIndex;
                if (options.get(i).divider()) {
                    context.render().fill(context.graphics(), row.x() + 4, row.y() + row.height() / 2, row.width() - 8, 1,
                        context.theme().color("borderMuted", 0xFF1A6F8A));
                    continue;
                }
                if (options.get(i).header()) {
                    context.graphics().text(context.font(), options.get(i).label(), row.x() + 6, row.y() + 7,
                        context.theme().color("textMuted", 0xFF8AAFC2), false);
                    continue;
                }
                if (selected || highlighted) {
                    context.render().fill(context.graphics(), row.x() + 1, row.y(), row.width() - 2, row.height(),
                        selected ? context.theme().color("cardSelected", 0xDD123E58)
                            : EchoRenderBridge.withAlpha(context.theme().color("cardHover", 0xCC12324A), 180));
                }
                String prefix = multi() ? (selected ? "[x] " : "[ ] ") : "";
                String label = context.font().plainSubstrByWidth(prefix + options.get(i).label(), Math.max(0, row.width() - 12));
                context.graphics().text(context.font(), label, row.x() + 6, row.y() + 7,
                    options.get(i).disabled() ? context.theme().color("textMuted", 0xFF8AAFC2)
                        : options.get(i).danger() ? context.theme().color("danger", 0xFFFF5A67)
                        : context.theme().color("textPrimary", 0xFFEAFBFF), false);
                if (!options.get(i).subtitle().isBlank()) {
                    context.graphics().text(context.font(), context.font().plainSubstrByWidth(options.get(i).subtitle(), Math.max(0, row.width() - 12)),
                        row.x() + 6, row.y() + 17, context.theme().color("textMuted", 0xFF8AAFC2), false);
                }
            }
        } finally {
            context.render().disableScissor(context.graphics());
        }
    }

    public void close() {
    }

    private EchoRect optionRect(int index) {
        int height = optionHeight();
        int y = bounds().bottom() + 2;
        int optionCount = lastContext == null ? children().size() : Math.max(1, Math.min(filteredOptions(lastContext).size(), maxVisibleOptions()));
        int total = (searchable() ? height : 0) + Math.max(height, optionCount * height);
        if (lastContext != null && y + total > lastContext.screenHeight() - 4) {
            y = Math.max(4, bounds().y() - total - 2);
        }
        int x = lastContext == null ? bounds().x() : Math.min(bounds().x(), Math.max(4, lastContext.screenWidth() - bounds().width() - 4));
        return new EchoRect(x, y + (searchable() ? height : 0) + index * height, bounds().width(), height);
    }

    private int optionHeight() {
        return Math.max(20, bounds().height());
    }

    private void choose(Option option, EchoInputRouter.ActionRunner actions) {
        if (option.disabled() || option.divider() || option.header()) {
            return;
        }
        String optionValue = option.actionValue().isBlank() ? option.value() : option.actionValue();
        if (multi()) {
            ensureMultiValues(lastContext);
            if (selectedValues.contains(optionValue)) {
                selectedValues.remove(optionValue);
            } else if (selectedValues.size() < maxSelected()) {
                selectedValues.add(optionValue);
            }
            value = String.join(",", selectedValues);
        } else {
            value = optionValue;
        }
        if (!option.action().isBlank()) {
            actions.run(option.action(), this, "select");
            return;
        }
        highlightedIndex = Math.max(0, filteredOptions(lastContext).indexOf(option));
        EchoDataContext context = dataContext();
        String raw = node().attribute(multi() ? "selected-values" : "value", "");
        if (context != null && raw.startsWith("{") && raw.endsWith("}")) {
            context.put(raw.substring(1, raw.length() - 1).split("\\|", 2)[0].trim(), value);
        }
        if (context != null && node().hasAttribute("state-key")) {
            EchoPageStateStore.put(context, node().attribute("state-key", ""), value);
        }
        String change = node().attribute("on-change", node().attribute("action", ""));
        if (!change.isBlank()) {
            actions.run(change, this, "change");
        }
    }

    private String selectedLabel(EchoRenderContext context) {
        if (multi()) {
            ensureMultiValues(context);
            if (selectedValues == null || selectedValues.isEmpty()) {
                return "";
            }
            ArrayList<String> labels = new ArrayList<>();
            for (Option option : options(context)) {
                if (selectedValues.contains(option.value())) {
                    labels.add(option.label());
                }
            }
            return labels.isEmpty() ? String.join(", ", selectedValues) : String.join(", ", labels);
        }
        for (Option option : options(context)) {
            if (option.value().equals(currentValue())) {
                return option.label();
            }
        }
        return currentValue();
    }

    private List<Option> options(EchoRenderContext context) {
        ArrayList<Option> options = new ArrayList<>();
        for (EchoComponent child : children()) {
            if ("divider".equals(child.node().tagName())) {
                options.add(Option.dividerOption());
                continue;
            }
            EchoDataContext childContext = child.dataContext() == null ? dataContext() : child.dataContext();
            String value = context.bindingResolver().resolve(child.node().attribute("value", child.node().text()), childContext, context.diagnostics());
            String label = context.bindingResolver().resolve(child.node().attribute("label", child.node().text()), childContext, context.diagnostics());
            String subtitle = context.bindingResolver().resolve(child.node().attribute("subtitle", child.node().attribute("description", "")), childContext, context.diagnostics());
            if (label.isBlank()) {
                label = value;
            }
            if ("menu-section".equals(child.node().tagName())) {
                options.add(Option.header(label));
            } else if (!value.isBlank() || !label.isBlank()) {
                options.add(new Option(value, label, subtitle, child.node().attribute("icon", ""),
                    child.node().attribute("action", ""), child.node().attribute("action-value", ""),
                    child.disabled(), child.node().hasAttribute("danger"), false, false));
            }
        }
        return List.copyOf(options);
    }

    private void ensureValue(EchoRenderContext context) {
        if (value == null) {
            if (node().hasAttribute("state-key")) {
                value = attr(context, "value", "{state." + node().attribute("state-key", "") + "|" + node().attribute("default-value", "") + "}");
            } else {
                value = attr(context, "value", "");
            }
            if (multi()) {
                ensureMultiValues(context);
            }
            List<Option> options = options(context);
            if (value.isBlank() && !options.isEmpty()) {
                value = node().attribute("default-value", "");
            }
            for (int i = 0; i < options.size(); i++) {
                if (options.get(i).value().equals(value)) {
                    highlightedIndex = i;
                    break;
                }
            }
        }
    }

    private List<Option> filteredOptions(EchoRenderContext context) {
        List<Option> all = options(context);
        if (query.isBlank()) {
            return all;
        }
        String clean = query.toLowerCase(Locale.ROOT);
        String mode = node().attribute("filter-mode", "contains").toLowerCase(Locale.ROOT);
        return all.stream()
            .filter(option -> option.divider() || option.header()
                || ("startswith".equals(mode)
                    ? option.label().toLowerCase(Locale.ROOT).startsWith(clean)
                    : option.label().toLowerCase(Locale.ROOT).contains(clean)
                        || option.subtitle().toLowerCase(Locale.ROOT).contains(clean)))
            .toList();
    }

    private void renderSearchRow(EchoRenderContext context, EchoRect listBounds) {
        String placeholder = attr(context, "search-placeholder", "Search...");
        String label = query.isBlank() ? placeholder : query;
        context.render().fill(context.graphics(), listBounds.x() + 1, listBounds.y() + 1, listBounds.width() - 2, optionHeight() - 2,
            context.theme().color("buttonBg", 0xCC10243A));
        context.graphics().text(context.font(), context.font().plainSubstrByWidth(label, Math.max(0, listBounds.width() - 12)),
            listBounds.x() + 6, listBounds.y() + 7,
            query.isBlank() ? context.theme().color("textMuted", 0xFF8AAFC2) : context.theme().color("textPrimary", 0xFFEAFBFF), false);
    }

    private void clear(EchoInputRouter.ActionRunner actions) {
        value = "";
        if (selectedValues != null) {
            selectedValues.clear();
        }
        query = "";
        EchoDataContext context = dataContext();
        if (context != null && node().hasAttribute("state-key")) {
            EchoPageStateStore.put(context, node().attribute("state-key", ""), "");
        }
        String change = node().attribute("on-change", node().attribute("action", ""));
        if (!change.isBlank()) {
            actions.run(change, this, "clear");
        }
    }

    private boolean selected(Option option) {
        if (multi()) {
            ensureMultiValues(lastContext);
            return selectedValues != null && selectedValues.contains(option.value());
        }
        return option.value().equals(currentValue());
    }

    private void ensureMultiValues(EchoRenderContext context) {
        if (selectedValues != null) {
            return;
        }
        selectedValues = new LinkedHashSet<>();
        String raw = context == null ? node().attribute("selected-values", value == null ? "" : value) : attr(context, "selected-values", attr(context, "value", ""));
        for (String part : raw.split(",")) {
            if (!part.isBlank()) {
                selectedValues.add(part.strip());
            }
        }
        value = String.join(",", selectedValues);
    }

    private boolean multi() {
        return "true".equalsIgnoreCase(node().attribute("multi", "false"));
    }

    private boolean searchable() {
        return "true".equalsIgnoreCase(node().attribute("searchable", "false"));
    }

    private boolean clearable() {
        return "true".equalsIgnoreCase(node().attribute("clearable", "false"));
    }

    private int maxVisibleOptions() {
        return Math.max(1, EchoStyleValues.intValue(node().attribute("max-visible-options", "8"), 8));
    }

    private int maxSelected() {
        int configured = EchoStyleValues.intValue(node().attribute("max-selected", "9999"), 9999);
        return Math.max(1, configured);
    }

    private record Option(String value, String label, String subtitle, String icon, String action, String actionValue,
            boolean disabled, boolean danger, boolean divider, boolean header) {
        private static Option dividerOption() {
            return new Option("", "", "", "", "", "", false, false, true, false);
        }

        private static Option header(String label) {
            return new Option("", label, "", "", "", "", false, false, false, true);
        }
    }
}
