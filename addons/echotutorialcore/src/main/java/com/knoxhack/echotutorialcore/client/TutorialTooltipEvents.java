package com.knoxhack.echotutorialcore.client;

import com.knoxhack.echo.adaptercore.EchoBackendClientBridge;
import com.knoxhack.echotutorialcore.config.TutorialConfig;
import net.minecraft.ChatFormatting;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class TutorialTooltipEvents {
    private TutorialTooltipEvents() {}

    public static void onItemTooltip(Object event) {
        var stack = EchoBackendClientBridge.tooltipItemStack(event);
        if (event == null || stack.isEmpty() || !enabled()) {
            return;
        }
        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        var tooltip = TutorialClientData.tooltip(itemId);
        if (tooltip == null || tooltip.lines().isEmpty()) {
            return;
        }
        if (tooltip.requireShift() && !shiftDown()) {
            EchoBackendClientBridge.addTooltipLine(event, Component.literal("Hold Shift for ECHO-7 field note.").withStyle(ChatFormatting.DARK_AQUA));
            return;
        }
        for (String line : tooltip.lines()) {
            EchoBackendClientBridge.addTooltipLine(event, Component.literal(line).withStyle(ChatFormatting.DARK_AQUA));
        }
    }

    private static boolean enabled() {
        try {
            return TutorialConfig.SHOW_TOOLTIP_HELP.get();
        } catch (RuntimeException ignored) {
            return true;
        }
    }

    private static boolean shiftDown() {
        var window = Minecraft.getInstance().getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
    }
}
