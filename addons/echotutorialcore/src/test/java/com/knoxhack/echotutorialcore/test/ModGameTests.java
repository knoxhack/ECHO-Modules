package com.knoxhack.echotutorialcore.test;

import com.knoxhack.echotutorialcore.EchoTutorialCore;
import com.knoxhack.echotutorialcore.api.TutorialCategory;
import com.knoxhack.echotutorialcore.api.TutorialConditionType;
import com.knoxhack.echotutorialcore.api.TutorialGuideMode;
import com.knoxhack.echotutorialcore.api.TutorialHintType;
import com.knoxhack.echotutorialcore.api.TutorialRequirement;
import com.knoxhack.echotutorialcore.api.card.TutorialCard;
import com.knoxhack.echotutorialcore.api.hint.TutorialHint;
import com.knoxhack.echotutorialcore.api.trigger.TutorialFlow;
import com.knoxhack.echotutorialcore.api.trigger.TutorialStep;
import com.knoxhack.echotutorialcore.api.trigger.TutorialTriggerType;
import com.knoxhack.echotutorialcore.data.TutorialCoreRegistries;
import com.knoxhack.echotutorialcore.data.TutorialPlayerData;
import com.knoxhack.echotutorialcore.network.SyncTutorialProgressPacket;
import com.knoxhack.echotutorialcore.server.TutorialConditionResolver;
import com.knoxhack.echotutorialcore.server.TutorialProgressManager;
import com.knoxhack.echotutorialcore.server.TutorialRequirementResolver;
import io.netty.buffer.Unpooled;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, EchoTutorialCore.MODID);

    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> REGISTRY_VALIDATION =
            TEST_FUNCTIONS.register("registry_validation", () -> ModGameTests::registryValidation);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> PROGRESS_FLOW =
            TEST_FUNCTIONS.register("progress_flow_unlocks", () -> ModGameTests::progressFlowUnlocks);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> PACKET_CODEC =
            TEST_FUNCTIONS.register("progress_packet_codec", () -> ModGameTests::progressPacketCodec);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CONDITION_KEYS =
            TEST_FUNCTIONS.register("condition_key_validation", () -> ModGameTests::conditionKeyValidation);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CONDITION_MATCHES =
            TEST_FUNCTIONS.register("condition_matching", () -> ModGameTests::conditionMatching);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> REQUIREMENT_RESOLVER =
            TEST_FUNCTIONS.register("requirement_resolver", () -> ModGameTests::requirementResolver);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> HUD_NOTICE_MAPPING =
            TEST_FUNCTIONS.register("hud_notice_mapping", () -> ModGameTests::hudNoticeMapping);

    private ModGameTests() {}

    public static void register(IEventBus eventBus) {
        TEST_FUNCTIONS.register(eventBus);
    }

    public static void registerTests(RegisterGameTestsEvent event) {
        if (!shouldRegisterTests()) {
            return;
        }
        register(event, "registry_validation", REGISTRY_VALIDATION.getId());
        register(event, "progress_flow_unlocks", PROGRESS_FLOW.getId());
        register(event, "progress_packet_codec", PACKET_CODEC.getId());
        register(event, "condition_key_validation", CONDITION_KEYS.getId());
        register(event, "condition_matching", CONDITION_MATCHES.getId());
        register(event, "requirement_resolver", REQUIREMENT_RESOLVER.getId());
        register(event, "hud_notice_mapping", HUD_NOTICE_MAPPING.getId());
    }

    private static void registryValidation(GameTestHelper helper) {
        TutorialCoreRegistries.resetForTests();
        Identifier missing = id("missing_card");
        TutorialCoreRegistries.registerCard(new TutorialCard(
                id("broken_card"),
                TutorialCategory.START_HERE,
                "Broken",
                "Validation test",
                List.of(),
                List.of(),
                List.of(),
                List.of(missing),
                List.of(""),
                true,
                EchoTutorialCore.MODID,
                1));
        TutorialCoreRegistries.registerHint(new TutorialHint(
                id("broken_hint"),
                TutorialHintType.INFO,
                TutorialCategory.START_HERE,
                "Broken",
                "Validation test",
                "",
                "Open",
                missing,
                20,
                EnumSet.of(TutorialGuideMode.NORMAL),
                1,
                true,
                List.of("unknown_prefix:value")));
        TutorialCoreRegistries.registerFlow(new TutorialFlow(
                id("broken_flow"),
                "Broken",
                TutorialCategory.START_HERE,
                List.of(new TutorialStep("same", TutorialTriggerType.CUSTOM, id("one"), "", false),
                        new TutorialStep("same", TutorialTriggerType.CUSTOM, id("two"), "", false)),
                List.of(missing),
                true));

        List<String> warnings = TutorialCoreRegistries.validate();
        helper.assertTrue(warnings.stream().anyMatch(warning -> warning.contains("missing related card")),
                "Registry validation should catch missing related cards.");
        helper.assertTrue(warnings.stream().anyMatch(warning -> warning.contains("missing action card")),
                "Registry validation should catch missing hint action cards.");
        helper.assertTrue(warnings.stream().anyMatch(warning -> warning.contains("unknown condition key")),
                "Registry validation should catch unknown hint conditions.");
        helper.assertTrue(warnings.stream().anyMatch(warning -> warning.contains("duplicate step id")),
                "Registry validation should catch duplicate flow steps.");
        helper.assertTrue(warnings.stream().anyMatch(warning -> warning.contains("unlocks missing card")),
                "Registry validation should catch missing flow unlock cards.");
        helper.succeed();
    }

    private static void progressFlowUnlocks(GameTestHelper helper) {
        TutorialCoreRegistries.resetForTests();
        Identifier progress = id("stabilized");
        Identifier cardId = id("stabilize_card");
        Identifier flowId = id("first_flow");
        TutorialCoreRegistries.registerCard(new TutorialCard(
                cardId,
                TutorialCategory.SURVIVAL,
                "Stabilize",
                "Validation test",
                List.of("Hold the line."),
                List.of(),
                List.of(),
                List.of(),
                List.of("progress:" + progress),
                false,
                EchoTutorialCore.MODID,
                10));
        TutorialCoreRegistries.registerFlow(new TutorialFlow(
                flowId,
                "First Flow",
                TutorialCategory.SURVIVAL,
                List.of(new TutorialStep("stabilized", TutorialTriggerType.CUSTOM, progress, "Stabilize", false)),
                List.of(cardId),
                true));

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        TutorialProgressManager.markProgress(player, progress);
        TutorialPlayerData data = TutorialPlayerData.get(player);
        helper.assertTrue(data.hasProgress(progress), "Progress should persist to TutorialPlayerData.");
        helper.assertTrue(data.isCardUnlocked(cardId), "Progress should unlock matching tutorial cards.");
        helper.assertTrue(data.isCardUnread(cardId), "Newly unlocked cards should be marked unread.");
        helper.assertTrue(data.hasFlowStep(flowId, "stabilized"), "Progress should complete matching flow steps.");
        helper.assertTrue(data.isFlowCompleted(flowId), "Required completed steps should complete the flow.");
        helper.succeed();
    }

    private static void progressPacketCodec(GameTestHelper helper) {
        SyncTutorialProgressPacket packet = new SyncTutorialProgressPacket(
                "ASSISTED",
                List.of(id("entered_world").toString()),
                List.of(id("card").toString()),
                List.of(id("card").toString()),
                List.of(id("flow").toString()),
                "what_now");
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        SyncTutorialProgressPacket.CODEC.encode(buf, packet);
        SyncTutorialProgressPacket decoded = SyncTutorialProgressPacket.CODEC.decode(buf);
        helper.assertTrue(decoded.guideMode().equals("ASSISTED"), "Progress packet should preserve guide mode.");
        helper.assertTrue(decoded.progressFlags().equals(packet.progressFlags()), "Progress packet should preserve progress flags.");
        helper.assertTrue(decoded.unreadCardIds().equals(packet.unreadCardIds()), "Progress packet should preserve unread cards.");
        helper.assertTrue(decoded.completedFlowIds().equals(packet.completedFlowIds()), "Progress packet should preserve completed flows.");
        helper.assertTrue(decoded.lastRecommendationReason().equals("what_now"), "Progress packet should preserve recommendation reason.");
        helper.succeed();
    }

    private static void conditionKeyValidation(GameTestHelper helper) {
        helper.assertTrue(TutorialConditionResolver.isKnownConditionKey("progress:echotutorialcore:entered_world"),
                "Progress condition should be recognized.");
        helper.assertTrue(TutorialConditionResolver.isKnownConditionKey("missing_progress:echotutorialcore:used_scanner"),
                "Missing-progress condition should be recognized.");
        helper.assertTrue(TutorialConditionResolver.isKnownConditionKey("power_alert:brownout"),
                "Power alert condition should be recognized.");
        helper.assertTrue(TutorialConditionResolver.isKnownConditionKey("time_since_progress_minutes:10"),
                "No-progress timer condition should be recognized.");
        helper.assertFalse(TutorialConditionResolver.isKnownConditionKey("mystery:value"),
                "Unknown condition keys should be rejected.");
        helper.succeed();
    }

    private static void conditionMatching(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        TutorialProgressManager.markProgress(player, id("opened_terminal"));
        player.getInventory().add(new ItemStack(Items.REDSTONE, 2));

        helper.assertTrue(TutorialConditionResolver.matches(player, "progress:echotutorialcore:opened_terminal"),
                "Progress condition should match player progress.");
        helper.assertTrue(TutorialConditionResolver.matches(player, "!progress:echotutorialcore:used_scanner"),
                "Inverted progress condition should match missing progress.");
        helper.assertTrue(TutorialConditionResolver.matches(player, "has_item:minecraft:redstone"),
                "Inventory item condition should match held items.");
        helper.assertTrue(TutorialConditionResolver.matches(player, "missing_item:minecraft:diamond"),
                "Missing item condition should match absent items.");
        helper.succeed();
    }

    private static void requirementResolver(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        TutorialRequirement itemRequirement = TutorialRequirementResolver.resolveRequirement(
                Identifier.fromNamespaceAndPath("minecraft", "item_redstone"));
        helper.assertTrue(itemRequirement.type() == TutorialConditionType.HAS_ITEM,
                "Item requirement id should resolve to a HAS_ITEM requirement.");
        helper.assertTrue(TutorialRequirementResolver.resolveMissingRequirements(player, List.of(itemRequirement)).size() == 1,
                "Missing item requirement should be reported.");

        player.getInventory().add(new ItemStack(Items.REDSTONE, 1));
        helper.assertTrue(TutorialRequirementResolver.resolveMissingRequirements(player, List.of(itemRequirement)).isEmpty(),
                "Satisfied item requirement should not be reported missing.");
        helper.succeed();
    }

    private static void hudNoticeMapping(GameTestHelper helper) {
        try {
            Class<?> integration = Class.forName(
                    "com.knoxhack.echotutorialcore.integration.terminal.TutorialTerminalNoticeIntegration");
            Object guide = integration.getMethod("noticeForHudForTests",
                            String.class, String.class, String.class, boolean.class)
                    .invoke(null, "Guide Card", "Unlocked: First Hour Survival", "", false);
            assertNoticeField(helper, guide, "sourceLabel", "ECHO-7",
                    "Tutorial notices should identify ECHO-7 as the HUD source");
            assertNoticeField(helper, guide, "statusLabel", "GUIDE",
                    "Guide card notices should map to GUIDE status");
            assertNoticeField(helper, guide, "title", "Guide Card",
                    "Guide card title should survive compact mapping");
            assertNoticeField(helper, guide, "detail", "Unlocked: First Hour Survival",
                    "Guide card detail should survive compact mapping");
            helper.assertTrue(((Integer) guide.getClass().getMethod("accentColor").invoke(guide)) == 0xFF92F7A6,
                    "Guide card notices should use the normal TutorialCore accent");

            Object danger = integration.getMethod("noticeForHudForTests",
                            String.class, String.class, String.class, boolean.class)
                    .invoke(null, "Oxygen", "Seal breach detected", "Find shelter", true);
            assertNoticeField(helper, danger, "statusLabel", "DANGER",
                    "Danger tutorial notices should map to DANGER status");
            helper.assertTrue(((Integer) danger.getClass().getMethod("accentColor").invoke(danger)) == 0xFFFF665E,
                    "Danger tutorial notices should use the danger accent");
            helper.succeed();
        } catch (ClassNotFoundException | LinkageError ignored) {
            helper.succeed();
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Failed to inspect TutorialCore HUD notice mapping", exception);
        }
    }

    private static void assertNoticeField(GameTestHelper helper, Object notice, String accessor,
            String expected, String message) throws ReflectiveOperationException {
        helper.assertTrue(expected.equals(notice.getClass().getMethod(accessor).invoke(notice)), message);
    }

    private static void register(RegisterGameTestsEvent event, String testName, Identifier functionId) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(id("tutorialcore_" + testName));
        TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(
                environment, Identifier.withDefaultNamespace("empty"), 200, 0, true, Rotation.NONE, false, 1, 1,
                false, 8);
        event.registerTest(id(testName), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, functionId), data));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoTutorialCore.MODID, path);
    }

    private static boolean shouldRegisterTests() {
        String namespaces = System.getProperty("neoforge.enabledGameTestNamespaces", "");
        if (namespaces == null || namespaces.isBlank()) {
            return true;
        }
        for (String namespace : namespaces.split(",")) {
            String normalized = namespace.trim();
            if (normalized.equals(EchoTutorialCore.MODID) || normalized.equals("*") || normalized.equalsIgnoreCase("all")) {
                return true;
            }
        }
        return false;
    }
}
