package com.knoxhack.echoashfallprotocol.client.screen;

import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * Cinematic terminal background plates used by client-only pre-game screens.
 */
public final class EchoTerminalBackgrounds {
    private static final int SOURCE_WIDTH = 1920;
    private static final int SOURCE_HEIGHT = 1080;

    private EchoTerminalBackgrounds() {
    }

    public enum Plate {
        MAIN_MENU("nexus_main_menu"),
        WORLD_ARCHIVE("world_archive"),
        CREATE_SIMULATION("create_simulation"),
        MULTIPLAYER_UPLINK("multiplayer_uplink"),
        LOADING_BOOT("loading_boot"),
        TERRAIN_LOADING("terrain_loading");

        private final Identifier texture;

        Plate(String name) {
            this.texture = Identifier.fromNamespaceAndPath(
                    EchoAshfallProtocol.MODID, "textures/gui/terminal/" + name + ".png");
        }
    }

    public static Plate forScreen(Screen screen) {
        EchoAshfallScreenSurface surface = EchoAshfallScreenSurface.classify(screen);
        return surface.owned() ? surface.plate() : Plate.WORLD_ARCHIVE;
    }

    public static void render(GuiGraphicsExtractor graphics, Plate plate, int width, int height, int ticks, float partialTick) {
        render(graphics, plate, width, height, ticks, partialTick, 1.0F);
    }

    public static void render(
            GuiGraphicsExtractor graphics, Plate plate, int width, int height, int ticks, float partialTick, float alpha) {
        float clampedAlpha = EchoTerminalStyle.clamp(alpha, 0.0F, 1.0F);
        if (clampedAlpha <= 0.0F) {
            return;
        }

        try {
            drawCoverImage(graphics, plate, width, height, clampedAlpha);
            drawCinematicGrade(graphics, width, height, ticks, partialTick, clampedAlpha);
            EchoTerminalStyle.renderTerminalOverlay(graphics, width, height, ticks, partialTick, clampedAlpha);
        } catch (RuntimeException | LinkageError ignored) {
            EchoTerminalStyle.renderTerminalBackground(graphics, width, height, ticks, partialTick, clampedAlpha);
        }
    }

    private static void drawCoverImage(GuiGraphicsExtractor graphics, Plate plate, int width, int height, float alpha) {
        Plate selected = plate == null ? Plate.WORLD_ARCHIVE : plate;
        float screenAspect = width / (float) Math.max(1, height);
        float sourceAspect = SOURCE_WIDTH / (float) SOURCE_HEIGHT;
        int srcWidth = SOURCE_WIDTH;
        int srcHeight = SOURCE_HEIGHT;
        float u = 0.0F;
        float v = 0.0F;

        if (screenAspect > sourceAspect) {
            srcHeight = Math.max(1, Math.round(SOURCE_WIDTH / screenAspect));
            v = (SOURCE_HEIGHT - srcHeight) / 2.0F;
        } else if (screenAspect < sourceAspect) {
            srcWidth = Math.max(1, Math.round(SOURCE_HEIGHT * screenAspect));
            u = (SOURCE_WIDTH - srcWidth) / 2.0F;
        }

        graphics.blit(RenderPipelines.GUI_TEXTURED, selected.texture, 0, 0, u, v, width, height,
                srcWidth, srcHeight, SOURCE_WIDTH, SOURCE_HEIGHT, EchoTerminalStyle.fade(0xFFFFFFFF, alpha));
    }

    private static void drawCinematicGrade(
            GuiGraphicsExtractor graphics, int width, int height, int ticks, float partialTick, float alpha) {
        graphics.fill(0, 0, width, height, EchoTerminalStyle.fade(0x52020108, alpha));
        graphics.fill(0, 0, width, Math.max(1, height / 5), EchoTerminalStyle.fade(0x74010410, alpha));
        graphics.fill(0, Math.max(0, height - height / 3), width, height, EchoTerminalStyle.fade(0x98000006, alpha));
        graphics.fill(0, 0, Math.max(1, width / 6), height, EchoTerminalStyle.fade(0x56000008, alpha));
        graphics.fill(Math.max(0, width - width / 6), 0, width, height, EchoTerminalStyle.fade(0x52000008, alpha));

        int purpleTint = EchoTerminalStyle.fade(0x281B0042, alpha);
        int cyanTint = EchoTerminalStyle.fade(0x1C003844, alpha);
        graphics.fill(0, 0, width, height / 2, purpleTint);
        graphics.fill(0, height / 2, width, height, cyanTint);

        if (EchoTerminalStyle.terminalAnimation()) {
            int sweepWidth = Math.max(80, width / 7);
            int travel = width + sweepWidth * 2;
            int left = (int) ((ticks * 3L + Math.round(partialTick * 3.0F)) % travel) - sweepWidth * 2;
            graphics.fill(Math.max(0, left), 0, Math.min(width, left + 2), height,
                    EchoTerminalStyle.fade(0x6638DFF4, alpha));
            graphics.fill(Math.max(0, left + 2), 0, Math.min(width, left + sweepWidth), height,
                    EchoTerminalStyle.fade(0x102D7DFF, alpha));
        }
    }
}
