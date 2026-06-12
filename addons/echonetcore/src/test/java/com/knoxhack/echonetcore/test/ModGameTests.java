package com.knoxhack.echonetcore.test;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.EchoServiceRegistry;
import com.echoplatform.echocore.api.network.EchoPacketDirection;
import com.echoplatform.echocore.api.network.EchoPacketKind;
import com.echoplatform.echocore.api.network.EchoDiscoveryToast;
import com.echoplatform.echocore.api.network.NoOpNetworkService;
import com.echoplatform.echocore.api.network.PacketDebugHook;
import com.knoxhack.echonetcore.EchoNetCore;
import com.knoxhack.echonetcore.api.EchoClientSyncRegistry;
import com.knoxhack.echonetcore.api.EchoRateLimitPolicy;
import com.knoxhack.echonetcore.api.EchoRateLimiter;
import com.knoxhack.echonetcore.config.EchoNetCoreConfig;
import com.knoxhack.echonetcore.network.DiscoveryToastPacket;
import com.knoxhack.echonetcore.network.EchoDebugCommandPacket;
import com.knoxhack.echonetcore.network.EchoFactionSyncPacket;
import com.knoxhack.echonetcore.network.EchoNetDebug;
import com.knoxhack.echonetcore.network.EchoSyncPayload;
import com.knoxhack.echonetcore.network.EchoSyncType;
import com.knoxhack.echonetcore.service.NetCoreNetworkService;
import io.netty.buffer.Unpooled;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, EchoNetCore.MODID);

    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SERVICE_FALLBACKS =
            TEST_FUNCTIONS.register("service_fallbacks", () -> ModGameTests::serviceFallbacks);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> PACKET_IDS =
            TEST_FUNCTIONS.register("packet_ids", () -> ModGameTests::packetIds);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> RATE_LIMITER =
            TEST_FUNCTIONS.register("rate_limiter", () -> ModGameTests::rateLimiter);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DEBUG_HOOKS =
            TEST_FUNCTIONS.register("debug_hooks", () -> ModGameTests::debugHooks);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CLIENT_SYNC_REGISTRY =
            TEST_FUNCTIONS.register("client_sync_registry", () -> ModGameTests::clientSyncRegistry);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CODEC_ROUND_TRIPS =
            TEST_FUNCTIONS.register("codec_round_trips", () -> ModGameTests::codecRoundTrips);

    private ModGameTests() {
    }

    public static void register(IEventBus eventBus) {
        TEST_FUNCTIONS.register(eventBus);
    }

    public static void registerTests(RegisterGameTestsEvent event) {
        if (!shouldRegisterTests()) {
            return;
        }
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(id("netcore"));
        register(event, environment, "service_fallbacks", SERVICE_FALLBACKS.getId());
        register(event, environment, "packet_ids", PACKET_IDS.getId());
        register(event, environment, "rate_limiter", RATE_LIMITER.getId());
        register(event, environment, "debug_hooks", DEBUG_HOOKS.getId());
        register(event, environment, "client_sync_registry", CLIENT_SYNC_REGISTRY.getId());
        register(event, environment, "codec_round_trips", CODEC_ROUND_TRIPS.getId());
    }

    private static void serviceFallbacks(GameTestHelper helper) {
        EchoServiceRegistry.withClearedForTests(() -> {
            helper.assertTrue(EchoCoreServices.networkService() == NoOpNetworkService.INSTANCE,
                    "Core should expose NoOpNetworkService without NetCore registration.");
            EchoCoreServices.registerNetworkService(NetCoreNetworkService.INSTANCE);
            helper.assertTrue(EchoCoreServices.networkService() == NetCoreNetworkService.INSTANCE,
                    "NetCore service should register through Echo Core.");
        });
        helper.succeed();
    }

    private static void packetIds(GameTestHelper helper) {
        helper.assertTrue(EchoFactionSyncPacket.ID.equals(Identifier.fromNamespaceAndPath("echocore", "faction_sync")),
                "Faction sync packet id must preserve the old Core wire id.");
        helper.assertTrue(DiscoveryToastPacket.ID.equals(Identifier.fromNamespaceAndPath("echocore", "discovery_toast")),
                "Discovery toast packet id must preserve the old Core wire id.");
        helper.succeed();
    }

    private static void rateLimiter(GameTestHelper helper) {
        EchoRateLimiter.clearForTests();
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Identifier packetId = Identifier.fromNamespaceAndPath(EchoNetCore.MODID, "test_action");
        EchoRateLimitPolicy policy = EchoRateLimitPolicy.of(10, "test");
        helper.assertTrue(EchoRateLimiter.tryAcquire(player, packetId, policy),
                "First packet should pass the rate limiter.");
        helper.assertFalse(EchoRateLimiter.tryAcquire(player, packetId, policy),
                "Second same-tick packet should be rate-limited.");
        helper.assertTrue(EchoRateLimiter.tryAcquire(player, packetId, EchoRateLimitPolicy.NONE),
                "Disabled policies should always pass.");
        EchoRateLimiter.clearForTests();
        helper.succeed();
    }

    private static void debugHooks(GameTestHelper helper) {
        EchoNetDebug.clearCountersForTests();
        AtomicInteger count = new AtomicInteger();
        Identifier debugProbe = Identifier.fromNamespaceAndPath(EchoNetCore.MODID, "debug_probe");
        PacketDebugHook hook = event -> {
            if (event.payloadId().equals(debugProbe)) {
                count.incrementAndGet();
            }
        };
        boolean previousDebugLogging = EchoNetCoreConfig.DEBUG_PACKET_LOGGING.get();
        boolean previousDroppedLogging = EchoNetCoreConfig.LOG_DROPPED_PACKETS.get();
        boolean previousDebugPackets = EchoNetCoreConfig.ENABLE_DEBUG_PACKETS.get();
        try {
            EchoNetCoreConfig.DEBUG_PACKET_LOGGING.set(false);
            EchoNetCoreConfig.LOG_DROPPED_PACKETS.set(false);
            EchoNetCoreConfig.ENABLE_DEBUG_PACKETS.set(false);
            EchoNetDebug.HOOKS.add(hook);
            EchoNetDebug.emit(debugProbe, EchoPacketDirection.SERVERBOUND, EchoPacketKind.DEBUG_DEV,
                    "tester", true, "probe");
            helper.assertTrue(count.get() == 0, "Packet debug hooks should stay silent when debug config is disabled.");
            helper.assertTrue(EchoNetDebug.counterSnapshot().values().stream().mapToLong(Long::longValue).sum() == 1L,
                    "Packet counters should count events even when hooks are silent.");

            EchoNetCoreConfig.DEBUG_PACKET_LOGGING.set(true);
            EchoNetDebug.emit(debugProbe, EchoPacketDirection.SERVERBOUND, EchoPacketKind.DEBUG_DEV,
                    "tester", true, "probe");
            helper.assertTrue(count.get() == 1, "Packet debug hooks should receive events when debug logging is enabled.");
        } finally {
            EchoNetDebug.HOOKS.remove(hook);
            EchoNetDebug.clearCountersForTests();
            EchoNetCoreConfig.DEBUG_PACKET_LOGGING.set(previousDebugLogging);
            EchoNetCoreConfig.LOG_DROPPED_PACKETS.set(previousDroppedLogging);
            EchoNetCoreConfig.ENABLE_DEBUG_PACKETS.set(previousDebugPackets);
        }
        helper.succeed();
    }

    private static void clientSyncRegistry(GameTestHelper helper) {
        EchoClientSyncRegistry.clearForTests();
        Identifier channel = Identifier.fromNamespaceAndPath(EchoNetCore.MODID, "sync_test");
        EchoSyncPayload payload = new EchoSyncPayload(EchoSyncType.PLAYER_DATA, channel, null, new CompoundTag());
        AtomicInteger first = new AtomicInteger();
        AtomicInteger second = new AtomicInteger();
        AtomicInteger throwing = new AtomicInteger();
        try {
            AutoCloseable firstSubscription = EchoClientSyncRegistry.subscribe(EchoSyncType.PLAYER_DATA, channel,
                    ignored -> first.incrementAndGet());
            EchoClientSyncRegistry.register(EchoSyncType.PLAYER_DATA, channel, ignored -> second.incrementAndGet());
            EchoClientSyncRegistry.register(EchoSyncType.PLAYER_DATA, channel, ignored -> {
                throwing.incrementAndGet();
                throw new IllegalStateException("intentional test failure");
            });
            EchoClientSyncRegistry.dispatch(null);
            EchoClientSyncRegistry.dispatch(payload);
            helper.assertTrue(first.get() == 1 && second.get() == 1 && throwing.get() == 1,
                    "All matching sync consumers should receive the payload and failures should be isolated.");
            firstSubscription.close();
            EchoClientSyncRegistry.dispatch(payload);
            helper.assertTrue(first.get() == 1 && second.get() == 2 && throwing.get() == 2,
                    "Closed sync subscriptions should stop receiving payloads.");
        } catch (Exception exception) {
            helper.fail("Client sync registry test failed: " + exception.getMessage());
        } finally {
            EchoClientSyncRegistry.clearForTests();
        }
        helper.succeed();
    }

    private static void codecRoundTrips(GameTestHelper helper) {
        Identifier channel = Identifier.fromNamespaceAndPath(EchoNetCore.MODID, "codec_test");
        CompoundTag tag = new CompoundTag();
        tag.putString("probe", "value");

        EchoSyncPayload sync = roundTrip(new EchoSyncPayload(EchoSyncType.MACHINE_STATE,
                channel, new BlockPos(1, 2, 3), tag), EchoSyncPayload.CODEC);
        helper.assertTrue(sync.syncType() == EchoSyncType.MACHINE_STATE
                        && channel.equals(sync.channelId())
                        && new BlockPos(1, 2, 3).equals(sync.pos())
                        && "value".equals(sync.payload().getStringOr("probe", "")),
                "Generic sync payload should round trip.");

        DiscoveryToastPacket toast = roundTrip(new DiscoveryToastPacket(new EchoDiscoveryToast(
                null, "cat", "title", "subtitle", "icon", "hero", 0x44AAFF)), DiscoveryToastPacket.CODEC);
        helper.assertTrue(toast.toast().featureId() != null && "title".equals(toast.toast().title()),
                "Discovery toasts should round trip with a safe fallback feature id.");

        EchoFactionSyncPacket faction = roundTrip(new EchoFactionSyncPacket(tag), EchoFactionSyncPacket.CODEC);
        helper.assertTrue("value".equals(faction.factionRoot().getStringOr("probe", "")),
                "Faction sync should round trip NBT.");

        EchoDebugCommandPacket debug = roundTrip(new EchoDebugCommandPacket(channel, tag), EchoDebugCommandPacket.CODEC);
        helper.assertTrue(channel.equals(debug.commandId()) && "value".equals(debug.payload().getStringOr("probe", "")),
                "Debug command packets should round trip.");
        helper.succeed();
    }

    private static <T> T roundTrip(T value, StreamCodec<FriendlyByteBuf, T> codec) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        codec.encode(buffer, value);
        return codec.decode(buffer);
    }

    private static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment,
            String testName, Identifier functionId) {
        TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(
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
                2
        );
        event.registerTest(id(testName), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, functionId), data));
    }

    private static boolean shouldRegisterTests() {
        String namespaces = System.getProperty("neoforge.enabledGameTestNamespaces", "");
        if (namespaces == null || namespaces.isBlank()) {
            return true;
        }
        for (String namespace : namespaces.split(",")) {
            String normalized = namespace.trim();
            if (normalized.equals(EchoNetCore.MODID) || normalized.equals("*") || normalized.equalsIgnoreCase("all")) {
                return true;
            }
        }
        return false;
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoNetCore.MODID, path);
    }
}
