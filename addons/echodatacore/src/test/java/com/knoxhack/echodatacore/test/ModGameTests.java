package com.knoxhack.echodatacore.test;

import com.knoxhack.echocore.api.DataScope;
import com.knoxhack.echocore.api.DataChangeKind;
import com.knoxhack.echocore.api.DataKeyMetadata;
import com.knoxhack.echocore.api.DataValueKind;
import com.knoxhack.echocore.api.EchoDataBus;
import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.EchoServiceRegistry;
import com.knoxhack.echocore.api.EchoWorldRuntimeBus;
import com.knoxhack.echocore.api.EchoDiagnosticBlocker;
import com.knoxhack.echocore.api.IDataKey;
import com.knoxhack.echocore.api.NoOpDataService;
import com.knoxhack.echocore.api.index.IndexContentSnapshot;
import com.knoxhack.echocore.api.network.EchoPacketDirection;
import com.knoxhack.echocore.api.network.EchoPacketKind;
import com.knoxhack.echocore.api.network.PacketDebugHook;
import com.knoxhack.echocore.api.WorldDiscoverySource;
import com.knoxhack.echocore.api.WorldHazardSnapshot;
import com.knoxhack.echocore.api.WorldMarker;
import com.knoxhack.echocore.api.WorldMarkerType;
import com.knoxhack.echocore.api.WorldRegionInstance;
import com.knoxhack.echocore.api.WorldRegionType;
import com.knoxhack.echodatacore.Config;
import com.knoxhack.echodatacore.integration.DataCoreWorldCoreConsumer;
import com.knoxhack.echodatacore.integration.DataCoreDiagnostics;
import com.knoxhack.echodatacore.integration.DataCoreIndexProvider;
import com.knoxhack.echodatacore.content.DataCoreJsonReloadListener;
import com.knoxhack.echodatacore.DataCoreBuiltinKeys;
import com.knoxhack.echodatacore.DataCoreDataService;
import com.knoxhack.echodatacore.DataCoreWorldData;
import com.knoxhack.echodatacore.EchoDataCore;
import com.knoxhack.echodatacore.legacy.DataCoreLegacyAdapters;
import com.knoxhack.echodatacore.network.DataCoreMetadataSyncPacket;
import com.knoxhack.echodatacore.network.DataCoreSyncPacket;
import com.knoxhack.echonetcore.api.EchoNetSend;
import com.knoxhack.echonetcore.config.EchoNetCoreConfig;
import com.knoxhack.echonetcore.network.EchoNetDebug;
import net.minecraft.nbt.CompoundTag;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, EchoDataCore.MODID);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DATACORE_CONTRACTS =
            TEST_FUNCTIONS.register("datacore_contracts", () -> ModGameTests::dataCoreContracts);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> NETCORE_DIRTY_DATA_SYNC =
            TEST_FUNCTIONS.register("netcore_dirty_data_sync", () -> ModGameTests::netCoreDirtyDataSync);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> NETCORE_ACCEPTED_DATA_SYNC_RESULT =
            TEST_FUNCTIONS.register("netcore_accepted_data_sync_result", () -> ModGameTests::netCoreAcceptedDataSyncResult);

    private ModGameTests() {
    }

    public static void register(IEventBus eventBus) {
        TEST_FUNCTIONS.register(eventBus);
    }

    public static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(id("datacore_release"));
        register(event, environment, "datacore_contracts", DATACORE_CONTRACTS.getId());
        register(event, environment, "netcore_dirty_data_sync", NETCORE_DIRTY_DATA_SYNC.getId());
        register(event, environment, "netcore_accepted_data_sync_result", NETCORE_ACCEPTED_DATA_SYNC_RESULT.getId());
    }

    private static void dataCoreContracts(GameTestHelper helper) {
        noOpFallback(helper);
        duplicateKeyRegistration(helper);
        scopeMismatchRejection(helper);
        datapackMetadataParser(helper);
        metadataSyncCache(helper);
        playerWorldTeamValues(helper);
        worldDataV2TeamMigration(helper);
        migrationAndTerminalProbe(helper);
        legacyReadThrough(helper);
        legacyMetadataRootField(helper);
        clientSyncTombstoneRemoval(helper);
        indexProviderSnapshot(helper);
        diagnosticProviderOutput(helper);
        runtimeBus(helper);
        worldRuntimeBusConsumer(helper);
        helper.succeed();
    }

    private static void noOpFallback(GameTestHelper helper) {
        EchoServiceRegistry.withClearedForTests(() -> {
            NoOpDataService.INSTANCE.clearRegisteredKeysForTests();
            IDataKey<String> key = IDataKey.string(id("test/noop"), DataScope.PLAYER, "default", true);
            helper.assertTrue(EchoCoreServices.registerDataKey(key) == key,
                    "NoOp data service should retain key metadata.");
            helper.assertTrue("default".equals(EchoCoreServices.playerData(null).get(key)),
                    "NoOp reads should return defaults.");
            helper.assertFalse(EchoCoreServices.playerData(null).set(key, "changed"),
                    "NoOp writes should fail safely.");
            helper.assertTrue(EchoCoreServices.platformProviderSummary().contains("dataKeys=1"),
                    "Provider summary should expose registered NoOp data keys.");
            helper.assertFalse(EchoCoreServices.dataService().diagnostics().available(),
                    "NoOp diagnostics should report unavailable backend.");
            helper.assertTrue(EchoCoreServices.dataService().allKeyMetadata().containsKey(key.id()),
                    "NoOp metadata should expose registered fallback keys.");
        });
    }

    private static void duplicateKeyRegistration(GameTestHelper helper) {
        Identifier duplicateId = id("test/duplicate");
        IDataKey<Long> first = IDataKey.counter(duplicateId, DataScope.PLAYER, 7L, true);
        IDataKey<Boolean> second = IDataKey.flag(duplicateId, DataScope.WORLD, false, true);
        IDataKey<Long> registeredFirst = DataCoreDataService.INSTANCE.registerKey(first);
        IDataKey<Boolean> registeredSecond = DataCoreDataService.INSTANCE.registerKey(second);
        helper.assertTrue(registeredFirst == (Object) registeredSecond,
                "Duplicate key registration should keep the first definition.");
        helper.assertTrue(DataCoreDataService.INSTANCE.key(duplicateId).orElseThrow().scope() == DataScope.PLAYER,
                "Duplicate key scope should remain from the first definition.");
    }

    private static void scopeMismatchRejection(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        IDataKey<Boolean> worldFlag = IDataKey.flag(id("test/scope_world_flag"), DataScope.WORLD, false, true);
        DataCoreDataService.INSTANCE.registerKey(worldFlag);
        helper.assertFalse(DataCoreDataService.INSTANCE.player(player).set(worldFlag, true),
                "Player view should reject world-scoped writes.");
        helper.assertFalse(DataCoreDataService.INSTANCE.player(player).get(worldFlag),
                "Player view should return the world key default after scope rejection.");
        helper.assertFalse(player.getPersistentData().getCompoundOrEmpty(DataCoreDataService.PLAYER_ROOT)
                        .getCompoundOrEmpty("values").contains(worldFlag.id().toString()),
                "Rejected scoped write should not touch player persistent data.");
    }

    private static void datapackMetadataParser(GameTestHelper helper) {
        DataKeyMetadata meta = DataCoreJsonReloadListener.parseKeyForTests(id("json/parser_flag"),
                JsonParser.parseString("""
                        {"scope":"player","kind":"flag","default":true,"synced":true,
                        "title":"Parser Flag","description":"Parser metadata test.","owner":"echodatacore"}
                        """).getAsJsonObject());
        DataCoreDataService.INSTANCE.registerMetadata(meta, true);
        helper.assertTrue(DataCoreDataService.INSTANCE.key(meta.id()).isPresent(),
                "Datapack metadata should register simple keys.");
        helper.assertTrue(DataCoreDataService.INSTANCE.keyMetadata(meta.id()).orElseThrow().title().equals("Parser Flag"),
                "Datapack metadata should enrich key title.");
        helper.assertTrue(DataCoreDataService.INSTANCE.key(meta.id()).orElseThrow().defaultValue().equals(true),
                "Datapack default should be applied to simple JSON keys.");

        IDataKey<Boolean> javaKey = IDataKey.flag(id("json/java_conflict"), DataScope.PLAYER, false, true);
        DataCoreDataService.INSTANCE.registerKey(javaKey);
        DataCoreDataService.INSTANCE.registerMetadata(new DataKeyMetadata(javaKey.id(), DataScope.WORLD,
                DataValueKind.COUNTER, true, "Wrong Contract", "", "echodatacore",
                "", "", "99", "datapack:test/conflict"), true);
        helper.assertTrue(DataCoreDataService.INSTANCE.key(javaKey.id()).orElseThrow().scope() == DataScope.PLAYER,
                "Java key scope should remain authoritative over JSON metadata.");
        helper.assertTrue(DataCoreDataService.INSTANCE.key(javaKey.id()).orElseThrow().kind() == DataValueKind.FLAG,
                "Java key kind should remain authoritative over JSON metadata.");

        Identifier staleId = id("json/stale_metadata");
        DataCoreDataService.INSTANCE.replaceDatapackMetadata(Map.of(staleId,
                new DataKeyMetadata(staleId, DataScope.PLAYER, DataValueKind.STRING, true,
                        "Stale", "", "echodatacore", "", "", "old", "datapack:test/stale")));
        helper.assertTrue(DataCoreDataService.INSTANCE.allKeyMetadata().containsKey(staleId),
                "Datapack metadata should register during reload.");
        DataCoreDataService.INSTANCE.replaceDatapackMetadata(Map.of());
        helper.assertFalse(DataCoreDataService.INSTANCE.allKeyMetadata().containsKey(staleId),
                "Stale datapack-only metadata should be removed on reload.");

        Identifier recordId = id("json/record_without_codec");
        DataCoreDataService.INSTANCE.replaceDatapackMetadata(Map.of(recordId,
                new DataKeyMetadata(recordId, DataScope.PLAYER, DataValueKind.RECORD, true,
                        "Record", "", "echodatacore", "", "", "{\"x\":1}", "datapack:test/record")));
        helper.assertTrue(DataCoreDataService.INSTANCE.key(recordId).isEmpty(),
                "JSON RECORD metadata should not register a key without a Java codec.");
    }

    private static void metadataSyncCache(GameTestHelper helper) {
        Identifier syncedId = id("client/metadata_synced");
        Identifier serverOnlyId = id("client/metadata_server_only");
        DataKeyMetadata synced = new DataKeyMetadata(syncedId, DataScope.PLAYER, DataValueKind.FLAG, true,
                "Synced Metadata", "Client metadata sync test.", "echodatacore",
                "legacy_root", "legacy_field", "false", "test");
        DataKeyMetadata serverOnly = new DataKeyMetadata(serverOnlyId, DataScope.PLAYER, DataValueKind.STRING, false,
                "Server Only Metadata", "", "echodatacore", "", "", "", "test");
        DataCoreDataService.INSTANCE.applyClientMetadataSync(new DataCoreMetadataSyncPacket(99L, List.of(synced)));
        helper.assertTrue(DataCoreDataService.INSTANCE.keyMetadata(syncedId).orElseThrow().title().equals("Synced Metadata"),
                "Client metadata sync should populate metadata lookup.");
        helper.assertTrue(DataCoreDataService.INSTANCE.allKeyMetadata().containsKey(syncedId),
                "Client metadata sync should merge into all metadata.");
        helper.assertFalse(DataCoreDataService.INSTANCE.allKeyMetadata().containsKey(serverOnlyId),
                "Unsynced metadata should not appear unless a packet includes it.");
        DataCoreDataService.INSTANCE.applyClientMetadataSync(new DataCoreMetadataSyncPacket(100L, List.of()));
        helper.assertFalse(DataCoreDataService.INSTANCE.allKeyMetadata().containsKey(syncedId),
                "An empty metadata packet should replace the client metadata cache.");
    }

    private static void playerWorldTeamValues(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        IDataKey<Boolean> flag = IDataKey.flag(id("test/player_flag"), DataScope.PLAYER, false, true);
        IDataKey<Long> counter = IDataKey.counter(id("test/world_counter"), DataScope.WORLD, 0L, true);
        IDataKey<String> enumName = IDataKey.enumName(id("test/team_mode"), DataScope.TEAM, "idle", true);
        IDataKey<CompoundTag> record = IDataKey.record(id("test/player_record"),
                DataScope.PLAYER, CompoundTag.CODEC, new CompoundTag(), true);

        DataCoreDataService.INSTANCE.registerKey(flag);
        DataCoreDataService.INSTANCE.registerKey(counter);
        DataCoreDataService.INSTANCE.registerKey(enumName);
        DataCoreDataService.INSTANCE.registerKey(record);

        int dirtyBefore = DataCoreDataService.INSTANCE.debugDirtyPlayerKeyCount(player.getUUID());
        long revisionBefore = DataCoreDataService.INSTANCE.syncBridge().revision();
        helper.assertTrue(DataCoreDataService.INSTANCE.player(player).set(flag, true),
                "Changed player flag should write.");
        helper.assertTrue(DataCoreDataService.INSTANCE.player(player).get(flag),
                "Player flag should read back.");
        helper.assertTrue(DataCoreDataService.INSTANCE.debugDirtyPlayerKeyCount(player.getUUID()) == dirtyBefore + 1,
                "Repeated dirty writes to the same key should coalesce by key id.");
        long revisionAfter = DataCoreDataService.INSTANCE.syncBridge().revision();
        helper.assertTrue(revisionAfter > revisionBefore, "Changed write should advance revision.");
        helper.assertFalse(DataCoreDataService.INSTANCE.player(player).set(flag, true),
                "Identical player flag write should not dirty.");
        helper.assertTrue(DataCoreDataService.INSTANCE.syncBridge().revision() == revisionAfter,
                "Identical write should not advance revision.");

        CompoundTag storedRecord = new CompoundTag();
        storedRecord.putString("mode", "scan");
        helper.assertTrue(DataCoreDataService.INSTANCE.player(player).set(record, storedRecord),
                "Structured record should write.");
        helper.assertTrue("scan".equals(DataCoreDataService.INSTANCE.player(player).get(record).getStringOr("mode", "")),
                "Structured record should read back.");
        helper.assertTrue(player.getPersistentData().getCompoundOrEmpty(DataCoreDataService.PLAYER_ROOT)
                        .getCompoundOrEmpty("values").contains(flag.id().toString()),
                "Player values should be stored under the DataCore root.");

        helper.assertTrue(DataCoreDataService.INSTANCE.world(helper.getLevel()).set(counter, 12L),
                "World counter should write.");
        helper.assertTrue(DataCoreDataService.INSTANCE.world(helper.getLevel()).get(counter) == 12L,
                "World counter should read back.");
        helper.assertTrue(DataCoreDataService.INSTANCE.dirtyOwnerCounts().getOrDefault("worldOwners", 0) >= 1,
                "World dirty writes should remain queued until the configured sync interval.");
        Identifier teamId = id("team/release_test");
        helper.assertTrue(DataCoreDataService.INSTANCE.team(helper.getLevel(), teamId).set(enumName, "active"),
                "Team enum should write.");
        helper.assertTrue("active".equals(DataCoreDataService.INSTANCE.team(helper.getLevel(), teamId).get(enumName)),
                "Team enum should read back.");
        helper.assertTrue(DataCoreDataService.INSTANCE.dirtyOwnerCounts().getOrDefault("teamOwners", 0) >= 1,
                "Team dirty writes should remain queued until the configured sync interval.");
    }

    private static void worldDataV2TeamMigration(GameTestHelper helper) {
        CompoundTag legacyEntry = new CompoundTag();
        legacyEntry.putString("kind", "STRING");
        legacyEntry.putString("value", "active");
        java.util.Map<String, CompoundTag> legacyTeamValues = java.util.Map.of(
                id("team/legacy").toString() + "|" + id("test/legacy_team_mode"), legacyEntry);
        DataCoreWorldData migrated = DataCoreWorldData.CODEC.decode(
                net.minecraft.nbt.NbtOps.INSTANCE,
                legacyWorldDataTag(legacyTeamValues)).result().orElseThrow().getFirst();
        helper.assertTrue("active".equals(migrated.teamValue(id("team/legacy"),
                        id("test/legacy_team_mode").toString()).getStringOr("value", "")),
                "V1 delimiter team values should migrate into v2 team groups.");
        helper.assertTrue(migrated.migrations().getOrDefault(DataCoreWorldData.MIGRATION_V1_TEAM_VALUES, 0)
                        == DataCoreDataService.CURRENT_VERSION,
                "V1 team migration should record a migration marker.");
    }

    private static void migrationAndTerminalProbe(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        CompoundTag root = new CompoundTag();
        root.putInt("version", 0);
        player.getPersistentData().put(DataCoreDataService.PLAYER_ROOT, root);

        DataCoreDataService.INSTANCE.onPlayerLogin(player);
        CompoundTag migrated = player.getPersistentData().getCompoundOrEmpty(DataCoreDataService.PLAYER_ROOT);
        helper.assertTrue(migrated.getIntOr("version", 0) == DataCoreDataService.CURRENT_VERSION,
                "Player migration should advance the DataCore root version.");
        helper.assertTrue(migrated.getCompoundOrEmpty("migrations")
                        .getIntOr(EchoDataCore.MODID, 0) == DataCoreDataService.CURRENT_VERSION,
                "Player migration should record the DataCore namespace version.");
        helper.assertTrue("online".equals(DataCoreDataService.INSTANCE.player(player)
                        .get(com.knoxhack.echodatacore.DataCoreBuiltinKeys.TERMINAL_PROBE)),
                "Login should expose the Terminal probe value.");

        CompoundTag firstPass = migrated.copy();
        DataCoreDataService.INSTANCE.onPlayerLogin(player);
        CompoundTag secondPass = player.getPersistentData().getCompoundOrEmpty(DataCoreDataService.PLAYER_ROOT);
        helper.assertTrue(firstPass.equals(secondPass),
                "Repeated migration/login writes should be idempotent when values are unchanged.");
    }

    private static void legacyReadThrough(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        CompoundTag legacy = new CompoundTag();
        legacy.putBoolean("telemetry_tier", true);
        player.getPersistentData().put("echoorbitalremnants_progress", legacy.copy());

        IDataKey<Boolean> legacyKey = IDataKey.flag(
                Identifier.fromNamespaceAndPath("echoorbitalremnants", "unlock/telemetry_tier"),
                DataScope.PLAYER,
                false,
                true);
        DataCoreDataService.INSTANCE.registerKey(legacyKey);
        helper.assertTrue(DataCoreDataService.INSTANCE.player(player).get(legacyKey),
                "Legacy adapter should read existing Orbital progress.");
        helper.assertTrue(legacy.equals(player.getPersistentData().getCompoundOrEmpty("echoorbitalremnants_progress")),
                "Legacy adapter reads should not modify the old save root.");
        DataCoreLegacyAdapters.MigrationReport preview = DataCoreLegacyAdapters.preview(player, "echoorbitalremnants");
        helper.assertTrue(preview.candidates() == 1 && preview.applied() == 0,
                "Migration preview should count candidates without applying values.");
        DataCoreLegacyAdapters.MigrationReport applied = DataCoreLegacyAdapters.apply(player, "echoorbitalremnants");
        helper.assertTrue(applied.applied() == 1 && applied.failedDecode() == 0,
                "Migration apply should copy a decoded legacy value into DataCore storage.");
        DataCoreLegacyAdapters.MigrationReport secondApply = DataCoreLegacyAdapters.apply(player, "echoorbitalremnants");
        helper.assertTrue(secondApply.alreadyMirrored() == 1 && secondApply.applied() == 0,
                "Repeated migration apply should report already mirrored values.");
        helper.assertTrue(DataCoreDataService.INSTANCE.hasStoredPlayerValue(player, legacyKey.id()),
                "Migration apply should create an explicit DataCore value even while legacy read-through remains available.");
    }

    private static void legacyMetadataRootField(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        CompoundTag legacy = new CompoundTag();
        legacy.putLong("custom_count", 42L);
        player.getPersistentData().put("custom_progress_root", legacy.copy());

        Identifier keyId = id("legacy/custom_counter");
        IDataKey<Long> key = IDataKey.counter(keyId, DataScope.PLAYER, 0L, true);
        DataCoreDataService.INSTANCE.registerKey(key);
        DataCoreDataService.INSTANCE.registerMetadata(new DataKeyMetadata(keyId, DataScope.PLAYER,
                DataValueKind.COUNTER, true, "Custom Legacy Counter", "", "echodatacore",
                "custom_progress_root", "custom_count", "0", "test"), false);

        helper.assertTrue(DataCoreDataService.INSTANCE.player(player).get(key) == 42L,
                "Legacy adapter should prefer metadata legacyRoot/legacyField over path heuristics.");
        DataCoreLegacyAdapters.MigrationReport report = DataCoreLegacyAdapters.apply(player, "echodatacore");
        helper.assertTrue(report.values().containsKey(keyId),
                "Migration report should include metadata-root legacy candidates.");
        helper.assertTrue("custom_progress_root".equals(report.details().get(keyId).legacyRoot()),
                "Migration report should expose candidate root details.");
        helper.assertTrue(legacy.equals(player.getPersistentData().getCompoundOrEmpty("custom_progress_root")),
                "Metadata-driven migration should not rewrite legacy roots.");
    }

    private static void indexProviderSnapshot(GameTestHelper helper) {
        Identifier keyId = id("index/catalog_flag");
        DataCoreDataService.INSTANCE.registerMetadata(new DataKeyMetadata(keyId, DataScope.PLAYER,
                DataValueKind.FLAG, true, "Catalog Flag", "", "echodatacore",
                "legacy_root", "legacy_flag", "false", "test"), true);
        IndexContentSnapshot snapshot = DataCoreIndexProvider.INSTANCE.snapshot(null);
        helper.assertTrue(snapshot.entries().stream()
                        .anyMatch(entry -> entry.subtitleKey().equals(keyId.toString())),
                "DataCore Index provider should publish key metadata as catalog entries.");
        helper.assertTrue(snapshot.sourceFacts().stream()
                        .anyMatch(fact -> fact.sourceId().equals(keyId)),
                "DataCore Index provider should publish key metadata as source facts.");
    }

    private static void diagnosticProviderOutput(GameTestHelper helper) {
        List<EchoDiagnosticBlocker> diagnostics = DataCoreDiagnostics.INSTANCE.diagnostics(null);
        helper.assertTrue(diagnostics.stream().anyMatch(row -> row.id().getPath().contains("backend")),
                "DataCore diagnostics should report backend status.");
        helper.assertTrue(diagnostics.stream().anyMatch(row -> row.id().getPath().contains("sync_pressure")),
                "DataCore diagnostics should report sync pressure.");
    }

    private static void clientSyncTombstoneRemoval(GameTestHelper helper) {
        Identifier owner = id("client/snapshot_owner");
        Identifier key = id("client/tombstone_flag");
        CompoundTag stored = new CompoundTag();
        stored.putString("kind", DataValueKind.FLAG.name());
        stored.putBoolean("value", true);
        DataCoreDataService.INSTANCE.applyClientSync(new DataCoreSyncPacket(DataScope.PLAYER, owner.toString(),
                true, 1L, List.of(new DataCoreSyncPacket.Entry(key, DataValueKind.FLAG, stored))));
        helper.assertTrue(DataCoreDataService.INSTANCE.debugClientSnapshot(DataScope.PLAYER, owner.toString()).containsKey(key),
                "Client full snapshot should store synced values.");
        DataCoreDataService.INSTANCE.applyClientSync(new DataCoreSyncPacket(DataScope.PLAYER, owner.toString(),
                false, 2L, List.of(new DataCoreSyncPacket.Entry(key, DataValueKind.FLAG, new CompoundTag(), true))));
        helper.assertFalse(DataCoreDataService.INSTANCE.debugClientSnapshot(DataScope.PLAYER, owner.toString()).containsKey(key),
                "Client tombstone packet should remove cleared synced values.");
        DataCoreDataService.INSTANCE.applyClientSync(new DataCoreSyncPacket(DataScope.PLAYER, owner.toString(),
                true, 3L, List.of()));
        helper.assertTrue(DataCoreDataService.INSTANCE.debugClientSnapshot(DataScope.PLAYER, owner.toString()).isEmpty(),
                "Empty full snapshot should leave the client owner cache empty.");
    }

    private static void runtimeBus(GameTestHelper helper) {
        IDataKey<Boolean> flag = IDataKey.flag(id("test/bus_flag"), DataScope.PLAYER, false, true);
        DataCoreDataService.INSTANCE.registerKey(flag);
        helper.assertFalse(DataCoreDataService.INSTANCE.player(null).get(flag),
                "Null player reads should return the key default.");
        helper.assertFalse(DataCoreDataService.INSTANCE.player(null).set(flag, true),
                "Null player writes should fail safely.");

        AtomicInteger events = new AtomicInteger();
        AtomicInteger clears = new AtomicInteger();
        try {
            AutoCloseable ignored = EchoDataBus.subscribe(message -> {
                if (flag.id().equals(message.keyId())) {
                    events.incrementAndGet();
                    if (message.changeKind() == DataChangeKind.CLEAR) {
                        clears.incrementAndGet();
                    }
                }
            });
            DataCoreDataService.INSTANCE.syncBridge().markDirty(DataScope.PLAYER, "test-player", flag.id());
            ServerPlayer player = helper.makeMockServerPlayerInLevel();
            DataCoreDataService.INSTANCE.player(player).set(flag, true);
            DataCoreDataService.INSTANCE.player(player).clear(flag);
            ignored.close();
        } catch (Exception exception) {
            throw new AssertionError("Data bus listener should close cleanly.", exception);
        }
        helper.assertTrue(events.get() >= 3, "Data bus should publish set and clear messages.");
        helper.assertTrue(clears.get() == 1, "Data bus should publish one clear message.");
    }

    private static void worldRuntimeBusConsumer(GameTestHelper helper) {
        EchoWorldRuntimeBus.clearForTests();
        DataCoreWorldCoreConsumer.registerForTests();
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Identifier regionId = Identifier.fromNamespaceAndPath("echoashfallprotocol", "crash_zone_wasteland");
        WorldRegionInstance region = new WorldRegionInstance(id("runtime/region"), regionId,
                WorldRegionType.CRASH_ZONE, "Crash Zone", player.level().dimension(),
                BlockPos.ZERO, 96, List.of(id("hazard/radiation")), true);
        WorldMarker marker = new WorldMarker(id("runtime/marker"), regionId, WorldMarkerType.CRASH_SITE,
                "Runtime Marker", "Runtime marker.", player.level().dimension(), BlockPos.ZERO,
                32, true, player.level().getGameTime());

        EchoWorldRuntimeBus.fireRegionEntered(new EchoWorldRuntimeBus.RegionEntered(player, region));
        helper.assertTrue(regionId.toString().equals(DataCoreDataService.INSTANCE.player(player)
                        .get(DataCoreBuiltinKeys.WORLDCORE_LAST_REGION)),
                "WorldCore region enter should update DataCore last-region state.");
        EchoWorldRuntimeBus.fireRegionDiscovered(new EchoWorldRuntimeBus.RegionDiscovered(
                player, region, WorldDiscoverySource.ENTER, true));
        helper.assertTrue(DataCoreDataService.INSTANCE.player(player)
                        .get(DataCoreBuiltinKeys.WORLDCORE_REGION_DISCOVERIES) == 1L,
                "WorldCore first discovery should increment DataCore discovery count.");
        EchoWorldRuntimeBus.fireMarkerRevealed(new EchoWorldRuntimeBus.MarkerRevealed(player, marker));
        helper.assertTrue(marker.id().toString().equals(DataCoreDataService.INSTANCE.player(player)
                        .get(DataCoreBuiltinKeys.WORLDCORE_LAST_MARKER)),
                "WorldCore marker reveal should update DataCore marker state.");
        EchoWorldRuntimeBus.fireHazardChanged(new EchoWorldRuntimeBus.HazardChanged(player,
                WorldHazardSnapshot.nominal(),
                new WorldHazardSnapshot(List.of(regionId), List.of(id("hazard/radiation")), 41, false, "Radiation")));
        helper.assertTrue(DataCoreDataService.INSTANCE.player(player)
                        .get(DataCoreBuiltinKeys.WORLDCORE_ACTIVE_HAZARD_SEVERITY) == 41L,
                "WorldCore hazard changes should update DataCore hazard severity.");
        EchoWorldRuntimeBus.clearForTests();
    }

    private static void netCoreDirtyDataSync(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        IDataKey<String> key = IDataKey.string(id("runtime_spine/netcore_dirty_sync_smoke"),
                DataScope.PLAYER, "", true);
        DataCoreDataService.INSTANCE.registerKey(key);

        AtomicInteger acceptedSyncPackets = new AtomicInteger();
        AtomicInteger failedSyncPackets = new AtomicInteger();
        PacketDebugHook hook = event -> {
            if (DataCoreSyncPacket.ID.equals(event.payloadId())
                    && event.direction() == EchoPacketDirection.CLIENTBOUND
                    && event.kind() == EchoPacketKind.CLIENTBOUND_SYNC) {
                if (event.accepted()) {
                    acceptedSyncPackets.incrementAndGet();
                } else {
                    failedSyncPackets.incrementAndGet();
                }
            }
        };

        boolean previousDebugLogging = EchoNetCoreConfig.DEBUG_PACKET_LOGGING.get();
        boolean previousDroppedLogging = EchoNetCoreConfig.LOG_DROPPED_PACKETS.get();
        boolean previousDebugPackets = EchoNetCoreConfig.ENABLE_DEBUG_PACKETS.get();
        int previousSyncInterval = Config.SYNC_INTERVAL_TICKS.get();
        try {
            EchoNetDebug.clearCountersForTests();
            EchoNetCoreConfig.DEBUG_PACKET_LOGGING.set(true);
            EchoNetCoreConfig.LOG_DROPPED_PACKETS.set(true);
            EchoNetCoreConfig.ENABLE_DEBUG_PACKETS.set(true);
            Config.SYNC_INTERVAL_TICKS.set(1);
            EchoNetDebug.HOOKS.add(hook);

            int dirtyBefore = DataCoreDataService.INSTANCE.debugDirtyPlayerKeyCount(player.getUUID());
            long revisionBefore = DataCoreDataService.INSTANCE.syncBridge().revision();
            helper.assertTrue(DataCoreDataService.INSTANCE.player(player).set(key, "netcore-sync-" + player.getUUID()),
                    "Runtime DataCore mutation should dirty a synced player key.");
            int dirtyAfterMutation = DataCoreDataService.INSTANCE.debugDirtyPlayerKeyCount(player.getUUID());
            helper.assertTrue(dirtyAfterMutation == dirtyBefore + 1,
                    "Runtime DataCore mutation should queue exactly one dirty player key.");

            DataCoreDataService.INSTANCE.onPlayerTick(player);

            helper.assertTrue(DataCoreDataService.INSTANCE.syncBridge().revision() > revisionBefore,
                    "Runtime DataCore mutation should advance the sync revision.");
            int dirtyAfterFlush = DataCoreDataService.INSTANCE.debugDirtyPlayerKeyCount(player.getUUID());
            boolean sent = acceptedSyncPackets.get() >= 1;
            boolean unavailable = failedSyncPackets.get() >= 1;
            helper.assertTrue(sent || unavailable,
                    "NetCore should report a concrete send result for DataCore dirty runtime data.");
            helper.assertFalse(sent && unavailable,
                    "One DataCore dirty runtime sync flush should not report both accepted and failed sends.");
            if (sent) {
                helper.assertTrue(dirtyAfterFlush < dirtyAfterMutation,
                        "Accepted NetCore dirty sync should drain the queued player key.");
            } else {
                helper.assertTrue(dirtyAfterFlush == dirtyAfterMutation,
                        "DataCore should retain dirty runtime keys when NetCore reports an unavailable client channel.");
            }
            helper.assertTrue(EchoNetDebug.counterSnapshot().entrySet().stream()
                            .anyMatch(entry -> DataCoreSyncPacket.ID.equals(entry.getKey().payloadId())
                                    && entry.getKey().direction() == EchoPacketDirection.CLIENTBOUND
                                    && entry.getKey().kind() == EchoPacketKind.CLIENTBOUND_SYNC
                                    && entry.getKey().accepted() == sent
                                    && entry.getValue() >= 1L),
                    "NetCore packet counters should record the DataCore sync send result.");
        } finally {
            EchoNetDebug.HOOKS.remove(hook);
            EchoNetDebug.clearCountersForTests();
            EchoNetCoreConfig.DEBUG_PACKET_LOGGING.set(previousDebugLogging);
            EchoNetCoreConfig.LOG_DROPPED_PACKETS.set(previousDroppedLogging);
            EchoNetCoreConfig.ENABLE_DEBUG_PACKETS.set(previousDebugPackets);
            Config.SYNC_INTERVAL_TICKS.set(previousSyncInterval);
        }
        helper.succeed();
    }

    private static void netCoreAcceptedDataSyncResult(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        IDataKey<String> key = IDataKey.string(id("runtime_spine/netcore_accepted_sync_smoke"),
                DataScope.PLAYER, "", true);
        DataCoreDataService.INSTANCE.registerKey(key);

        List<DataCoreSyncPacket> deliveredPackets = new ArrayList<>();
        AtomicInteger acceptedSyncPackets = new AtomicInteger();
        AtomicInteger failedSyncPackets = new AtomicInteger();
        PacketDebugHook hook = event -> {
            if (DataCoreSyncPacket.ID.equals(event.payloadId())
                    && event.direction() == EchoPacketDirection.CLIENTBOUND
                    && event.kind() == EchoPacketKind.CLIENTBOUND_SYNC) {
                if (event.accepted()) {
                    acceptedSyncPackets.incrementAndGet();
                } else {
                    failedSyncPackets.incrementAndGet();
                }
            }
        };

        boolean previousDebugLogging = EchoNetCoreConfig.DEBUG_PACKET_LOGGING.get();
        boolean previousDroppedLogging = EchoNetCoreConfig.LOG_DROPPED_PACKETS.get();
        boolean previousDebugPackets = EchoNetCoreConfig.ENABLE_DEBUG_PACKETS.get();
        int previousSyncInterval = Config.SYNC_INTERVAL_TICKS.get();
        EchoNetSend.TestSendOverrideHandle sendOverride = EchoNetSend.installSendOverrideForTests((target, payload, kind) -> {
            if (target == player
                    && kind == EchoPacketKind.CLIENTBOUND_SYNC
                    && payload instanceof DataCoreSyncPacket packet) {
                deliveredPackets.add(packet);
                return Optional.of(true);
            }
            return Optional.empty();
        });
        try {
            EchoNetDebug.clearCountersForTests();
            EchoNetCoreConfig.DEBUG_PACKET_LOGGING.set(true);
            EchoNetCoreConfig.LOG_DROPPED_PACKETS.set(true);
            EchoNetCoreConfig.ENABLE_DEBUG_PACKETS.set(true);
            Config.SYNC_INTERVAL_TICKS.set(1);
            EchoNetDebug.HOOKS.add(hook);

            int dirtyBefore = DataCoreDataService.INSTANCE.debugDirtyPlayerKeyCount(player.getUUID());
            long revisionBefore = DataCoreDataService.INSTANCE.syncBridge().revision();
            String value = "netcore-accepted-sync-" + player.getUUID();
            helper.assertTrue(DataCoreDataService.INSTANCE.player(player).set(key, value),
                    "Accepted-branch DataCore mutation should dirty a synced player key.");
            int dirtyAfterMutation = DataCoreDataService.INSTANCE.debugDirtyPlayerKeyCount(player.getUUID());
            helper.assertTrue(dirtyAfterMutation == dirtyBefore + 1,
                    "Accepted-branch DataCore mutation should queue exactly one dirty player key.");

            DataCoreDataService.INSTANCE.onPlayerTick(player);

            helper.assertTrue(DataCoreDataService.INSTANCE.syncBridge().revision() > revisionBefore,
                    "Accepted-branch DataCore mutation should advance the sync revision.");
            helper.assertTrue(!deliveredPackets.isEmpty(),
                    "Accepted NetCore dirty sync should expose the DataCore sync packet to the send path.");
            helper.assertTrue(deliveredPackets.stream()
                            .anyMatch(packet -> packet.entries().stream().anyMatch(entry -> key.id().equals(entry.keyId()))),
                    "Accepted NetCore dirty sync should report the changed DataCore key through the send path.");
            helper.assertTrue(acceptedSyncPackets.get() >= 1,
                    "Accepted NetCore dirty sync should report at least one accepted clientbound result.");
            helper.assertTrue(DataCoreDataService.INSTANCE.debugDirtyPlayerKeyCount(player.getUUID()) < dirtyAfterMutation,
                    "Accepted NetCore dirty sync should drain the queued player key.");

            DataCoreSyncPacket delivered = deliveredPackets.stream()
                    .filter(packet -> packet.entries().stream().anyMatch(entry -> key.id().equals(entry.keyId())))
                    .findFirst()
                    .orElseThrow();
            DataCoreDataService.INSTANCE.applyClientSync(delivered);
            Map<Identifier, String> clientSnapshot = DataCoreDataService.INSTANCE.debugClientSnapshot(
                    DataScope.PLAYER, player.getUUID().toString());
            helper.assertTrue(clientSnapshot.containsKey(key.id()) && clientSnapshot.get(key.id()).contains(value),
                    "Accepted NetCore payload should be consumable by the DataCore client cache.");
            helper.assertTrue(EchoNetDebug.counterSnapshot().entrySet().stream()
                            .anyMatch(entry -> DataCoreSyncPacket.ID.equals(entry.getKey().payloadId())
                                    && entry.getKey().direction() == EchoPacketDirection.CLIENTBOUND
                                    && entry.getKey().kind() == EchoPacketKind.CLIENTBOUND_SYNC
                                    && entry.getKey().accepted()
                                    && entry.getValue() >= 1L),
                    "NetCore counters should record the accepted DataCore sync result.");
        } finally {
            sendOverride.close();
            EchoNetDebug.HOOKS.remove(hook);
            EchoNetDebug.clearCountersForTests();
            EchoNetCoreConfig.DEBUG_PACKET_LOGGING.set(previousDebugLogging);
            EchoNetCoreConfig.LOG_DROPPED_PACKETS.set(previousDroppedLogging);
            EchoNetCoreConfig.ENABLE_DEBUG_PACKETS.set(previousDebugPackets);
            Config.SYNC_INTERVAL_TICKS.set(previousSyncInterval);
        }
        helper.succeed();
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
                2);
        event.registerTest(id(testName),
                new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, functionId), data));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoDataCore.MODID, path);
    }

    private static CompoundTag legacyWorldDataTag(java.util.Map<String, CompoundTag> legacyTeamValues) {
        CompoundTag root = new CompoundTag();
        root.putInt("version", 1);
        CompoundTag teams = new CompoundTag();
        legacyTeamValues.forEach(teams::put);
        root.put("teamValues", teams);
        return root;
    }
}
