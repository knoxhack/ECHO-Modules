package com.knoxhack.echoscreencore.client.component.data;

import com.knoxhack.echoscreencore.api.component.EchoComponentFactory;
import com.knoxhack.echoscreencore.api.layout.EchoMeasureResult;
import com.knoxhack.echoscreencore.client.component.EchoComponent;
import com.knoxhack.echoscreencore.client.component.layout.ContainerComponent;
import com.knoxhack.echoscreencore.client.render.EchoRenderContext;
import com.knoxhack.echoscreencore.client.style.EchoStyleValues;

public final class ListComponent extends ContainerComponent {
    public ListComponent(EchoComponentFactory.Context context) {
        super(context);
    }

    @Override
    public EchoMeasureResult measure(EchoRenderContext context, int availableWidth, int availableHeight) {
        int gap = EchoStyleValues.length(style(), "gap", 0, 4, context.theme(), context.diagnostics());
        int height = 0;
        for (EchoComponent child : children()) {
            height += child.measure(context, availableWidth, availableHeight).height();
        }
        height += Math.max(0, children().size() - 1) * gap;
        int minHeight = EchoStyleValues.length(style(), "min-height", availableHeight, children().isEmpty() ? 42 : height, context.theme(), context.diagnostics());
        return new EchoMeasureResult(Math.max(0, availableWidth), Math.max(minHeight, height));
    }

    @Override
    protected void renderSelf(EchoRenderContext context) {
        super.renderSelf(context);
        if (children().isEmpty()) {
            String label = attr(context, "empty", "No entries");
            context.graphics().text(context.font(), label, bounds().x() + 8, bounds().y() + 8,
                context.theme().color("textMuted", 0xFF8AAFC2), false);
        }
    }
}
