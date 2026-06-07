package com.knoxhack.echoterminal.api;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;

public interface ClientTerminalTab extends TerminalTab {
    void render(TerminalRenderContext context, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick);

    default void onSelected(TerminalRenderContext context) {
    }

    default int contentHeight(TerminalRenderContext context) {
        return context.contentHeight();
    }

    default boolean mouseClicked(TerminalRenderContext context, double mouseX, double mouseY, int button) {
        return false;
    }

    default boolean mouseDragged(TerminalRenderContext context, double mouseX, double mouseY, int button,
            double dragX, double dragY) {
        return false;
    }

    default boolean mouseReleased(TerminalRenderContext context, double mouseX, double mouseY, int button) {
        return false;
    }

    default boolean mouseScrolled(TerminalRenderContext context, double mouseX, double mouseY, double delta) {
        return false;
    }

    default boolean keyPressed(TerminalRenderContext context, KeyEvent event) {
        return false;
    }

    default boolean charTyped(TerminalRenderContext context, CharacterEvent event) {
        return false;
    }
}
