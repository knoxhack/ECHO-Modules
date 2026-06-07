package com.knoxhack.echotextureforge.client.integration;

import com.knoxhack.echocore.api.EchoRuntimeModules;

public final class TextureForgeThemeCoreBridge {
    private TextureForgeThemeCoreBridge() {
    }

    public static boolean isAvailable() {
        return EchoRuntimeModules.isLoaded("echothemecore");
    }
}
