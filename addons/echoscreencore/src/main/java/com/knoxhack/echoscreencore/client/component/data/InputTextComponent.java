package com.knoxhack.echoscreencore.client.component.data;

import com.knoxhack.echoscreencore.api.EchoDataContext;
import com.knoxhack.echoscreencore.api.component.EchoComponentFactory;
import com.knoxhack.echoscreencore.api.layout.EchoMeasureResult;
import com.knoxhack.echoscreencore.api.style.EchoStyle;
import com.knoxhack.echoscreencore.client.component.AbstractEchoComponent;
import com.knoxhack.echoscreencore.client.component.EchoComponentSurfaces;
import com.knoxhack.echoscreencore.client.component.EchoComponentSupport;
import com.knoxhack.echoscreencore.client.input.EchoInputRouter;
import com.knoxhack.echoscreencore.client.render.EchoRenderBridge;
import com.knoxhack.echoscreencore.client.render.EchoRenderContext;
import com.knoxhack.echoscreencore.client.state.EchoPageStateStore;
import com.knoxhack.echoscreencore.client.style.EchoStyleValues;
import org.lwjgl.glfw.GLFW;

public class InputTextComponent extends AbstractEchoComponent {
    private String value;
    private long lastChangeAction;

    public InputTextComponent(EchoComponentFactory.Context context) {
        super(EchoComponentSupport.node(context), EchoComponentSupport.children(context));
    }

    @Override
    public boolean focusable() {
        return !disabled();
    }

    @Override
    public String currentValue() {
        return value == null ? "" : value;
    }

    @Override
    public EchoMeasureResult measure(EchoRenderContext context, int availableWidth, int availableHeight) {
        int height = EchoStyleValues.length(style(), "height", availableHeight, 30, context.theme(), context.diagnostics());
        return new EchoMeasureResult(Math.max(80, availableWidth), Math.max(24, height));
    }

    @Override
    public boolean keyPressed(int key, EchoInputRouter.ActionRunner actions) {
        if (disabled() || readOnly()) {
            return false;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE && clearable() && !currentValue().isBlank()) {
            setValue("");
            runChange(actions, true);
            return true;
        }
        if (key == GLFW.GLFW_KEY_BACKSPACE) {
            if (!currentValue().isEmpty()) {
                setValue(currentValue().substring(0, currentValue().length() - 1));
                runChange(actions, false);
            }
            return true;
        }
        if (key == GLFW.GLFW_KEY_DELETE) {
            setValue("");
            runChange(actions, true);
            return true;
        }
        if (key == GLFW.GLFW_KEY_ENTER) {
            String enter = node().attribute("on-enter", node().attribute("on-submit", ""));
            return !enter.isBlank() && actions.run(enter, this, "enter");
        }
        return false;
    }

