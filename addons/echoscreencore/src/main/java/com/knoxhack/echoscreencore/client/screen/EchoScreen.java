package com.knoxhack.echoscreencore.client.screen;

import com.knoxhack.echoscreencore.api.EchoDataContext;
import com.knoxhack.echoscreencore.api.theme.EchoAccessibilitySettings;
import com.knoxhack.echoscreencore.client.api.EchoFitScreenSurface;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import java.util.ArrayList;
import java.util.List;

public final class EchoScreen extends Screen {
    private final Identifier pageId;
    private final EchoDataContext dataContext;
    private final EchoAccessibilitySettings accessibility;
    private final EchoFitScreenSurface surface;
    private final List<Identifier> history;

    public EchoScreen(Identifier pageId, EchoDataContext dataContext) {
        this(pageId, dataContext, EchoAccessibilitySettings.DEFAULT);
    }

    public EchoScreen(Identifier pageId, EchoDataContext dataContext, EchoAccessibilitySettings accessibility) {
        this(pageId, dataContext, accessibility, false);
    }

    public EchoScreen(Identifier pageId, EchoDataContext dataContext, EchoAccessibilitySettings accessibility, boolean debug) {
        this(pageId, dataContext, accessibility, debug, List.of());
    }

    private EchoScreen(Identifier pageId, EchoDataContext dataContext, EchoAccessibilitySettings accessibility, boolean debug, List<Identifier> history) {
        super(Component.literal("ECHO: ScreenCore"));
        this.pageId = pageId;
        this.dataContext = dataContext == null ? EchoDataContext.empty() : dataContext;
        this.accessibility = accessibility == null ? EchoAccessibilitySettings.DEFAULT : accessibility;
        this.history = history == null ? List.of() : List.copyOf(history);
        this.surface = new EchoFitScreenSurface(pageId, this.dataContext, this.accessibility, new Controls());
        this.surface.setDebug(debug);
    }

    public Identifier pageId() {
        return pageId;
    }

    public EchoDataContext dataContext() {
        return dataContext;
    }

    public void markDataDirty() {
        surface.markDataDirty();
    }

    public void reloadPage() {
        surface.reloadPage();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        surface.render(graphics, 0, 0, width, height, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return surface.mouseClicked(event.x(), event.y(), event.button()) || super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return surface.mouseReleased(event.x(), event.y(), event.button()) || super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        return surface.mouseDragged(event.x(), event.y(), event.button(), dragX, dragY)
                || super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return surface.mouseScrolled(mouseX, mouseY, scrollY) || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return surface.keyPressed(event.key()) || super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return event != null && event.isAllowedChatCharacter() && surface.charTyped(event.codepointAsString()) || super.charTyped(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private final class Controls implements com.knoxhack.echoscreencore.api.action.EchoActionContext.ScreenControls {
        @Override
        public boolean close() {
            Minecraft.getInstance().setScreen(null);
            return true;
        }

        @Override
        public boolean back() {
            if (!history.isEmpty()) {
                ArrayList<Identifier> nextHistory = new ArrayList<>(history);
                Identifier previous = nextHistory.remove(nextHistory.size() - 1);
                Minecraft.getInstance().setScreen(new EchoScreen(previous, dataContext, accessibility, surface.debug(), nextHistory));
                return true;
            }
            Minecraft.getInstance().setScreen(null);
            return true;
        }

        @Override
        public boolean open(Identifier nextPage, EchoDataContext context) {
            ArrayList<Identifier> nextHistory = new ArrayList<>(history);
            nextHistory.add(pageId);
            Minecraft.getInstance().setScreen(new EchoScreen(nextPage == null ? pageId : nextPage, context == null ? dataContext : context, accessibility, surface.debug(), nextHistory));
            return true;
        }

        @Override
        public boolean toggleDebug() {
            return surface.runAction("debug_toggle");
        }
    }
}
