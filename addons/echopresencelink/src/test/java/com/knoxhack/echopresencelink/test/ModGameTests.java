package com.knoxhack.echopresencelink.test;

import com.google.gson.JsonObject;
import com.knoxhack.echopresencelink.EchoPresenceLink;
import com.knoxhack.echopresencelink.api.EchoPresenceButton;
import com.knoxhack.echopresencelink.api.EchoPresenceContext;
import com.knoxhack.echopresencelink.api.EchoPresenceProvider;
import com.knoxhack.echopresencelink.api.EchoPresenceRegistry;
import com.knoxhack.echopresencelink.api.EchoPresenceSnapshot;
import com.knoxhack.echopresencelink.api.PresenceSanitizer;
import com.knoxhack.echopresencelink.discord.PresenceActivityPayload;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
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
            DeferredRegister.create(Registries.TEST_FUNCTION, EchoPresenceLink.MODID);

    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> PROVIDER_PRIORITY =
            TEST_FUNCTIONS.register("provider_priority", () -> ModGameTests::providerPriority);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> PRIVACY_SANITIZATION =
            TEST_FUNCTIONS.register("privacy_sanitization", () -> ModGameTests::privacySanitization);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> PAYLOAD_JSON =
            TEST_FUNCTIONS.register("payload_json", () -> ModGameTests::payloadJson);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MINIMAL_PAYLOAD_JSON =
            TEST_FUNCTIONS.register("minimal_payload_json", () -> ModGameTests::minimalPayloadJson);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> BUTTON_FILTER =
            TEST_FUNCTIONS.register("button_filter", () -> ModGameTests::buttonFilter);

    private ModGameTests() {
    }

    public static void register(IEventBus eventBus) {
        TEST_FUNCTIONS.register(eventBus);
    }

    public static void registerTests(RegisterGameTestsEvent event) {
        if (!shouldRegisterTests()) {
            return;
        }
        var environment = event.registerEnvironment(id("presence_link"));
        register(event, environment, "provider_priority", PROVIDER_PRIORITY.getId());
        register(event, environment, "privacy_sanitization", PRIVACY_SANITIZATION.getId());
        register(event, environment, "payload_json", PAYLOAD_JSON.getId());
        register(event, environment, "minimal_payload_json", MINIMAL_PAYLOAD_JSON.getId());
        register(event, environment, "button_filter", BUTTON_FILTER.getId());
    }

    private static void providerPriority(GameTestHelper helper) {
        EchoPresenceContext context = new EchoPresenceContext(null, "", "", "", "", BlockPos.ZERO,
                true, false, false, false, true, 100L, 50L);
        EchoPresenceProvider low = fixed("low", 10, "Low", "Low state");
        EchoPresenceProvider high = fixed("high", 90, "High", "High state");
        EchoPresenceSnapshot selected = EchoPresenceRegistry.select(List.of(low, high), context).orElseThrow();
        helper.assertTrue(selected.details().equals("High"), "Highest priority provider should win");
        helper.succeed();
    }

    private static void privacySanitization(GameTestHelper helper) {
        String cleaned = PresenceSanitizer.text("hello\r\n\u0000\u00A7cworld", 20, "");
        helper.assertTrue(!cleaned.contains("\u0000"), "Control characters should be stripped");
        helper.assertTrue(!cleaned.contains("\u00A7"), "Formatting codes should be stripped");
        helper.assertTrue(cleaned.equals("hello world"), "Whitespace should be compacted");
        String asset = PresenceSanitizer.assetKey("Hazard Radiation!", "echo_ashfall");
        helper.assertTrue(asset.equals("hazard_radiation"), "Asset keys should be lowercase Discord keys");
        helper.succeed();
    }

    private static void payloadJson(GameTestHelper helper) {
        EchoPresenceSnapshot snapshot = new EchoPresenceSnapshot(id("test"), 10,
                "Ashfall Protocol | P4 Signal Contact",
                "Tracking a faction distress lead",
                "echo_ashfall",
                "Ashfall Protocol",
                "echo_terminal",
                "ECHO Terminal",
                123L,
                List.of(),
                false);
        JsonObject activity = PresenceActivityPayload.activity(snapshot,
                List.of(new EchoPresenceButton("Join Discord", "https://discord.gg/example")));
        helper.assertTrue(activity.get("type").getAsInt() == 0, "Discord activity type should be Playing");
        helper.assertTrue(activity.get("details").getAsString().contains("Ashfall Protocol"), "Details should serialize");
        helper.assertTrue(activity.getAsJsonObject("assets").get("large_image").getAsString().equals("echo_ashfall"),
                "Large image should serialize");
        helper.assertTrue(activity.getAsJsonArray("buttons").size() == 1, "Buttons should serialize");
        PresenceActivityPayload.CommandPayload command = PresenceActivityPayload.setActivity(activity);
        helper.assertTrue(command.json().get("cmd").getAsString().equals("SET_ACTIVITY"), "SET_ACTIVITY should serialize");
        helper.assertTrue(!command.nonce().isBlank(), "SET_ACTIVITY nonce should be exposed for response matching");
        helper.assertTrue(command.json().get("nonce").getAsString().equals(command.nonce()),
                "Command nonce should match payload nonce");
        helper.succeed();
    }

    private static void minimalPayloadJson(GameTestHelper helper) {
        EchoPresenceSnapshot snapshot = new EchoPresenceSnapshot(id("minimal"), 10,
                "ECHO Presence Link",
                "Manual no-asset RPC test",
                "echo_ashfall",
                "Ashfall Protocol",
                "echo_terminal",
                "ECHO Terminal",
                123L,
                List.of(new EchoPresenceButton("Join Discord", "https://discord.gg/example")),
                false);
        JsonObject minimal = PresenceActivityPayload.minimalActivity(snapshot);
        helper.assertTrue(minimal.get("type").getAsInt() == 0, "Minimal payload should still set Playing type");
        helper.assertTrue(minimal.has("details"), "Minimal payload should include details");
        helper.assertTrue(minimal.has("state"), "Minimal payload should include state");
        helper.assertTrue(!minimal.has("assets"), "Minimal payload should omit assets");
        helper.assertTrue(!minimal.has("buttons"), "Minimal payload should omit buttons");
        PresenceActivityPayload.CommandPayload clear = PresenceActivityPayload.clearActivity();
        helper.assertTrue(clear.json().getAsJsonObject("args").get("activity").isJsonNull(),
                "Clear activity should send null activity");
        helper.succeed();
    }

    private static void buttonFilter(GameTestHelper helper) {
        EchoPresenceButton invalid = new EchoPresenceButton("Bad", "javascript:alert(1)");
        EchoPresenceButton valid = new EchoPresenceButton("Docs", "https://docs.discord.com/");
        EchoPresenceSnapshot snapshot = new EchoPresenceSnapshot(id("buttons"), 10, "ECHO", "",
                "echo_ashfall", "", "", "", 0L, List.of(invalid, valid), false);
        helper.assertTrue(snapshot.buttons().size() == 1, "Invalid button URLs should be dropped");
        helper.succeed();
    }

    private static EchoPresenceProvider fixed(String path, int priority, String details, String state) {
        Identifier id = id(path);
        return new EchoPresenceProvider() {
            @Override
            public Identifier id() {
                return id;
            }

            @Override
            public EchoPresenceSnapshot snapshot(EchoPresenceContext context) {
                return EchoPresenceSnapshot.of(id, priority, details, state, "echo_ashfall", 0L);
            }
        };
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
            if (normalized.equals(EchoPresenceLink.MODID) || normalized.equals("*") || normalized.equalsIgnoreCase("all")) {
                return true;
            }
        }
        return false;
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoPresenceLink.MODID, path);
    }
}
