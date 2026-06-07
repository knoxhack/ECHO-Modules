package com.knoxhack.echoashfallprotocol.mixin.client;

import com.knoxhack.echoashfallprotocol.client.screen.EchoVanillaScreenTheme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Button.Plain.class)
public abstract class EchoButtonMixin {
    @Inject(method = "extractContents", at = @At("HEAD"), cancellable = true)
    private void echo$renderTerminalButton(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (EchoVanillaScreenTheme.renderButton((Button) (Object) this, graphics)) {
            ci.cancel();
        }
    }
}
