package com.knoxhack.echocommunitybridge.test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.knoxhack.echocommunitybridge.EchoCommunityBridge;
import com.knoxhack.echocommunitybridge.config.CommunityBridgeConfig;
import com.knoxhack.echocommunitybridge.discord.DiscordGatewayClient;
import com.knoxhack.echocommunitybridge.launcher.LauncherChatBridgeClient;
import com.knoxhack.echocommunitybridge.server.OfficialChatService;
import com.knoxhack.echocommunitybridge.server.ServerStatusService;
import java.util.function.Consumer;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, EchoCommunityBridge.MODID);

    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> STATUS_JSON =
            TEST_FUNCTIONS.register("status_json_contract", () -> ModGameTests::statusJsonContract);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> EVENT_BUFFER =
            TEST_FUNCTIONS.register("event_ring_buffer", () -> ModGameTests::eventRingBuffer);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SANITIZE =
            TEST_FUNCTIONS.register("chat_sanitization", () -> ModGameTests::chatSanitization);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TOKEN_HIDDEN =
            TEST_FUNCTIONS.register("token_hidden_from_status", () -> ModGameTests::tokenHiddenFromStatus);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> LAUNCHER_CHAT_DEFAULTS =
            TEST_FUNCTIONS.register("launcher_chat_defaults", () -> ModGameTests::launcherChatDefaults);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> LAUNCHER_CHAT_SANITIZE =
            TEST_FUNCTIONS.register("launcher_chat_sanitization", () -> ModGameTests::launcherChatSanitization);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> OFFICIAL_CHAT_CONTRACT =
            TEST_FUNCTIONS.register("official_chat_contract", () -> ModGameTests::officialChatContract);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DISCORD_INBOUND_SANITIZE =
            TEST_FUNCTIONS.register("discord_inbound_sanitization", () -> ModGameTests::discordInboundSanitization);

    private ModGameTests() {
    }

    public static void register(IEventBus eventBus) {
        TEST_FUNCTIONS.register(eventBus);
    }

    public static void registerTests(RegisterGameTestsEvent event) {
        if (!shouldRegisterTests()) {
            return;
        }
        var environment = event.registerEnvironment(id("community_bridge"));
        register(event, environment, "status_json_contract", STATUS_JSON.getId());
        register(event, environment, "event_ring_buffer", EVENT_BUFFER.getId());
        register(event, environment, "chat_sanitization", SANITIZE.getId());
        register(event, environment, "token_hidden_from_status", TOKEN_HIDDEN.getId());
        register(event, environment, "launcher_chat_defaults", LAUNCHER_CHAT_DEFAULTS.getId());
        register(event, environment, "launcher_chat_sanitization", LAUNCHER_CHAT_SANITIZE.getId());
        register(event, environment, "official_chat_contract", OFFICIAL_CHAT_CONTRACT.getId());
        register(event, environment, "discord_inbound_sanitization", DISCORD_INBOUND_SANITIZE.getId());
    }

    private static void statusJsonContract(GameTestHelper helper) {
        JsonObject snapshot = ServerStatusService.INSTANCE.snapshotJson();
        helper.assertTrue(snapshot.get("schemaVersion").getAsInt() == 1, "Schema version should be 1");
        helper.assertTrue(snapshot.has("serverId"), "Snapshot should include serverId");
        helper.assertTrue(snapshot.has("playerCount"), "Snapshot should include playerCount");
        helper.assertTrue(snapshot.has("maxPlayers"), "Snapshot should include maxPlayers");
        helper.assertTrue(snapshot.has("players"), "Snapshot should include players");
        helper.assertTrue(snapshot.has("discord"), "Snapshot should include discord");
        helper.assertTrue(snapshot.has("version"), "Snapshot should include version");
        helper.assertTrue(snapshot.has("recentEvents"), "Snapshot should include recentEvents");
        helper.assertTrue(snapshot.has("lastUpdated"), "Snapshot should include lastUpdated");
        helper.succeed();
    }

    private static void eventRingBuffer(GameTestHelper helper) {
        int limit = CommunityBridgeConfig.RECENT_EVENT_LIMIT.get();
        for (int index = 0; index < limit + 5; index++) {
            ServerStatusService.INSTANCE.addEvent("chat", "Tester", "message " + index);
        }
        int size = ServerStatusService.INSTANCE.snapshotJson().getAsJsonArray("recentEvents").size();
        helper.assertTrue(size <= limit, "Recent events should not exceed configured limit");
        helper.succeed();
    }

    private static void chatSanitization(GameTestHelper helper) {
        String sanitized = ServerStatusService.sanitizePublicText("hello\r\n@everyone\u0000world", 20);
        helper.assertTrue(!sanitized.contains("\u0000"), "Control characters should be removed");
        helper.assertTrue(!sanitized.contains("@everyone"), "Mass mention should be broken");
        helper.assertTrue(sanitized.length() <= 20, "Text should be truncated");
        helper.succeed();
    }

    private static void tokenHiddenFromStatus(GameTestHelper helper) {
        String json = ServerStatusService.INSTANCE.snapshotJson().toString();
        helper.assertTrue(!json.contains("botToken"), "Status JSON should not expose token field names");
        helper.assertTrue(!json.contains("ECHO_DISCORD_BOT_TOKEN"), "Status JSON should not expose env token names");
        helper.succeed();
    }

    private static void launcherChatDefaults(GameTestHelper helper) {
        helper.assertTrue(CommunityBridgeConfig.LAUNCHER_CHAT_ENABLED.get(), "Mod-hosted official chat should default enabled");
        helper.assertTrue(CommunityBridgeConfig.launcherChatReady(), "Mod-hosted official chat should not require bridge tokens");
        helper.succeed();
    }

    private static void launcherChatSanitization(GameTestHelper helper) {
        var safe = LauncherChatBridgeClient.launcherChatLine("Launcher User", "hello @everyone\u0000");
        helper.assertTrue(safe.isPresent(), "Launcher chat text should be accepted");
        helper.assertTrue(!safe.get().contains("\u0000"), "Launcher chat text should strip controls");
        helper.assertTrue(!safe.get().contains("@everyone"), "Launcher chat text should break mass mentions");
        helper.assertTrue(LauncherChatBridgeClient.launcherChatLine("Launcher User", "/op Knox").isEmpty(),
                "Launcher slash commands should not relay into Minecraft");
        var android = LauncherChatBridgeClient.launcherChatLine("android", "Pixel Tester", "hello");
        helper.assertTrue(android.isPresent() && android.get().startsWith("[Android]"),
                "Android chat should use an Android relay label");
        helper.succeed();
    }

    private static void officialChatContract(GameTestHelper helper) {
        OfficialChatService.INSTANCE.clearForTests();
        var launcher = OfficialChatService.INSTANCE.acceptPublicMessage(
                "launcher", "launcher-client", "Launcher Tester", "hello from launcher", "launcher-nonce");
        var android = OfficialChatService.INSTANCE.acceptPublicMessage(
                "android", "android-client", "Android Tester", "hello from android", "android-nonce");
        var duplicate = OfficialChatService.INSTANCE.acceptPublicMessage(
                "android", "android-client", "Android Tester", "duplicate body", "android-nonce");

        helper.assertTrue("launcher".equals(launcher.source()), "Launcher source should be stored");
        helper.assertTrue("android".equals(android.source()), "Android source should be stored");
        helper.assertTrue(duplicate.id().equals(android.id()), "Duplicate nonce should return existing message");
        helper.assertTrue(OfficialChatService.INSTANCE.historyCount() == 2, "Duplicate nonce should not append history");

        JsonObject bootstrap = OfficialChatService.INSTANCE.bootstrap("launcher-client", "Launcher Tester");
        JsonObject messages = bootstrap.getAsJsonObject("messages");
        helper.assertTrue(messages.has(OfficialChatService.channelId()), "Bootstrap should include official channel history");

        boolean rejected = false;
        try {
            OfficialChatService.normalizePublicSource("discord");
        } catch (OfficialChatService.ChatRequestException ex) {
            rejected = ex.statusCode() == 400;
        }
        helper.assertTrue(rejected, "Public POST source should reject discord/system/minecraft echoes");
        OfficialChatService.INSTANCE.clearForTests();
        helper.succeed();
    }

    private static void discordInboundSanitization(GameTestHelper helper) {
        JsonObject message = JsonParser.parseString("""
                {
                  "channel_id": "1411441469449044180",
                  "content": "hello @everyone\\u0000",
                  "author": { "id": "12345", "username": "Discord Tester", "bot": false }
                }
                """).getAsJsonObject();
        var parsed = DiscordGatewayClient.parseMessageCreate(message, "1411441469449044180");
        helper.assertTrue(parsed.isPresent(), "Discord user message should parse");
        helper.assertTrue(!parsed.get().body().contains("\u0000"), "Discord body should strip controls");
        helper.assertTrue(!parsed.get().body().contains("@everyone"), "Discord body should break mass mentions");
        helper.assertTrue(DiscordGatewayClient.discordChatLine(parsed.get().authorName(), parsed.get().body()).isPresent(),
                "Discord chat line should be generated");

        JsonObject botMessage = JsonParser.parseString("""
                {
                  "channel_id": "1411441469449044180",
                  "content": "ignore me",
                  "author": { "id": "bot", "username": "Relay", "bot": true }
                }
                """).getAsJsonObject();
        helper.assertTrue(DiscordGatewayClient.parseMessageCreate(botMessage, "1411441469449044180").isEmpty(),
                "Bot messages should not relay back into Minecraft");
        helper.succeed();
    }

    private static void register(RegisterGameTestsEvent event,
            net.minecraft.core.Holder<TestEnvironmentDefinition<?>> environment,
            String testName,
            Identifier functionId) {
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
            if (normalized.equals(EchoCommunityBridge.MODID) || normalized.equals("*") || normalized.equalsIgnoreCase("all")) {
                return true;
            }
        }
        return false;
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoCommunityBridge.MODID, path);
    }
}
