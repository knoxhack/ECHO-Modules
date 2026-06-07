package com.knoxhack.echoscreencore.client.component.basic;

import com.knoxhack.echoscreencore.api.component.EchoComponentFactory;
import com.knoxhack.echoscreencore.client.component.layout.ContainerComponent;
import com.knoxhack.echoscreencore.client.render.EchoRenderContext;

public final class TooltipComponent extends ContainerComponent {
    public TooltipComponent(EchoComponentFactory.Context context) {
        super(context);
    }

    @Override
    public void render(EchoRenderContext context) {
        super.render(context);
        if (context.overlays() != null && (hovered() || focused())) {
            String text = attr(context, "text", node().text());
            if (!text.isBlank()) {
                context.overlays().requestTooltip(this, text);
            }
        }
    }
}
