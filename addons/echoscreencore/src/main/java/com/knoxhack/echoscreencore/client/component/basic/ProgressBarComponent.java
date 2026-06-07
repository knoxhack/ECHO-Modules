package com.knoxhack.echoscreencore.client.component.basic;

import com.knoxhack.echoscreencore.api.component.EchoComponentFactory;
import com.knoxhack.echoscreencore.api.layout.EchoRect;
import com.knoxhack.echoscreencore.api.layout.EchoMeasureResult;
import com.knoxhack.echoscreencore.api.style.EchoStyle;
import com.knoxhack.echoscreencore.client.component.AbstractEchoComponent;
import com.knoxhack.echoscreencore.client.component.EchoComponentSurfaces;
import com.knoxhack.echoscreencore.client.component.EchoComponentSupport;
import com.knoxhack.echoscreencore.client.render.EchoRenderBridge;
import com.knoxhack.echoscreencore.client.render.EchoRenderContext;
import com.knoxhack.echoscreencore.client.style.EchoStyleValues;

public class ProgressBarComponent extends AbstractEchoComponent {
    public ProgressBarComponent(EchoComponentFactory.Context context) {
        super(EchoComponentSupport.node(context), EchoComponentSupport.children(context));
    }

    @Override
    public EchoMeasureResult measure(EchoRenderContext context, int availableWidth, int availableHeight) {
        int height = EchoStyleValues.length(style(), "height", availableHeight, 18, context.theme(),
                context.diagnostics());
        height = Math.max(height, EchoStyleValues.length(style(), "min-height", availableHeight, height,
                context.theme(), context.diagnostics()));
        return new EchoMeasureResult(Math.max(80, availableWidth), Math.max(3, height));
    }

    @Override
    protected void renderSelf(EchoRenderContext context) {
        EchoStyle current = effectiveStyle(context);
        double value = number(attr(context, "value", "0"));
        double max = Math.max(1.0D, number(attr(context, "max", "100")));
        float progress = (float) Math.max(0.0D, Math.min(1.0D, value / max));
        int bg = EchoStyleValues.color(current, "track-color", context.theme(),
                EchoStyleValues.color(current, "background", context.theme(),
                        EchoRenderBridge.withAlpha(context.theme().color("borderMuted", 0xFF1A6F8A), 70)),
                context.diagnostics());
        int border = EchoStyleValues.color(current, "border-color", context.theme(),
                EchoRenderBridge.withAlpha(context.theme().color("accentMuted", 0xFF1A6F8A), 120),
                context.diagnostics());
        int fill = EchoStyleValues.color(current, "fill-color", context.theme(),
                EchoStyleValues.color(current, "accent-color", context.theme(),
                        StatusChipComponent.statusColor(context, attr(context, "status", "ready")),
                        context.diagnostics()),
                context.diagnostics());
        int barHeight = Math.max(3, Math.min(bounds().height(), EchoStyleValues.length(current, "height",
                bounds().height(), Math.min(8, bounds().height()), context.theme(), context.diagnostics())));
        int y = bounds().y() + Math.max(0, (bounds().height() - barHeight) / 2);
        EchoRect track = new EchoRect(bounds().x(), y, bounds().width(), barHeight);
        if (!EchoComponentSurfaces.renderGlass(context, current, track, bg, border, fill, false)) {
            context.render().fill(context.graphics(), track.x(), track.y(), track.width(), track.height(), bg);
            context.render().outline(context.graphics(), track.x(), track.y(), track.width(), track.height(), border);
        }
        int fillWidth = Math.max(0, Math.round(Math.max(0, track.width() - 2) * progress));
        if (fillWidth > 0) {
            int fillHeight = Math.max(1, track.height() - 2);
            context.render().fill(context.graphics(), track.x() + 1, track.y() + 1, fillWidth, fillHeight,
                    EchoRenderBridge.withAlpha(fill, 205));
            context.render().fill(context.graphics(), track.x() + 1, track.y() + 1, Math.max(1, fillWidth), 1,
                    EchoRenderBridge.withAlpha(0xFFFFFFFF, 92));
            context.render().fill(context.graphics(), track.x() + fillWidth, track.y(),
                    Math.min(2, Math.max(1, track.width() - fillWidth)), track.height(),
                    EchoRenderBridge.withAlpha(fill, 180));
        }
        if (current.bool("segmented", false) && track.width() > 28) {
            int segment = EchoStyleValues.length(current, "segment-size", 48, 18, context.theme(),
                    context.diagnostics());
            for (int x = track.x() + Math.max(8, segment); x < track.right() - 3; x += Math.max(6, segment)) {
                context.render().fill(context.graphics(), x, track.y() + 1, 1, Math.max(1, track.height() - 2),
                        EchoRenderBridge.withAlpha(0xFF000000, 110));
                context.render().fill(context.graphics(), x + 1, track.y() + 1, 1, Math.max(1, track.height() - 2),
                        EchoRenderBridge.withAlpha(fill, 46));
            }
        }
    }

    protected static double number(String value) {
        try {
            return Double.parseDouble(value == null || value.isBlank() ? "0" : value);
        } catch (NumberFormatException exception) {
            return 0.0D;
        }
    }
}
