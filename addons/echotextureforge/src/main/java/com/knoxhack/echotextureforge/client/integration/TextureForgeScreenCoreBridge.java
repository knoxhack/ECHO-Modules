package com.knoxhack.echotextureforge.client.integration;

import com.knoxhack.echocore.api.EchoRuntimeModules;

public final class TextureForgeScreenCoreBridge {
    private TextureForgeScreenCoreBridge() {
    }

    public static boolean isAvailable() {
        return EchoRuntimeModules.isLoaded("echoscreencore");
    }
}
