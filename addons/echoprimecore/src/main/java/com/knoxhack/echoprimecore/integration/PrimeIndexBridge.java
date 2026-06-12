package com.knoxhack.echoprimecore.integration;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.index.IIndexContentProvider;
import com.echoplatform.echocore.api.index.IndexBuildContext;
import com.echoplatform.echocore.api.index.IndexCategory;
import com.echoplatform.echocore.api.index.IndexContentBuilder;
import com.echoplatform.echocore.api.index.IndexContentSnapshot;
import com.echoplatform.echocore.api.index.IndexEntry;
import com.echoplatform.echocore.api.index.IndexEntryState;
import com.echoplatform.echocore.api.index.IndexRecipeCategory;
import com.echoplatform.echocore.api.prime.PrimeIndexRegistry;
import com.knoxhack.echoprimecore.EchoPrimeCore;
import com.knoxhack.echoprimecore.progression.PrimePlayerData;
import com.knoxhack.echoprimecore.registry.ModItems;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public enum PrimeIndexBridge implements IIndexContentProvider {
    INSTANCE;

    private static PrimeIntegrationRegistry registry;

    public static void register(PrimeIntegrationRegistry source) {
        registry = source;
        EchoCoreServices.registerIndexContentProvider(INSTANCE);
    }

    @Override
    public Identifier id() {
        return EchoPrimeCore.id("provider/prime_index");
    }

    @Override
    public IndexContentSnapshot snapshot(IndexBuildContext context) {
        PrimeIntegrationRegistry safeRegistry = registry == null ? PrimeIntegrationLoader.registry() : registry;
        Player player = context == null ? null : context.player();
        IndexContentBuilder builder = IndexContentBuilder.create(id());
        for (PrimeIndexRegistry.PrimeIndexCategory category : safeRegistry.categories()) {
            builder.registerCategory(new IndexCategory(
                    category.id(),
                    category.title(),
                    category.summary(),
                    new ItemStack(ModItems.PRIME_FIELD_MANUAL.get()),
                    category.order(),
                    category.sourceModule()));
            builder.addRecipeCategories(List.of(new IndexRecipeCategory(
                    category.id(),
                    category.title(),
                    new ItemStack(ModItems.PRIME_FIELD_MANUAL.get()),
                    0xFF65E6D6,
                    category.order())));
        }
        for (PrimeIndexRegistry.PrimeRecipeHint hint : safeRegistry.recipeHints()) {
            builder.registerEntry(new IndexEntry(
                    hint.id(),
                    hint.categoryId(),
                    hint.title(),
                    "Source: " + hint.sourceModule(),
                    hint.hint(),
                    "Progress requirement: " + (hint.unlockFlag() == null ? "none" : hint.unlockFlag())
                            + "\n" + hint.hint(),
                    iconFor(hint.id()),
                    hint.sourceModule(),
                    List.of("prime", "recipe_hint"),
                    entryState(player, hint.unlockFlag()),
                    List.of(),
                    List.of(),
                    List.of(),
                    hint.order()));
        }
        return builder.snapshot();
    }

    private static IndexEntryState entryState(Player player, Identifier flag) {
        if (flag == null || player == null || PrimePlayerData.get(player).hasFlag(flag)) {
            return IndexEntryState.VISIBLE;
        }
        return IndexEntryState.LOCKED;
    }

    private static ItemStack iconFor(Identifier id) {
        String path = id.getPath();
        if (path.contains("crude_scanner")) {
            return new ItemStack(ModItems.CRUDE_SCANNER.get());
        }
        if (path.contains("prime_circuit")) {
            return new ItemStack(ModItems.PRIME_CIRCUIT.get());
        }
        if (path.contains("machine_frame")) {
            return new ItemStack(ModItems.MACHINE_FRAME.get());
        }
        return new ItemStack(ModItems.CIRCUIT_PLATE.get());
    }
}
