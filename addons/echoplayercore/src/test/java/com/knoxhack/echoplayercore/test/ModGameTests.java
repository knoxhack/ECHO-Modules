package com.knoxhack.echoplayercore.test;

import com.knoxhack.echoplayercore.EchoPlayerCore;
import com.knoxhack.echoplayercore.data.HomeLocation;
import com.knoxhack.echoplayercore.data.PlayerCoreSavedData;
import com.knoxhack.echoplayercore.data.PlayerTravelData;
import com.knoxhack.echoplayercore.data.TeleportLocation;
import com.knoxhack.echoplayercore.data.WarpLocation;
import com.knoxhack.echoplayercore.data.WarpSavedData;
import com.knoxhack.echoplayercore.service.CooldownService;
import com.knoxhack.echoplayercore.service.TpaService;
import com.knoxhack.echoplayercore.teleport.TeleportAction;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, EchoPlayerCore.MODID);

    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SAVED_DATA_CODEC =
            TEST_FUNCTIONS.register("saved_data_codec", () -> ModGameTests::savedDataCodec);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WARP_DATA_CODEC =
            TEST_FUNCTIONS.register("warp_data_codec", () -> ModGameTests::warpDataCodec);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> HOME_VALIDATION =
            TEST_FUNCTIONS.register("home_validation", () -> ModGameTests::homeValidation);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> COOLDOWN_SERVICE =
            TEST_FUNCTIONS.register("cooldown_service", () -> ModGameTests::cooldownService);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TPA_SERVICE =
            TEST_FUNCTIONS.register("tpa_service", () -> ModGameTests::tpaService);

    private ModGameTests() {
    }

    public static void register(IEventBus eventBus) {
        TEST_FUNCTIONS.register(eventBus);
    }

    public static void registerTests(RegisterGameTestsEvent event) {
        if (!shouldRegisterTests()) {
            return;
        }
        var environment = event.registerEnvironment(id("playercore"));
        register(event, environment, "saved_data_codec", SAVED_DATA_CODEC.getId());
        register(event, environment, "warp_data_codec", WARP_DATA_CODEC.getId());
        register(event, environment, "home_validation", HOME_VALIDATION.getId());
        register(event, environment, "cooldown_service", COOLDOWN_SERVICE.getId());
        register(event, environment, "tpa_service", TPA_SERVICE.getId());
    }

    private static void savedDataCodec(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        PlayerCoreSavedData data = PlayerCoreSavedData.get(level);
        helper.assertTrue(data != null, "SavedData should not be null");

        UUID playerId = UUID.randomUUID();
        PlayerTravelData travel = data.getOrCreate(playerId);
        helper.assertTrue(travel.homeCount() == 0, "New player should have no homes");

        HomeLocation home = new HomeLocation("test", Level.OVERWORLD, 1.0, 2.0, 3.0, 0.0F, 0.0F, 0L, 0L);
        travel.setHome(home);
        helper.assertTrue(travel.homeCount() == 1, "Player should have one home after set");
        helper.assertTrue(travel.home("test").isPresent(), "Home 'test' should be present");

        TeleportLocation back = new TeleportLocation(Level.OVERWORLD, 10.0, 20.0, 30.0, 0.0F, 0.0F, "back", 0L);
        travel.setLastBackLocation(back);
        helper.assertTrue(travel.lastBackLocation().isPresent(), "Back location should be present");

        travel.setCooldown("rtp", System.currentTimeMillis());
        helper.assertTrue(travel.getCooldown("rtp") > 0, "Cooldown should be stored");

        data.markDirty();
        helper.succeed();
    }

    private static void warpDataCodec(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        WarpSavedData data = WarpSavedData.get(level);
        helper.assertTrue(data != null, "WarpSavedData should not be null");
        helper.assertTrue(data.warpCount() == 0, "Fresh warp data should be empty");

        WarpLocation warp = new WarpLocation("spawn", "Spawn", Level.OVERWORLD, 0.0, 64.0, 0.0, 0.0F, 0.0F, true, "");
        helper.assertTrue(data.setWarp(warp), "Set warp should succeed");
        helper.assertTrue(data.warpCount() == 1, "Warp count should be 1");
        helper.assertTrue(data.getWarp("spawn").isPresent(), "Warp 'spawn' should exist");

        helper.assertTrue(data.deleteWarp("spawn"), "Delete warp should succeed");
        helper.assertTrue(data.warpCount() == 0, "Warp count should be 0 after delete");
        helper.succeed();
    }

    private static void homeValidation(GameTestHelper helper) {
        helper.assertTrue(HomeLocation.validName("home"), "'home' should be valid");
        helper.assertTrue(HomeLocation.validName("base_camp"), "'base_camp' should be valid");
        helper.assertTrue(!HomeLocation.validName(""), "Empty name should be invalid");
        helper.assertTrue(!HomeLocation.validName("   "), "Whitespace name should be invalid");
        helper.assertTrue(!HomeLocation.validName("a".repeat(65)), "65-char name should be invalid");
        helper.succeed();
    }

    private static void cooldownService(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        helper.assertTrue(!CooldownService.isOnCooldown(player, TeleportAction.HOME),
                "New player should not be on cooldown");
        helper.assertTrue(CooldownService.getCooldownRemaining(player, TeleportAction.HOME) == 0,
                "Remaining cooldown should be 0");
        CooldownService.applyCooldown(player, TeleportAction.RTP);
        helper.assertTrue(CooldownService.isOnCooldown(player, TeleportAction.RTP),
                "Player should be on RTP cooldown after apply");
        CooldownService.clearCooldown(player, TeleportAction.RTP);
        helper.assertTrue(!CooldownService.isOnCooldown(player, TeleportAction.RTP),
                "Cooldown should be cleared");
        helper.succeed();
    }

    private static void tpaService(GameTestHelper helper) {
        var requester = helper.makeMockServerPlayerInLevel();
        var target = helper.makeMockServerPlayerInLevel();
        helper.assertTrue(!TpaService.accept(target), "Accept with no request should fail");
        helper.assertTrue(!TpaService.deny(target), "Deny with no request should fail");

        helper.assertTrue(TpaService.request(requester, target, false), "TPA request should succeed");
        helper.assertTrue(TpaService.getPending(target.getUUID()).isPresent(), "Pending request should exist");

        helper.assertTrue(!TpaService.request(requester, target, false), "Duplicate TPA request should fail");
        helper.assertTrue(TpaService.deny(target), "Deny should succeed");
        helper.assertTrue(TpaService.getPending(target.getUUID()).isEmpty(), "Pending should be empty after deny");

        helper.assertTrue(TpaService.request(requester, target, true), "TPA-here request should succeed");
        helper.assertTrue(TpaService.accept(target), "Accept TPA-here should succeed");
        helper.assertTrue(TpaService.getPending(target.getUUID()).isEmpty(), "Pending should be empty after accept");

        helper.succeed();
    }

    private static void register(RegisterGameTestsEvent event,
            net.minecraft.core.Holder<TestEnvironmentDefinition<?>> environment,
            String testName, Identifier functionId) {
        TestData<net.minecraft.core.Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(
                environment,
                Identifier.withDefaultNamespace("empty"),
                100,
                0,
                true,
                net.minecraft.world.level.block.Rotation.NONE,
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
            if (normalized.equals(EchoPlayerCore.MODID) || normalized.equals("*") || normalized.equalsIgnoreCase("all")) {
                return true;
            }
        }
        return false;
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoPlayerCore.MODID, path);
    }
}
