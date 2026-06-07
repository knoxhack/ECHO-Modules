package com.knoxhack.echosoundcore.integration;

import com.knoxhack.echocore.api.EchoAddonChapter;
import com.knoxhack.echocore.api.EchoAddonRegistry;
import com.knoxhack.echosoundcore.EchoSoundCore;
import net.minecraft.world.entity.player.Player;

public final class SoundCoreCoreIntegration {
    public static final String CHAPTER_ID = "soundcore";

    private SoundCoreCoreIntegration() {}

    public static void registerAddonChapter() {
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
                return EchoSoundCore.MODID;
            }

            @Override
            public String displayName() {
                return "ECHO: SoundCore";
            }

            @Override
            public String summary() {
                return "Shared adaptive audio, music, ambience, stingers, and UI sounds for the ECHO ecosystem.";
            }

            @Override
            public String statusLine(Player player) {
                return "SoundCore: online";
            }
        });
    }
}
