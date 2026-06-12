package com.echoplatform.echocore.api;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

public final class EchoFactionDataService {
    private static final Map<UUID, CompoundTag> FACTION_ROOTS = new ConcurrentHashMap<>();

    private EchoFactionDataService() {
    }

    public static CompoundTag exportRoot(Player player) {
        if (player == null) {
            return new CompoundTag();
        }
        return FACTION_ROOTS.getOrDefault(player.getUUID(), new CompoundTag()).copy();
    }

    public static void importRoot(Player player, CompoundTag root) {
        if (player != null) {
            FACTION_ROOTS.put(player.getUUID(), root == null ? new CompoundTag() : root.copy());
        }
    }
}
