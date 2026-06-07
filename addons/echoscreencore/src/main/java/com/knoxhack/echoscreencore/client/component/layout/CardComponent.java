package com.knoxhack.echoscreencore.client.component.layout;

import com.knoxhack.echoscreencore.api.component.EchoComponentFactory;
import com.knoxhack.echoscreencore.api.style.EchoStyle;
import com.knoxhack.echoscreencore.client.component.EchoComponentSurfaces;
import com.knoxhack.echoscreencore.client.render.EchoRenderContext;
import com.knoxhack.echoscreencore.client.style.EchoStyleValues;

public class CardComponent extends ContainerComponent {
    public CardComponent(EchoComponentFactory.Context context) {
        super(context);
    }

    @Override
    public boolean focusable() {
        return !disabled() && !action().isBlank();
    }

    @Override
    protected void renderSelf(EchoRenderContext context) {
        EchoStyle current = effectiveStyle(context);
        String status = attr(context, "status", "");
        int fallbackBorder = switch (status) {
            case "ready", "success", "done" -> context.theme().color("success", 0xFF45FFB0);
            case "warning" -> context.theme().color("warning", 0xFFFFD166);
            case "danger" -> context.theme().color("danger", 0xFFFF4D6D);
            case "disabled", "locked" -> context.theme().color("disabled", 0xFF3B4652);
            default -> context.theme().color("borderMuted", 0xFF1A6F8A);
        };
        int background = EchoStyleValues.color(current, "background", context.theme(), context.theme().color("card", 0xCC0D1A2E));
        int border = EchoStyleValues.color(current, "border-color", context.theme(), fallbackBorder);
        if (!EchoComponentSurfaces.renderGlass(context, current, bounds(), background, border,
                context.theme().color("accent", 0xFF00E5FF), hovered() || focused())) {
            context.render().panel(context.graphics(), context.font(), bounds().x(), bounds().y(), bounds().width(), bounds().height(),
                background, border, context.accessibility().quietVisuals());
        }
    }
}
