package com.knoxhack.echoscreencore.client.component.basic;

import com.knoxhack.echoscreencore.api.component.EchoComponentFactory;
import com.knoxhack.echoscreencore.client.component.layout.ContainerComponent;
import com.knoxhack.echoscreencore.client.render.EchoRenderContext;
import com.knoxhack.echoscreencore.client.style.EchoStyleValues;

public final class PageComponent extends ContainerComponent {
    public PageComponent(EchoComponentFactory.Context context) {
        super(context);
    }

    @Override
    protected void renderSelf(EchoRenderContext context) {
        int background = EchoStyleValues.color(style(), "background", context.theme(), context.theme().color("background", 0xFF030711));
        context.render().fill(context.graphics(), bounds().x(), bounds().y(), bounds().width(), bounds().height(), background);
    }
}
