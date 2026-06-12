package com.knoxhack.echorecovery.test;

import com.google.gson.JsonParser;
import com.echoplatform.echocore.api.EchoCoreServices;
import com.knoxhack.echorecovery.api.RecoveryGraveSnapshot;
import com.knoxhack.echorecovery.api.RecoveryIntegrations;
import com.knoxhack.echorecovery.EchoRecovery;
import com.knoxhack.echorecovery.api.RecoveryItemRuleResult;
import com.knoxhack.echorecovery.block.entity.GraveBlockEntity;
import com.knoxhack.echorecovery.content.RecoveryJsonReloadListener;
import com.knoxhack.echorecovery.content.RecoveryRuleDefinition;
import com.knoxhack.echorecovery.data.RecoveryWorldData;
import com.knoxhack.echorecovery.grave.GraveAccessResult;
import com.knoxhack.echorecovery.grave.DeathHandler;
import com.knoxhack.echorecovery.grave.GraveManager;
import com.knoxhack.echorecovery.item.GraveKeyItem;
import com.knoxhack.echorecovery.registry.ModBlockEntities;
import com.knoxhack.echorecovery.registry.ModBlocks;
import com.knoxhack.echorecovery.registry.ModItems;
import com.knoxhack.echorecovery.registry.ModMenus;
import com.knoxhack.echorecovery.registry.ModSounds;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.fml.ModList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, EchoRecovery.MODID);

    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> METADATA_REGISTRY =
            TEST_FUNCTIONS.register("metadata_registry", () -> ModGameTests::metadataRegistry);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ACCESS_RECOVERY =
            TEST_FUNCTIONS.register("access_recovery", () -> ModGameTests::accessRecovery);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FIELD_CACHE_RECOVERY_XP_AND_KEY =
            TEST_FUNCTIONS.register("field_cache_recovery_xp_and_key", () -> ModGameTests::fieldCacheRecoveryXpAndKey);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> XP_CAPTURE_FALLBACK =
            TEST_FUNCTIONS.register("xp_capture_fallback", () -> ModGameTests::xpCaptureFallback);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DATAPACK_PARSERS =
            TEST_FUNCTIONS.register("datapack_parsers", () -> ModGameTests::datapackParsers);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CORE_SERVICE =
            TEST_FUNCTIONS.register("core_service", () -> ModGameTests::coreService);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WORLD_DATA_LOOKUP =
            TEST_FUNCTIONS.register("world_data_lookup", () -> ModGameTests::worldDataLookup);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ORIGINAL_SLOT_RESTORE =
            TEST_FUNCTIONS.register("original_slot_restore", () -> ModGameTests::originalSlotRestore);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> LOCAL_API_HOOKS =
            TEST_FUNCTIONS.register("local_api_hooks", () -> ModGameTests::localApiHooks);

    private ModGameTests() {
    }

    public static void register(IEventBus eventBus) {
        TEST_FUNCTIONS.register(eventBus);
    }

    public static void registerTests(RegisterGameTestsEvent event) {
        if (!shouldRegisterTests()) {
            return;
        }
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(id("recovery"));
        register(event, environment, "metadata_registry", METADATA_REGISTRY.getId());
        register(event, environment, "access_recovery", ACCESS_RECOVERY.getId());
        register(event, environment, "field_cache_recovery_xp_and_key", FIELD_CACHE_RECOVERY_XP_AND_KEY.getId());
        register(event, environment, "xp_capture_fallback", XP_CAPTURE_FALLBACK.getId());
        register(event, environment, "datapack_parsers", DATAPACK_PARSERS.getId());
        register(event, environment, "core_service", CORE_SERVICE.getId());
        register(event, environment, "world_data_lookup", WORLD_DATA_LOOKUP.getId());
        register(event, environment, "original_slot_restore", ORIGINAL_SLOT_RESTORE.getId());
        register(event, environment, "local_api_hooks", LOCAL_API_HOOKS.getId());
    }

    private static void metadataRegistry(GameTestHelper helper) {
        helper.assertTrue(ModBlocks.GRAVE.get() != Blocks.AIR, "Grave block must be registered.");
        helper.assertTrue(ModBlocks.DEATH_CACHE.get() != Blocks.AIR, "Death cache block must be registered.");
        helper.assertTrue(ModItems.GRAVE_KEY.get() != Items.AIR, "Grave key item must be registered.");
        helper.assertTrue(ModItems.RECOVERY_COMPASS.get() != Items.AIR, "Recovery compass item must be registered.");
        helper.assertTrue(ModBlockEntities.GRAVE.get() != null, "Grave block entity type must be registered.");
        helper.assertTrue(ModMenus.GRAVE.get() != null, "Grave menu must be registered.");
        helper.assertTrue(ModSounds.GRAVE_CREATE.get() != null, "Grave create sound must be registered.");
        String version = ModList.get().getModContainerById(EchoRecovery.MODID)
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse("");
        helper.assertTrue("1.3.0".equals(version), "Recovery runtime version must be 1.3.0.");
        helper.succeed();
    }

    private static void fieldCacheRecoveryXpAndKey(GameTestHelper helper) {
        ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        owner.getInventory().clearContent();
        BlockPos local = new BlockPos(1, 1, 1);
        helper.setBlock(local.below(), Blocks.STONE);
        helper.setBlock(local, ModBlocks.RECOVERY_CACHE.get());
        GraveBlockEntity grave = helper.getBlockEntity(local, GraveBlockEntity.class);
        UUID graveId = UUID.randomUUID();
        grave.setGraveId(graveId);
        grave.setOwner(owner.getUUID(), owner.getScoreboardName());
        grave.setCreatedAt(System.currentTimeMillis());
        grave.setDimension(owner.level().dimension().identifier().toString());
        grave.setGraveTypeId(DeathHandler.ASHFALL_FIELD_RECOVERY_CACHE.toString());
        grave.items().set(0, new ItemStack(Items.DIAMOND));
        grave.setXpStored(37);

        ItemStack key = new ItemStack(ModItems.GRAVE_KEY.get());
        GraveKeyItem.bindToGrave(key, graveId, grave.getBlockPos(), owner.level().dimension().identifier());
        owner.getInventory().setItem(0, key);
        RecoveryWorldData data = RecoveryWorldData.getOrCreate((ServerLevel) owner.level());
        data.addGrave(owner.getUUID(), RecoveryWorldData.GraveEntry.fromBlockEntity(grave, grave.getBlockPos(), ""));

        helper.assertTrue(GraveManager.recoverGrave(grave, owner),
                "Field recovery cache should recover successfully with a matching bound key");
        helper.assertTrue(owner.totalExperience >= 37,
                "Field recovery cache should restore stored XP before marking recovered");
        helper.assertTrue(countInventory(owner, ModItems.GRAVE_KEY.get()) == 0,
                "Field recovery cache should consume the matching bound Grave Key");
        helper.assertTrue(countInventory(owner, Items.DIAMOND) == 1,
                "Field recovery cache should restore stored item contents");
        helper.assertTrue(helper.getLevel().getBlockState(grave.getBlockPos()).isAir(),
                "Recovered field recovery cache block should be removed");

        helper.assertTrue(data.graveList().stream()
                        .filter(entry -> entry.graveId().equals(graveId))
                        .anyMatch(RecoveryWorldData.GraveEntry::recovered),
                "Recovered field recovery cache should be marked recovered in world data");
        helper.succeed();
    }

    private static void xpCaptureFallback(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.totalExperience = 0;
        player.experienceLevel = 10;
        player.experienceProgress = 0.5F;

        int captured = DeathHandler.capturableExperience(player);
        helper.assertTrue(captured > 0,
                "XP capture should compute nonzero XP when totalExperience is stale");
        helper.assertTrue(captured >= 160,
                "XP capture should include completed levels from level/progress");
        helper.succeed();
    }

    private static void accessRecovery(GameTestHelper helper) {
        ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        ServerPlayer other = helper.makeMockServerPlayerInLevel();
        BlockPos local = new BlockPos(1, 1, 1);
        helper.setBlock(local.below(), Blocks.STONE);
        helper.setBlock(local, ModBlocks.GRAVE.get());
        GraveBlockEntity grave = helper.getBlockEntity(local, GraveBlockEntity.class);
        UUID graveId = UUID.randomUUID();
        grave.setGraveId(graveId);
        grave.setOwner(owner.getUUID(), owner.getScoreboardName());
        grave.setCreatedAt(System.currentTimeMillis());
        grave.setDimension(owner.level().dimension().identifier().toString());
        grave.items().set(0, new ItemStack(Items.DIAMOND, 3));

        helper.assertTrue(GraveManager.accessGrave(grave, owner, false) == GraveAccessResult.ALLOWED,
                "Owner access must be allowed.");
        helper.assertTrue(GraveManager.accessGrave(grave, other, false) == GraveAccessResult.DENIED,
                "Unshared player access must be denied.");
        grave.shareWith(other.getUUID());
        helper.assertTrue(GraveManager.accessGrave(grave, other, false) == GraveAccessResult.ALLOWED,
                "Shared player access must be allowed.");
        helper.assertTrue(GraveManager.recoverGrave(grave, owner), "Recover all should succeed.");
        helper.assertTrue(owner.getInventory().contains(new ItemStack(Items.DIAMOND)),
                "Recovered diamond should be in owner inventory.");
        helper.succeed();
    }

    private static void datapackParsers(GameTestHelper helper) {
        RecoveryRuleDefinition rule = RecoveryJsonReloadListener.parseRuleForTests(id("test/rule"),
                JsonParser.parseString("""
                    {
                      "id": "echorecovery:test_rule",
                      "action": "protected",
                      "item": "minecraft:diamond",
                      "priority": 10
                    }
                    """).getAsJsonObject());
        helper.assertTrue(rule.result() == RecoveryItemRuleResult.PROTECTED, "Rule action should parse.");
        helper.assertTrue(rule.matches(new ItemStack(Items.DIAMOND)), "Rule item selector should match.");
        assertParseFails(helper, () -> RecoveryJsonReloadListener.parseRuleForTests(id("test/bad_rule"),
                JsonParser.parseString("""
                    {
                      "id": "echorecovery:bad_rule",
                      "action": "explode",
                      "item": "minecraft:diamond"
                    }
                    """).getAsJsonObject()));
        assertParseFails(helper, () -> RecoveryJsonReloadListener.parseRuleForTests(id("test/missing_selector"),
                JsonParser.parseString("""
                    {
                      "id": "echorecovery:missing_selector",
                      "action": "protected"
                    }
                    """).getAsJsonObject()));
        assertParseFails(helper, () -> RecoveryJsonReloadListener.parseGraveTypeForTests(id("test/bad_grave_type"),
                JsonParser.parseString("""
                    {
                      "id": "echorecovery:bad_grave_type",
                      "block": "echorecovery:not_a_real_block"
                    }
                    """).getAsJsonObject()));
        assertParseFails(helper, () -> RecoveryJsonReloadListener.parsePresetForTests(id("test/bad_preset"),
                JsonParser.parseString("""
                    {
                      "id": "echorecovery:bad_preset",
                      "values": {
                        "unknown_setting": "true"
                      }
                    }
                    """).getAsJsonObject()));
        helper.succeed();
    }

    private static void coreService(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        helper.assertFalse(EchoCoreServices.recover(player, "unknown_recovery_id"),
                "Recovery service should reject unknown recovery ids.");
        helper.succeed();
    }

    private static void worldDataLookup(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ServerLevel level = (ServerLevel) player.level();
        RecoveryWorldData data = RecoveryWorldData.getOrCreate(level);
        UUID owner = player.getUUID();
        UUID first = UUID.fromString("12345678-0000-0000-0000-000000000001");
        UUID second = UUID.fromString("12345678-0000-0000-0000-000000000002");
        data.addGrave(owner, entry(first, owner, player.getScoreboardName(), helper.absolutePos(new BlockPos(1, 1, 1)), level));
        data.addGrave(owner, entry(second, owner, player.getScoreboardName(), helper.absolutePos(new BlockPos(2, 1, 1)), level));

        RecoveryWorldData.GraveLookup full = RecoveryWorldData.findLoaded(player, owner, first.toString());
        helper.assertTrue(full.entry().isPresent() && first.equals(full.entry().get().graveId()),
                "Full grave id lookup should find the exact grave.");
        helper.assertTrue(RecoveryWorldData.findLoaded(player, owner, "12345678").ambiguous(),
                "Shared grave id prefix should be reported as ambiguous.");
        helper.succeed();
    }

    private static void originalSlotRestore(GameTestHelper helper) {
        ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        owner.getInventory().clearContent();
        BlockPos local = new BlockPos(1, 1, 1);
        helper.setBlock(local.below(), Blocks.STONE);
        helper.setBlock(local, ModBlocks.GRAVE.get());
        GraveBlockEntity grave = helper.getBlockEntity(local, GraveBlockEntity.class);
        grave.setGraveId(UUID.randomUUID());
        grave.setOwner(owner.getUUID(), owner.getScoreboardName());
        grave.setCreatedAt(System.currentTimeMillis());
        grave.setDimension(owner.level().dimension().identifier().toString());
        grave.items().set(0, new ItemStack(Items.DIAMOND, 2));
        grave.setOriginalSlot(0, 5);

        helper.assertTrue(GraveManager.recoverGrave(grave, owner), "Recover all should succeed with an empty inventory.");
        ItemStack restored = owner.getInventory().getItem(5);
        helper.assertTrue(restored.is(Items.DIAMOND) && restored.getCount() == 2,
                "Recovered stack should return to the stored original inventory slot.");
        helper.succeed();
    }

    private static void localApiHooks(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        AtomicInteger events = new AtomicInteger();
        RecoveryIntegrations.registerEventHooks(new com.knoxhack.echorecovery.api.RecoveryEventHooks() {
            @Override
            public void graveCreated(ServerPlayer p, RecoveryGraveSnapshot snapshot) {
                if ("api-test".equals(snapshot.graveId())) {
                    events.incrementAndGet();
                }
            }
        });
        RecoveryIntegrations.graveCreated(player, new RecoveryGraveSnapshot("api-test", player.getUUID(),
                player.getScoreboardName(), player.blockPosition(), player.level().dimension().identifier().toString(),
                "echorecovery:vanilla_grave", 1, 0, 0L, 0L, false, false, false, false, List.of()));
        helper.assertTrue(events.get() == 1, "Local Recovery event hooks should receive grave creation events.");

        BlockPos origin = new BlockPos(5, 1, 5);
        BlockPos provided = new BlockPos(6, 1, 5);
        helper.setBlock(provided.below(), Blocks.STONE);
        helper.setBlock(provided, Blocks.AIR);
        RecoveryIntegrations.registerPlacementProvider((p, level, pos, cause) ->
                "api_test".equals(cause) ? Optional.of(helper.absolutePos(provided)) : Optional.empty());
        GraveManager.PlacementResult placement = GraveManager.findPlacement(player, (ServerLevel) player.level(),
                helper.absolutePos(origin), "api_test");
        helper.assertTrue(placement.pos().equals(helper.absolutePos(provided)),
                "Local placement providers should be able to supply a safe grave target.");
        helper.succeed();
    }

    private static RecoveryWorldData.GraveEntry entry(UUID graveId, UUID owner, String ownerName, BlockPos pos,
            ServerLevel level) {
        return new RecoveryWorldData.GraveEntry(graveId, owner, ownerName, pos, pos,
                level.dimension().identifier().toString(), level.dimension().identifier().toString(),
                System.currentTimeMillis(), 0L, "test", "test", "echorecovery:vanilla_grave",
                0, false, false, false, false, false, "", List.of(), List.of(), List.of());
    }

    private static void assertParseFails(GameTestHelper helper, Runnable body) {
        try {
            body.run();
            helper.fail("Expected Recovery JSON parser to reject malformed data.");
        } catch (RuntimeException expected) {
            // expected
        }
    }

    private static int countInventory(ServerPlayer player, Item item) {
        int count = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
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
            if (normalized.equals(EchoRecovery.MODID) || normalized.equals("*") || normalized.equalsIgnoreCase("all")) {
                return true;
            }
        }
        return false;
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoRecovery.MODID, path);
    }
}
