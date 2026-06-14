package com.knoxhack.echothemecore.mixin.client;

import com.knoxhack.echothemecore.client.replacement.ThemeCoreLoadingOverlayRenderer;
import com.knoxhack.echothemecore.config.ThemeCoreConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LoadingOverlay.class)
public abstract class ThemeCoreLoadingOverlayMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    @Final
    private ReloadInstance reload;

    @Shadow
    @Final
    private boolean fadeIn;

    @Shadow
    private float currentProgress;

    @Shadow
    protected long fadeOutStart;

    @Shadow
    private long fadeInStart;

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void echothemecore$extractLoadingOverlay(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!ThemeCoreLoadingOverlayRenderer.enabled()) {
            return;
        }
        try {
            extractThemeCoreLoading(graphics, mouseX, mouseY, partialTick);
            ci.cancel();
        } catch (RuntimeException | LinkageError exception) {
            if (!ThemeCoreConfig.safeFallbackEnabled()) {
                throw exception;
            }
        }
    }

    private void extractThemeCoreLoading(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        long now = Util.getMillis();
        if (this.fadeIn && this.fadeInStart == -1L) {
            this.fadeInStart = now;
        }

        float fadeOutAnim = this.fadeOutStart > -1L ? (float) (now - this.fadeOutStart) / 1000.0F : -1.0F;
        float fadeInAnim = this.fadeInStart > -1L ? (float) (now - this.fadeInStart) / 500.0F : -1.0F;
        float overlayAlpha;
        if (fadeOutAnim >= 1.0F) {
            extractDeferredSubtitlesOnly();
            graphics.nextStratum();
            overlayAlpha = 1.0F - Mth.clamp(fadeOutAnim - 1.0F, 0.0F, 1.0F);
        } else if (this.fadeIn) {
            extractDeferredSubtitlesOnly();
            graphics.nextStratum();
            overlayAlpha = Mth.clamp(fadeInAnim, 0.15F, 1.0F);
        } else {
            this.minecraft.gameRenderer.getGameRenderState().guiRenderState.clearColorOverride =
                    com.knoxhack.echothemecore.client.ClientThemeState.currentTheme().colors().background();
            overlayAlpha = 1.0F;
        }

        float actualProgress = this.reload.getActualProgress();
        this.currentProgress = Mth.clamp(this.currentProgress * 0.95F + actualProgress * 0.050000012F, 0.0F, 1.0F);
        if (overlayAlpha > 0.0F) {
            ThemeCoreLoadingOverlayRenderer.render(graphics, this.minecraft, partialTick, this.currentProgress, overlayAlpha);
        }

        if (fadeOutAnim >= 2.0F) {
            this.minecraft.setOverlay(null);
        }
    }

    private void extractDeferredSubtitlesOnly() {
        // Loading overlays share the GUI render state with the current screen; re-extracting that screen can request
        // a second vanilla blur in the same frame.
        this.minecraft.gui.extractDeferredSubtitles();
    }
}
