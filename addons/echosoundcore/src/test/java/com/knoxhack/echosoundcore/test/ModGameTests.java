package com.knoxhack.echosoundcore.test;

import com.google.gson.JsonParser;
import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.EchoOptionalServices;
import com.knoxhack.echocore.api.EchoServiceRegistry;
import com.knoxhack.echosoundcore.EchoSoundCore;
import com.knoxhack.echosoundcore.SoundCoreAudioPriority;
import com.knoxhack.echosoundcore.SoundCoreChapter;
import com.knoxhack.echosoundcore.SoundCoreCombatIntensity;
import com.knoxhack.echosoundcore.api.SoundCoreAmbienceProfile;
import com.knoxhack.echosoundcore.api.SoundCoreMusicProfile;
import com.knoxhack.echosoundcore.network.SoundCoreAudioAction;
import com.knoxhack.echosoundcore.network.SoundCoreAudioPacket;
import com.knoxhack.echosoundcore.service.SoundCoreService;
import com.knoxhack.echosoundcore.util.SoundCoreAudioIds;
import com.knoxhack.echosoundcore.util.SoundCoreCatalogValidator;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, EchoSoundCore.MODID);

    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SOUND_SERVICE_CONTRACT =
            TEST_FUNCTIONS.register("sound_service_contract", () -> ModGameTests::soundServiceContract);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SOUND_CATALOG_ASSETS =
            TEST_FUNCTIONS.register("sound_catalog_assets", () -> ModGameTests::soundCatalogAssets);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> AUDIO_PACKET_CONTRACTS =
            TEST_FUNCTIONS.register("audio_packet_contracts", () -> ModGameTests::audioPacketContracts);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> PROFILE_CONTRACTS =
            TEST_FUNCTIONS.register("profile_contracts", () -> ModGameTests::profileContracts);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> AMBIENCE_PROFILE_CONTRACTS =
            TEST_FUNCTIONS.register("ambience_profile_contracts", () -> ModGameTests::ambienceProfileContracts);

    private ModGameTests() {
    }

    public static void register(IEventBus eventBus) {
        TEST_FUNCTIONS.register(eventBus);
    }

    public static void registerTests(RegisterGameTestsEvent event) {
        if (!shouldRegisterTests()) {
            return;
        }
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(id("soundcore_release"));
        register(event, environment, "sound_service_contract", SOUND_SERVICE_CONTRACT.getId());
        register(event, environment, "sound_catalog_assets", SOUND_CATALOG_ASSETS.getId());
        register(event, environment, "audio_packet_contracts", AUDIO_PACKET_CONTRACTS.getId());
        register(event, environment, "profile_contracts", PROFILE_CONTRACTS.getId());
        register(event, environment, "ambience_profile_contracts", AMBIENCE_PROFILE_CONTRACTS.getId());
    }

    private static void soundServiceContract(GameTestHelper helper) {
        EchoServiceRegistry.withClearedForTests(() -> {
            helper.assertTrue(EchoOptionalServices.soundCore().isEmpty(),
                    "SoundCore optional service should be absent before registration");
            EchoCoreServices.registerSoundService(SoundCoreService.INSTANCE);
            helper.assertTrue(EchoOptionalServices.soundCore().isPresent(),
                    "SoundCore optional service should be present after registration");
            helper.assertTrue(EchoCoreServices.soundService().available(),
                    "SoundCore service should report available");
            helper.assertTrue(EchoCoreServices.soundService().diagnostics().available(),
                    "SoundCore diagnostics should report available");
        });
        helper.succeed();
    }

    private static void soundCatalogAssets(GameTestHelper helper) {
        SoundCoreCatalogValidator.clearCache();
        helper.assertTrue(SoundCoreCatalogValidator.missingAssetPaths().isEmpty(),
                "Every sounds.json asset should resolve to an ogg file");
        helper.assertTrue(SoundCoreCatalogValidator.issues().isEmpty(),
                "SoundCore sound events, subtitles, and assets should be catalog-complete: "
                        + SoundCoreCatalogValidator.issues());
        helper.succeed();
    }

    private static void audioPacketContracts(GameTestHelper helper) {
        Identifier event = id("ui.terminal.open");
        SoundCoreAudioPacket oneShot = SoundCoreAudioPacket.playOneShot(event, -1.0f, 0.0f);
        helper.assertTrue(oneShot.action() == SoundCoreAudioAction.PLAY_ONESHOT,
                "One-shot packets should keep their action");
        helper.assertTrue(oneShot.eventId().equals(event), "One-shot packets should carry the event id");
        helper.assertTrue(oneShot.volume() == 0.0f, "Packet volume should clamp to zero");
        helper.assertTrue(oneShot.pitch() == 1.0f, "Packet pitch should fall back to one");

        SoundCoreAudioPacket patch = SoundCoreAudioPacket.patchContext(Map.of(
                "terminalOpen", "true",
                "hazardLevel", "3"));
        helper.assertTrue(patch.action() == SoundCoreAudioAction.PATCH_CONTEXT,
                "Context patches should keep their action");
        helper.assertTrue("true".equals(patch.context().get("terminalOpen")),
                "Context patches should carry values");
        helper.assertTrue(SoundCoreAudioPacket.stopControlledAudio().action() == SoundCoreAudioAction.STOP_CONTROLLED_AUDIO,
                "Stop controlled audio should keep its action");
        SoundCoreAudioPacket musicOneShot = SoundCoreAudioPacket.playOneShot(id("music.biome.wasteland"), 1.0f, 1.0f);
        helper.assertTrue(musicOneShot.action() == SoundCoreAudioAction.PLAY_ONESHOT
                        && SoundCoreAudioIds.isSoundCoreMusic(musicOneShot.eventId()),
                "Music one-shots should remain identifiable for controlled client routing");
        helper.assertTrue(SoundCoreAudioIds.matchesControlledMusicStop(
                        id("wasteland_exploration"),
                        id("wasteland_exploration"),
                        id("music.biome.wasteland")),
                "Controlled music stops should match the profile id");
        helper.assertTrue(SoundCoreAudioIds.matchesControlledMusicStop(
                        id("music.biome.wasteland"),
                        id("wasteland_exploration"),
                        id("music.biome.wasteland")),
                "Controlled music stops should match the actual sound id");
        helper.assertTrue(SoundCoreAudioIds.matchesControlledMusicStop(
                        id("music.combat.light"),
                        id("wasteland_exploration"),
                        id("music.biome.wasteland")),
                "Any SoundCore music event should stop controlled music as duplicate recovery");
        helper.assertFalse(SoundCoreAudioIds.matchesControlledMusicStop(
                        id("ui.terminal.open"),
                        id("wasteland_exploration"),
                        id("music.biome.wasteland")),
                "Non-music one-shots should not stop controlled music unless they match the current ids");
        helper.succeed();
    }

    private static void profileContracts(GameTestHelper helper) {
        SoundCoreMusicProfile music = new SoundCoreMusicProfile(
                id("test_music"),
                id("music.gameplay.exploration"),
                null,
                "test",
                SoundCoreChapter.UNKNOWN,
                null,
                null,
                null,
                null,
                SoundCoreCombatIntensity.NONE,
                null,
                -20,
                -10,
                -1.0f,
                -2.0f,
                -3.0f,
                true);
        helper.assertTrue(music.priority() == SoundCoreAudioPriority.IDLE,
                "Music profile should default null priority");
        helper.assertTrue(music.minDelay() == 0 && music.maxDelay() == 0,
                "Music profile delays should clamp safely");
        helper.assertTrue(music.fadeIn() == 0.0f && music.fadeOut() == 0.0f,
                "Music profile fades should clamp safely");

        SoundCoreAmbienceProfile ambience = new SoundCoreAmbienceProfile(
                id("test_ambience"),
                id("nexus.ambience.low_hum"),
                null,
                null,
                null,
                "",
                null,
                null,
                true,
                -1.0f,
                -1.0f,
                -1.0f,
                -1.0f);
        helper.assertTrue("default".equals(ambience.layer()), "Ambience profile should default its layer");
        helper.assertTrue(ambience.volume() == 1.0f && ambience.pitch() == 1.0f,
                "Ambience profile volume and pitch should clamp safely");
        assertThrows(helper, () -> new SoundCoreMusicProfile(
                null,
                id("music.gameplay.exploration"),
                null,
                "test",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                0,
                0.0f,
                0.0f,
                1.0f,
                true), "Music profile should reject a missing id");
        helper.succeed();
    }

    private static void ambienceProfileContracts(GameTestHelper helper) {
        for (String path : List.of(
                "data/echosoundcore/audio_profiles/ambience/blackbox_memory.json",
                "data/echosoundcore/audio_profiles/ambience/radiation_zone.json",
                "data/echosoundcore/audio_profiles/ambience/stationfall_horror.json",
                "data/echosoundcore/audio_profiles/ambience/toxic_swamp.json")) {
            try (var stream = ModGameTests.class.getClassLoader().getResourceAsStream(path)) {
                helper.assertTrue(stream != null, "Bundled ambience profile should exist: " + path);
                var json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
                Identifier sound = Identifier.parse(json.get("sound").getAsString());
                helper.assertFalse(SoundCoreAudioIds.isSoundCoreMusic(sound),
                        "Bundled ambience profiles must not reference music events: " + path);
            } catch (Exception exception) {
                helper.fail("Could not validate ambience profile " + path + ": " + exception.getMessage());
            }
        }
        helper.succeed();
    }

    private static void assertThrows(GameTestHelper helper, Runnable action, String message) {
        try {
            action.run();
            helper.fail(message);
        } catch (IllegalArgumentException expected) {
            // Expected by contract.
        }
    }

    private static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment,
            String testName, Identifier functionId) {
        TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(
                environment,
                Identifier.withDefaultNamespace("empty"),
                100,
                0,
                true,
                Rotation.NONE,
                false,
                1,
                1,
                false,
                2);
        event.registerTest(id(testName), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, functionId), data));
    }

    private static boolean shouldRegisterTests() {
        String namespaces = System.getProperty("neoforge.enabledGameTestNamespaces", "");
        if (namespaces == null || namespaces.isBlank()) {
            return true;
        }
        for (String namespace : namespaces.split(",")) {
            String normalized = namespace.trim();
            if (normalized.equals(EchoSoundCore.MODID) || normalized.equals("*") || normalized.equalsIgnoreCase("all")) {
                return true;
            }
        }
        return false;
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoSoundCore.MODID, path);
    }
}
