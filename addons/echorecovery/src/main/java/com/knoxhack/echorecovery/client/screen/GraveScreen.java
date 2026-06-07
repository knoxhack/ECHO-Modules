package com.knoxhack.echorecovery.client.screen;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.client.ui.EchoCyberGlassUi;
import com.knoxhack.echorecovery.config.RecoveryConfig;
import com.knoxhack.echorecovery.menu.GraveMenu;
import com.knoxhack.echorecovery.net.RecoverAllPacket;
import com.knoxhack.echonetcore.client.EchoNetClientActions;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class GraveScreen extends AbstractContainerScreen<GraveMenu> {
    private static final int PANEL_COLOR = 0xEE1A1A1A;
    private static final int BORDER_COLOR = 0xFF444444;
    private static final int TITLE_COLOR = 0xFFCCCCCC;
    private static final int TEXT_COLOR = 0xFFAAAAAA;
    private static final int BUTTON_BG = 0xFF333333;
    private static final int BUTTON_HOVER = 0xFF555555;
    private static final int BUTTON_TEXT = 0xFF00E5FF;
    private static final int WARN_COLOR = 0xFFFFAA00;

    public GraveScreen(GraveMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 222);
        this.inventoryLabelY = 128;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.translatable("screen.echorecovery.grave.recover_all"), button -> {
            if (menu.getGrave() != null) {
                EchoNetClientActions.trySendServerboundAction(new RecoverAllPacket(menu.getPos()));
            }
        }).bounds(leftPos + imageWidth - 72, topPos + 4, 64, 14).build());
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int x = leftPos;
        int y = topPos;
        int panelColor = color("recovery.grave.panel", PANEL_COLOR);
        int borderColor = color("recovery.grave.border", BORDER_COLOR);
        int titleColor = color("recovery.grave.title", TITLE_COLOR);
        int textColor = color("recovery.grave.text", TEXT_COLOR);
        int warnColor = color("recovery.grave.warning", WARN_COLOR);

        EchoCyberGlassUi.panel(graphics, x, y, imageWidth, imageHeight, panelColor, borderColor);

        graphics.text(this.font, this.title, x + 8, y + 6, titleColor, false);

        graphics.text(this.font, this.playerInventoryTitle, x + 8, y + this.inventoryLabelY, textColor, false);

        var grave = menu.getGrave();
        if (grave != null) {
            int count = grave.itemCount();
            int max = grave.getContainerSize();
            String id = grave.graveId().toString();
            String info = count + "/" + max + " | " + grave.xpStored() + " XP";
            int infoWidth = this.font.width(info);
            graphics.text(this.font, info, x + imageWidth - 78 - infoWidth, y + 6, textColor, false);
            graphics.text(this.font, "Owner: " + (grave.ownerName().isBlank() ? "Unknown" : grave.ownerName()),
                    x + 8, y + imageHeight - 34, textColor, false);
            graphics.text(this.font, "ID: " + id.substring(0, Math.min(8, id.length())) + " | " + grave.graveTypeId(),
                    x + 8, y + imageHeight - 24, grave.contaminated() ? warnColor : textColor, false);

            int expirationMinutes = RecoveryConfig.GRAVE_EXPIRATION_MINUTES.get();
            if (expirationMinutes > 0) {
                long elapsed = (System.currentTimeMillis() - grave.createdAt()) / 60000L;
                long remaining = expirationMinutes - elapsed;
                if (remaining > 0) {
                    String timeText = "Expires in " + remaining + "m";
                    graphics.text(this.font, timeText, x + 8, y + imageHeight - 14, textColor, false);
                } else {
                    graphics.text(this.font, "Expired", x + 8, y + imageHeight - 14, warnColor, false);
                }
            }
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
    }

    private static int color(String token, int fallback) {
        try {
            return EchoCoreServices.themeService().resolveColor(token, fallback);
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }
}
