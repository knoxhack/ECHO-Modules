package com.knoxhack.echoashfallprotocol.client.screen;

import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * Blits generated machine panel skins behind code-rendered controls.
 */
public final class MachineGuiSkins {
    public static final int COMPACT_SOURCE_WIDTH = 704;
    public static final int COMPACT_SOURCE_HEIGHT = 664;
    public static final int STATUS_SOURCE_WIDTH = 704;
    public static final int STATUS_SOURCE_HEIGHT = 616;
    public static final int STATUS_INVENTORY_SOURCE_HEIGHT = 880;
    public static final int RESEARCH_SOURCE_WIDTH = 1200;
    public static final int RESEARCH_SOURCE_HEIGHT = 760;

    private static final String ROOT = "textures/gui/machine/";

    private MachineGuiSkins() {
    }

    public static boolean renderCompact(GuiGraphicsExtractor graphics, String skin, int x, int y, int width, int height) {
        return render(graphics, skin, x, y, width, height, COMPACT_SOURCE_WIDTH, COMPACT_SOURCE_HEIGHT);
    }

    public static boolean renderStatus(
            GuiGraphicsExtractor graphics, boolean inventory, int x, int y, int width, int height) {
        return render(graphics, inventory ? "machine_status_inventory" : "machine_status",
                x, y, width, height, STATUS_SOURCE_WIDTH,
                inventory ? STATUS_INVENTORY_SOURCE_HEIGHT : STATUS_SOURCE_HEIGHT);
    }

    public static boolean renderResearchLab(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        return render(graphics, "research_lab", x, y, width, height, RESEARCH_SOURCE_WIDTH, RESEARCH_SOURCE_HEIGHT);
    }

    private static boolean render(
            GuiGraphicsExtractor graphics, String skin, int x, int y, int width, int height,
            int sourceWidth, int sourceHeight) {
        try {
            Identifier texture = Identifier.fromNamespaceAndPath(
                    EchoAshfallProtocol.MODID, ROOT + skin + ".png");
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0.0F, 0.0F, width, height,
                    sourceWidth, sourceHeight, sourceWidth, sourceHeight, 0xFFFFFFFF);
            return true;
        } catch (RuntimeException | LinkageError ignored) {
            return false;
        }
    }
}
