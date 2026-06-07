package com.knoxhack.echoscreencore.client.api;

import com.knoxhack.echoscreencore.api.EchoDataContext;
import com.knoxhack.echoscreencore.api.action.EchoActionContext;
import com.knoxhack.echoscreencore.api.theme.EchoAccessibilitySettings;
import com.knoxhack.echoscreencore.client.engine.EchoScreenEngine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/**
 * ScreenCore host that can render responsive pages at the live viewport or
 * canvas pages at a declared design size, scaling the whole surface down when
 * Minecraft GUI scale makes the logical viewport tiny.
 */
public final class EchoFitScreenSurface {
    public static final int DEFAULT_DESIGN_WIDTH = 854;
    public static final int DEFAULT_DESIGN_HEIGHT = 480;

    private final EchoScreenEngine engine;
    private final Integer overrideDesignWidth;
    private final Integer overrideDesignHeight;
    private int x;
    private int y;
    private int width = 1;
    private int height = 1;
    private Fit fit = responsiveFit(1, 1);
    private boolean mouseCaptured;

    public EchoFitScreenSurface(Identifier pageId, EchoDataContext dataContext) {
        this(pageId, dataContext, EchoAccessibilitySettings.DEFAULT, new Controls());
    }

    public EchoFitScreenSurface(Identifier pageId, EchoDataContext dataContext,
            EchoAccessibilitySettings accessibility) {
        this(pageId, dataContext, accessibility, new Controls());
    }

    public EchoFitScreenSurface(Identifier pageId, EchoDataContext dataContext,
            EchoAccessibilitySettings accessibility, EchoActionContext.ScreenControls controls) {
        this(pageId, dataContext, accessibility, controls, null, null);
    }

    public EchoFitScreenSurface(Identifier pageId, EchoDataContext dataContext,
            EchoAccessibilitySettings accessibility, EchoActionContext.ScreenControls controls,
            int designWidth, int designHeight) {
        this(pageId, dataContext, accessibility, controls,
                Integer.valueOf(Math.max(1, designWidth)), Integer.valueOf(Math.max(1, designHeight)));
    }

    private EchoFitScreenSurface(Identifier pageId, EchoDataContext dataContext,
            EchoAccessibilitySettings accessibility, EchoActionContext.ScreenControls controls,
            Integer overrideDesignWidth, Integer overrideDesignHeight) {
        this.overrideDesignWidth = overrideDesignWidth;
        this.overrideDesignHeight = overrideDesignHeight;
        this.engine = new EchoScreenEngine(pageId,
                dataContext == null ? EchoDataContext.empty() : dataContext,
                accessibility == null ? EchoAccessibilitySettings.DEFAULT : accessibility,
                controls == null ? new Controls() : controls);
    }

