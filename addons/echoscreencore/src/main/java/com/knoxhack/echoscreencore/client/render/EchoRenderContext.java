package com.knoxhack.echoscreencore.client.render;

import com.knoxhack.echoscreencore.api.EchoDataContext;
import com.knoxhack.echoscreencore.api.theme.EchoAccessibilitySettings;
import com.knoxhack.echoscreencore.api.theme.EchoThemeTokenSnapshot;
import com.knoxhack.echoscreencore.client.debug.EchoScreenDiagnostics;
import com.knoxhack.echoscreencore.client.engine.EchoBindingResolver;
import com.knoxhack.echoscreencore.client.input.EchoFocusManager;
import com.knoxhack.echoscreencore.client.layout.EchoResponsiveContext;
import com.knoxhack.echoscreencore.client.overlay.EchoOverlayManager;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public record EchoRenderContext(
    GuiGraphicsExtractor graphics,
    Font font,
    int screenWidth,
    int screenHeight,
    int mouseX,
    int mouseY,
    float partialTick,
    EchoThemeTokenSnapshot themeTokens,
    EchoThemeBridge theme,
    EchoRenderBridge render,
    EchoAccessibilitySettings accessibility,
    EchoDataContext dataContext,
    EchoBindingResolver bindingResolver,
    EchoFocusManager focusManager,
    EchoResponsiveContext responsive,
    EchoOverlayManager overlays,
    EchoScreenDiagnostics diagnostics,
    EchoTextLayer textLayer,
    boolean debug
) {
}
