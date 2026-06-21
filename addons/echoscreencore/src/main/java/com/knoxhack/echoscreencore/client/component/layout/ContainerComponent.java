package com.knoxhack.echoscreencore.client.component.layout;

import com.knoxhack.echoscreencore.api.component.EchoComponentFactory;
import com.knoxhack.echoscreencore.api.layout.EchoMeasureResult;
import com.knoxhack.echoscreencore.client.component.EchoComponentSurfaces;
import com.knoxhack.echoscreencore.client.component.AbstractEchoComponent;
import com.knoxhack.echoscreencore.client.component.EchoComponent;
import com.knoxhack.echoscreencore.client.component.EchoComponentSupport;
import com.knoxhack.echoscreencore.client.render.EchoRenderBridge;
import com.knoxhack.echoscreencore.client.render.EchoRenderContext;
import com.knoxhack.echoscreencore.api.style.EchoStyle;
import com.knoxhack.echoscreencore.client.style.EchoStyleValues;

public class ContainerComponent extends AbstractEchoComponent {
    public ContainerComponent(EchoComponentFactory.Context context) {
        super(EchoComponentSupport.node(context), EchoComponentSupport.children(context));
    }

    @Override
    public EchoMeasureResult measure(EchoRenderContext context, int availableWidth, int availableHeight) {
        int minHeight = EchoStyleValues.length(style(), "min-height", availableHeight, defaultHeight(context), context.theme(), context.diagnostics());
        int height = EchoStyleValues.length(style(), "height", availableHeight, minHeight, context.theme(), context.diagnostics());
        return new EchoMeasureResult(Math.max(0, availableWidth), Math.max(minHeight, height));
    }

    @Override
    public void render(EchoRenderContext context) {
        if (!isSubtreeDirty()) {
            return;
        }
        if ("hidden".equalsIgnoreCase(style().value("visibility", "visible"))
                || bounds().width() <= 0
                || bounds().height() <= 0) {
            clearRenderDirty();
            return;
        }
        if (isDirty()) {
            renderSelf(context);
        }
        for (EchoComponent child : children()) {
            child.render(context);
        }
        renderInteractionTooltip(context);
        clearRenderDirty();
    }

    @Override
    protected void renderSelf(EchoRenderContext context) {
        EchoStyle current = effectiveStyle(context);
        int background = EchoStyleValues.color(current, "background", context.theme(), 0);
        int border = EchoStyleValues.color(current, "border-color", context.theme(), context.theme().color("borderMuted", 0xFF1A6F8A));
        int borderWidth = EchoStyleValues.length(current, "border-width", 0, 0, context.theme(), context.diagnostics());
        if (background != 0 || borderWidth > 0) {
            int bg = background == 0 ? EchoRenderBridge.withAlpha(context.theme().color("panel", 0xCC08111F), 90) : background;
            if (!EchoComponentSurfaces.renderGlass(context, current, bounds(), bg, border,
                    context.theme().color("accent", 0xFF00E5FF), hovered() || focused())) {
                context.render().panel(context.graphics(), context.font(), bounds().x(), bounds().y(), bounds().width(), bounds().height(),
                    bg,
                    border,
                    context.accessibility().quietVisuals());
            }
        }
        String title = attr(context, "title", "");
        if (!title.isBlank()) {
            context.graphics().text(context.font(), title, bounds().x() + 8, bounds().y() + 6,
                context.theme().color("textPrimary", 0xFFEAFBFF), false);
        }
    }
}
