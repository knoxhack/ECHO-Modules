package com.knoxhack.echoindex.service;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.index.IndexEntry;
import com.knoxhack.echocore.api.index.IndexEntryState;
import com.knoxhack.echoindex.Config;
import com.knoxhack.echoindex.EchoIndex;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class IndexDiscoveryStore {
    public static final IndexDiscoveryStore INSTANCE = new IndexDiscoveryStore();
    private static final String ROOT = "echoindex_state";
    private static final Identifier DATA_RECORD = EchoIndex.id("player/index_state");

    private IndexDiscoveryStore() {
    }

    public IndexEntryState state(Player player, Identifier entryId) {
        if (!Config.DISCOVERY_ENABLED.get()) {
            return IndexEntryState.VISIBLE;
        }
        IndexEntry entry = IndexService.INSTANCE.entry(player, entryId).orElse(null);
        if (entry == null) {
            return IndexEntryState.HIDDEN;
        }
        CompoundTag root = data(player);
        if (contains(root, "completed", entryId)) {
            return IndexEntryState.COMPLETED;
        }
        if (contains(root, "archived", entryId)) {
            return IndexEntryState.ARCHIVED;
        }
        if (contains(root, "discovered", entryId)) {
            return IndexEntryState.DISCOVERED;
        }
        IndexEntryState base = entry.defaultState();
        return base == IndexEntryState.HIDDEN || base == IndexEntryState.LOCKED ? base : IndexEntryState.VISIBLE;
    }

    public boolean discover(ServerPlayer player, Identifier entryId) {
        return add(player, "discovered", entryId);
    }

    public boolean markRead(ServerPlayer player, Identifier entryId) {
        return add(player, "read", entryId);
    }

    public boolean reset(ServerPlayer player, Identifier entryId) {
        if (player == null || entryId == null) {
            return false;
        }
        CompoundTag root = data(player);
        boolean changed = remove(root, "discovered", entryId)
                | remove(root, "read", entryId)
                | remove(root, "bookmarked", entryId)
                | remove(root, "completed", entryId)
                | remove(root, "archived", entryId);
        if (changed) {
            save(player, root);
        }
        return changed;
    }

    public boolean setBookmarked(ServerPlayer player, Identifier entryId, boolean bookmarked) {
        if (bookmarked) {
            return add(player, "bookmarked", entryId);
        }
        if (player == null || entryId == null) {
            return false;
        }
        CompoundTag root = data(player);
        boolean changed = remove(root, "bookmarked", entryId);
        if (changed) {
            save(player, root);
        }
        return changed;
    }

    public boolean isBookmarked(Player player, Identifier entryId) {
        return contains(data(player), "bookmarked", entryId);
    }

    public Set<Identifier> bookmarks(Player player) {
        return readSet(data(player), "bookmarked");
    }

    public boolean setRecipePinned(ServerPlayer player, Identifier recipeId, boolean pinned) {
        if (pinned) {
            return add(player, "pinned_recipe", recipeId);
        }
        if (player == null || recipeId == null) {
            return false;
        }
        CompoundTag root = data(player);
        boolean changed = remove(root, "pinned_recipe", recipeId);
        if (changed) {
            save(player, root);
        }
        return changed;
    }

    public boolean isRecipePinned(Player player, Identifier recipeId) {
        return contains(data(player), "pinned_recipe", recipeId);
    }

    public Set<Identifier> pinnedRecipes(Player player) {
        return readSet(data(player), "pinned_recipe");
    }

    public CompoundTag syncTag(Player player) {
        return data(player).copy();
    }

    public void applyClientSync(CompoundTag tag) {
        ClientIndexState.replace(tag == null ? new CompoundTag() : tag.copy());
    }

    private boolean add(ServerPlayer player, String prefix, Identifier entryId) {
        if (player == null || entryId == null) {
            return false;
        }
        CompoundTag root = data(player);
        LinkedHashSet<Identifier> values = new LinkedHashSet<>(readSet(root, prefix));
        boolean changed = values.add(entryId);
        if (changed) {
            writeSet(root, prefix, values);
            save(player, root);
        }
        return changed;
    }

    private CompoundTag data(Player player) {
        if (player == null) {
            return new CompoundTag();
        }
        CompoundTag record = EchoCoreServices.playerData(player).record(DATA_RECORD);
        if (!record.isEmpty()) {
            player.getPersistentData().put(ROOT, record.copy());
            return record.copy();
        }
        CompoundTag root = player.getPersistentData().getCompoundOrEmpty(ROOT);
        player.getPersistentData().put(ROOT, root);
        return root;
    }

    private void save(ServerPlayer player, CompoundTag root) {
        if (player == null || root == null) {
            return;
        }
        player.getPersistentData().put(ROOT, root);
        EchoCoreServices.playerData(player).putRecord(DATA_RECORD, root.copy());
        com.knoxhack.echoindex.network.IndexSync.send(player);
    }

    private static boolean contains(CompoundTag root, String prefix, Identifier id) {
        return id != null && readSet(root, prefix).contains(id);
    }

    private static boolean remove(CompoundTag root, String prefix, Identifier id) {
        LinkedHashSet<Identifier> values = new LinkedHashSet<>(readSet(root, prefix));
        if (!values.remove(id)) {
            return false;
        }
        writeSet(root, prefix, values);
        return true;
    }

    private static Set<Identifier> readSet(CompoundTag root, String prefix) {
        int count = root.getIntOr(prefix + "_count", 0);
        LinkedHashSet<Identifier> values = new LinkedHashSet<>();
        for (int index = 0; index < count; index++) {
            Identifier id = Identifier.tryParse(root.getStringOr(prefix + "_" + index, ""));
            if (id != null) {
                values.add(id);
            }
        }
        return Set.copyOf(values);
    }

    private static void writeSet(CompoundTag root, String prefix, Set<Identifier> values) {
        int index = 0;
        for (Identifier id : values) {
            if (id != null) {
                root.putString(prefix + "_" + index++, id.toString());
            }
        }
        root.putInt(prefix + "_count", index);
    }
}
