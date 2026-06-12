package com.echoplatform.echocore.api.index;

import net.minecraft.world.entity.player.Player;

public interface IIndexOverlayService {
    boolean overlayEnabled(Player player);

    boolean excludedScreen(String screenClassName);
}
