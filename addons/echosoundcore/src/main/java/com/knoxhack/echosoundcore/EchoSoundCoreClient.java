package com.knoxhack.echosoundcore;

import com.mojang.blaze3d.audio.Channel;
import com.knoxhack.echosoundcore.client.ambience.SoundCoreAmbienceManager;
import com.knoxhack.echosoundcore.client.music.SoundCoreMusicManager;
import com.knoxhack.echosoundcore.api.context.SoundCoreContext;
import com.knoxhack.echosoundcore.api.context.SoundCoreContextStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;

public class EchoSoundCoreClient {
    private boolean terminalOpen;

    public EchoSoundCoreClient() {
        clientSetup();
    }

    public void clientSetup() {
        EchoSoundCore.LOGGER.info("ECHO: SoundCore client audio managers initialized.");
    }

    public void onClientTick() {
        updateLocalUiContext();
        SoundCoreMusicManager.tick();
        SoundCoreAmbienceManager.tick();
    }

    public boolean shouldSuppressVanillaMusic() {
        if (SoundCoreMusicManager.shouldSuppressVanillaMusic()) {
            Minecraft.getInstance().getMusicManager().stopPlaying();
            return true;
        }
        return false;
    }

    public void onSoundChannelStarted(SoundInstance sound, Channel channel) {
        SoundCoreMusicManager.onSoundChannelStarted(sound, channel);
    }

    private void updateLocalUiContext() {
        Minecraft minecraft = Minecraft.getInstance();
        boolean open = minecraft.screen != null
                && minecraft.screen.getClass().getName().startsWith("com.knoxhack.echoterminal.");
        if (open == terminalOpen) {
            return;
        }
        terminalOpen = open;
        SoundCoreContext context = SoundCoreContextStack.current().copy();
        context.terminalOpen(open);
        SoundCoreContextStack.setBase(context);
    }
}
