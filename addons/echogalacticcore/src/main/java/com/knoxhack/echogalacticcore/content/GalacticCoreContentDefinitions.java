package com.knoxhack.echogalacticcore.content;

import com.knoxhack.echogalacticcore.GalacticCoreIds;

import java.util.List;
import java.util.Map;

public final class GalacticCoreContentDefinitions {
    public record Registration(String path, String kind, String legacySource, Map<String, Object> evidence) {
        public Registration {
            if (path == null || path.isBlank()) {
                throw new IllegalArgumentException("path must not be blank");
            }
            if (kind == null || kind.isBlank()) {
                throw new IllegalArgumentException("kind must not be blank");
            }
            legacySource = legacySource == null ? "" : legacySource;
            evidence = Map.copyOf(evidence == null ? Map.of() : evidence);
        }

        public String id() {
            return GalacticCoreIds.id(path);
        }
    }

    public static final List<Registration> BLOCKS = List.of(
            block("moon_turf", "micdoodle8.mods.galacticraft.core.blocks.BlockBasicMoon", "moon", "terrain"),
            block("moon_rock", "micdoodle8.mods.galacticraft.core.blocks.BlockBasicMoon", "moon", "terrain"),
            block("moon_dungeon_brick", "micdoodle8.mods.galacticraft.core.blocks.BlockDungeonBrick", "moon", "dungeon"),
            block("launch_pad", "micdoodle8.mods.galacticraft.core.blocks.BlockLandingPad", "rocket", "launch"),
            block("rocket_workbench", "micdoodle8.mods.galacticraft.core.blocks.BlockMachine", "machine", "schematic"),
            block("oxygen_collector", "micdoodle8.mods.galacticraft.core.blocks.BlockMachine", "oxygen", "machine"),
            block("oxygen_sealer", "micdoodle8.mods.galacticraft.core.blocks.BlockMachine", "oxygen", "machine"),
            block("fuel_loader", "micdoodle8.mods.galacticraft.core.blocks.BlockMachine", "rocket", "machine"),
            block("air_lock_frame", "micdoodle8.mods.galacticraft.core.blocks.BlockAirLockFrame", "oxygen", "structure")
    );

    public static final List<Registration> ITEMS = List.of(
            item("compressed_steel", "micdoodle8.mods.galacticraft.core.items.ItemBasic", "material"),
            item("compressed_aluminum", "micdoodle8.mods.galacticraft.core.items.ItemBasic", "material"),
            item("compressed_tin", "micdoodle8.mods.galacticraft.core.items.ItemBasic", "material"),
            item("basic_wafer", "micdoodle8.mods.galacticraft.core.items.ItemBasic", "circuit"),
            item("advanced_wafer", "micdoodle8.mods.galacticraft.core.items.ItemBasic", "circuit"),
            item("oxygen_mask", "micdoodle8.mods.galacticraft.core.items.ItemOxygenMask", "life_support"),
            item("oxygen_gear", "micdoodle8.mods.galacticraft.core.items.ItemOxygenGear", "life_support"),
            item("light_oxygen_tank", "micdoodle8.mods.galacticraft.core.items.ItemOxygenTank", "life_support"),
            item("parachute", "micdoodle8.mods.galacticraft.core.items.ItemParaChute", "flight"),
            item("tier_1_rocket", "micdoodle8.mods.galacticraft.core.items.ItemTier1Rocket", "vehicle")
    );

    public static final List<Registration> FLUIDS = List.of(
            fluid("fuel", "micdoodle8.mods.galacticraft.core.fluids.GCFluids", "rocket"),
            fluid("oxygen", "micdoodle8.mods.galacticraft.core.fluids.GCFluids", "life_support")
    );

    public static final List<Registration> BLOCK_ENTITIES = List.of(
            blockEntity("oxygen_collector", "micdoodle8.mods.galacticraft.core.tile.TileEntityOxygenCollector", "oxygen"),
            blockEntity("oxygen_sealer", "micdoodle8.mods.galacticraft.core.tile.TileEntityOxygenSealer", "oxygen"),
            blockEntity("fuel_loader", "micdoodle8.mods.galacticraft.core.tile.TileEntityFuelLoader", "rocket"),
            blockEntity("rocket_workbench", "micdoodle8.mods.galacticraft.core.tile.TileEntityNasaWorkbench", "schematic")
    );

