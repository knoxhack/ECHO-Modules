package com.knoxhack.echoscreencore.client.component.data;

import com.knoxhack.echoscreencore.api.component.EchoComponentFactory;
import com.knoxhack.echoscreencore.api.layout.EchoMeasureResult;
import com.knoxhack.echoscreencore.client.component.AbstractEchoComponent;
import com.knoxhack.echoscreencore.client.component.EchoComponentSupport;
import com.knoxhack.echoscreencore.client.render.EchoRenderContext;

public final class OptionComponent extends AbstractEchoComponent {
    public OptionComponent(EchoComponentFactory.Context context) {
        super(EchoComponentSupport.node(context), EchoComponentSupport.children(context));
    }

    @Override
    public EchoMeasureResult measure(EchoRenderContext context, int availableWidth, int availableHeight) {
        return new EchoMeasureResult(0, 0);
    }

    @Override
    public void render(EchoRenderContext context) {
    }
}
