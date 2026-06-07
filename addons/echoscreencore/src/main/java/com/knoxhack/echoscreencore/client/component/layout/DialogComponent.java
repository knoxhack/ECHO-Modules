package com.knoxhack.echoscreencore.client.component.layout;

import com.knoxhack.echoscreencore.api.component.EchoComponentFactory;
import com.knoxhack.echoscreencore.api.layout.EchoMeasureResult;
import com.knoxhack.echoscreencore.client.render.EchoRenderContext;
import com.knoxhack.echoscreencore.client.style.EchoStyleValues;

public final class DialogComponent extends ContainerComponent {
    public DialogComponent(EchoComponentFactory.Context context) {
        super(context);
    }

    @Override
    public EchoMeasureResult measure(EchoRenderContext context, int availableWidth, int availableHeight) {
        if (context.overlays() == null || !context.overlays().isModalActive(this)) {
            return new EchoMeasureResult(0, 0);
        }
        int width = EchoStyleValues.length(style(), "width", availableWidth, Math.min(availableWidth, 440), context.theme(), context.diagnostics());
        int height = EchoStyleValues.length(style(), "height", availableHeight, Math.min(availableHeight, 240), context.theme(), context.diagnostics());
        return new EchoMeasureResult(Math.max(160, width), Math.max(120, height));
    }

    @Override
    public boolean participatesInFocus() {
        return false;
    }

    @Override
    public void render(EchoRenderContext context) {
        if (context.overlays() != null && context.overlays().isRenderingModal(this)) {
            super.render(context);
        }
    }

    public boolean closeOnOutside() {
        return !"false".equalsIgnoreCase(node().attribute("close-on-outside", "true"));
    }

    public boolean closeOnEscape() {
        return !"false".equalsIgnoreCase(node().attribute("close-on-escape", "true"));
    }

    @Override
    protected int defaultHeight(EchoRenderContext context) {
        return 180;
    }
}
