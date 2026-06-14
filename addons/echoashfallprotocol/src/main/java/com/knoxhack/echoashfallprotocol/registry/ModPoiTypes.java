package com.knoxhack.echoashfallprotocol.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Registers Point of Interest types for faction villager professions.
 * Each POI type links a profession block to a villager profession.
 */
public class ModPoiTypes {

    public static final Object POI_TYPES =
            EchoBackendRegistryBridge.create(Registries.POINT_OF_INTEREST_TYPE, EchoAshfallProtocol.MODID);

    // === RADWARDEN POIs ===
    public static final EchoBackendRegistryEntry<PoiType> WEAPON_RACK_POI = registerPoi(
            "weapon_rack_poi", ModBlocks.WEAPON_RACK, 1, 1
    );
    public static final EchoBackendRegistryEntry<PoiType> SUPPLY_CRATE_POI = registerPoi(
            "supply_crate_poi", ModBlocks.SUPPLY_CRATE, 1, 1
    );

    // === CRASHBREAK POIs ===
    public static final EchoBackendRegistryEntry<PoiType> TRADE_COUNTER_POI = registerPoi(
            "trade_counter_poi", ModBlocks.TRADE_COUNTER, 1, 1
    );
    public static final EchoBackendRegistryEntry<PoiType> SURVEY_TABLE_POI = registerPoi(
            "survey_table_poi", ModBlocks.SURVEY_TABLE, 1, 1
    );

    // === SPOREBOUND POIs ===
    public static final EchoBackendRegistryEntry<PoiType> BIO_PROCESSING_STATION_POI = registerPoi(
            "bio_processing_station_poi", ModBlocks.BIO_PROCESSING_STATION, 1, 1
    );
    public static final EchoBackendRegistryEntry<PoiType> SPORE_GARDEN_POI = registerPoi(
            "spore_garden_poi", ModBlocks.SPORE_GARDEN, 1, 1
    );

    /**
     * Register a POI type for a given block.
     *
     * @param name       registry name
     * @param block      the block supplier that acts as the job site
     * @param maxTickets max villagers that can use this POI simultaneously
     * @param validRange how close a villager must be to claim it
     */
    private static EchoBackendRegistryEntry<PoiType> registerPoi(String name, Supplier<? extends Block> block,
            int maxTickets, int validRange) {
        return EchoBackendRegistryBridge.registerWithId(POI_TYPES, name, id -> {
            Set<BlockState> matchingStates = new HashSet<>(block.get().getStateDefinition().getPossibleStates());
            return new PoiType(
                    matchingStates,
                    maxTickets,
                    validRange
            );
        });
    }

    /**
     * Helper to get all registered POI keys for validation.
     */
    public static Set<ResourceKey<PoiType>> getAllPoiKeys() {
        return EchoBackendRegistryBridge.entries(POI_TYPES).stream()
                .map(entry -> (ResourceKey<PoiType>) entry.key())
                .collect(Collectors.toSet());
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(POI_TYPES, eventBus);
    }
}
