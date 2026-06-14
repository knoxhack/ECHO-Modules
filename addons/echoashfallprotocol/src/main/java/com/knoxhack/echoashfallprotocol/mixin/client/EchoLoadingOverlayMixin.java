package com.knoxhack.echoashfallprotocol.mixin.client;

import com.knoxhack.echoashfallprotocol.Config;
import com.knoxhack.echoashfallprotocol.client.screen.EchoNativeAshfallLoadingOverlay;
import com.knoxhack.echoashfallprotocol.client.screen.EchoTerminalBackgrounds;
import com.knoxhack.echoashfallprotocol.client.screen.EchoTerminalStyle;
import java.util.Map;
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
public abstract class EchoLoadingOverlayMixin {
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
    private void echo$renderTerminalLoadingOverlay(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!echo$isEnabled()) {
            return;
        }

        try {
            echo$extractTerminalRenderState(graphics, mouseX, mouseY, partialTick);
            ci.cancel();
        } catch (RuntimeException | LinkageError ignored) {
            // Let vanilla render the loading overlay if the custom shell cannot be drawn.
        }
    }

    private boolean echo$isEnabled() {
        if (echo$isNativeLoaderActive()) {
            return true;
        }
        try {
            return Config.ENABLE_ECHO_LOADING_SCREEN.get();
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static boolean echo$isNativeLoaderActive() {
        return dev.echo.nativeplatform.contracts.EchoNativeClientRuntimeEnvironment.isNativeLoaderActive();
    }

    private void echo$extractTerminalRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        long now = Util.getMillis();
        if (this.fadeIn && this.fadeInStart == -1L) {
            this.fadeInStart = now;
        }

        float fadeOutAnim = this.fadeOutStart > -1L ? (float) (now - this.fadeOutStart) / 1000.0F : -1.0F;
        float fadeInAnim = this.fadeInStart > -1L ? (float) (now - this.fadeInStart) / 500.0F : -1.0F;
        float overlayAlpha;
        if (fadeOutAnim >= 1.0F) {
            echo$extractDeferredSubtitlesOnly();
            graphics.nextStratum();
            overlayAlpha = 1.0F - Mth.clamp(fadeOutAnim - 1.0F, 0.0F, 1.0F);
        } else if (this.fadeIn) {
            echo$extractDeferredSubtitlesOnly();
            graphics.nextStratum();
            overlayAlpha = Mth.clamp(fadeInAnim, 0.15F, 1.0F);
        } else {
            this.minecraft.gameRenderer.getGameRenderState().guiRenderState.clearColorOverride = EchoTerminalStyle.BG;
            overlayAlpha = 1.0F;
        }

        float actualProgress = this.reload.getActualProgress();
        this.currentProgress = Mth.clamp(this.currentProgress * 0.95F + actualProgress * 0.050000012F, 0.0F, 1.0F);
        if (overlayAlpha > 0.0F) {
            echo$drawTerminalOverlay(graphics, partialTick, this.currentProgress, overlayAlpha);
        }

        if (fadeOutAnim >= 2.0F) {
            this.minecraft.setOverlay(null);
        }
    }

    private void echo$extractDeferredSubtitlesOnly() {
        // Loading overlays share the GUI render state with the current screen; re-extracting that screen can request
        // a second vanilla blur in the same frame.
        this.minecraft.gui.extractDeferredSubtitles();
    }

    private void echo$drawTerminalOverlay(GuiGraphicsExtractor graphics, float partialTick, float progress, float alpha) {
        int ticks = (int) (Util.getMillis() / 50L);
        if (echo$isNativeLoaderActive() && echo$drawNativeLoadingOverlay(graphics, partialTick, ticks, progress)) {
            return;
        }
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        int textScale = 1;
        EchoTerminalBackgrounds.render(
                graphics, EchoTerminalBackgrounds.Plate.LOADING_BOOT, width, height, ticks, partialTick, alpha);

        int margin = EchoTerminalStyle.clamp(width / 28, 14, 34);
        int panelWidth = Math.max(230, Math.min(width - margin * 2, width < 620 ? width - margin * 2 : 560));
        int panelHeight = height < 260 ? 126 : 154;
        int left = Math.max(margin, (width - panelWidth) / 2);
        int top = EchoTerminalStyle.clamp(height / 2 - panelHeight / 2, 24, Math.max(24, height - panelHeight - 24));
        int right = left + panelWidth;
        int bottom = top + panelHeight;

        graphics.fill(left, top, right, bottom, EchoTerminalStyle.fade(0xC207111A, alpha));
        graphics.outline(left, top, panelWidth, panelHeight, EchoTerminalStyle.fade(EchoTerminalStyle.LINE, alpha));
        graphics.fill(left + 1, top + 1, right - 1, top + 24, EchoTerminalStyle.fade(0x8620024A, alpha));
        graphics.fill(left + 14, top + 25, right - 14, top + 26,
                EchoTerminalStyle.fade(EchoTerminalStyle.pulseColor(ticks, 0x5038DFF4, 0xB466E8FF, 48), alpha));

        String percent = Math.min(100, Math.max(0, Math.round(progress * 100.0F))) + "%";
        EchoTerminalStyle.pixelText(graphics, "ECHO TERMINAL // ASHFALL PROTOCOL", left + 16, top + 8, EchoTerminalStyle.CYAN, alpha, textScale);
        EchoTerminalStyle.pixelText(graphics, percent, right - 16 - EchoTerminalStyle.pixelTextWidth(percent, textScale), top + 8,
                EchoTerminalStyle.GREEN, alpha, textScale);
        EchoTerminalStyle.pixelText(graphics, "RESOURCE RELOAD HANDSHAKE", left + 16, top + 36, EchoTerminalStyle.TEXT, alpha, textScale);
        EchoTerminalStyle.pixelText(graphics, "BOOT VECTOR: " + echo$statusLine(progress), left + 16, top + 51,
                echo$statusColor(progress), alpha, textScale);

        int barX = left + 16;
        int barY = bottom - 34;
        int barW = panelWidth - 32;
        EchoTerminalStyle.drawProgressBar(graphics, barX, barY, barW, 10, progress, alpha);

        if (panelHeight > 136) {
            String diagnostics = EchoTerminalStyle.clipPixelText(
                    "MOUNTING CLIENT RESOURCES / MODEL BAKE / TEXTURE ATLAS / TERMINAL SHELL",
                    panelWidth - 32, textScale);
            EchoTerminalStyle.pixelText(graphics, diagnostics, left + 16, bottom - 52, EchoTerminalStyle.MUTED, alpha, textScale);
        }

        String footer = "ECHO LISTENS WHILE THE WORLD RELOADS.";
        int footerWidth = EchoTerminalStyle.pixelTextWidth(footer, textScale);
        if (height > 210 && footerWidth + 44 < width) {
            EchoTerminalStyle.pixelText(graphics, footer, width - footerWidth - 22, height - 26, EchoTerminalStyle.CYAN_DIM, alpha, textScale);
        }
    }

    private static boolean echo$drawNativeLoadingOverlay(
            GuiGraphicsExtractor graphics,
            float partialTick,
            int ticks,
            float progress
    ) {
        String phase = echo$nativeLoadingPhase(progress);
        if (echo$invokeNativeLoadingRenderBridge(graphics, partialTick, ticks, progress, phase)) {
            return true;
        }
        try {
            Map<String, Object> state = EchoNativeAshfallLoadingOverlay.render(graphics, partialTick, ticks, progress, phase);
            return Boolean.TRUE.equals(state.get("rendered"));
        } catch (RuntimeException | LinkageError ignored) {
            return false;
        }
    }

    private static boolean echo$invokeNativeLoadingRenderBridge(
            GuiGraphicsExtractor graphics,
            float partialTick,
            int ticks,
            float progress,
            String phase
    ) {
        try {
            Class<?> bridge = Class.forName("dev.echo.nativeplatform.bootstrap.EchoNativeLiveLoadingRenderBridge");
            bridge.getMethod("render", Object.class, float.class, int.class, float.class, String.class)
                    .invoke(null, graphics, partialTick, ticks, progress, phase);
            Object snapshot = bridge.getMethod("snapshot").invoke(null);
            if (snapshot instanceof Map<?, ?> state) {
                return Boolean.TRUE.equals(state.get("rendered"));
            }
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return false;
        }
    }

    private static String echo$nativeLoadingPhase(float progress) {
        if (progress < 0.20F) {
            return "mounting product resources";
        }
        if (progress < 0.48F) {
            return "baking native render assets";
        }
        if (progress < 0.75F) {
            return "syncing Ashfall runtime surfaces";
        }
        if (progress < 0.98F) {
            return "verifying product world handoff";
        }
        return "native loading surface ready";
    }

    private static String echo$statusLine(float progress) {
        if (progress < 0.20F) {
            return "MOUNTING MOD RESOURCES";
        }
        if (progress < 0.48F) {
            return "BAKING RENDER ASSETS";
        }
        if (progress < 0.75F) {
            return "SYNCING ORBITAL UPLINK";
        }
        if (progress < 0.98F) {
            return "VERIFYING TERMINAL FRAME";
        }
        return "BOOT VECTOR STABLE";
    }

    private static int echo$statusColor(float progress) {
        if (progress < 0.20F) {
            return EchoTerminalStyle.AMBER;
        }
        if (progress < 0.98F) {
            return EchoTerminalStyle.CYAN;
        }
        return EchoTerminalStyle.GREEN;
    }
}
