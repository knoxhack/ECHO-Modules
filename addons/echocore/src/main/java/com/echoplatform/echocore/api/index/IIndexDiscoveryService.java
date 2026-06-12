package com.echoplatform.echocore.api.index;

import java.util.Set;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public interface IIndexDiscoveryService {
    IndexEntryState state(Player player, Identifier entryId);

    boolean discover(ServerPlayer player, Identifier entryId);

    boolean markRead(ServerPlayer player, Identifier entryId);

    boolean setBookmarked(ServerPlayer player, Identifier entryId, boolean bookmarked);

    boolean isBookmarked(Player player, Identifier entryId);

    Set<Identifier> bookmarks(Player player);
}
