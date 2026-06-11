package com.knoxhack.echoskyrelayprotocol.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoskyrelayprotocol.EchoSkyRelayProtocol;
import com.knoxhack.echoskyrelayprotocol.contract.SkyRelayRuntimeContracts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public final class SkyRelayBlocks {
    public static final Object BLOCKS = EchoBackendRegistryBridge.create(BuiltInRegistries.BLOCK, EchoSkyRelayProtocol.MODID);

    public static final EchoBackendRegistryEntry<Block> DAMAGED_RELAY_CORE = relayDevice(
            "damaged_relay_core", MapColor.COLOR_RED, 5.0F, 7,
            "Relay Core damaged. Restore emergency power, then scan for anchor candidates.");
    public static final EchoBackendRegistryEntry<Block> RELAY_ANCHOR_NODE = relayDevice(
            "relay_anchor_node", MapColor.COLOR_CYAN, 4.0F, 5,
            "Anchor node idle. Insert a Relay Anchor Key and stabilize power before docking.");
    public static final EchoBackendRegistryEntry<Block> FRAGMENT_DOCKING_CLAMP = relayDevice(
            "fragment_docking_clamp", MapColor.METAL, 4.0F, 2,
            "Docking clamp ready. Nearby fragments require scan proof and anchor power.");
    public static final EchoBackendRegistryEntry<Block> ATMOSPHERIC_CONDENSER = relayDevice(
            "atmospheric_condenser", MapColor.COLOR_LIGHT_BLUE, 3.0F, 2,
            "Condenser intake primed. Filters required before water is safe.");
    public static final EchoBackendRegistryEntry<Block> STORM_SHIELD_PYLON = relayDevice(
            "storm_shield_pylon", MapColor.COLOR_BLUE, 4.0F, 8,
            "Shield pylon on standby. Brownouts can drop shield coverage.");
    public static final EchoBackendRegistryEntry<Block> PRESSURE_BULKHEAD = relayDevice(
            "pressure_bulkhead", MapColor.METAL, 5.0F, 0,
            "Bulkhead sealed. Shelter score increases when all storm-facing gaps are closed.");
    public static final EchoBackendRegistryEntry<Block> SKY_FRAGMENT_BEACON = relayDevice(
            "sky_fragment_beacon", MapColor.COLOR_YELLOW, 3.0F, 9,
            "Beacon listening. HoloMap fragment layer can reveal nearby drift signatures.");
    public static final EchoBackendRegistryEntry<Block> RELAY_SIGNAL_ARRAY = relayDevice(
            "relay_signal_array", MapColor.COLOR_PURPLE, 4.0F, 10,
            "Signal array incomplete. Calibrate chips to recover relay network permissions.");
    public static final EchoBackendRegistryEntry<Block> RELAY_MARKER_LIGHT = relayDevice(
            "relay_marker_light", MapColor.COLOR_ORANGE, 1.0F, 12,
            "Marker light reporting platform state.");
    public static final EchoBackendRegistryEntry<Block> AERO_SALVAGE_CRATE = relayDevice(
            "aero_salvage_crate", MapColor.COLOR_BROWN, 2.0F, 0,
            "Aero salvage crate. Expected contents: scrap, coils, circuits, and storm-worn parts.");
    public static final EchoBackendRegistryEntry<Block> VOID_RECOVERY_CACHE = relayDevice(
            "void_recovery_cache", MapColor.COLOR_BLACK, 3.0F, 4,
            "Void recovery cache reserved. Falls should punish travel, not erase progress.");
    public static final EchoBackendRegistryEntry<Block> SKYBRIDGE_PROJECTOR = relayDevice(
            "skybridge_projector", MapColor.COLOR_LIGHT_BLUE, 4.0F, 8,
            "Skybridge projector offline. Late-game route bridges require stable platform cores.");
    public static final EchoBackendRegistryEntry<Block> SIGNAL_CROWN_INTERFACE = relayDevice(
            "signal_crown_interface", MapColor.COLOR_PURPLE, 6.0F, 11,
            "Signal Crown interface locked. Complete final restoration sequence to reconnect the network.");
    public static final EchoBackendRegistryEntry<Block> STORM_OUTPUT_COLLECTOR = relayDevice(
            "storm_output_collector", MapColor.COLOR_CYAN, 4.0F, 6,
            "Storm output collector armed. Higher storm risk can produce rarer materials.");

    public static final List<EchoBackendRegistryEntry<Block>> ALL_BLOCKS = List.of(
            DAMAGED_RELAY_CORE,
            RELAY_ANCHOR_NODE,
            FRAGMENT_DOCKING_CLAMP,
            ATMOSPHERIC_CONDENSER,
            STORM_SHIELD_PYLON,
            PRESSURE_BULKHEAD,
            SKY_FRAGMENT_BEACON,
            RELAY_SIGNAL_ARRAY,
            RELAY_MARKER_LIGHT,
            AERO_SALVAGE_CRATE,
            VOID_RECOVERY_CACHE,
            SKYBRIDGE_PROJECTOR,
            SIGNAL_CROWN_INTERFACE,
            STORM_OUTPUT_COLLECTOR
    );

    private SkyRelayBlocks() {
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(BLOCKS, eventBus);
    }

    public static List<String> ids() {
        return ALL_BLOCKS.stream().map(entry -> entry.id().getPath()).toList();
    }

    private static EchoBackendRegistryEntry<Block> relayDevice(String id, MapColor color, float strength, int light, String status) {
        return block(id, properties -> new RelayDeviceBlock(properties, status), properties -> {
            BlockBehaviour.Properties configured = properties
                    .mapColor(color)
                    .strength(strength, strength * 2.0F)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops();
            if (light > 0) {
                configured = configured.lightLevel(state -> light);
            }
            return configured;
        });
    }

    private static EchoBackendRegistryEntry<Block> block(String name,
            Function<BlockBehaviour.Properties, Block> factory,
            UnaryOperator<BlockBehaviour.Properties> properties) {
        return EchoBackendRegistryBridge.registerWithId(BLOCKS, name, id -> factory.apply(
                properties.apply(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, id)))));
    }

    public static final class RelayDeviceBlock extends Block {
        private final String status;

        public RelayDeviceBlock(Properties properties, String status) {
            super(properties);
            this.status = status;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!level.isClientSide()) {
                player.sendSystemMessage(Component.literal("SKY RELAY // " + status));
            }
            return InteractionResult.SUCCESS_SERVER;
        }
    }
}
