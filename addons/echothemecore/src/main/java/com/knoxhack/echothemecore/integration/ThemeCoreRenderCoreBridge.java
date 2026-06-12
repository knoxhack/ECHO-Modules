package com.knoxhack.echothemecore.integration;

import com.knoxhack.echothemecore.api.EchoRenderThemeProvider;
import com.knoxhack.echothemecore.api.EchoThemeApi;
import com.knoxhack.echothemecore.api.EchoThemeRenderColorKey;
import com.knoxhack.echothemecore.api.EchoThemeRenderIntensityKey;
import com.knoxhack.echothemecore.api.EchoThemeRenderProfile;
import com.echoplatform.echocore.api.EchoRuntimeModules;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public final class ThemeCoreRenderCoreBridge {
    private static final EchoRenderThemeProvider PROVIDER = new Provider();

    private ThemeCoreRenderCoreBridge() {
    }

    public static boolean isRenderCoreLoaded() {
        return EchoRuntimeModules.isLoaded("echorendercore");
    }

    public static EchoRenderThemeProvider provider() {
        return PROVIDER;
    }

    public static boolean registerIfAvailable() {
        return isRenderCoreLoaded();
    }

    private static final class Provider implements EchoRenderThemeProvider {
        @Override
        public Identifier getThemeId(Player player) {
            return EchoThemeApi.getClientThemeId();
        }

        @Override
        public EchoThemeRenderProfile getRenderProfile(Player player) {
            return EchoThemeApi.getClientTheme().renderProfile();
        }

        @Override
        public int resolveColor(Player player, EchoThemeRenderColorKey key) {
            return getRenderProfile(player).color(key);
        }

        @Override
        public float resolveIntensity(Player player, EchoThemeRenderIntensityKey key) {
            return getRenderProfile(player).intensity(key);
        }
    }
}
