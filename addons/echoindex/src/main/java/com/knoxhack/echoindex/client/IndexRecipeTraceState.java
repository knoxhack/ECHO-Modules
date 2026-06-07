package com.knoxhack.echoindex.client;

import com.knoxhack.echocore.api.index.IndexRecipeView;
import com.knoxhack.echocore.api.index.IndexSlotRole;
import com.knoxhack.echoindex.network.IndexRecipeQueryPacket;
import com.knoxhack.echoindex.service.IndexIngredientNeed;
import com.knoxhack.echoindex.service.IndexRecipePlan;
import com.knoxhack.echoindex.service.IndexRecipeQueryClientState;
import com.knoxhack.echoindex.service.IndexService;
import com.knoxhack.echonetcore.client.EchoNetClientActions;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public final class IndexRecipeTraceState {
    private static Trace current = Trace.empty();
    private static int lastPrefetched;
    private static int lastCacheHits;
    private static int lastCacheMisses;
    private static int lastStaleWarnings;

    private IndexRecipeTraceState() {
    }

    public static Trace open(ItemStack rootStack, IndexRecipeView recipe, IndexRecipePlan plan) {
        current = build(rootStack, recipe, plan, true);
        return current;
    }

    public static Trace preview(ItemStack rootStack, IndexRecipeView recipe, IndexRecipePlan plan) {
        return build(rootStack, recipe, plan, false);
    }

    public static void clear() {
        current = Trace.empty();
    }

    public static Trace current() {
        return current;
    }

    public static boolean activeFor(ItemStack stack) {
        return stack != null && !stack.isEmpty() && current.active()
                && current.rootItemId().equals(IndexService.itemId(stack.getItem()));
    }

    public static String diagnosticsLine() {
        if (!current.active()) {
            return "trace none / depth 0 / prefetch 0 / hits 0 / misses 0 / stale 0";
        }
        return "trace " + current.rootItemId()
                + " / depth " + current.depth()
                + " / nodes " + current.entries().size()
                + " / prefetch " + lastPrefetched
                + " / hits " + lastCacheHits
                + " / misses " + lastCacheMisses
                + " / stale " + lastStaleWarnings;
    }

    public static int lastPrefetched() {
        return lastPrefetched;
    }

    public static int lastCacheHits() {
        return lastCacheHits;
    }

    public static int lastCacheMisses() {
        return lastCacheMisses;
    }

    public static int lastStaleWarnings() {
        return lastStaleWarnings;
    }

    private static Trace build(ItemStack rootStack, IndexRecipeView recipe, IndexRecipePlan plan, boolean prefetch) {
        if (rootStack == null || rootStack.isEmpty() || recipe == null || plan == null) {
            return Trace.empty();
        }
        Map<Identifier, TraceEntryBuilder> builders = new LinkedHashMap<>();
        for (IndexIngredientNeed need : plan.needs()) {
            if (need.missing() <= 0 || need.selected().isEmpty()
                    || need.role() == IndexSlotRole.OUTPUT
                    || need.role() == IndexSlotRole.MACHINE) {
                continue;
            }
            Identifier itemId = IndexService.itemId(need.selected().getItem());
            builders.computeIfAbsent(itemId, ignored -> new TraceEntryBuilder(need.selected()))
                    .add(need);
        }
        int hits = 0;
        int misses = 0;
        int prefetched = 0;
        int stale = 0;
        long healthGeneration = IndexRecipeQueryClientState.health().generation();
        List<TraceEntry> entries = new ArrayList<>();
        for (TraceEntryBuilder builder : builders.values()) {
            ItemStack stack = builder.stack();
            Identifier itemId = IndexService.itemId(stack.getItem());
            var cached = IndexRecipeQueryClientState.result(stack.getItem());
            boolean cacheHit = cached.isPresent() && cached.get().generation() >= healthGeneration;
            boolean staleCache = cached.isPresent() && cached.get().generation() < healthGeneration;
            if (cacheHit) {
                hits++;
            } else {
                misses++;
            }
            if (staleCache) {
                stale++;
            }
            boolean requested = false;
            if (prefetch && IndexRecipeQueryClientState.shouldRequest(itemId)) {
                EchoNetClientActions.trySendServerboundAction(new IndexRecipeQueryPacket(itemId, true, true, true));
                requested = true;
                prefetched++;
            }
            int recipeCount = cached.map(result -> result.recipes().size()).orElse(0);
            int useCount = cached.map(result -> result.uses().size()).orElse(0);
            int sourceCount = cached.map(result -> result.sources().size()).orElse(0);
            entries.add(new TraceEntry(itemId, stack.copy(), builder.required(), builder.available(),
                    builder.missing(), recipeCount, useCount, sourceCount, cacheHit, requested));
        }
        if (prefetch) {
            lastCacheHits = hits;
            lastCacheMisses = misses;
            lastPrefetched = prefetched;
            lastStaleWarnings = stale;
        }
        return new Trace(IndexService.itemId(rootStack.getItem()), rootStack.copy(), recipe.id(),
                recipe.title(), entries, System.currentTimeMillis(), 1);
    }

    private static final class TraceEntryBuilder {
        private final ItemStack stack;
        private int required;
        private int available;
        private int missing;

        private TraceEntryBuilder(ItemStack stack) {
            this.stack = stack.copy();
        }

        private void add(IndexIngredientNeed need) {
            required += Math.max(0, need.required());
            available += Math.max(0, Math.min(need.available(), need.required()));
            missing += Math.max(0, need.missing());
        }

        private ItemStack stack() {
            return stack;
        }

        private int required() {
            return required;
        }

        private int available() {
            return available;
        }

        private int missing() {
            return Math.min(required, missing);
        }
    }

    public record Trace(
            Identifier rootItemId,
            ItemStack rootStack,
            Identifier rootRecipeId,
            String rootRecipeTitle,
            List<TraceEntry> entries,
            long createdAtMillis,
            int depth) {
        public Trace {
            rootStack = rootStack == null ? ItemStack.EMPTY : rootStack.copy();
            rootRecipeTitle = rootRecipeTitle == null ? "" : rootRecipeTitle;
            entries = entries == null ? List.of() : List.copyOf(entries);
            depth = Math.max(0, depth);
        }

        static Trace empty() {
            return new Trace(null, ItemStack.EMPTY, null, "", List.of(), 0L, 0);
        }

        public boolean active() {
            return rootItemId != null && rootRecipeId != null;
        }

        public String label() {
            if (!active()) {
                return "No active trace";
            }
            return rootStack.getHoverName().getString() + " > Missing";
        }
    }

    public record TraceEntry(
            Identifier itemId,
            ItemStack stack,
            int required,
            int available,
            int missing,
            int recipeCount,
            int useCount,
            int sourceCount,
            boolean cacheHit,
            boolean requested) {
        public TraceEntry {
            stack = stack == null ? ItemStack.EMPTY : stack.copy();
            required = Math.max(0, required);
            available = Math.max(0, available);
            missing = Math.max(0, missing);
            recipeCount = Math.max(0, recipeCount);
            useCount = Math.max(0, useCount);
            sourceCount = Math.max(0, sourceCount);
        }

        public String countLabel() {
            return available + "/" + required + (missing > 0 ? " missing " + missing : "");
        }

        public String dataLabel() {
            return "R" + recipeCount + " U" + useCount + " S" + sourceCount
                    + (requested ? " loading" : cacheHit ? " cached" : "");
        }

        public boolean sourceOnly() {
            return recipeCount == 0 && sourceCount > 0;
        }

        public boolean hasRecipeData() {
            return recipeCount > 0 || useCount > 0 || sourceCount > 0;
        }
    }
}
