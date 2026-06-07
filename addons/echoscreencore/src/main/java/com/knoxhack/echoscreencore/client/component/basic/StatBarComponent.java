package com.knoxhack.echoscreencore.client.component.basic;

import com.knoxhack.echoscreencore.api.component.EchoComponentFactory;
import com.knoxhack.echoscreencore.api.layout.EchoMeasureResult;
import com.knoxhack.echoscreencore.client.render.EchoRenderContext;

public final class StatBarComponent extends ProgressBarComponent {
    public StatBarComponent(EchoComponentFactory.Context context) {
        super(context);
    }

    @Override
    public EchoMeasureResult measure(EchoRenderContext context, int availableWidth, int availableHeight) {
        return new EchoMeasureResult(Math.max(120, availableWidth), 28);
    }

    @Override
    protected void renderSelf(EchoRenderContext context) {
        String label = attr(context, "label", "");
        String value = attr(context, "value", "0");
        context.graphics().text(context.font(), label, bounds().x(), bounds().y(), context.theme().color("textSecondary", 0xFFB7D7E3), false);
        String right = value + "/" + attr(context, "max", "100");
        context.graphics().text(context.font(), right, bounds().right() - context.font().width(right), bounds().y(), context.theme().color("textMuted", 0xFF8AAFC2), false);
        int oldY = bounds().y();
        setBounds(new com.knoxhack.echoscreencore.api.layout.EchoRect(bounds().x(), oldY + 12, bounds().width(), 14));
        super.renderSelf(context);
        setBounds(new com.knoxhack.echoscreencore.api.layout.EchoRect(bounds().x(), oldY, bounds().width(), 28));
    }
}
