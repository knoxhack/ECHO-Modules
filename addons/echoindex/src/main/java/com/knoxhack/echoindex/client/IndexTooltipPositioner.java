package com.knoxhack.echoindex.client;

import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import org.joml.Vector2i;
import org.joml.Vector2ic;

final class IndexTooltipPositioner implements ClientTooltipPositioner {
    static final IndexTooltipPositioner INSTANCE = new IndexTooltipPositioner();
    private static final int MARGIN = 4;

    private IndexTooltipPositioner() {
    }

    @Override
    public Vector2ic positionTooltip(int screenWidth, int screenHeight, int mouseX, int mouseY,
            int tooltipWidth, int tooltipHeight) {
        int maxX = Math.max(MARGIN, screenWidth - tooltipWidth - MARGIN);
        int maxY = Math.max(MARGIN, screenHeight - tooltipHeight - MARGIN);
        boolean compact = screenWidth <= 480 || screenHeight <= 300;
        int x;
        int y;
        if (compact) {
            x = clamp(mouseX - tooltipWidth / 2, MARGIN, maxX);
            y = mouseY + 10;
            if (y > maxY) {
                y = mouseY - tooltipHeight - 10;
            }
        } else {
            x = mouseX > screenWidth * 0.58F ? mouseX - tooltipWidth - 16 : mouseX + 14;
            y = mouseY - 12;
        }
        return new Vector2i(clamp(x, MARGIN, maxX), clamp(y, MARGIN, maxY));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
