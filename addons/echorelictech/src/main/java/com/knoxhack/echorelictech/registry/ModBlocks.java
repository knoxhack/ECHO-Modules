package com.knoxhack.echorelictech.registry;

import net.minecraft.core.registries.Registries;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echorelictech.EchoRelicTech;
import com.knoxhack.echorelictech.block.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import java.util.ArrayList;
import java.util.List;

public final class ModBlocks {
    public static final Object BLOCKS = EchoBackendRegistryBridge.create(Registries.BLOCK, EchoRelicTech.MODID);
    private static final List<EchoBackendRegistryEntry<Block>> BLOCK_ITEMS = new ArrayList<>();

    public static final EchoBackendRegistryEntry<Block> RELIC_ANALYZER = tracked(EchoBackendRegistryBridge.registerBlock(BLOCKS, "relic_analyzer", RelicAnalyzerBlock::new, defaultProps()));
    public static final EchoBackendRegistryEntry<Block> PROTOTYPE_WORKBENCH = tracked(EchoBackendRegistryBridge.registerBlock(BLOCKS, "prototype_workbench", PrototypeWorkbenchBlock::new, defaultProps()));
    public static final EchoBackendRegistryEntry<Block> CONTAINMENT_LOCKER = tracked(EchoBackendRegistryBridge.registerBlock(BLOCKS, "containment_locker", ContainmentLockerBlock::new, defaultProps()));
    public static final EchoBackendRegistryEntry<Block> NULL_BATTERY_DOCK = tracked(EchoBackendRegistryBridge.registerBlock(BLOCKS, "null_battery_dock", NullBatteryDockBlock::new, defaultProps()));

    // Shell blocks for future expansion
    public static final EchoBackendRegistryEntry<Block> AI_CORE_CRADLE = tracked(EchoBackendRegistryBridge.registerSimpleBlock(BLOCKS, "ai_core_cradle", defaultProps()));
    public static final EchoBackendRegistryEntry<Block> RELIC_VAULT_DOOR = tracked(EchoBackendRegistryBridge.registerSimpleBlock(BLOCKS, "relic_vault_door", defaultProps()));
    public static final EchoBackendRegistryEntry<Block> RELIC_DISPLAY_STAND = tracked(EchoBackendRegistryBridge.registerSimpleBlock(BLOCKS, "relic_display_stand", defaultProps()));
    public static final EchoBackendRegistryEntry<Block> RELIC_CONTAINMENT_CASE = tracked(EchoBackendRegistryBridge.registerSimpleBlock(BLOCKS, "relic_containment_case", defaultProps()));
    public static final EchoBackendRegistryEntry<Block> NULL_SHIELDED_VAULT = tracked(EchoBackendRegistryBridge.registerSimpleBlock(BLOCKS, "null_shielded_vault", defaultProps()));

    private ModBlocks() {}

    public static void register(Object eventBus) {
      EchoBackendRegistryBridge.registerEventBus(BLOCKS, eventBus);
   }

    public static List<EchoBackendRegistryEntry<Block>> blockItems() {
        return List.copyOf(BLOCK_ITEMS);
    }

    private static java.util.function.UnaryOperator<BlockBehaviour.Properties> defaultProps() {
        return p -> p.mapColor(MapColor.COLOR_GRAY).strength(2.5F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops();
    }

    private static EchoBackendRegistryEntry<Block> tracked(EchoBackendRegistryEntry<Block> block) {
        BLOCK_ITEMS.add(block);
        return block;
    }
}