    @Override
    public boolean charTyped(String typed, EchoInputRouter.ActionRunner actions) {
        if (typed == null || typed.isEmpty() || disabled() || readOnly()) {
            return false;
        }
        int max = Math.max(1, EchoStyleValues.intValue(node().attribute("max-length", "64"), 64));
        String clean = typed.replaceAll("[\\p{Cntrl}]", "");
        if (clean.isEmpty()) {
            return false;
        }
        setValue((currentValue() + clean).substring(0, Math.min(max, currentValue().length() + clean.length())));
        runChange(actions, false);
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button, EchoInputRouter.ActionRunner actions) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && clearable() && !currentValue().isBlank() && mouseX >= bounds().right() - 18) {
            setValue("");
            runChange(actions, true);
            return true;
        }
        return false;
    }

    @Override
    protected void renderSelf(EchoRenderContext context) {
        ensureValue(context);
        EchoStyle current = effectiveStyle(context);
        boolean styledBackground = current.value("background").isPresent();
        boolean styledBorder = current.value("border-color").isPresent();
        int bg = EchoStyleValues.color(current, "background", context.theme(), context.theme().color("buttonBg", 0xCC10243A), context.diagnostics());
        int border = focused() && !styledBorder ? context.theme().color("accent", 0xFF00E5FF)
            : EchoStyleValues.color(current, "border-color", context.theme(), context.theme().color("borderMuted", 0xFF1A6F8A), context.diagnostics());
        if (disabled()) {
            if (!styledBackground) {
                bg = EchoRenderBridge.withAlpha(context.theme().color("disabled", 0xFF3B4652), 110);
            }
            if (!styledBorder) {
                border = context.theme().color("disabled", 0xFF3B4652);
            }
        }
        if (!EchoComponentSurfaces.renderGlass(context, current, bounds(), bg, border,
                context.theme().color("accent", 0xFF00E5FF), focused() || hovered())) {
            context.render().panel(context.graphics(), context.font(), bounds().x(), bounds().y(), bounds().width(), bounds().height(), bg, border, true);
        }
        String text = currentValue();
        boolean placeholder = text.isBlank();
        if (placeholder) {
            text = attr(context, "placeholder", "");
        }
        int color = placeholder ? context.theme().color("textMuted", 0xFF8AAFC2)
                : EchoStyleValues.color(current, "color", context.theme(), context.theme().color("textPrimary", 0xFFEAFBFF),
                        context.diagnostics());
        String clipped = context.font().plainSubstrByWidth(text, Math.max(0, bounds().width() - 16));
        context.graphics().text(context.font(), clipped, bounds().x() + 8, bounds().y() + Math.max(6, (bounds().height() - 8) / 2), color, false);
        if (focused() && !disabled()) {
            int cursorX = bounds().x() + 8 + context.font().width(clipped);
            context.render().fill(context.graphics(), cursorX + 1, bounds().y() + 6, 1, bounds().height() - 12, context.theme().color("accent", 0xFF00E5FF));
        }
        if (clearable() && !currentValue().isBlank()) {
            context.graphics().text(context.font(), "x", bounds().right() - 14, bounds().y() + Math.max(6, (bounds().height() - 8) / 2),
                context.theme().color("textMuted", 0xFF8AAFC2), false);
        }
        String error = validationMessage(context);
        if (!error.isBlank()) {
            context.graphics().text(context.font(), context.font().plainSubstrByWidth(error, Math.max(0, bounds().width())),
                bounds().x(), bounds().bottom() + 2, context.theme().color("danger", 0xFFFF5A67), false);
        }
    }

    protected void ensureValue(EchoRenderContext context) {
        if (value == null) {
            if (node().hasAttribute("state-key")) {
                value = attr(context, "value", "{state." + node().attribute("state-key", "") + "|" + node().attribute("default-value", "") + "}");
            } else {
                value = attr(context, "value", "");
            }
        }
    }

    protected String bindingPath(EchoRenderContext context) {
        String raw = node().attribute("value", "");
        return context.bindingResolver().containsBinding(raw) ? context.bindingResolver().bindingPath(raw) : "";
    }

    protected void setValue(String next) {
        value = next == null ? "" : next;
    }

    private void runChange(EchoInputRouter.ActionRunner actions, boolean force) {
        String change = node().attribute("on-change", "");
        EchoDataContext context = dataContext();
        if (context != null && !node().attribute("value", "").isBlank()) {
            String raw = node().attribute("value", "");
            if (raw.startsWith("{") && raw.endsWith("}")) {
                context.put(raw.substring(1, raw.length() - 1).split("\\|", 2)[0].trim(), currentValue());
            }
        }
        if (context != null && node().hasAttribute("state-key")) {
            EchoPageStateStore.put(context, node().attribute("state-key", ""), currentValue());
        }
        if (!change.isBlank()) {
            long now = System.currentTimeMillis();
            int debounce = Math.max(0, EchoStyleValues.intValue(node().attribute("debounce-ms", "0"), 0));
            if (!force && debounce > 0 && now - lastChangeAction < debounce) {
                return;
            }
            lastChangeAction = now;
            int minQuery = Math.max(0, EchoStyleValues.intValue(node().attribute("min-query-length", "0"), 0));
            if (currentValue().length() < minQuery && !currentValue().isBlank()) {
                return;
            }
            actions.run(change, this, "change");
        }
    }

    private boolean clearable() {
        return "true".equalsIgnoreCase(node().attribute("clearable", node().tagName().equals("search-box") ? "true" : "false"));
    }

    private boolean readOnly() {
        return node().hasAttribute("readonly") || node().hasAttribute("read-only");
    }

    private String validationMessage(EchoRenderContext context) {
        String explicit = attr(context, "error", "");
        if (!explicit.isBlank()) {
            return explicit;
        }
        if (node().hasAttribute("required") && currentValue().isBlank()) {
            return "Required";
        }
        int min = EchoStyleValues.intValue(node().attribute("min-length", "0"), 0);
        if (min > 0 && !currentValue().isBlank() && currentValue().length() < min) {
            return "Minimum " + min + " characters";
        }
        String pattern = node().attribute("pattern", "");
        if (!pattern.isBlank() && !currentValue().isBlank()) {
            try {
                if (!currentValue().matches(pattern)) {
                    return "Invalid format";
                }
            } catch (RuntimeException exception) {
                if (context.diagnostics() != null) {
                    context.diagnostics().warnOnce("invalid_input_pattern", node().id());
                }
            }
        }
        return "";
    }
}
