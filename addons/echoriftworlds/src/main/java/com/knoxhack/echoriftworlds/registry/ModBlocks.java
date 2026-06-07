package com.knoxhack.echoriftworlds.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoriftworlds.EchoRiftWorlds;
import com.knoxhack.echoriftworlds.block.PocketRiftBlock;
import com.knoxhack.echoriftworlds.block.RiftCrackBlock;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class ModBlocks {
    public static final Object BLOCKS = EchoBackendRegistryBridge.create(BuiltInRegistries.BLOCK, EchoRiftWorlds.MODID);
    private static final List<EchoBackendRegistryEntry<Block>> BLOCK_ITEMS = new ArrayList<>();

    public static final EchoBackendRegistryEntry<Block> RIFT_CRACK =
            tracked(EchoBackendRegistryBridge.registerBlock(BLOCKS, "rift_crack", RiftCrackBlock::new, crackProps()));
    public static final EchoBackendRegistryEntry<Block> POCKET_RIFT =
            tracked(EchoBackendRegistryBridge.registerBlock(BLOCKS, "pocket_rift", PocketRiftBlock::new, pocketProps()));

    private ModBlocks() {
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(BLOCKS, eventBus);
    }

    public static List<EchoBackendRegistryEntry<Block>> blockItems() {
        return List.copyOf(BLOCK_ITEMS);
    }

    private static java.util.function.UnaryOperator<BlockBehaviour.Properties> crackProps() {
        return p -> p.mapColor(MapColor.COLOR_PURPLE).strength(1.0F, 3.0F)
                .lightLevel(state -> 6).sound(SoundType.AMETHYST);
    }

    private static java.util.function.UnaryOperator<BlockBehaviour.Properties> pocketProps() {
        return p -> p.mapColor(MapColor.COLOR_BLACK).strength(2.0F, 9.0F)
                .lightLevel(state -> 9).sound(SoundType.SCULK);
    }

    private static EchoBackendRegistryEntry<Block> tracked(EchoBackendRegistryEntry<Block> block) {
        BLOCK_ITEMS.add(block);
        return block;
    }
}
