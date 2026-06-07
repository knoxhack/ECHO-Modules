package com.knoxhack.echoscreencore.client.component.basic;

import com.knoxhack.echoscreencore.api.component.EchoComponentFactory;
import com.knoxhack.echoscreencore.api.style.EchoStyle;
import com.knoxhack.echoscreencore.client.render.EchoRenderContext;
import com.knoxhack.echoscreencore.client.style.EchoStyleValues;

public final class TitleComponent extends TextComponent {
    public TitleComponent(EchoComponentFactory.Context context) {
        super(context);
    }

    @Override
    protected void renderSelf(EchoRenderContext context) {
        EchoStyle current = effectiveStyle(context);
        int color = EchoStyleValues.color(current, "color", context.theme(), context.theme().color("textPrimary", 0xFFEAFBFF));
        renderText(context, current, color, true);
    }
}
