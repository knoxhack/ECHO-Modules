package com.echoplatform.echocore.api.index;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public interface IIndexRegistry {
    default boolean register(IndexEntry entry) {
        return registerEntry(entry);
    }

    default boolean registerCategory(IndexCategory category) {
        return false;
    }

    default boolean registerEntry(IndexEntry entry) {
        return false;
    }

    default boolean registerContentProvider(IIndexContentProvider provider) {
        return false;
    }

    default List<IndexContentSnapshot> contentSnapshots(Player player) {
        return List.of();
    }

    default List<IndexCategory> categories(Player player) {
        return List.of();
    }

    default List<IndexEntry> entries(Player player) {
        return List.of();
    }

    default Optional<IndexEntry> entry(Player player, Identifier id) {
        return Optional.empty();
    }

    default Optional<IndexEntry> find(String id) {
        return Optional.empty();
    }

    default Collection<IndexEntry> all() {
        return List.of();
    }
}
