package com.knoxhack.echoscreencore.client.component.data;

import com.knoxhack.echoscreencore.api.EchoDataContext;
import com.knoxhack.echoscreencore.api.component.EchoComponentFactory;
import com.knoxhack.echoscreencore.api.layout.EchoMeasureResult;
import com.knoxhack.echoscreencore.client.component.AbstractEchoComponent;
import com.knoxhack.echoscreencore.client.component.EchoComponentSupport;
import com.knoxhack.echoscreencore.client.input.EchoInputRouter;
import com.knoxhack.echoscreencore.client.render.EchoRenderBridge;
import com.knoxhack.echoscreencore.client.render.EchoRenderContext;
import com.knoxhack.echoscreencore.client.state.EchoPageStateStore;
import com.knoxhack.echoscreencore.client.style.EchoStyleValues;
import java.util.Locale;
import org.lwjgl.glfw.GLFW;

public class ToggleComponent extends AbstractEchoComponent {
    private Boolean checked;

    public ToggleComponent(EchoComponentFactory.Context context) {
        super(EchoComponentSupport.node(context), EchoComponentSupport.children(context));
    }

    @Override
    public boolean focusable() {
        return !disabled();
    }

    @Override
    public String currentValue() {
        return String.valueOf(Boolean.TRUE.equals(checked));
    }

    @Override
    public EchoMeasureResult measure(EchoRenderContext context, int availableWidth, int availableHeight) {
        return new EchoMeasureResult(Math.max(72, availableWidth), EchoStyleValues.length(style(), "height", availableHeight, 24, context.theme(), context.diagnostics()));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button, EchoInputRouter.ActionRunner actions) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT || disabled()) {
            return false;
        }
        toggle(actions);
        return true;
    }

    @Override
    public boolean keyPressed(int key, EchoInputRouter.ActionRunner actions) {
        if (key == GLFW.GLFW_KEY_SPACE || key == GLFW.GLFW_KEY_ENTER) {
            toggle(actions);
            return true;
        }
        return false;
    }

    @Override
    protected void renderSelf(EchoRenderContext context) {
        ensureChecked(context);
        int accent = Boolean.TRUE.equals(checked) ? context.theme().color("success", 0xFF45FFB0) : context.theme().color("borderMuted", 0xFF1A6F8A);
        int bg = Boolean.TRUE.equals(checked) ? EchoRenderBridge.withAlpha(accent, 75) : context.theme().color("overlay", 0x6610243A);
        if (disabled()) {
            accent = context.theme().color("disabled", 0xFF3B4652);
            bg = EchoRenderBridge.withAlpha(accent, 70);
        }
        int box = Math.min(18, bounds().height() - 4);
        int boxY = bounds().y() + Math.max(2, (bounds().height() - box) / 2);
        context.render().fill(context.graphics(), bounds().x(), boxY, box, box, bg);
        context.render().outline(context.graphics(), bounds().x(), boxY, box, box, focused() ? context.theme().color("accent", 0xFF00E5FF) : accent);
        if (Boolean.TRUE.equals(checked)) {
            context.render().fill(context.graphics(), bounds().x() + 4, boxY + box / 2, box - 8, 2, accent);
            context.render().fill(context.graphics(), bounds().x() + box / 2, boxY + 4, 2, box - 8, accent);
        }
        String label = attr(context, "label", node().text());
        if (!label.isBlank()) {
            context.graphics().text(context.font(), context.font().plainSubstrByWidth(label, Math.max(0, bounds().width() - box - 8)),
                bounds().x() + box + 6, bounds().y() + Math.max(6, (bounds().height() - 8) / 2),
                context.theme().color("textSecondary", 0xFFB7D7E3), false);
        }
    }

    protected void ensureChecked(EchoRenderContext context) {
        if (checked == null) {
            String fallback = node().hasAttribute("state-key")
                ? "{state." + node().attribute("state-key", "") + "|" + node().attribute("default-value", "false") + "}"
                : attr(context, "value", "false");
            checked = parse(attr(context, "checked", fallback));
        }
    }

    private void toggle(EchoInputRouter.ActionRunner actions) {
        checked = !Boolean.TRUE.equals(checked);
        EchoDataContext context = dataContext();
        if (context != null) {
            String raw = node().attribute("checked", node().attribute("value", ""));
            if (raw.startsWith("{") && raw.endsWith("}")) {
                context.put(raw.substring(1, raw.length() - 1).split("\\|", 2)[0].trim(), checked);
            }
            if (node().hasAttribute("state-key")) {
                EchoPageStateStore.put(context, node().attribute("state-key", ""), checked);
            }
        }
        String change = node().attribute("on-change", node().attribute("action", ""));
        if (!change.isBlank()) {
            actions.run(change, this, "change");
        }
    }

    private static boolean parse(String raw) {
        return switch (raw == null ? "" : raw.toLowerCase(Locale.ROOT)) {
            case "true", "yes", "1", "on", "checked" -> true;
            default -> false;
        };
    }
}
