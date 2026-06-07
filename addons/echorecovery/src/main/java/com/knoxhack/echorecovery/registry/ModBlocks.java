package com.knoxhack.echorecovery.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echorecovery.EchoRecovery;
import com.knoxhack.echorecovery.block.GraveBlock;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class ModBlocks {
    public static final Object BLOCKS = EchoBackendRegistryBridge.create(BuiltInRegistries.BLOCK, EchoRecovery.MODID);
    private static final List<EchoBackendRegistryEntry<Block>> BLOCK_ITEMS = new ArrayList<>();

    public static final EchoBackendRegistryEntry<Block> GRAVE = grave("grave", GraveBlock.GraveVariant.GRAVE);
    public static final EchoBackendRegistryEntry<Block> DEATH_CACHE = grave("death_cache", GraveBlock.GraveVariant.DEATH_CACHE);
    public static final EchoBackendRegistryEntry<Block> RECOVERY_CACHE = grave("recovery_cache", GraveBlock.GraveVariant.RECOVERY_CACHE);
    public static final EchoBackendRegistryEntry<Block> SOUL_URN = grave("soul_urn", GraveBlock.GraveVariant.SOUL_URN);
    public static final EchoBackendRegistryEntry<Block> VOID_CACHE = grave("void_cache", GraveBlock.GraveVariant.VOID_CACHE);

    private ModBlocks() {}

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(BLOCKS, eventBus);
    }

    public static List<EchoBackendRegistryEntry<Block>> blockItems() {
        return List.copyOf(BLOCK_ITEMS);
    }

    private static EchoBackendRegistryEntry<Block> grave(String name, GraveBlock.GraveVariant variant) {
        EchoBackendRegistryEntry<Block> block = EchoBackendRegistryBridge.registerWithId(BLOCKS, name, id -> {
            BlockBehaviour.Properties properties = BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, id))
                    .mapColor(MapColor.STONE)
                    .strength(2.0F, 6.0F)
                    .sound(SoundType.STONE)
                    .noOcclusion();
            return new GraveBlock(variant, properties);
        });
        BLOCK_ITEMS.add(block);
        return block;
    }
}
