package com.knoxhack.echoscreencore.client.component.basic;

import com.knoxhack.echoscreencore.api.component.EchoComponentFactory;
import com.knoxhack.echoscreencore.client.component.layout.CardComponent;
import com.knoxhack.echoscreencore.client.render.EchoRenderContext;

public final class StatCardComponent extends CardComponent {
    public StatCardComponent(EchoComponentFactory.Context context) {
        super(context);
    }

    @Override
    protected void renderSelf(EchoRenderContext context) {
        super.renderSelf(context);
        String label = attr(context, "label", "");
        String value = attr(context, "value", "");
        context.graphics().text(context.font(), label, bounds().x() + 8, bounds().y() + 8, context.theme().color("textMuted", 0xFF8AAFC2), false);
        context.graphics().text(context.font(), value, bounds().x() + 8, bounds().y() + 21, context.theme().color("textPrimary", 0xFFEAFBFF), true);
    }
}
