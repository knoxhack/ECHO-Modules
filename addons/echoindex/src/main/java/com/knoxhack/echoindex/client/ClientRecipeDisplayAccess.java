package com.knoxhack.echoindex.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;

public final class ClientRecipeDisplayAccess {
    private ClientRecipeDisplayAccess() {
    }

    public static List<RecipeDisplayEntry> recipeDisplays(Player player) {
        if (!(player instanceof LocalPlayer localPlayer)) {
            return List.of();
        }
        List<RecipeDisplayEntry> entries = new ArrayList<>();
        Set<Integer> seen = new LinkedHashSet<>();
        for (RecipeCollection collection : localPlayer.getRecipeBook().getCollections()) {
            for (RecipeDisplayEntry entry : collection.getRecipes()) {
                if (entry != null && seen.add(entry.id().index())) {
                    entries.add(entry);
                }
            }
        }
        return entries;
    }

    public static List<ItemStack> creativeCatalogStacks(Player player) {
        if (!(player instanceof LocalPlayer) || Minecraft.getInstance().level == null) {
            return List.of();
        }
        try {
            CreativeModeTabs.tryRebuildTabContents(FeatureFlags.DEFAULT_FLAGS, true,
                    Minecraft.getInstance().level.registryAccess());
        } catch (RuntimeException ignored) {
            return List.of();
        }
        Map<String, ItemStack> stacks = new LinkedHashMap<>();
        for (CreativeModeTab tab : CreativeModeTabs.allTabs()) {
            addStacks(stacks, tab.getDisplayItems());
            addStacks(stacks, tab.getSearchTabDisplayItems());
        }
        return List.copyOf(stacks.values());
    }

    private static void addStacks(Map<String, ItemStack> target, Iterable<ItemStack> stacks) {
        if (stacks == null) {
            return;
        }
        for (ItemStack stack : stacks) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            target.putIfAbsent(stackKey(stack), stack.copy());
        }
    }

    private static String stackKey(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()) + "#" + ItemStack.hashItemAndComponents(stack);
    }
}
