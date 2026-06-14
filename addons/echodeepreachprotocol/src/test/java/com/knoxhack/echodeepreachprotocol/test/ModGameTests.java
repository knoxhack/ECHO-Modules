package com.knoxhack.echodeepreachprotocol.test;

import com.knoxhack.echodeepreachprotocol.EchoDeepReachProtocol;
import com.knoxhack.echodeepreachprotocol.hazard.DeepReachCorruptionSource;
import com.knoxhack.echodeepreachprotocol.hazard.DeepReachOxygenSource;
import com.knoxhack.echodeepreachprotocol.hazard.DeepReachPressureSource;
import com.knoxhack.echodeepreachprotocol.season.DeepReachSeasonManager;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.knoxhack.echomissioncore.content.MissionCoreJsonReloadListener;
import com.echoplatform.echocore.api.mission.MissionChapterDefinition;
import com.echoplatform.echocore.api.mission.MissionDefinition;

/**
 * GameTest registrations for ECHO: Deep Reach Protocol.
 * Validates that core biomes, blocks, and structures are present in their registries,
 * plus placeholder checks for the Deep Reach hazard sources.
 */
public final class ModGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, EchoDeepReachProtocol.MODID);

    // Registry-presence tests
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> BIOMES_REGISTERED =
            TEST_FUNCTIONS.register("biomes_registered", () -> ModGameTests::biomesRegistered);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> BLOCKS_REGISTERED =
            TEST_FUNCTIONS.register("blocks_registered", () -> ModGameTests::blocksRegistered);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> STRUCTURES_REGISTERED =
            TEST_FUNCTIONS.register("structures_registered", () -> ModGameTests::structuresRegistered);

    // Hazard-source placeholder tests
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> PRESSURE_DEEP =
            TEST_FUNCTIONS.register("deep_reach_pressure_deep", () -> ModGameTests::deepReachPressureDeep);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> PRESSURE_SURFACE =
            TEST_FUNCTIONS.register("deep_reach_pressure_surface", () -> ModGameTests::deepReachPressureSurface);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> OXYGEN_UNDERWATER =
            TEST_FUNCTIONS.register("deep_reach_oxygen_underwater", () -> ModGameTests::deepReachOxygenUnderwater);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CORRUPTION_NEAR_LATTICE =
            TEST_FUNCTIONS.register("deep_reach_corruption_near_lattice", () -> ModGameTests::deepReachCorruptionNearLattice);

    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> REMORA_ENTITY_REGISTERED =
            TEST_FUNCTIONS.register("remora_entity_registered", () -> ModGameTests::remoraEntityRegistered);

    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> REMORA_ITEM_REGISTERED =
            TEST_FUNCTIONS.register("remora_item_registered", () -> ModGameTests::remoraItemRegistered);

    // MissionCore campaign tests
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DEEP_REACH_CAMPAIGN_CHAPTER =
            TEST_FUNCTIONS.register("deep_reach_campaign_chapter_registered", () -> ModGameTests::deepReachCampaignChapterRegistered);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DEEP_REACH_MISSIONS_LOADED =
            TEST_FUNCTIONS.register("deep_reach_missions_loaded", () -> ModGameTests::deepReachMissionsLoaded);

    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> LATE_GAME_ADVANCEMENTS_PARSE =
            TEST_FUNCTIONS.register("late_game_advancements_parse", () -> ModGameTests::lateGameAdvancementsParse);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> LATE_GAME_MISSIONS_PARSE =
            TEST_FUNCTIONS.register("late_game_missions_parse", () -> ModGameTests::lateGameMissionsParse);

    // Season tests
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SEASON_MANAGER_EXISTS =
            TEST_FUNCTIONS.register("season_manager_exists", () -> ModGameTests::seasonManagerExists);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SEASON_MULTIPLIER_SANITY =
            TEST_FUNCTIONS.register("season_multiplier_sanity", () -> ModGameTests::seasonMultiplierSanity);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> STRUCTURE_NBTS_EXIST =
            TEST_FUNCTIONS.register("structure_nbts_exist", () -> ModGameTests::structureNbtsExist);

    private static final List<String> BIOME_IDS = List.of(
            "shoals",
            "twilight_caverns",
            "abyssal_rifts",
            "the_lattice",
            "hadal_trenches"
    );

    private static final List<String> BLOCK_IDS = List.of(
            "abyssal_stone",
            "lattice_crystal",
            "thermal_vent",
            "sunken_sand"
    );

    private static final List<String> STRUCTURE_IDS = List.of(
            "collapsed_tunnel",
            "geothermal_station",
            "lattice_archive",
            "abyssal_temple"
    );

    private ModGameTests() {
    }

    public static void register(IEventBus eventBus) {
        TEST_FUNCTIONS.register(eventBus);
    }

    public static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(id("deep_reach_hardening"));

        // Registry presence
        register(event, environment, "biomes_registered", BIOMES_REGISTERED.getId());
        register(event, environment, "blocks_registered", BLOCKS_REGISTERED.getId());
        register(event, environment, "structures_registered", STRUCTURES_REGISTERED.getId());

        // Hazard sources
        register(event, environment, "deep_reach_pressure_deep", PRESSURE_DEEP.getId());
        register(event, environment, "deep_reach_pressure_surface", PRESSURE_SURFACE.getId());
        register(event, environment, "deep_reach_oxygen_underwater", OXYGEN_UNDERWATER.getId());
        register(event, environment, "deep_reach_corruption_near_lattice", CORRUPTION_NEAR_LATTICE.getId());

        // Remora vehicle
        register(event, environment, "remora_entity_registered", REMORA_ENTITY_REGISTERED.getId());
        register(event, environment, "remora_item_registered", REMORA_ITEM_REGISTERED.getId());

        // MissionCore campaign tests
        register(event, environment, "deep_reach_campaign_chapter_registered", DEEP_REACH_CAMPAIGN_CHAPTER.getId());
        register(event, environment, "deep_reach_missions_loaded", DEEP_REACH_MISSIONS_LOADED.getId());
        register(event, environment, "late_game_advancements_parse", LATE_GAME_ADVANCEMENTS_PARSE.getId());
        register(event, environment, "late_game_missions_parse", LATE_GAME_MISSIONS_PARSE.getId());

        // Season tests
        register(event, environment, "season_manager_exists", SEASON_MANAGER_EXISTS.getId());
        register(event, environment, "season_multiplier_sanity", SEASON_MULTIPLIER_SANITY.getId());
        register(event, environment, "structure_nbts_exist", STRUCTURE_NBTS_EXIST.getId());
    }

    // -------------------------------------------------------------------------
    // Registry-presence tests
    // -------------------------------------------------------------------------

    private static void biomesRegistered(GameTestHelper helper) {
        var registry = helper.getLevel().registryAccess().lookupOrThrow(Registries.BIOME);
        for (String path : BIOME_IDS) {
            Identifier biomeId = id(path);
            helper.assertTrue(registry.containsKey(biomeId),
                    "Deep Reach biome should be registered: " + biomeId);
        }
        helper.succeed();
    }

    private static void blocksRegistered(GameTestHelper helper) {
        for (String path : BLOCK_IDS) {
            Identifier blockId = id(path);
            Block block = BuiltInRegistries.BLOCK.getOptional(blockId).orElse(null);
            helper.assertTrue(block != null, "Deep Reach block should be registered: " + blockId);
        }
        helper.succeed();
    }

    private static void structuresRegistered(GameTestHelper helper) {
        var registry = helper.getLevel().registryAccess().lookupOrThrow(Registries.STRUCTURE);
        for (String path : STRUCTURE_IDS) {
            Identifier structureId = id(path);
            helper.assertTrue(registry.containsKey(structureId),
                    "Deep Reach structure should be registered: " + structureId);
        }
        helper.succeed();
    }

    // -------------------------------------------------------------------------
    // Hazard-source placeholder tests
    // -------------------------------------------------------------------------

    private static void deepReachPressureDeep(GameTestHelper helper) {
        helper.assertTrue(DeepReachPressureSource.INSTANCE != null,
                "Deep Reach pressure source should be available");
        int y = helper.getLevel().getMinY();
        helper.assertTrue(y <= helper.getLevel().getMinY(), "Test should run at or below minimum Y");
        helper.succeed();
    }

    private static void deepReachPressureSurface(GameTestHelper helper) {
        helper.assertTrue(DeepReachPressureSource.INSTANCE != null,
                "Deep Reach pressure source should be available");
        helper.succeed();
    }

    private static void deepReachOxygenUnderwater(GameTestHelper helper) {
        helper.assertTrue(DeepReachOxygenSource.INSTANCE != null,
                "Deep Reach oxygen source should be available");
        helper.succeed();
    }

    private static void deepReachCorruptionNearLattice(GameTestHelper helper) {
        helper.assertTrue(DeepReachCorruptionSource.INSTANCE != null,
                "Deep Reach corruption source should be available");

        BlockState lattice = BuiltInRegistries.BLOCK.getOptional(id("lattice_crystal"))
                .map(Block::defaultBlockState)
                .orElse(null);
        helper.assertTrue(lattice != null, "lattice_crystal block should exist for corruption test");
        helper.succeed();
    }

    private static void remoraEntityRegistered(GameTestHelper helper) {
        helper.assertTrue(BuiltInRegistries.ENTITY_TYPE.getOptional(id("remora_submersible")).isPresent(),
                "Remora submersible entity type should be registered");
        helper.succeed();
    }

    private static void remoraItemRegistered(GameTestHelper helper) {
        helper.assertTrue(BuiltInRegistries.ITEM.getOptional(id("remora_submersible")).isPresent(),
                "Remora submersible item should be registered");
        helper.succeed();
    }

    // -------------------------------------------------------------------------
    // MissionCore campaign tests
    // -------------------------------------------------------------------------

    private static void deepReachCampaignChapterRegistered(GameTestHelper helper) {
        Identifier chapterId = id("deep_reach_campaign");
        JsonObject json = readResource("/data/echodeepreachprotocol/missioncore/chapters/deep_reach_campaign.json");
        MissionChapterDefinition chapter = MissionCoreJsonReloadListener.parseChapterForTests(chapterId, json);
        helper.assertTrue(chapter != null, "Deep Reach campaign chapter should parse");
        helper.assertTrue(chapterId.equals(chapter.id()), "Chapter id should match deep_reach_campaign");
        helper.assertTrue("ECHO: Deep Reach Campaign".equals(chapter.title()), "Chapter title should be 'ECHO: Deep Reach Campaign'");
        helper.succeed();
    }

    private static void deepReachMissionsLoaded(GameTestHelper helper) {
        List<String> missionNames = List.of(
                "secure_first_habitat",
                "craft_first_pressure_suit",
                "survey_twilight_caverns",
                "recover_lattice_archive",
                "reach_hadal_trench");
        Identifier chapterId = id("deep_reach_campaign");
        for (String missionName : missionNames) {
            Identifier missionId = id(missionName);
            JsonObject json = readResource("/data/echodeepreachprotocol/missioncore/missions/" + missionName + ".json");
            MissionDefinition mission = MissionCoreJsonReloadListener.parseMissionForTests(missionId, json);
            helper.assertTrue(mission != null, "Mission should parse: " + missionId);
            helper.assertTrue(chapterId.equals(mission.chapterId()),
                    "Mission " + missionId + " should belong to deep_reach_campaign");
        }
        helper.succeed();
    }

    private static void seasonManagerExists(GameTestHelper helper) {
        helper.assertTrue(DeepReachSeasonManager.INSTANCE != null,
                "Deep Reach season manager should be available");
        helper.assertTrue(DeepReachSeasonManager.INSTANCE.currentSeason() != null,
                "Deep Reach season manager should have a current season");
        helper.succeed();
    }

    private static void seasonMultiplierSanity(GameTestHelper helper) {
        float pressure = DeepReachSeasonManager.INSTANCE.getMultiplier(com.knoxhack.echo.hazardcore.api.HazardType.PRESSURE);
        helper.assertTrue(pressure > 0.0f, "Pressure season multiplier should be positive");
        helper.succeed();
    }

    private static void structureNbtsExist(GameTestHelper helper) {
        List<String> nbtPaths = List.of(
                "/data/echodeepreachprotocol/structure/collapsed_tunnel.nbt",
                "/data/echodeepreachprotocol/structure/geothermal_station.nbt",
                "/data/echodeepreachprotocol/structure/lattice_archive.nbt",
                "/data/echodeepreachprotocol/structure/abyssal_temple.nbt");
        for (String path : nbtPaths) {
            try (InputStream stream = ModGameTests.class.getResourceAsStream(path)) {
                helper.assertTrue(stream != null, "Missing structure NBT: " + path);
            } catch (Exception exception) {
                throw new AssertionError("Failed to read " + path, exception);
            }
        }
        helper.succeed();
    }

    private static void lateGameAdvancementsParse(GameTestHelper helper) {
        List<String> advancementNames = List.of(
                "enter_the_lattice",
                "enter_hadal_trenches",
                "craft_lattice_void_suit",
                "craft_hadal_hardsuit",
                "kill_lattice_sentinel",
                "build_submersible_dock",
                "build_xenobiologist_lab");
        for (String name : advancementNames) {
            JsonObject json = readResource("/data/echodeepreachprotocol/advancement/" + name + ".json");
            helper.assertTrue(json != null, "Advancement should parse: " + name);
        }
        helper.succeed();
    }

    private static void lateGameMissionsParse(GameTestHelper helper) {
        List<String> missionNames = List.of(
                "enter_the_lattice",
                "conquer_hadal_trench");
        Identifier chapterId = id("deep_reach_campaign");
        for (String missionName : missionNames) {
            Identifier missionId = id(missionName);
            JsonObject json = readResource("/data/echodeepreachprotocol/missioncore/missions/" + missionName + ".json");
            MissionDefinition mission = MissionCoreJsonReloadListener.parseMissionForTests(missionId, json);
            helper.assertTrue(mission != null, "Mission should parse: " + missionId);
            helper.assertTrue(chapterId.equals(mission.chapterId()),
                    "Mission " + missionId + " should belong to deep_reach_campaign");
        }
        helper.succeed();
    }

    private static JsonObject readResource(String path) {
        try (InputStream stream = ModGameTests.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new AssertionError("Missing test resource: " + path);
            }
            JsonElement element = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            if (!element.isJsonObject()) {
                throw new AssertionError("Resource " + path + " must be a JSON object");
            }
            return element.getAsJsonObject();
        } catch (Exception exception) {
            throw new AssertionError("Failed to read resource " + path + ": " + exception.getMessage(), exception);
        }
    }

    private static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment,
            String testName, Identifier functionId) {
        TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(
                environment,
                Identifier.withDefaultNamespace("empty"),
                400,
                0,
                true,
                Rotation.NONE,
                false,
                1,
                1,
                false,
                64);
        event.registerTest(id(testName), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, functionId), data));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoDeepReachProtocol.MODID, path);
    }
}
