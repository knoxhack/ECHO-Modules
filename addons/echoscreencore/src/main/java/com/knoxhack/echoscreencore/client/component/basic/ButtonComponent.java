package com.knoxhack.echoscreencore.client.component.basic;

import com.knoxhack.echoscreencore.api.component.EchoComponentFactory;
import com.knoxhack.echoscreencore.api.layout.EchoMeasureResult;
import com.knoxhack.echoscreencore.api.style.EchoStyle;
import com.knoxhack.echoscreencore.client.component.AbstractEchoComponent;
import com.knoxhack.echoscreencore.client.component.EchoComponentSurfaces;
import com.knoxhack.echoscreencore.client.component.EchoComponentSupport;
import com.knoxhack.echoscreencore.client.render.EchoRenderBridge;
import com.knoxhack.echoscreencore.client.render.EchoRenderContext;
import com.knoxhack.echoscreencore.client.style.EchoStyleValues;

public final class ButtonComponent extends AbstractEchoComponent {
    public ButtonComponent(EchoComponentFactory.Context context) {
        super(EchoComponentSupport.node(context), EchoComponentSupport.children(context));
    }

    @Override
    public boolean focusable() {
        return !disabled();
    }

    @Override
    public EchoMeasureResult measure(EchoRenderContext context, int availableWidth, int availableHeight) {
        EchoStyle current = effectiveStyle(context);
        int height = EchoStyleValues.length(current, "height", availableHeight, 32, context.theme(), context.diagnostics());
        height = Math.max(height, EchoStyleValues.length(current, "min-height", availableHeight, height, context.theme(), context.diagnostics()));
        int width = EchoStyleValues.length(current, "width", availableWidth,
                intrinsicWidth(context, availableWidth),
                context.theme(), context.diagnostics());
        return new EchoMeasureResult(Math.max(0, width), Math.max(0, height));
    }

    @Override
    protected void renderSelf(EchoRenderContext context) {
        EchoStyle current = effectiveStyle(context);
        boolean active = hovered() || focused();
        boolean styledBackground = current.value("background").isPresent();
        boolean styledBorder = current.value("border-color").isPresent();
        boolean styledColor = current.value("color").isPresent();
        int bg = EchoStyleValues.color(current, "background", context.theme(), context.theme().color("buttonBg", 0xCC10243A));
        int border = EchoStyleValues.color(current, "border-color", context.theme(), context.theme().color("accentMuted", 0xFF1A6F8A));
        int text = EchoStyleValues.color(current, "color", context.theme(), context.theme().color("textPrimary", 0xFFEAFBFF));
        if (disabled()) {
            if (!styledBackground) {
                bg = EchoRenderBridge.withAlpha(context.theme().color("disabled", 0xFF3B4652), 150);
            }
            if (!styledBorder) {
                border = context.theme().color("disabled", 0xFF3B4652);
            }
            if (!styledColor) {
                text = context.theme().color("textMuted", 0xFF8AAFC2);
            }
        } else if (active) {
            if (!styledBorder) {
                border = context.theme().color("accent", 0xFF00E5FF);
            }
            if (!styledBackground) {
                bg = EchoRenderBridge.withAlpha(context.theme().color("accentDim", 0xFF0B3C4A), 230);
            }
        }
        if (!EchoComponentSurfaces.renderGlass(context, current, bounds(), bg, border,
                context.theme().color("accent", 0xFF00E5FF), active)) {
            context.render().panel(context.graphics(), context.font(), bounds().x(), bounds().y(), bounds().width(), bounds().height(), bg, border, true);
        }
        String label = fitLabel(context, text(context), Math.max(0, bounds().width() - 10));
        int labelX = bounds().x() + Math.max(4, (bounds().width() - context.font().width(label)) / 2);
        int labelY = bounds().y() + Math.max(4, (bounds().height() - 8) / 2);
        context.graphics().text(context.font(), label, labelX, labelY, text, false);
    }

    private static String fitLabel(EchoRenderContext context, String value, int maxWidth) {
        String safe = value == null ? "" : value;
        if (maxWidth <= 0 || context.font().width(safe) <= maxWidth) {
            return safe;
        }
        if (maxWidth <= context.font().width("...")) {
            return context.font().plainSubstrByWidth(safe, maxWidth);
        }
        String trimmed = context.font().plainSubstrByWidth(safe, maxWidth - context.font().width("..."));
        return trimmed.stripTrailing() + "...";
    }

    private int intrinsicWidth(EchoRenderContext context, int availableWidth) {
        int labelWidth = context.font() == null ? Math.max(0, text(context).length() * 6) : context.font().width(text(context));
        return Math.min(Math.max(0, availableWidth), Math.max(72, labelWidth + 28));
    }
}
