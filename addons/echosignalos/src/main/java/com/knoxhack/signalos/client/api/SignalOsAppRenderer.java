package com.knoxhack.signalos.client.api;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;

public interface SignalOsAppRenderer {
    void render(SignalOsAppRenderContext context, GuiGraphicsExtractor graphics, int mouseX, int mouseY,
            float partialTick);

    default boolean mouseClicked(SignalOsAppRenderContext context, double mouseX, double mouseY, int button) {
        return false;
    }

    default boolean mouseReleased(SignalOsAppRenderContext context, double mouseX, double mouseY, int button) {
        return false;
    }

    default boolean mouseDragged(SignalOsAppRenderContext context, double mouseX, double mouseY, int button,
            double dragX, double dragY) {
        return false;
    }

    default boolean mouseScrolled(SignalOsAppRenderContext context, double mouseX, double mouseY, double deltaY) {
        return false;
    }

    default boolean keyPressed(SignalOsAppRenderContext context, KeyEvent event) {
        return false;
    }

    default boolean charTyped(SignalOsAppRenderContext context, CharacterEvent event) {
        return false;
    }
}
