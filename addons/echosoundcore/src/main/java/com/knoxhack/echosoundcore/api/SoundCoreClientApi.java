package com.knoxhack.echosoundcore.api;

import com.knoxhack.echosoundcore.api.context.SoundCoreContext;
import com.knoxhack.echosoundcore.api.context.SoundCoreContextStack;
import com.knoxhack.echosoundcore.client.music.SoundCoreMusicManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

public final class SoundCoreClientApi {
    private static final Minecraft MC = Minecraft.getInstance();

    private SoundCoreClientApi() {}

    public static void playLocalUi(SoundEvent sound, float volume, float pitch) {
        if (MC.level == null || MC.player == null) return;
        if (SoundCoreMusicManager.isMusicEvent(sound.location())) {
            SoundCoreMusicManager.playSoundEvent(sound.location(), "client-api:" + sound.location());
            return;
        }
        MC.getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
    }

    public static void playLocalUi(SoundEvent sound) {
        playLocalUi(sound, 1.0f, 1.0f);
    }

    public static void playLocalStinger(SoundEvent sound) {
        playLocalUi(sound, 1.0f, 1.0f);
    }

    public static void setLocalContext(SoundCoreContext context) {
        SoundCoreContextStack.setBase(context);
    }

    public static void stopLocalMusic() {
        SoundCoreMusicManager.stopControlled();
    }
}
