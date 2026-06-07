package com.knoxhack.echogalacticcore.nativeapi;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EchoGalacticCoreResourceMigrationTest {
    private static final Path RESOURCES = Path.of("src/main/resources");

    @Test
    void migratedAssetsUseEchoGalacticCoreNamespace() throws IOException {
        Path assets = RESOURCES.resolve("assets/echogalacticcore");

        assertTrue(Files.isDirectory(assets), "migrated echogalacticcore assets must exist");
        assertTrue(countFiles(assets) >= 2000, "legacy Galacticraft assets should be migrated into echogalacticcore");
        assertFalse(Files.exists(RESOURCES.resolve("assets/galacticraftcore")), "old galacticraftcore namespace should not remain");
        assertFalse(Files.exists(RESOURCES.resolve("assets/galacticraftplanets")), "old galacticraftplanets namespace should not remain");
        assertFalse(treeContainsLegacyNamespace(assets), "migrated text resources should not reference old asset namespaces");
    }

    @Test
    void legacyForgeAndMicCoreRuntimeSurfacesAreRemoved() {
        assertFalse(Files.exists(RESOURCES.resolve("mcmod.info")), "Forge mcmod.info should not remain");
        assertFalse(Files.exists(RESOURCES.resolve("pack.mcmeta")), "legacy pack.mcmeta should not remain");
        assertFalse(Files.exists(RESOURCES.resolve("META-INF/accesstransformer.cfg")), "access transformer should not remain");
        assertFalse(Files.exists(RESOURCES.resolve("META-INF/accesstransformer_deobf.cfg")), "deobf access transformer should not remain");
        assertFalse(Files.exists(Path.of("src/main/java/micdoodle8")), "legacy micdoodle8 Java runtime tree should not remain");
    }

    @Test
    void releaseIdentityUsesUnofficialForkLabelAndAttribution() throws IOException {
        Path readme = Path.of("README.md");
        Path license = Path.of("LICENSE");
        Path credits = Path.of("CREDITS.md");
        Path descriptor = RESOURCES.resolve("META-INF/echo.mod.json");
        Path lang = RESOURCES.resolve("assets/echogalacticcore/lang/en_us.json");
        String honestLabel = "Unofficial ECHO Platform port/fork of Galacticraft Legacy";

        assertTrue(fileContains(readme, honestLabel), "README must include the honest public label");
        assertTrue(fileContains(descriptor, honestLabel), "descriptor must include the honest public label");
        assertTrue(fileContains(lang, honestLabel + "."), "language metadata must include the honest public label");
        assertTrue(fileContains(license, "MIT License"), "license must be MIT");
        assertTrue(fileContains(license, "Original Galacticraft Legacy code copyright TeamGalacticraft."));
        assertTrue(fileContains(license, "Portions of this project are derived from Galacticraft Legacy."));
        assertTrue(fileContains(credits, "Galacticraft Legacy by TeamGalacticraft"));
        assertTrue(fileContains(credits, "KnoxHack / ECHO Labs"));
        for (Path publicCopy : List.of(readme, credits, descriptor, lang)) {
            assertFalse(fileContainsCaseInsensitive(publicCopy, "Official Galacticraft"));
            assertFalse(fileContainsCaseInsensitive(publicCopy, "Galacticraft 6"));
            assertFalse(fileContainsCaseInsensitive(publicCopy, "Galacticraft for ECHO"));
            assertFalse(fileContainsCaseInsensitive(publicCopy, "The new Galacticraft"));
            assertFalse(fileContainsCaseInsensitive(publicCopy, "Made by TeamGalacticraft"));
        }
    }

    @Test
    void migratedRecipesUseNativeTypesAndEchoOreDictTags() throws IOException {
        Path recipes = RESOURCES.resolve("assets/echogalacticcore/recipes");
        Path oreDictTags = RESOURCES.resolve("data/echogalacticcore/tags/items/legacy_oredict");
        Path oreDictManifest = RESOURCES.resolve("data/echogalacticcore/port/legacy_oredict_tags.json");
        Path recipeFactoryManifest = RESOURCES.resolve("data/echogalacticcore/port/recipe_factory_migration.json");

        assertTrue(Files.isDirectory(recipes), "migrated recipes must exist");
        assertFalse(treeContains(recipes, "forge:"), "migrated recipe JSON should not use Forge recipe or ore-dict types");
        assertFalse(treeContains(recipes, "micdoodle8."), "migrated runtime recipes should not carry legacy factory class references");
        assertFalse(Files.exists(recipes.resolve("_factories.json")), "legacy Forge recipe factory metadata should not remain in runtime recipe assets");
        assertTrue(countFiles(oreDictTags) >= 40, "legacy ore-dict recipes should have ECHO-owned tag replacements");
        assertTrue(Files.isRegularFile(oreDictManifest), "legacy ore-dict tag migration manifest must be present");
        assertTrue(fileContains(oreDictManifest, "galacticraft_legacy_ore_dictionary"));
        assertTrue(Files.isRegularFile(recipeFactoryManifest), "legacy recipe factory migration manifest must be present");
        assertTrue(fileContains(recipeFactoryManifest, "galacticraft_legacy_recipe_factories"));
        assertTrue(fileContains(recipeFactoryManifest, "EchoNativeConfigService"));
    }

    @Test
    void migratedBlockstatesUseNativeEchoModels() throws IOException {
        Path blockstates = RESOURCES.resolve("assets/echogalacticcore/blockstates");
        Path nativeFluidModels = RESOURCES.resolve("assets/echogalacticcore/models/block/native_fluid");
        Path nativeMultiLayerModels = RESOURCES.resolve("assets/echogalacticcore/models/block/native_multi_layer");
        Path blockstateManifest = RESOURCES.resolve("data/echogalacticcore/port/blockstate_migration.json");

        assertTrue(Files.isDirectory(blockstates), "migrated blockstates must exist");
        assertTrue(countFiles(blockstates) >= 100, "legacy Galacticraft blockstates should be migrated into echogalacticcore");
        assertFalse(treeContains(blockstates, "\"forge_marker\""), "migrated blockstate JSON should not use Forge markers");
        assertFalse(treeContains(blockstates, "forge:"), "migrated blockstate JSON should not use Forge helper model references");
        assertTrue(countFiles(nativeFluidModels) >= 4, "Forge fluid helper blockstates should have native ECHO model replacements");
        assertTrue(countFiles(nativeMultiLayerModels) >= 6, "Forge multi-layer blockstates should have native ECHO model replacements");
        assertTrue(Files.isRegularFile(blockstateManifest), "blockstate migration manifest must be present");
        assertTrue(fileContains(blockstateManifest, "galacticraft_legacy_forge_blockstate"));
    }

    @Test
    void migratedLootAndSoundsUseEchoNativeIdentifiers() throws IOException {
        Path lootTables = RESOURCES.resolve("assets/echogalacticcore/loot_tables");
        Path sounds = RESOURCES.resolve("assets/echogalacticcore/sounds.json");
        Path manifest = RESOURCES.resolve("data/echogalacticcore/port/loot_sound_migration.json");

        assertTrue(Files.isDirectory(lootTables), "migrated loot tables must exist");
        assertFalse(treeContains(lootTables, "\"type\": \"item\""), "loot entry types should be namespaced");
        assertFalse(treeContains(lootTables, "\"function\": \"set_"), "loot functions should be namespaced");
        assertFalse(treeContains(lootTables, "minecraft:set_data"), "legacy set_data should use the ECHO legacy-data migration function");
        assertFalse(treeContains(lootTables, "minecraft:record_"), "old record item ids should be modern music disc ids");
        assertTrue(Files.isRegularFile(sounds), "sounds.json must exist");
        assertFalse(fileContains(sounds, "galacticraft.music_space"), "runtime sound event keys should not use legacy Galacticraft branding");
        assertTrue(fileContains(sounds, "\"music.space\""));
        assertTrue(Files.isRegularFile(manifest), "loot/sound migration manifest must be present");
        assertTrue(fileContains(manifest, "galacticraft_legacy_loot_sound_data"));
        assertTrue(fileContains(manifest, "echogalacticcore:set_legacy_data"));
    }

    @Test
    void migratedLanguagesUseJsonOnlyEchoResources() throws IOException {
        Path lang = RESOURCES.resolve("assets/echogalacticcore/lang");
        Path manifest = RESOURCES.resolve("data/echogalacticcore/port/language_migration.json");

        assertTrue(Files.isDirectory(lang), "migrated language assets must exist");
        assertTrue(countFilesWithExtension(lang, ".json") >= 25, "legacy Galacticraft locales should be converted to JSON");
        assertFalse(containsFileWithExtension(lang, ".lang"), "legacy .lang files should not remain in runtime assets");
        assertTrue(Files.isRegularFile(lang.resolve("en_us.json")), "en_us.json must exist");
        assertTrue(fileContains(lang.resolve("en_us.json"), "Unofficial ECHO Platform port/fork of Galacticraft Legacy."));
        assertTrue(Files.isRegularFile(manifest), "language migration manifest must be present");
        assertTrue(fileContains(manifest, "galacticraft_legacy_lang_assets"));
    }

    @Test
    void migratedModelsUseNativeTextureRoots() throws IOException {
        Path textures = RESOURCES.resolve("assets/echogalacticcore/textures");
        Path assets = RESOURCES.resolve("assets/echogalacticcore");
        Path blockTextures = textures.resolve("block");
        Path itemTextures = textures.resolve("item");
        Path manifest = RESOURCES.resolve("data/echogalacticcore/port/model_texture_migration.json");

        assertTrue(Files.isDirectory(blockTextures), "block textures should use native singular root");
        assertTrue(Files.isDirectory(itemTextures), "item textures should use native singular root");
        assertFalse(Files.exists(textures.resolve("blocks")), "legacy plural block texture root should not remain");
        assertFalse(Files.exists(textures.resolve("items")), "legacy plural item texture root should not remain");
        assertTrue(countFilesWithExtension(blockTextures, ".png") >= 340, "legacy block textures should be migrated");
        assertTrue(countFilesWithExtension(itemTextures, ".png") >= 200, "legacy item textures should be migrated");
        assertFalse(treeContains(assets, "echogalacticcore:blocks/"), "ECHO model texture ids should use block/");
        assertFalse(treeContains(assets, "echogalacticcore:items/"), "ECHO model texture ids should use item/");
        assertFalse(treeContains(assets, "minecraft:blocks/"), "Minecraft texture ids should use block/");
        assertFalse(treeContains(assets, "minecraft:items/"), "Minecraft texture ids should use item/");
        assertTrue(countMigratedTextureReferences(assets) >= 500, "migrated model/blockstate references should remain present");
        assertTrue(Files.isRegularFile(manifest), "model/texture migration manifest must be present");
        assertTrue(fileContains(manifest, "galacticraft_legacy_model_texture_assets"));
    }

    @Test
    void gameplayParityManifestsCoverCoreLoops() throws IOException {
        Path foundation = RESOURCES.resolve("data/echogalacticcore/port/gameplay_foundation.json");
        Path machines = RESOURCES.resolve("data/echogalacticcore/port/gameplay_machines.json");
        Path lifeSupport = RESOURCES.resolve("data/echogalacticcore/port/life_support_contracts.json");
        Path rocketProgression = RESOURCES.resolve("data/echogalacticcore/port/rocket_progression.json");
        Path energyBridge = RESOURCES.resolve("data/echogalacticcore/port/energy_bridge.json");
        Path playerGear = RESOURCES.resolve("data/echogalacticcore/port/player_gear.json");
        Path celestialRoutes = RESOURCES.resolve("data/echogalacticcore/port/celestial_routes.json");
        Path celestialEnvironments = RESOURCES.resolve("data/echogalacticcore/port/celestial_environments.json");
        Path outerPlanetProgression = RESOURCES.resolve("data/echogalacticcore/port/outer_planet_progression.json");
        Path dungeonRewardProgression = RESOURCES.resolve("data/echogalacticcore/port/dungeon_reward_progression.json");
        Path dungeonBossParity = RESOURCES.resolve("data/echogalacticcore/port/dungeon_boss_parity.json");
        Path echoIntegrations = RESOURCES.resolve("data/echogalacticcore/port/echo_integrations.json");

        assertTrue(Files.isRegularFile(foundation), "foundation gameplay parity manifest must be present");
        assertTrue(Files.isRegularFile(machines), "machine gameplay parity manifest must be present");
        assertTrue(Files.isRegularFile(lifeSupport), "life support gameplay parity manifest must be present");
        assertTrue(Files.isRegularFile(rocketProgression), "rocket progression gameplay parity manifest must be present");
        assertTrue(Files.isRegularFile(energyBridge), "energy bridge gameplay parity manifest must be present");
        assertTrue(Files.isRegularFile(playerGear), "player gear gameplay parity manifest must be present");
        assertTrue(Files.isRegularFile(celestialRoutes), "celestial route gameplay parity manifest must be present");
        assertTrue(Files.isRegularFile(celestialEnvironments), "celestial environment gameplay parity manifest must be present");
        assertTrue(Files.isRegularFile(outerPlanetProgression), "outer planet progression manifest must be present");
        assertTrue(Files.isRegularFile(dungeonRewardProgression), "dungeon reward progression manifest must be present");
        assertTrue(Files.isRegularFile(dungeonBossParity), "dungeon boss parity manifest must be present");
        assertTrue(Files.isRegularFile(echoIntegrations), "ECHO integration manifest must be present");

        assertTrue(fileContains(foundation, "compressed_steel"));
        assertTrue(fileContains(foundation, "moon_rock"));
        assertTrue(fileContains(foundation, "rocket_workbench"));
        assertTrue(fileContains(foundation, "circuit_fabricator"));

        assertTrue(fileContains(machines, "oxygen_collector"));
        assertTrue(fileContains(machines, "oxygen_sealer"));
        assertTrue(fileContains(machines, "fuel_loader"));
        assertTrue(fileContains(machines, "rocket_workbench"));
        assertTrue(fileContains(machines, "\"typedSurfaces\""));

        assertTrue(fileContains(lifeSupport, "player_oxygen_state"));
        assertTrue(fileContains(lifeSupport, "oxygen_mask"));
        assertTrue(fileContains(lifeSupport, "breathability_checked"));

        assertTrue(fileContains(rocketProgression, "tier_1_rocket"));
        assertTrue(fileContains(rocketProgression, "route/moon"));
        assertTrue(fileContains(rocketProgression, "route/mars"));
        assertTrue(fileContains(rocketProgression, "echoholomap"));

        assertTrue(fileContains(energyBridge, "IEnergyHandlerGC"));
        assertTrue(fileContains(energyBridge, "echoadaptercore.energy_bridge"));
        assertTrue(fileContains(energyBridge, "echogalacticcore:galactic_energy"));

        assertTrue(fileContains(playerGear, "GCPlayerStats"));
        assertTrue(fileContains(playerGear, "inventoryHacks"));
        assertTrue(fileContains(playerGear, "echogalacticcore:hud/oxygen_tanks"));

        assertTrue(fileContains(celestialRoutes, "route/earth_orbit"));
        assertTrue(fileContains(celestialRoutes, "route/moon"));
        assertTrue(fileContains(celestialRoutes, "route/mars"));
        assertTrue(fileContains(celestialRoutes, "route/asteroids"));
        assertTrue(fileContains(celestialRoutes, "route/venus"));

        assertTrue(fileContains(celestialEnvironments, "WorldProviderMoon"));
        assertTrue(fileContains(celestialEnvironments, "WorldProviderMars"));
        assertTrue(fileContains(celestialEnvironments, "WorldProviderVenus"));
        assertTrue(fileContains(celestialEnvironments, "echoashfallprotocol"));

        assertTrue(fileContains(outerPlanetProgression, "route/mars"));
        assertTrue(fileContains(outerPlanetProgression, "route/asteroids"));
        assertTrue(fileContains(outerPlanetProgression, "route/venus"));
        assertTrue(fileContains(outerPlanetProgression, "tier_3_rocket"));
        assertTrue(fileContains(outerPlanetProgression, "echoashfallprotocol"));

        assertTrue(fileContains(dungeonRewardProgression, "evolved_skeleton_boss"));
        assertTrue(fileContains(dungeonRewardProgression, "evolved_creeper_boss"));
        assertTrue(fileContains(dungeonRewardProgression, "spider_queen"));
        assertTrue(fileContains(dungeonRewardProgression, "schematic/tier_2_rocket"));
        assertTrue(fileContains(dungeonRewardProgression, "schematic/tier_3_rocket"));
        assertTrue(fileContains(dungeonRewardProgression, "legacyInventoryMutation"));

        assertTrue(fileContains(dungeonBossParity, "moon_dungeon_tier_1"));
        assertTrue(fileContains(dungeonBossParity, "mars_dungeon_tier_2"));
        assertTrue(fileContains(dungeonBossParity, "venus_dungeon_tier_3"));

        assertTrue(fileContains(echoIntegrations, "echopackcore"));
        assertTrue(fileContains(echoIntegrations, "echoindex"));
        assertTrue(fileContains(echoIntegrations, "echolens"));
        assertTrue(fileContains(echoIntegrations, "echoholomap"));
        assertTrue(fileContains(echoIntegrations, "echoscreencore"));
        assertTrue(fileContains(echoIntegrations, "echoashfallprotocol"));
    }

    private static long countFiles(Path root) throws IOException {
        try (var stream = Files.walk(root)) {
            return stream.filter(Files::isRegularFile).count();
        }
    }

    private static long countFilesWithExtension(Path root, String extension) throws IOException {
        try (var stream = Files.walk(root)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(extension))
                    .count();
        }
    }

    private static boolean containsFileWithExtension(Path root, String extension) throws IOException {
        try (var stream = Files.walk(root)) {
            return stream
                    .filter(Files::isRegularFile)
                    .anyMatch(path -> path.toString().endsWith(extension));
        }
    }

    private static boolean treeContainsLegacyNamespace(Path root) throws IOException {
        List<String> textExtensions = List.of(".json", ".mcmeta", ".mtl", ".obj", ".txt");
        try (var stream = Files.walk(root)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> textExtensions.stream().anyMatch(path.toString()::endsWith))
                    .anyMatch(EchoGalacticCoreResourceMigrationTest::fileContainsLegacyNamespace);
        }
    }

    private static boolean fileContainsLegacyNamespace(Path path) {
        try {
            String text = Files.readString(path, StandardCharsets.UTF_8);
            return text.contains("galacticraftcore") || text.contains("galacticraftplanets");
        } catch (IOException e) {
            throw new IllegalStateException("Unable to inspect " + path, e);
        }
    }

    private static boolean treeContains(Path root, String needle) throws IOException {
        try (var stream = Files.walk(root)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(EchoGalacticCoreResourceMigrationTest::isTextResource)
                    .anyMatch(path -> fileContains(path, needle));
        }
    }

    private static long countMigratedTextureReferences(Path root) throws IOException {
        Pattern pattern = Pattern.compile("echogalacticcore:(block|item)/([A-Za-z0-9_./-]+)");
        try (var stream = Files.walk(root)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(EchoGalacticCoreResourceMigrationTest::isTextResource)
                    .mapToLong(path -> {
                        try {
                            return pattern.matcher(Files.readString(path, StandardCharsets.UTF_8)).results().count();
                        } catch (IOException e) {
                            throw new IllegalStateException("Unable to inspect " + path, e);
                        }
                    })
                    .sum();
        }
    }

    private static boolean isTextResource(Path path) {
        List<String> textExtensions = List.of(".json", ".mcmeta", ".mtl", ".obj", ".txt");
        return textExtensions.stream().anyMatch(path.toString()::endsWith);
    }

    private static boolean fileContains(Path path, String needle) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8).contains(needle);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to inspect " + path, e);
        }
    }

    private static boolean fileContainsCaseInsensitive(Path path, String needle) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8)
                    .toLowerCase(java.util.Locale.ROOT)
                    .contains(needle.toLowerCase(java.util.Locale.ROOT));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to inspect " + path, e);
        }
    }
}
