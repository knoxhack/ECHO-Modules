package com.knoxhack.echothemecore.mixin.client;

import com.knoxhack.echothemecore.client.vanilla.VanillaUiSkinLayer;
import com.knoxhack.echothemecore.config.ThemeCoreConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ThemeCoreScreenMixin {
    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void echothemecore$extractThemeScreenBackground(GuiGraphicsExtractor graphics,
            int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        try {
            VanillaUiSkinLayer.renderReplacementScreenBackground((Screen) (Object) this, graphics);
        } catch (RuntimeException | LinkageError exception) {
            if (!ThemeCoreConfig.safeFallbackEnabled()) {
                throw exception;
            }
        }
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void echothemecore$extractThemeScreenAccents(GuiGraphicsExtractor graphics,
            int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        try {
            VanillaUiSkinLayer.renderReplacementScreenAccents((Screen) (Object) this, graphics, mouseX, mouseY);
        } catch (RuntimeException | LinkageError exception) {
            if (!ThemeCoreConfig.safeFallbackEnabled()) {
                throw exception;
            }
        }
    }
}
