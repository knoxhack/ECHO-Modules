package com.knoxhack.echocore.client.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class EchoNativeHubScreen extends Screen {
    public EchoNativeHubScreen() {
        super(Component.literal("ECHO Native Hub"));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.text(this.font, this.title, this.width / 2 - this.font.width(this.title) / 2, 24, 0xFFE6F7FF, false);
    }
}
