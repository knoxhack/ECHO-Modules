package com.knoxhack.echothemecore.mixin.client;

import com.knoxhack.echothemecore.client.replacement.ThemeCoreReplacementRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Button.Plain.class)
public abstract class ThemeCoreButtonMixin {
    @Inject(method = "extractContents", at = @At("HEAD"), cancellable = true)
    private void echothemecore$extractThemeButton(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (ThemeCoreReplacementRenderer.renderButton((Button) (Object) this, graphics)) {
            ci.cancel();
        }
    }
}