    public static final List<Registration> RECIPES = List.of(
            recipe("compressor/compressed_steel", "assets/galacticraftcore/recipes/compressed_steel.json", "compressor"),
            recipe("circuit_fabricator/basic_wafer", "micdoodle8.mods.galacticraft.core.recipe.CircuitFabricatorRecipes", "circuit_fabricator"),
            recipe("rocket_workbench/tier_1_rocket", "micdoodle8.mods.galacticraft.core.recipe.NasaWorkbenchRecipe", "rocket_workbench")
    );

    public static final List<Registration> ENTITIES = List.of(
            entity("tier_1_rocket", "micdoodle8.mods.galacticraft.core.entities.EntityTier1Rocket", "rocket"),
            entity("lander", "micdoodle8.mods.galacticraft.core.entities.EntityLander", "rocket"),
            entity("evolved_zombie", "micdoodle8.mods.galacticraft.core.entities.EntityEvolvedZombie", "mob")
    );

    public static final List<Registration> DIMENSIONS = List.of(
            dimension("earth_orbit", "micdoodle8.mods.galacticraft.core.dimension.WorldProviderOverworldOrbit", "orbit"),
            dimension("moon", "micdoodle8.mods.galacticraft.core.dimension.WorldProviderMoon", "moon"),
            dimension("mars", "micdoodle8.mods.galacticraft.planets.mars.dimension.WorldProviderMars", "mars"),
            dimension("asteroids", "micdoodle8.mods.galacticraft.planets.asteroids.dimension.WorldProviderAsteroids", "asteroids"),
            dimension("venus", "micdoodle8.mods.galacticraft.planets.venus.dimension.WorldProviderVenus", "venus")
    );

    public static final List<Registration> SCREENS = List.of(
            screen("launch_checklist", "micdoodle8.mods.galacticraft.core.client.gui.screen.GuiPreLaunchChecklist", "rocket"),
            screen("celestial_selection", "micdoodle8.mods.galacticraft.core.client.gui.screen.GuiCelestialSelection", "holomap"),
            screen("oxygen_collector", "micdoodle8.mods.galacticraft.core.client.gui.container.GuiOxygenCollector", "machine"),
            screen("rocket_inventory", "micdoodle8.mods.galacticraft.core.client.gui.container.GuiRocketInventory", "rocket"),
            screen("treasure_chest", "micdoodle8.mods.galacticraft.core.client.gui.container.GuiTreasureChest", "dungeon")
    );

    public static final List<Registration> PACKETS = List.of(
            packet("launch_state_sync", "micdoodle8.mods.galacticraft.core.network.PacketSimple", "rocket"),
            packet("oxygen_state_sync", "micdoodle8.mods.galacticraft.core.network.PacketSimple", "life_support"),
            packet("celestial_route_action", "micdoodle8.mods.galacticraft.core.network.PacketSimple", "holomap")
    );

    public static final List<Registration> CAPABILITIES = List.of(
            capability("life_support", "micdoodle8.mods.galacticraft.api.entity.IEntityBreathable", "oxygen"),
            capability("thermal_state", "micdoodle8.mods.galacticraft.api.item.IItemThermal", "thermal"),
            capability("galactic_energy", "micdoodle8.mods.galacticraft.api.power.IEnergyHandlerGC", "energy"),
            capability("rocket_flight", "micdoodle8.mods.galacticraft.api.entity.IRocketType", "vehicle")
    );

    public static final List<Registration> ATTACHMENTS = List.of(
            attachment("player_oxygen_state", "micdoodle8.mods.galacticraft.core.entities.player.GCPlayerStats", "player"),
            attachment("player_thermal_state", "micdoodle8.mods.galacticraft.core.entities.player.GCPlayerStats", "player"),
            attachment("rocket_launch_state", "micdoodle8.mods.galacticraft.api.prefab.entity.EntitySpaceshipBase", "rocket"),
            attachment("space_station_state", "micdoodle8.mods.galacticraft.core.dimension.SpaceStationWorldData", "world")
    );

    public static final List<Registration> CELESTIAL_ROUTES = List.of(
            route("earth_orbit", "micdoodle8.mods.galacticraft.core.dimension.WorldProviderOverworldOrbit", "tier_1_rocket"),
            route("moon", "micdoodle8.mods.galacticraft.core.dimension.WorldProviderMoon", "tier_1_rocket"),
            route("mars", "micdoodle8.mods.galacticraft.planets.mars.dimension.WorldProviderMars", "tier_2_rocket"),
            route("asteroids", "micdoodle8.mods.galacticraft.planets.asteroids.dimension.WorldProviderAsteroids", "tier_3_rocket"),
            route("venus", "micdoodle8.mods.galacticraft.planets.venus.dimension.WorldProviderVenus", "tier_3_rocket")
    );

