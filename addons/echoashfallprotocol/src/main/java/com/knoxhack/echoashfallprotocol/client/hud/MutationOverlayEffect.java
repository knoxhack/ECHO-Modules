package com.knoxhack.echoashfallprotocol.client.hud;

import com.knoxhack.echoashfallprotocol.Config;
import com.knoxhack.echoashfallprotocol.registry.ModAttachments;
import com.knoxhack.echoashfallprotocol.survival.MutationData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;

/**
 * Visual glitch overlay when VISUAL_GLITCH side effect is active.
 */
public class MutationOverlayEffect {

    private static final java.util.Random RANDOM = new java.util.Random();

    public static void render(GuiGraphicsExtractor graphics, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui) return;

        MutationData data = player.getData(ModAttachments.MUTATION_DATA.get());
        if (!data.hasSideEffect(MutationData.SideEffect.VISUAL_GLITCH)) return;
        double intensity = Config.HUD_WARNING_INTENSITY.get();
        if (intensity <= 0.0D) return;

        int width = graphics.guiWidth();
        int height = graphics.guiHeight();

        long time = System.currentTimeMillis();

        if ((time / 100) % 80 < 4) {
            for (int i = 0; i < 5; i++) {
                int y = RANDOM.nextInt(height);
                int lineHeight = 1 + RANDOM.nextInt(3);
                int xOffset = RANDOM.nextInt(10) - 5;
                int alpha = Math.min(255, (int)((40 + RANDOM.nextInt(60)) * intensity));
                int color = RANDOM.nextBoolean() ?
                        ((alpha << 24) | 0x00FF00) :
                        ((alpha << 24) | 0xFF0055);
                graphics.fill(xOffset, y, width + xOffset, y + lineHeight, color);
            }
        }

        if (data.getMutationCount() >= 3) {
            int edgeAlpha = Math.min(255, (int)((15 + (int)(Math.sin(time / 500.0) * 10)) * intensity));
            graphics.fill(0, 0, width, 8, (edgeAlpha << 24) | 0x33FF33);
            graphics.fill(0, height - 8, width, height, (edgeAlpha << 24) | 0x33FF33);
        }
    }
}
