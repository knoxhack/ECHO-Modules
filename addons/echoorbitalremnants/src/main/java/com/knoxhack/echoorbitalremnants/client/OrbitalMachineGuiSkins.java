package com.knoxhack.echoorbitalremnants.client;

import com.knoxhack.echoorbitalremnants.EchoOrbitalRemnants;
import com.knoxhack.echoorbitalremnants.block.OrbitalMachineBlock.MachineKind;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * Blits generated orbital machine panel skins behind code-rendered controls.
 */
public final class OrbitalMachineGuiSkins {
    private static final int SOURCE_WIDTH = 704;
    private static final int SOURCE_HEIGHT = 664;
    private static final String ROOT = "textures/gui/machine/";

    private OrbitalMachineGuiSkins() {
    }

    public static boolean render(GuiGraphicsExtractor graphics, MachineKind kind, int x, int y, int width, int height) {
        String skin = kind == null ? MachineKind.OXYGEN_COMPRESSOR.getSerializedName() : kind.getSerializedName();
        try {
            Identifier texture = Identifier.fromNamespaceAndPath(
                    EchoOrbitalRemnants.MODID, ROOT + skin + ".png");
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0.0F, 0.0F, width, height,
                    SOURCE_WIDTH, SOURCE_HEIGHT, SOURCE_WIDTH, SOURCE_HEIGHT, 0xFFFFFFFF);
            return true;
        } catch (RuntimeException | LinkageError ignored) {
            return false;
        }
    }
}
