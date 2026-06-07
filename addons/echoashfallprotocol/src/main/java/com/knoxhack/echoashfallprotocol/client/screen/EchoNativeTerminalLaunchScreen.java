package com.knoxhack.echoashfallprotocol.client.screen;

import com.knoxhack.echoterminal.client.screen.EchoTerminalScreens;
import com.knoxhack.echoterminal.menu.EchoTerminalMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class EchoNativeTerminalLaunchScreen extends Screen {
    private final String mode = "TERMINAL";
    private int attempts;

    public EchoNativeTerminalLaunchScreen() {
        super(Component.translatable("container.echoterminal.echo_terminal"));
    }

    public EchoNativeTerminalLaunchScreen(String ignoredSurface) {
        this();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.attempts++ < 40) {
            openTerminalIfReady();
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        int panelWidth = Math.max(220, Math.min(460, width - 48));
        int left = Math.max(24, (width - panelWidth) / 2);
        int top = Math.max(32, height / 2 - 34);
        graphics.fill(0, 0, width, height, 0xDD03070C);
        graphics.fill(left, top, left + panelWidth, top + 68, 0xDD07131D);
        graphics.outline(left, top, panelWidth, 68, 0xDD38DFF4);
        graphics.text(this.font, "ECHO TERMINAL", left + 14, top + 14, 0xFF66E8FF, false);
        graphics.text(this.font, "opening live module screen", left + 14, top + 34, 0xFFE8F8FF, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void openTerminalIfReady() {
        Minecraft minecraft = this.minecraft == null ? Minecraft.getInstance() : this.minecraft;
        if (minecraft.player == null) {
            return;
        }
        minecraft.setScreen(EchoTerminalScreens.create(
                new EchoTerminalMenu(0, minecraft.player.getInventory()),
                minecraft.player.getInventory(),
                Component.translatable("container.echoterminal.echo_terminal")));
    }
}
