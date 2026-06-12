package com.echoplatform.echocore.api;

import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public interface ILensService {
    default boolean available() {
        return false;
    }

    default boolean registerScanType(Identifier scanId, String displayName) {
        return false;
    }

    default List<Identifier> scanTypes() {
        return List.of();
    }

    default boolean openLens(Player player) {
        return false;
    }
}
