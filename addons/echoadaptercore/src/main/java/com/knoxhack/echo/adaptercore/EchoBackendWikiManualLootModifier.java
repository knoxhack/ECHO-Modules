package com.knoxhack.echo.adaptercore;

import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * AdapterCore backend implementation for Ashfall's wiki manual loot injection.
 */
public abstract class EchoBackendWikiManualLootModifier extends LootModifier {
    private static volatile Identifier defaultGuideBookId;
    private static volatile Function<Identifier, ItemStack> guideBookFactory = id -> ItemStack.EMPTY;
    private static volatile Supplier<Object> codecSupplier;

    private final Identifier guideBookId;
    private final float chance;

    protected EchoBackendWikiManualLootModifier(Object conditions, int priority, Identifier guideBookId, float chance) {
        super((LootItemCondition[]) conditions, priority);
        this.guideBookId = guideBookId == null ? defaultGuideBookId : guideBookId;
        this.chance = chance;
    }

    public static void configure(Identifier fallbackGuideBookId, Function<Identifier, ItemStack> factory,
            Supplier<Object> codecHolder) {
        defaultGuideBookId = fallbackGuideBookId;
        guideBookFactory = factory == null ? id -> ItemStack.EMPTY : factory;
        codecSupplier = codecHolder;
    }

    public Identifier guideBookId() {
        return guideBookId;
    }

    public float chance() {
        return chance;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (shouldInject(chance, context.getRandom().nextFloat())) {
            ItemStack manual = guideBookFactory.apply(guideBookId);
            if (!manual.isEmpty()) {
                generatedLoot.add(manual);
            }
        }
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        Object codec = codecSupplier == null ? null : codecSupplier.get();
        return codec instanceof MapCodec<?> mapCodec ? castCodec(mapCodec) : null;
    }

    public static boolean shouldInjectForTests(float chance, float roll) {
        return shouldInject(chance, roll);
    }

    protected static boolean shouldInject(float chance, float roll) {
        return chance > 0.0F && roll < chance;
    }

    @SuppressWarnings("unchecked")
    private static MapCodec<? extends IGlobalLootModifier> castCodec(MapCodec<?> codec) {
        return (MapCodec<? extends IGlobalLootModifier>) codec;
    }
}
