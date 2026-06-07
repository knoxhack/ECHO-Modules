package com.knoxhack.echoholomap.api;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public interface IHoloMapChunkActionProvider {
    Identifier providerId();

    HoloMapChunkActionResult handle(ServerPlayer player, HoloMapChunkSelection selection, Identifier actionId);
}
