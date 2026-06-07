package com.knoxhack.echoscreencore.client.component.data;

import com.knoxhack.echoscreencore.api.component.EchoComponentFactory;
import com.knoxhack.echoscreencore.api.layout.EchoMeasureResult;
import com.knoxhack.echoscreencore.api.style.EchoStyle;
import com.knoxhack.echoscreencore.client.component.EchoComponentSurfaces;
import com.knoxhack.echoscreencore.client.component.layout.ContainerComponent;
import com.knoxhack.echoscreencore.client.render.EchoRenderBridge;
import com.knoxhack.echoscreencore.client.render.EchoRenderContext;
import com.knoxhack.echoscreencore.client.style.EchoStyleValues;
import java.util.Locale;

public final class ListRowComponent extends ContainerComponent {
    public ListRowComponent(EchoComponentFactory.Context context) {
        super(context);
    }

    @Override
    public boolean focusable() {
        return !disabled() && !action().isBlank();
    }

    @Override
    public EchoMeasureResult measure(EchoRenderContext context, int availableWidth, int availableHeight) {
        int height = EchoStyleValues.length(style(), "height", availableHeight, 48, context.theme(), context.diagnostics());
        height = Math.max(height, EchoStyleValues.length(style(), "min-height", availableHeight, height, context.theme(), context.diagnostics()));
        return new EchoMeasureResult(Math.max(0, availableWidth), height);
    }

    @Override
    protected void renderSelf(EchoRenderContext context) {
        EchoStyle current = effectiveStyle(context);
        boolean selected = selected(context);
        int defaultBg = selected ? 0xDD062331 : context.theme().color("card", 0xCC0D1A2E);
        int defaultBorder = selected || focused() ? 0xFF00EAFF : context.theme().color("borderMuted", 0xFF1A6F8A);
        boolean styledBackground = current.value("background").isPresent();
        boolean styledBorder = current.value("border-color").isPresent();
        int bg = EchoStyleValues.color(current, "background", context.theme(),
            defaultBg,
            context.diagnostics());
        int border = EchoStyleValues.color(current, "border-color", context.theme(),
            defaultBorder,
            context.diagnostics());
        if (hovered() && !disabled()) {
            if (!styledBackground) {
                bg = selected ? 0xEE073247 : EchoRenderBridge.withAlpha(context.theme().color("cardHover", 0xCC12324A), 220);
            }
            if (!styledBorder) {
                border = 0xFF00EAFF;
            }
        }
        if (disabled()) {
            if (!styledBackground) {
                bg = EchoRenderBridge.withAlpha(context.theme().color("disabled", 0xFF3B4652), 85);
            }
            if (!styledBorder) {
                border = context.theme().color("disabled", 0xFF3B4652);
            }
        }
        if (!EchoComponentSurfaces.renderGlass(context, current, bounds(), bg, border,
                context.theme().color("accent", 0xFF00E5FF), selected || hovered() || focused())) {
            context.render().panel(context.graphics(), context.font(), bounds().x(), bounds().y(), bounds().width(), bounds().height(),
                bg, border, context.accessibility().quietVisuals());
        }
    }

    private boolean selected(EchoRenderContext context) {
        String value = attr(context, "selected", "");
        if (value.isBlank()) {
            value = attr(context, "active", "");
        }
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "true", "yes", "1", "on", "selected", "active" -> true;
            default -> false;
        };
    }
}
