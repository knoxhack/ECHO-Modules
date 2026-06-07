package com.knoxhack.echoscreencore.client.component.layout;

import com.knoxhack.echoscreencore.api.component.EchoComponentFactory;
import com.knoxhack.echoscreencore.api.style.EchoStyle;
import com.knoxhack.echoscreencore.client.component.EchoComponentSurfaces;
import com.knoxhack.echoscreencore.client.render.EchoRenderBridge;
import com.knoxhack.echoscreencore.client.render.EchoRenderContext;
import com.knoxhack.echoscreencore.client.style.EchoStyleValues;

public final class HeaderComponent extends ContainerComponent {
    public HeaderComponent(EchoComponentFactory.Context context) {
        super(context);
    }

    @Override
    public int contentTopInset(EchoRenderContext context) {
        return 34;
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
                        bg, border, context.accessibility().quietVisuals());
            }
        }
        String title = attr(context, "title", "");
        String subtitle = attr(context, "subtitle", "");
        int x = bounds().x() + 12;
        int y = bounds().y() + 9;
        if (!title.isBlank()) {
            context.graphics().text(context.font(), title, x, y, context.theme().color("accent", 0xFF00E5FF), false);
        }
        if (!subtitle.isBlank()) {
            context.graphics().text(context.font(), subtitle, x, y + 13, context.theme().color("textSecondary", 0xFFB7D7E3), false);
        }
    }
}
