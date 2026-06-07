package com.knoxhack.echoashfallprotocol.client;

import com.knoxhack.echoashfallprotocol.item.AshfallTooltip;
import com.knoxhack.echoashfallprotocol.registry.ModDataComponents;

/**
 * Renders Ashfall item tooltip data components without deprecated Item tooltip overrides.
 */
public final class AshfallTooltipHandler {
    private AshfallTooltipHandler() {
    }

    public static void onItemTooltip(Object event) {
        ClientTooltipEventView view = ClientTooltipEventView.from(event);
        if (view == null) {
            return;
        }
        AshfallTooltip tooltip = view.itemStack().get(ModDataComponents.ASHFALL_TOOLTIP.get());
        if (tooltip == null) {
            return;
        }
        tooltip.addToTooltip(view.context(), view.tooltip()::add, view.flags(),
                view.itemStack().getComponents());
    }
}
