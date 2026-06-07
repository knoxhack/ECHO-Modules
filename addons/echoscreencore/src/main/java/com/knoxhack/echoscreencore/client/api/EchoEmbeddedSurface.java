package com.knoxhack.echoscreencore.client.api;

import com.knoxhack.echoscreencore.api.EchoDataContext;
import com.knoxhack.echoscreencore.api.action.EchoActionContext;
import com.knoxhack.echoscreencore.api.theme.EchoAccessibilitySettings;
import com.knoxhack.echoscreencore.client.engine.EchoScreenEngine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/**
 * Client-only ScreenCore host for embedding an EUI page inside another screen.
 */
public final class EchoEmbeddedSurface {
    private final EchoScreenEngine engine;
    private int x;
    private int y;
    private int width = 1;
    private int height = 1;

    public EchoEmbeddedSurface(Identifier pageId, EchoDataContext dataContext) {
        this(pageId, dataContext, EchoAccessibilitySettings.DEFAULT);
    }

    public EchoEmbeddedSurface(Identifier pageId, EchoDataContext dataContext, EchoAccessibilitySettings accessibility) {
        engine = new EchoScreenEngine(pageId, dataContext, accessibility, new Controls());
    }

    public void render(GuiGraphicsExtractor graphics, int x, int y, int width, int height,
            int mouseX, int mouseY, float partialTick) {
        this.x = x;
        this.y = y;
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.enableScissor(0, 0, this.width, this.height);
        engine.render(graphics, Minecraft.getInstance().font, this.width, this.height,
                mouseX - x, mouseY - y, partialTick);
        graphics.disableScissor();
        graphics.pose().popMatrix();
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return inside(mouseX, mouseY) && engine.mouseClicked(mouseX - x, mouseY - y, button);
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return inside(mouseX, mouseY) && engine.mouseReleased(mouseX - x, mouseY - y, button);
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return inside(mouseX, mouseY) && engine.mouseDragged(mouseX - x, mouseY - y, button, dragX, dragY);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double deltaY) {
        return inside(mouseX, mouseY) && engine.mouseScrolled(mouseX - x, mouseY - y, deltaY);
    }

    public boolean keyPressed(int key) {
        return engine.keyPressed(key);
    }

    public boolean charTyped(String typed) {
        return engine.charTyped(typed);
    }

    public void markDataDirty() {
        engine.markDataDirty();
    }

    public void reloadPage() {
        engine.reloadPage();
    }

    public void setDebug(boolean debug) {
        engine.setDebug(debug);
    }

    private boolean inside(double mouseX, double mouseY) {
        return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
    }

    private static final class Controls implements EchoActionContext.ScreenControls {
        @Override
        public boolean close() {
            return false;
        }

        @Override
        public boolean back() {
            return false;
        }

        @Override
        public boolean open(Identifier pageId, EchoDataContext context) {
            return false;
        }

        @Override
        public boolean toggleDebug() {
            return false;
        }
    }
}
