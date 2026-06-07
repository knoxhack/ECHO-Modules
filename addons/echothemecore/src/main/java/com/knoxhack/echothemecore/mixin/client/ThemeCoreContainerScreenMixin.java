package com.knoxhack.echothemecore.mixin.client;

import com.knoxhack.echothemecore.client.vanilla.VanillaUiSkinLayer;
import com.knoxhack.echothemecore.config.ThemeCoreConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class ThemeCoreContainerScreenMixin {
    @Inject(method = "extractContents", at = @At("HEAD"))
    private void echothemecore$extractThemeContainerBackground(GuiGraphicsExtractor graphics,
            int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        try {
            VanillaUiSkinLayer.renderReplacementContainerBackground((AbstractContainerScreen<?>) (Object) this, graphics);
        } catch (RuntimeException | LinkageError exception) {
            if (!ThemeCoreConfig.safeFallbackEnabled()) {
                throw exception;
            }
        }
    }

    @Inject(method = "extractContents", at = @At("TAIL"))
    private void echothemecore$extractThemeContainerAccents(GuiGraphicsExtractor graphics,
            int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        try {
            VanillaUiSkinLayer.renderReplacementContainerAccents((AbstractContainerScreen<?>) (Object) this, graphics);
        } catch (RuntimeException | LinkageError exception) {
            if (!ThemeCoreConfig.safeFallbackEnabled()) {
                throw exception;
            }
        }
    }
}
