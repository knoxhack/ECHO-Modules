package com.knoxhack.echoscreencore;

import com.knoxhack.echocore.api.EchoAddonChapter;
import com.knoxhack.echocore.api.EchoAddonRegistry;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

public final class EchoScreenCoreMod {
    public static final String MOD_ID = "echoscreencore";
    public static final Logger LOGGER = LogUtils.getLogger();
    private static final String CHAPTER_ID = "screencore";

    public EchoScreenCoreMod() {
        commonSetup();
        LOGGER.info("ECHO: ScreenCore core loaded.");
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public void commonSetup() {
        EchoScreenCoreMod.registerAddonChapter();
    }

    private static void registerAddonChapter() {
        if (EchoAddonRegistry.isRegistered(CHAPTER_ID)) {
            return;
        }
        EchoAddonRegistry.register(new EchoAddonChapter() {
            @Override
            public String id() {
                return CHAPTER_ID;
            }

            @Override
            public String modId() {
                return MOD_ID;
            }

            @Override
            public String displayName() {
                return "ECHO: ScreenCore";
            }

            @Override
            public String summary() {
                return "Minecraft-native page markup, layout, component, style, binding, and accessibility framework for ECHO screens.";
            }

            @Override
            public String statusLine(Player player) {
                return "ScreenCore: XML pages, CSS-like styles, ThemeCore tokens, and RenderCore drawing bridge ready.";
            }
        });
    }
}
