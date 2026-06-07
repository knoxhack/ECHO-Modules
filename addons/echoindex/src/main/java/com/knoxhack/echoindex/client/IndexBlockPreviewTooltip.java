package com.knoxhack.echoindex.client;

import com.knoxhack.echocore.client.ui.EchoCyberGlassUi;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

final class IndexBlockPreviewTooltip implements ClientTooltipComponent {
    private static final int FRAME_PAD = 2;
    private static final int GAP = 2;

    private final ItemStack stack;
    private final ClientTooltipComponent vanillaImage;
    private final int previewSize;
    private final int frameSize;

    IndexBlockPreviewTooltip(IndexBlockPreviewTooltipData data) {
        this.stack = data.stack().copy();
        this.stack.setCount(1);
        this.vanillaImage = data.vanillaImage().map(IndexBlockPreviewTooltip::clientComponent).orElse(null);
        this.previewSize = data.previewSize();
        this.frameSize = previewSize + FRAME_PAD * 2;
    }

    @Override
    public int getHeight(Font font) {
        return frameSize + (vanillaImage == null ? 0 : vanillaImage.getHeight(font) + GAP);
    }

    @Override
    public int getWidth(Font font) {
        return Math.max(frameSize, vanillaImage == null ? 0 : vanillaImage.getWidth(font));
    }

    @Override
    public void extractImage(Font font, int x, int y, int width, int height, GuiGraphicsExtractor graphics) {
        int cy = y;
        if (vanillaImage != null) {
            vanillaImage.extractImage(font, x, cy, width, height, graphics);
            cy += vanillaImage.getHeight(font) + GAP;
        }
        int frameX = x + Math.max(0, (getWidth(font) - frameSize) / 2);
        graphics.fill(frameX, cy, frameX + frameSize, cy + frameSize,
                IndexThemeStyle.alpha(IndexThemeStyle.FALLBACK_PANEL, 218));
        graphics.fill(frameX + FRAME_PAD, cy + FRAME_PAD, frameX + frameSize - FRAME_PAD, cy + frameSize - FRAME_PAD,
                IndexThemeStyle.alpha(IndexThemeStyle.FALLBACK_ROW, 168));
        EchoCyberGlassUi.calmFrame(graphics, frameX, cy, frameSize, frameSize, IndexThemeStyle.FALLBACK_ACCENT);

        graphics.pose().pushMatrix();
        try {
            graphics.pose().translate(frameX + FRAME_PAD, cy + FRAME_PAD);
            graphics.pose().scale(previewSize / 16.0F, previewSize / 16.0F);
            graphics.item(stack, 0, 0);
        } finally {
            graphics.pose().popMatrix();
        }
    }

    private static ClientTooltipComponent clientComponent(TooltipComponent component) {
        return ClientTooltipComponent.create(component);
    }
}
