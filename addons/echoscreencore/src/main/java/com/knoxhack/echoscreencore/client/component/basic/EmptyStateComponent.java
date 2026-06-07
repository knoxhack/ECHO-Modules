package com.knoxhack.echoscreencore.client.component.basic;

import com.knoxhack.echoscreencore.api.component.EchoComponentFactory;
import com.knoxhack.echoscreencore.api.layout.EchoMeasureResult;
import com.knoxhack.echoscreencore.client.component.AbstractEchoComponent;
import com.knoxhack.echoscreencore.client.component.EchoComponentSurfaces;
import com.knoxhack.echoscreencore.client.component.EchoComponentSupport;
import com.knoxhack.echoscreencore.client.render.EchoRenderBridge;
import com.knoxhack.echoscreencore.client.render.EchoRenderContext;
import com.knoxhack.echoscreencore.client.style.EchoStyleValues;

public final class EmptyStateComponent extends AbstractEchoComponent {
    public EmptyStateComponent(EchoComponentFactory.Context context) {
        super(EchoComponentSupport.node(context), EchoComponentSupport.children(context));
    }

    @Override
    public EchoMeasureResult measure(EchoRenderContext context, int availableWidth, int availableHeight) {
        return new EchoMeasureResult(Math.max(120, availableWidth), 54);
    }

    @Override
    protected void renderSelf(EchoRenderContext context) {
        int bg = EchoStyleValues.color(style(), "background", context.theme(), EchoRenderBridge.withAlpha(context.theme().color("overlay", 0x6610243A), 120));
        int border = EchoStyleValues.color(style(), "border-color", context.theme(), context.theme().color("borderMuted", 0xFF1A6F8A));
        if (!EchoComponentSurfaces.renderGlass(context, style(), bounds(), bg, border,
                context.theme().color("accent", 0xFF00E5FF), false)) {
            context.render().panel(context.graphics(), context.font(), bounds().x(), bounds().y(), bounds().width(), bounds().height(), bg, border, true);
        }
        String title = attr(context, "title", "");
        String body = attr(context, "body", "");
        context.graphics().text(context.font(), title, bounds().x() + 8, bounds().y() + 9, context.theme().color("textPrimary", 0xFFEAFBFF), false);
        context.graphics().text(context.font(), context.font().plainSubstrByWidth(body, bounds().width() - 16), bounds().x() + 8, bounds().y() + 24,
            context.theme().color("textMuted", 0xFF8AAFC2), false);
    }
}
