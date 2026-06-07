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

public final class StatusChipComponent extends AbstractEchoComponent {
    public StatusChipComponent(EchoComponentFactory.Context context) {
        super(EchoComponentSupport.node(context), EchoComponentSupport.children(context));
    }

    @Override
    public EchoMeasureResult measure(EchoRenderContext context, int availableWidth, int availableHeight) {
        String label = text(context);
        return new EchoMeasureResult(Math.min(availableWidth, Math.max(48, context.font().width(label) + 18)), 18);
    }

    @Override
    protected void renderSelf(EchoRenderContext context) {
        EchoStyle current = effectiveStyle(context);
        String status = attr(context, "status", "info");
        int color = statusColor(context, status);
        color = EchoStyleValues.color(current, "color", context.theme(), color, context.diagnostics());
        int bg = EchoStyleValues.color(current, "background", context.theme(), EchoRenderBridge.withAlpha(color, 38),
                context.diagnostics());
        int border = EchoStyleValues.color(current, "border-color", context.theme(),
                EchoRenderBridge.withAlpha(color, hovered() || focused() ? 210 : 145), context.diagnostics());
        if (!EchoComponentSurfaces.renderGlass(context, current, bounds(), bg, border, color, hovered() || focused())) {
            context.render().panel(context.graphics(), context.font(), bounds().x(), bounds().y(), bounds().width(), bounds().height(), bg, border, true);
        }
        String label = context.font().plainSubstrByWidth(text(context).isBlank() ? status.toUpperCase(java.util.Locale.ROOT) : text(context), bounds().width() - 8);
        context.graphics().centeredText(context.font(), label, bounds().x() + bounds().width() / 2, bounds().y() + 5, color);
    }

    static int statusColor(EchoRenderContext context, String status) {
        return switch (status == null ? "" : status) {
            case "active", "info" -> 0xFF00EAFF;
            case "ready", "done", "success" -> 0xFF58FF9A;
            case "warning", "optional" -> 0xFFFFB734;
            case "danger", "critical", "missing" -> 0xFFFF4F68;
            case "locked", "disabled" -> 0xFF8EA3AE;
            default -> 0xFF7EC8FF;
        };
    }
}
