package com.knoxhack.echoindex.service;

import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;

public final class ClientIndexState {
    private static volatile CompoundTag snapshot = new CompoundTag();
    private static volatile long revision;

    private ClientIndexState() {
    }

    public static void replace(CompoundTag tag) {
        snapshot = tag == null ? new CompoundTag() : tag.copy();
        IndexRecipeQueryClientState.applyHealth(snapshot.getCompoundOrEmpty("recipe_health"));
        revision++;
    }

    public static long revision() {
        return revision;
    }

    public static boolean isBookmarked(Identifier id) {
        return contains("bookmarked", id);
    }

    public static Set<Identifier> bookmarks() {
        return readSet("bookmarked");
    }

    public static boolean isRecipePinned(Identifier id) {
        return contains("pinned_recipe", id);
    }

    public static Set<Identifier> pinnedRecipes() {
        return readSet("pinned_recipe");
    }

    private static boolean contains(String prefix, Identifier id) {
        return id != null && readSet(prefix).contains(id);
    }

    private static Set<Identifier> readSet(String prefix) {
        CompoundTag root = snapshot;
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
}
