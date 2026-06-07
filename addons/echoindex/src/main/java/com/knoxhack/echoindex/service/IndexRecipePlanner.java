package com.knoxhack.echoindex.service;

import com.knoxhack.echocore.api.index.IndexRecipeSlot;
import com.knoxhack.echocore.api.index.IndexRecipeView;
import com.knoxhack.echocore.api.index.IndexSlotRole;
import com.knoxhack.echoindex.EchoIndex;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class IndexRecipePlanner {
    public static final Identifier CRAFTING_CATEGORY = EchoIndex.id("recipe/crafting");
    private static final int MAX_PLAN_CACHE = 256;
    private static final Map<PlanCacheKey, IndexRecipePlan> PLAN_CACHE = new LinkedHashMap<>(64, 0.75F, true);

    private IndexRecipePlanner() {
    }

    public static IndexRecipePlan plan(Player player, IndexRecipeView recipe) {
        if (recipe == null) {
            return new IndexRecipePlan(null, IndexRecipeActionState.PLAN_ONLY, List.of(), false, false, false,
                    "No recipe selected.");
        }
        if (cacheable(player)) {
            PlanCacheKey key = PlanCacheKey.of(player, recipe);
            synchronized (PLAN_CACHE) {
                IndexRecipePlan cached = PLAN_CACHE.get(key);
                if (cached != null) {
                    return cached;
                }
            }
            IndexRecipePlan computed = computePlan(player, recipe);
            synchronized (PLAN_CACHE) {
                PLAN_CACHE.put(key, computed);
                while (PLAN_CACHE.size() > MAX_PLAN_CACHE) {
                    PLAN_CACHE.remove(PLAN_CACHE.keySet().iterator().next());
                }
            }
            return computed;
        }
        return computePlan(player, recipe);
    }

    private static IndexRecipePlan computePlan(Player player, IndexRecipeView recipe) {
        boolean sourceCard = IndexRecipeSourceKind.isSourceCard(recipe);
        boolean crafting = isCraftingRecipe(recipe);
        List<IndexIngredientNeed> needs = needs(player, recipe);
        int missing = needs.stream().mapToInt(IndexIngredientNeed::missing).sum();
        boolean pinned = isPinned(player, recipe.id());
        String blocker = transferBlocker(player, recipe, crafting, sourceCard, missing);
        IndexRecipeActionState state;
        if (!crafting || sourceCard || recipe.locked()) {
            state = IndexRecipeActionState.PLAN_ONLY;
        } else if (missing > 0) {
            state = IndexRecipeActionState.MISSING;
        } else {
            state = IndexRecipeActionState.READY;
        }
        return new IndexRecipePlan(recipe.id(), state, needs, crafting, sourceCard, pinned, blocker);
    }

    private static boolean cacheable(Player player) {
        return player == null || player.level().isClientSide();
    }

    public static boolean isCraftingRecipe(IndexRecipeView recipe) {
        return recipe != null && CRAFTING_CATEGORY.equals(recipe.categoryId());
    }

    public static boolean isPinned(Player player, Identifier recipeId) {
        return player == null ? ClientIndexState.isRecipePinned(recipeId)
                : IndexDiscoveryStore.INSTANCE.isRecipePinned(player, recipeId);
    }

    private static List<IndexIngredientNeed> needs(Player player, IndexRecipeView recipe) {
        Map<Item, Integer> remaining = inventoryCounts(player);
        List<IndexIngredientNeed> needs = new ArrayList<>();
        for (IndexRecipeSlot slot : recipe.slots()) {
            if (slot.role() == IndexSlotRole.OUTPUT
                    || slot.role() == IndexSlotRole.INFO
                    || slot.role() == IndexSlotRole.MACHINE
                    || slot.stacks().isEmpty()) {
                continue;
            }
            ItemStack selected = selectChoice(slot.stacks(), remaining);
            if (selected.isEmpty()) {
                continue;
            }
            int required = Math.max(1, selected.getCount());
            Item item = selected.getItem();
            int available = Math.max(0, remaining.getOrDefault(item, 0));
            int missing = Math.max(0, required - available);
            int consumed = Math.min(required, available);
            if (consumed > 0) {
                remaining.put(item, available - consumed);
            }
            needs.add(new IndexIngredientNeed(slot.role(), slot.label(), slot.stacks(), selected,
                    required, available, missing));
        }
        return needs;
    }

    private static ItemStack selectChoice(List<ItemStack> choices, Map<Item, Integer> remaining) {
        ItemStack fallback = ItemStack.EMPTY;
        int bestAvailable = -1;
        ItemStack best = ItemStack.EMPTY;
        for (ItemStack choice : choices) {
            if (choice == null || choice.isEmpty()) {
                continue;
            }
            if (fallback.isEmpty()) {
                fallback = choice;
            }
            int available = remaining.getOrDefault(choice.getItem(), 0);
            if (available > bestAvailable) {
                bestAvailable = available;
                best = choice;
            }
        }
        return best.isEmpty() ? fallback.copy() : best.copy();
    }

    private static Map<Item, Integer> inventoryCounts(Player player) {
        Map<Item, Integer> counts = new LinkedHashMap<>();
        if (player == null) {
            return counts;
        }
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (!stack.isEmpty()) {
                counts.merge(stack.getItem(), stack.getCount(), Integer::sum);
            }
        }
        return counts;
    }

    private static String transferBlocker(Player player, IndexRecipeView recipe, boolean crafting,
            boolean sourceCard, int missing) {
        if (sourceCard) {
            return "Source cards cannot be transferred.";
        }
        if (!crafting) {
            return machineBlocker(recipe);
        }
        if (recipe.locked()) {
            return "Recipe is locked.";
        }
        if (missing > 0) {
            return "Missing ingredients.";
        }
        if (!(player != null && player.containerMenu instanceof AbstractCraftingMenu menu)) {
            return "Open the inventory crafting grid or a crafting table.";
        }
        if (!gridEmpty(menu)) {
            return "Clear the crafting grid first.";
        }
        return "";
    }

    private static String machineBlocker(IndexRecipeView recipe) {
        ItemStack machine = recipe.machine();
        if ((machine == null || machine.isEmpty()) && recipe != null) {
            for (IndexRecipeSlot slot : recipe.slots()) {
                if (slot.role() == IndexSlotRole.MACHINE) {
                    machine = selectFirst(slot.stacks());
                    break;
                }
            }
        }
        if (machine != null && !machine.isEmpty()) {
            return "Open " + machine.getHoverName().getString() + ".";
        }
        return "Open the required machine or station.";
    }

    private static ItemStack selectFirst(List<ItemStack> choices) {
        if (choices == null) {
            return ItemStack.EMPTY;
        }
        for (ItemStack choice : choices) {
            if (choice != null && !choice.isEmpty()) {
                return choice;
            }
        }
        return ItemStack.EMPTY;
    }

    private static boolean gridEmpty(AbstractCraftingMenu menu) {
        for (Slot slot : menu.getInputGridSlots()) {
            if (slot.hasItem()) {
                return false;
            }
        }
        return true;
    }

    private static int inventorySignature(Player player) {
        if (player == null) {
            return 0;
        }
        int hash = 1;
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (!stack.isEmpty()) {
                hash = 31 * hash + stack.getItem().hashCode();
                hash = 31 * hash + stack.getCount();
            }
        }
        return hash;
    }

    private static int gridSignature(Player player) {
        if (!(player != null && player.containerMenu instanceof AbstractCraftingMenu menu)) {
            return 0;
        }
        int hash = 1;
        for (Slot slot : menu.getInputGridSlots()) {
            ItemStack stack = slot.getItem();
            hash = 31 * hash + (stack.isEmpty() ? 0 : stack.getItem().hashCode());
            hash = 31 * hash + stack.getCount();
        }
        return hash;
    }

    private record PlanCacheKey(Identifier recipeId, int recipeHash, int inventoryHash, int gridHash,
            long clientRevision, boolean pinned) {
        private static PlanCacheKey of(Player player, IndexRecipeView recipe) {
            Identifier recipeId = recipe.id();
            return new PlanCacheKey(recipeId, recipe.hashCode(), inventorySignature(player), gridSignature(player),
                    ClientIndexState.revision(), ClientIndexState.isRecipePinned(recipeId));
        }
    }
}