    public static final List<Registration> DUNGEONS = List.of(
            dungeon("moon_dungeon_tier_1", "micdoodle8.mods.galacticraft.core.world.gen.dungeon.DungeonConfigurationMoon", "moon", "tier_1"),
            dungeon("mars_dungeon_tier_2", "micdoodle8.mods.galacticraft.planets.mars.world.gen.dungeon.DungeonConfigurationMars", "mars", "tier_2"),
            dungeon("venus_dungeon_tier_3", "micdoodle8.mods.galacticraft.planets.venus.world.gen.dungeon.DungeonConfigurationVenus", "venus", "tier_3")
    );

    public static final List<Registration> BOSSES = List.of(
            boss("evolved_skeleton_boss", "micdoodle8.mods.galacticraft.core.entities.EntitySkeletonBoss", "moon", "tier_1_key"),
            boss("evolved_creeper_boss", "micdoodle8.mods.galacticraft.planets.mars.entities.EntityCreeperBoss", "mars", "tier_2_key"),
            boss("spider_queen", "micdoodle8.mods.galacticraft.planets.venus.entities.EntitySpiderQueen", "venus", "tier_3_key")
    );

    public static final List<Registration> DUNGEON_LOOT = List.of(
            loot("moon_dungeon_tier_1", "assets/galacticraftcore/loot_tables/chests/moon_dungeon.json", "moon", "tier_1_key"),
            loot("mars_dungeon_tier_2", "assets/galacticraftplanets/loot_tables/chests/mars_dungeon.json", "mars", "tier_2_key"),
            loot("venus_dungeon_tier_3", "assets/galacticraftplanets/loot_tables/chests/venus_dungeon.json", "venus", "tier_3_key")
    );

    private GalacticCoreContentDefinitions() {
    }

    private static Registration block(String path, String legacySource, String system, String category) {
        return new Registration(path, "block", legacySource, Map.of("system", system, "category", category));
    }

    private static Registration item(String path, String legacySource, String system) {
        return new Registration(path, "item", legacySource, Map.of("system", system));
    }

    private static Registration fluid(String path, String legacySource, String system) {
        return new Registration(path, "fluid", legacySource, Map.of("system", system));
    }

    private static Registration blockEntity(String path, String legacySource, String system) {
        return new Registration(path, "block_entity", legacySource, Map.of("system", system));
    }

    private static Registration recipe(String path, String legacySource, String station) {
        return new Registration(path, "recipe", legacySource, Map.of("station", station));
    }

    private static Registration entity(String path, String legacySource, String system) {
        return new Registration("entity/" + path, "entity", legacySource, Map.of("system", system));
    }

    private static Registration dimension(String path, String legacySource, String body) {
        return new Registration(path, "dimension", legacySource, Map.of("celestialBody", body));
    }

    private static Registration screen(String path, String legacySource, String system) {
        return new Registration(path, "screen", legacySource, Map.of("system", system));
    }

    private static Registration packet(String path, String legacySource, String system) {
        return new Registration(path, "packet", legacySource, Map.of("system", system));
    }

    private static Registration capability(String path, String legacySource, String system) {
        return new Registration(path, "capability", legacySource, Map.of("system", system));
    }

    private static Registration attachment(String path, String legacySource, String owner) {
        return new Registration(path, "attachment", legacySource, Map.of("owner", owner));
    }

    private static Registration route(String path, String legacySource, String rocketTier) {
        return new Registration("route/" + path, "celestial_route", legacySource, Map.of("requiredVehicle", rocketTier));
    }

    private static Registration dungeon(String path, String legacySource, String body, String treasureTier) {
        return new Registration(
                "dungeon/" + path,
                "dungeon",
                legacySource,
                Map.of("celestialBody", body, "treasureTier", treasureTier, "system", "dungeons")
        );
    }

    private static Registration boss(String path, String legacySource, String body, String unlockKey) {
        return new Registration(
                "boss/" + path,
                "boss",
                legacySource,
                Map.of("celestialBody", body, "unlockKey", unlockKey, "system", "dungeons")
        );
    }

    private static Registration loot(String path, String legacySource, String body, String requiredKey) {
        return new Registration(
                "loot/" + path,
                "loot_table",
                legacySource,
                Map.of("celestialBody", body, "requiredKey", requiredKey, "system", "dungeons")
        );
    }
}