    public void render(GuiGraphicsExtractor graphics, int x, int y, int width, int height,
            int mouseX, int mouseY, float partialTick) {
        this.x = x;
        this.y = y;
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
        EchoScreenEngine.FitPolicy policy = fitPolicy();
        this.fit = fit(this.width, this.height, policy.designWidth(), policy.designHeight(), policy.canvas());

        int drawX = x + fit.offsetX();
        int drawY = y + fit.offsetY();
        graphics.pose().pushMatrix();
        try {
            graphics.pose().translate(drawX, drawY);
            graphics.pose().scale((float) fit.scale(), (float) fit.scale());
            graphics.enableScissor(0, 0, fit.layoutWidth(), fit.layoutHeight());
            engine.render(graphics, Minecraft.getInstance().font, fit.layoutWidth(), fit.layoutHeight(),
                    (int) localX(mouseX), (int) localY(mouseY), partialTick);
        } finally {
            graphics.disableScissor();
            graphics.pose().popMatrix();
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        mouseCaptured = false;
        if (!inside(mouseX, mouseY)) {
            return false;
        }
        boolean handled = engine.mouseClicked(localX(mouseX), localY(mouseY), button);
        mouseCaptured = handled;
        return handled;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!mouseCaptured && !inside(mouseX, mouseY)) {
            return false;
        }
        boolean handled = engine.mouseReleased(localX(mouseX), localY(mouseY), button);
        mouseCaptured = false;
        return handled;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!mouseCaptured && !inside(mouseX, mouseY)) {
            return false;
        }
        return engine.mouseDragged(localX(mouseX), localY(mouseY), button, dragX / fit.scale(), dragY / fit.scale());
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double deltaY) {
        return inside(mouseX, mouseY) && engine.mouseScrolled(localX(mouseX), localY(mouseY), deltaY);
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

    public boolean runAction(String actionId) {
        return engine.runAction(actionId);
    }

    public boolean debug() {
        return engine.debug();
    }

    public void setDebug(boolean debug) {
        engine.setDebug(debug);
    }

    public Fit currentFit() {
        return fit;
    }

    public static Fit responsiveFit(int viewportWidth, int viewportHeight) {
        return fit(viewportWidth, viewportHeight, viewportWidth, viewportHeight, false);
    }

    public static Fit canvasFit(int viewportWidth, int viewportHeight, int designWidth, int designHeight) {
        return fit(viewportWidth, viewportHeight, designWidth, designHeight, true);
    }

    public static Fit fit(int viewportWidth, int viewportHeight, int designWidth, int designHeight) {
        return canvasFit(viewportWidth, viewportHeight, designWidth, designHeight);
    }

    public static Fit fit(int viewportWidth, int viewportHeight, int designWidth, int designHeight, boolean canvas) {
        int safeViewportWidth = Math.max(1, viewportWidth);
        int safeViewportHeight = Math.max(1, viewportHeight);
        int layoutWidth = canvas ? Math.max(safeViewportWidth, Math.max(1, designWidth)) : safeViewportWidth;
        int layoutHeight = canvas ? Math.max(safeViewportHeight, Math.max(1, designHeight)) : safeViewportHeight;
        double scale = canvas
                ? Math.min(1.0D, Math.min((double) safeViewportWidth / layoutWidth,
                        (double) safeViewportHeight / layoutHeight))
                : 1.0D;
        if (!Double.isFinite(scale) || scale <= 0.0D) {
            scale = 1.0D;
        }
        int scaledWidth = Math.max(1, (int) Math.floor(layoutWidth * scale));
        int scaledHeight = Math.max(1, (int) Math.floor(layoutHeight * scale));
        int offsetX = Math.max(0, (safeViewportWidth - scaledWidth) / 2);
        int offsetY = Math.max(0, (safeViewportHeight - scaledHeight) / 2);
        return new Fit(layoutWidth, layoutHeight, scaledWidth, scaledHeight, offsetX, offsetY, scale);
    }

    private EchoScreenEngine.FitPolicy fitPolicy() {
        if (overrideDesignWidth != null && overrideDesignHeight != null) {
            return EchoScreenEngine.FitPolicy.canvas(overrideDesignWidth, overrideDesignHeight);
        }
        return engine.fitPolicy();
    }

    private boolean inside(double mouseX, double mouseY) {
        int drawX = x + fit.offsetX();
        int drawY = y + fit.offsetY();
        return mouseX >= drawX && mouseY >= drawY
                && mouseX < drawX + fit.scaledWidth()
                && mouseY < drawY + fit.scaledHeight();
    }

    private double localX(double mouseX) {
        return fit.localX(mouseX, x);
    }

    private double localY(double mouseY) {
        return fit.localY(mouseY, y);
    }

    public record Fit(int layoutWidth, int layoutHeight, int scaledWidth, int scaledHeight,
            int offsetX, int offsetY, double scale) {
        public double localX(double mouseX, int originX) {
            return (mouseX - originX - offsetX) / scale;
        }

        public double localY(double mouseY, int originY) {
            return (mouseY - originY - offsetY) / scale;
        }

        public double localDelta(double delta) {
            return delta / scale;
        }
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
