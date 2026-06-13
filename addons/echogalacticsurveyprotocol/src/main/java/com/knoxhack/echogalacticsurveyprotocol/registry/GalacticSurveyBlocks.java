package com.knoxhack.echogalacticsurveyprotocol.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echogalacticsurveyprotocol.EchoGalacticSurveyProtocol;
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

public final class GalacticSurveyBlocks {
    public static final Object BLOCKS = EchoBackendRegistryBridge.create(BuiltInRegistries.BLOCK, EchoGalacticSurveyProtocol.MODID);

    public static final EchoBackendRegistryEntry<Block> SURVEY_TERMINAL = surveyDevice(
            "survey_terminal", MapColor.COLOR_CYAN, 4.0F, 8,
            "Survey Network Offline. Restore relay power, then queue a starter probe.");
    public static final EchoBackendRegistryEntry<Block> PROBE_LAUNCHER = surveyDevice(
            "probe_launcher", MapColor.METAL, 4.5F, 7,
            "Probe launcher idle. Select a target sector and confirm charge before launch.");
    public static final EchoBackendRegistryEntry<Block> FUEL_MIXER = surveyDevice(
            "fuel_mixer", MapColor.COLOR_ORANGE, 3.5F, 3,
            "Fuel mixer calibrated for short-range canisters. Quality affects return margin.");
    public static final EchoBackendRegistryEntry<Block> SIGNAL_DISH = surveyDevice(
            "signal_dish", MapColor.COLOR_LIGHT_BLUE, 3.0F, 6,
            "Signal dish receiving partial telemetry. Confidence improves with probe coverage.");
    public static final EchoBackendRegistryEntry<Block> NAVIGATION_TABLE = surveyDevice(
            "navigation_table", MapColor.COLOR_BLUE, 2.5F, 4,
            "Navigation table shows route risk, fuel bands, and depot reach.");
    public static final EchoBackendRegistryEntry<Block> ORBITAL_SALVAGE_CRATE = surveyDevice(
            "orbital_salvage_crate", MapColor.COLOR_BROWN, 2.0F, 0,
            "Orbital salvage crate. Lens scan before opening near unstable wreckage.");
    public static final EchoBackendRegistryEntry<Block> REMOTE_DEPOT_ANCHOR = surveyDevice(
            "remote_depot_anchor", MapColor.COLOR_YELLOW, 4.0F, 5,
            "Remote depot anchor ready. Fuel, cargo, and probe recovery depend on placement.");
    public static final EchoBackendRegistryEntry<Block> SURVEY_BEACON_PYLON = surveyDevice(
            "survey_beacon_pylon", MapColor.COLOR_PURPLE, 3.5F, 9,
            "Survey beacon pylon marks a confirmed route objective.");
    public static final EchoBackendRegistryEntry<Block> ROUTE_STABILIZER_STATION = surveyDevice(
            "route_stabilizer_station", MapColor.COLOR_RED, 4.0F, 6,
            "Route stabilizer station reduces marginal-route risk after calibration.");
    public static final EchoBackendRegistryEntry<Block> SURVEY_ARRAY_CONSOLE = surveyDevice(
            "survey_array_console", MapColor.COLOR_BLACK, 6.0F, 11,
            "Galactic Survey Array locked. Publish a complete sector atlas to restore it.");

    public static final List<EchoBackendRegistryEntry<Block>> ALL_BLOCKS = List.of(
            SURVEY_TERMINAL,
            PROBE_LAUNCHER,
            FUEL_MIXER,
            SIGNAL_DISH,
            NAVIGATION_TABLE,
            ORBITAL_SALVAGE_CRATE,
            REMOTE_DEPOT_ANCHOR,
            SURVEY_BEACON_PYLON,
            ROUTE_STABILIZER_STATION,
            SURVEY_ARRAY_CONSOLE
    );

    private GalacticSurveyBlocks() {
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(BLOCKS, eventBus);
    }

    public static List<String> ids() {
        return ALL_BLOCKS.stream().map(entry -> entry.id().getPath()).toList();
    }

    private static EchoBackendRegistryEntry<Block> surveyDevice(String id, MapColor color, float strength, int light, String status) {
        return block(id, properties -> new SurveyDeviceBlock(properties, status), properties -> {
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

    public static final class SurveyDeviceBlock extends Block {
        private final String status;

        public SurveyDeviceBlock(Properties properties, String status) {
            super(properties);
            this.status = status;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!level.isClientSide()) {
                player.sendSystemMessage(Component.literal("GALACTIC SURVEY // " + status));
            }
            return InteractionResult.SUCCESS_SERVER;
        }
    }
}
