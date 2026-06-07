package com.knoxhack.echobasegrid.test;

import com.google.gson.JsonElement;
import com.knoxhack.echobasegrid.EchoBaseGrid;
import com.knoxhack.echobasegrid.api.ClaimActionResult;
import com.knoxhack.echobasegrid.api.ClaimMember;
import com.knoxhack.echobasegrid.api.ClaimPermission;
import com.knoxhack.echobasegrid.api.ClaimRecord;
import com.knoxhack.echobasegrid.api.ClaimRole;
import com.knoxhack.echobasegrid.command.BaseGridCommands;
import com.knoxhack.echobasegrid.client.BaseGridClientState;
import com.knoxhack.echobasegrid.client.BaseGridDataProviders;
import com.knoxhack.echobasegrid.data.BaseGridSavedData;
import com.knoxhack.echobasegrid.integration.holomap.BaseGridHoloMapIntegration;
import com.knoxhack.echobasegrid.network.BaseGridClaimActionPacket;
import com.knoxhack.echobasegrid.network.BaseGridSnapshotPacket;
import com.knoxhack.echobasegrid.service.BaseGridClaimService;
import com.knoxhack.echoscreencore.api.EchoDataContext;
import com.knoxhack.echoholomap.api.HoloMapQuery;
import com.knoxhack.echoholomap.map.HoloMapService;
import com.knoxhack.echoholomap.map.HoloMapTerrainTile;
import com.knoxhack.echoholomap.world.HoloMapTerrainSavedData;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.buffer.Unpooled;
import com.mojang.brigadier.CommandDispatcher;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.Connection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, EchoBaseGrid.MODID);

    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CLAIM_RECORD_PERMISSIONS =
            TEST_FUNCTIONS.register("claim_record_permissions", () -> ModGameTests::claimRecordPermissions);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SAVED_DATA_CODEC =
            TEST_FUNCTIONS.register("saved_data_codec", () -> ModGameTests::savedDataCodec);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MALFORMED_ACTION_PACKET =
            TEST_FUNCTIONS.register("malformed_action_packet", () -> ModGameTests::malformedActionPacket);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SCREENCORE_PROVIDER_ROWS =
            TEST_FUNCTIONS.register("screencore_provider_rows", () -> ModGameTests::screenCoreProviderRows);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CLAIM_ACTION_DIMENSION_AUTHORITY =
            TEST_FUNCTIONS.register("claim_action_dimension_authority", () -> ModGameTests::claimActionDimensionAuthority);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ADD_MEMBER_REQUIRES_ONLINE_TARGET =
            TEST_FUNCTIONS.register("add_member_requires_online_target", () -> ModGameTests::addMemberRequiresOnlineTarget);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CLAIM_PERMISSION_ACCESS_MATRIX =
            TEST_FUNCTIONS.register("claim_permission_access_matrix", () -> ModGameTests::claimPermissionAccessMatrix);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SNAPSHOT_PACKET_ROUND_TRIP =
            TEST_FUNCTIONS.register("snapshot_packet_round_trip", () -> ModGameTests::snapshotPacketRoundTrip);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> COMMAND_REGISTRATION_AND_FLOW =
            TEST_FUNCTIONS.register("command_registration_and_flow", () -> ModGameTests::commandRegistrationAndFlow);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> RELEASE_AUTHORITY =
            TEST_FUNCTIONS.register("release_authority", () -> ModGameTests::releaseAuthority);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> HOLOMAP_CLAIM_ZONES_REQUIRE_TERRAIN =
            TEST_FUNCTIONS.register("holomap_claim_zones_require_terrain",
                    () -> ModGameTests::holoMapClaimZonesRequireTerrain);

    private ModGameTests() {
    }

    public static void register(IEventBus eventBus) {
        TEST_FUNCTIONS.register(eventBus);
    }

    public static void registerTests(RegisterGameTestsEvent event) {
        if (!shouldRegisterTests()) {
            return;
        }
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(id("base_grid"));
        register(event, environment, "claim_record_permissions", CLAIM_RECORD_PERMISSIONS.getId());
        register(event, environment, "saved_data_codec", SAVED_DATA_CODEC.getId());
        register(event, environment, "malformed_action_packet", MALFORMED_ACTION_PACKET.getId());
        register(event, environment, "screencore_provider_rows", SCREENCORE_PROVIDER_ROWS.getId());
        register(event, environment, "claim_action_dimension_authority", CLAIM_ACTION_DIMENSION_AUTHORITY.getId());
        register(event, environment, "add_member_requires_online_target", ADD_MEMBER_REQUIRES_ONLINE_TARGET.getId());
        register(event, environment, "claim_permission_access_matrix", CLAIM_PERMISSION_ACCESS_MATRIX.getId());
        register(event, environment, "snapshot_packet_round_trip", SNAPSHOT_PACKET_ROUND_TRIP.getId());
        register(event, environment, "command_registration_and_flow", COMMAND_REGISTRATION_AND_FLOW.getId());
        register(event, environment, "release_authority", RELEASE_AUTHORITY.getId());
        register(event, environment, "holomap_claim_zones_require_terrain",
                HOLOMAP_CLAIM_ZONES_REQUIRE_TERRAIN.getId());
    }

    private static void claimRecordPermissions(GameTestHelper helper) {
        UUID owner = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();
        ClaimRecord claim = new ClaimRecord(
                "minecraft:overworld",
                3,
                -4,
                owner,
                "Owner",
                Map.of(
                        memberId, new ClaimMember(memberId, "Member", ClaimRole.MEMBER, ClaimRole.MEMBER.defaultPermissions()),
                        managerId, new ClaimMember(managerId, "Manager", ClaimRole.MANAGER, ClaimRole.MANAGER.defaultPermissions())),
                10L,
                10L);

        helper.assertTrue(claim.allows(owner, ClaimPermission.BUILD)
                        && claim.allows(owner, ClaimPermission.MANAGE)
                        && claim.allows(owner, ClaimPermission.CONTAINERS),
                "Claim owner should always have every permission");
        helper.assertTrue(claim.allows(memberId, ClaimPermission.INTERACT)
                        && claim.allows(memberId, ClaimPermission.CONTAINERS)
                        && !claim.allows(memberId, ClaimPermission.BUILD)
                        && !claim.allows(memberId, ClaimPermission.MANAGE),
                "Default members should interact with storage but not build or manage");
        helper.assertTrue(claim.allows(managerId, ClaimPermission.BUILD)
                        && claim.allows(managerId, ClaimPermission.INTERACT)
                        && claim.allows(managerId, ClaimPermission.CONTAINERS)
                        && claim.allows(managerId, ClaimPermission.MANAGE),
                "Managers should receive every claim permission");
        helper.assertTrue(!claim.allows(UUID.randomUUID(), ClaimPermission.INTERACT),
                "Visitors should not receive protected claim permissions");
        helper.succeed();
    }

    private static void savedDataCodec(GameTestHelper helper) {
        UUID owner = UUID.randomUUID();
        UUID member = UUID.randomUUID();
        BaseGridSavedData data = new BaseGridSavedData();
        data.put(new ClaimRecord(
                "minecraft:the_nether",
                9,
                11,
                owner,
                "CodecOwner",
                Map.of(member, new ClaimMember(member, "CodecMember", ClaimRole.MANAGER,
                        ClaimRole.MANAGER.defaultPermissions())),
                42L,
                77L));

        JsonElement encoded = BaseGridSavedData.CODEC.encodeStart(JsonOps.INSTANCE, data)
                .result()
                .orElseThrow(() -> new IllegalStateException("Base Grid data should encode"));
        BaseGridSavedData decoded = BaseGridSavedData.CODEC.parse(JsonOps.INSTANCE, encoded)
                .result()
                .orElseThrow(() -> new IllegalStateException("Base Grid data should decode"));
        ClaimRecord roundTrip = decoded.claim("minecraft:the_nether", 9, 11)
                .orElseThrow(() -> new IllegalStateException("Claim should survive codec round-trip"));

        helper.assertTrue(roundTrip.ownerId().equals(owner), "Codec round-trip should preserve owner UUID");
        helper.assertTrue(roundTrip.members().containsKey(member), "Codec round-trip should preserve members");
        helper.assertTrue(roundTrip.members().get(member).permissions().contains(ClaimPermission.MANAGE),
                "Codec round-trip should preserve member permissions");
        helper.assertTrue(decoded.remove("minecraft:the_nether", 9, 11)
                        && decoded.claim("minecraft:the_nether", 9, 11).isEmpty(),
                "Saved data should remove claims by dimension and chunk key");
        helper.succeed();
    }

    private static void malformedActionPacket(GameTestHelper helper) {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        BaseGridClaimActionPacket decoded;
        try {
            buffer.writeUtf("NOT_A_REAL_ACTION", 48);
            buffer.writeUtf("minecraft:overworld", 160);
            buffer.writeInt(12);
            buffer.writeInt(-2);
            buffer.writeBoolean(false);
            buffer.writeUtf("Nobody", 160);
            buffer.writeUtf("NOT_A_ROLE", 48);
            buffer.writeUtf("NOT_A_PERMISSION", 48);
            decoded = BaseGridClaimActionPacket.CODEC.decode(buffer);
        } finally {
            buffer.release();
        }

        helper.assertTrue(decoded.action() == BaseGridClaimActionPacket.Action.REFRESH,
                "Malformed action ids should decode to refresh");
        helper.assertTrue(decoded.role() == ClaimRole.MEMBER,
                "Malformed role ids should decode to member");
        helper.assertTrue(decoded.permission() == ClaimPermission.BUILD,
                "Malformed permission ids should decode to build");
        helper.succeed();
    }

    private static void claimActionDimensionAuthority(GameTestHelper helper) {
        ServerPlayer owner = makeServerPlayer(helper, "grid-dim-owner", false);
        ServerLevel level = helper.getLevel();
        String currentDimension = BaseGridClaimService.dimension(level);
        String spoofedDimension = "echobasegrid:spoofed";
        int chunkX = 701;
        int chunkZ = 703;
        try {
            BaseGridSavedData data = BaseGridSavedData.get(level);
            data.remove(currentDimension, chunkX, chunkZ);
            data.remove(spoofedDimension, chunkX, chunkZ);

            ClaimActionResult result = BaseGridClaimService.claim(owner, spoofedDimension, chunkX, chunkZ);

            helper.assertTrue(result.success(), "Spoofed-dimension claim should still succeed in the current dimension");
            helper.assertTrue(data.claim(currentDimension, chunkX, chunkZ).isPresent(),
                    "Claim action should normalize to the player's current dimension");
            helper.assertTrue(data.claim(spoofedDimension, chunkX, chunkZ).isEmpty(),
                    "Claim action should not persist the spoofed packet dimension");
            helper.succeed();
        } finally {
            BaseGridSavedData.get(level).remove(currentDimension, chunkX, chunkZ);
            BaseGridSavedData.get(level).remove(spoofedDimension, chunkX, chunkZ);
            removePlayer(helper, owner);
        }
    }

    private static void addMemberRequiresOnlineTarget(GameTestHelper helper) {
        ServerPlayer owner = makeServerPlayer(helper, "grid-member-owner", false);
        ServerPlayer target = makeServerPlayer(helper, "grid-member-target", false);
        ServerLevel level = helper.getLevel();
        String currentDimension = BaseGridClaimService.dimension(level);
        String spoofedDimension = "echobasegrid:spoofed";
        int chunkX = 709;
        int chunkZ = 711;
        try {
            BaseGridSavedData data = BaseGridSavedData.get(level);
            data.remove(currentDimension, chunkX, chunkZ);
            data.remove(spoofedDimension, chunkX, chunkZ);
            BaseGridClaimService.claim(owner, currentDimension, chunkX, chunkZ);

            ClaimActionResult offline = BaseGridClaimService.addMember(owner, spoofedDimension, chunkX, chunkZ,
                    UUID.randomUUID(), "OfflineSpoof");
            helper.assertTrue(!offline.success(), "Adding an offline or unknown member should be rejected server-side");

            ClaimActionResult online = BaseGridClaimService.addMember(owner, spoofedDimension, chunkX, chunkZ,
                    target.getUUID(), "WrongPacketName");
            ClaimRecord claim = data.claim(currentDimension, chunkX, chunkZ)
                    .orElseThrow(() -> new IllegalStateException("Claim should exist in current dimension"));
            ClaimMember member = claim.members().get(target.getUUID());

            helper.assertTrue(online.success(), "Adding an online member should succeed");
            helper.assertTrue(member != null, "Online target should be stored as a member");
            helper.assertTrue(target.getScoreboardName().equals(member.playerName()),
                    "Stored member name should come from the server player list");
            helper.assertTrue(data.claim(spoofedDimension, chunkX, chunkZ).isEmpty(),
                    "Member mutation should not create or touch a spoofed-dimension claim");
            helper.succeed();
        } finally {
            BaseGridSavedData.get(level).remove(currentDimension, chunkX, chunkZ);
            BaseGridSavedData.get(level).remove(spoofedDimension, chunkX, chunkZ);
            removePlayer(helper, target);
            removePlayer(helper, owner);
        }
    }

    private static void claimPermissionAccessMatrix(GameTestHelper helper) {
        ServerPlayer owner = makeServerPlayer(helper, "grid-access-owner", false);
        ServerPlayer manager = makeServerPlayer(helper, "grid-access-manager", false);
        ServerPlayer member = makeServerPlayer(helper, "grid-access-member", false);
        ServerPlayer visitor = makeServerPlayer(helper, "grid-access-visitor", false);
        ServerPlayer op = makeServerPlayer(helper, "grid-access-op", true);
        ServerLevel level = helper.getLevel();
        String dimension = BaseGridClaimService.dimension(level);
        int chunkX = 719;
        int chunkZ = 727;
        BlockPos protectedPos = new BlockPos(chunkX << 4, 64, chunkZ << 4);
        try {
            BaseGridSavedData data = BaseGridSavedData.get(level);
            data.remove(dimension, chunkX, chunkZ);
            data.put(new ClaimRecord(
                    dimension,
                    chunkX,
                    chunkZ,
                    owner.getUUID(),
                    owner.getScoreboardName(),
                    Map.of(
                            manager.getUUID(), new ClaimMember(manager.getUUID(), manager.getScoreboardName(),
                                    ClaimRole.MANAGER, ClaimRole.MANAGER.defaultPermissions()),
                            member.getUUID(), new ClaimMember(member.getUUID(), member.getScoreboardName(),
                                    ClaimRole.MEMBER, ClaimRole.MEMBER.defaultPermissions())),
                    10L,
                    10L));

            helper.assertTrue(BaseGridClaimService.can(owner, level, protectedPos, ClaimPermission.MANAGE),
                    "Claim owner should keep full access");
            helper.assertTrue(BaseGridClaimService.can(manager, level, protectedPos, ClaimPermission.BUILD)
                            && BaseGridClaimService.can(manager, level, protectedPos, ClaimPermission.MANAGE),
                    "Managers should keep build and manage access");
            helper.assertTrue(BaseGridClaimService.can(member, level, protectedPos, ClaimPermission.INTERACT)
                            && BaseGridClaimService.can(member, level, protectedPos, ClaimPermission.CONTAINERS)
                            && !BaseGridClaimService.can(member, level, protectedPos, ClaimPermission.BUILD),
                    "Members should keep default interact/storage access without build access");
            helper.assertTrue(!BaseGridClaimService.can(visitor, level, protectedPos, ClaimPermission.INTERACT),
                    "Visitors should still be denied in protected claims");
            helper.assertTrue(BaseGridClaimService.can(op, level, protectedPos, ClaimPermission.BUILD),
                    "Configured op bypass should still allow protected actions");
            helper.succeed();
        } finally {
            BaseGridSavedData.get(level).remove(dimension, chunkX, chunkZ);
            removePlayer(helper, op);
            removePlayer(helper, visitor);
            removePlayer(helper, member);
            removePlayer(helper, manager);
            removePlayer(helper, owner);
        }
    }

    private static void snapshotPacketRoundTrip(GameTestHelper helper) {
        UUID memberId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();
        BaseGridSnapshotPacket packet = new BaseGridSnapshotPacket(
                "minecraft:overworld",
                4,
                5,
                6,
                7,
                ClaimRecord.key("minecraft:overworld", 6, 7),
                "mine",
                "Owner",
                true,
                true,
                true,
                8,
                64,
                6,
                "ok",
                List.of(new BaseGridSnapshotPacket.ChunkData(
                        ClaimRecord.key("minecraft:overworld", 6, 7),
                        "minecraft:overworld",
                        6,
                        7,
                        2,
                        2,
                        true,
                        false,
                        "mine",
                        "Owner",
                        "SEL")),
                List.of(new BaseGridSnapshotPacket.MemberData(
                        memberId.toString(),
                        "RoundTripMember",
                        "MANAGER",
                        "Manager",
                        true,
                        true,
                        true,
                        true,
                        true)),
                List.of(new BaseGridSnapshotPacket.PlayerData(candidateId.toString(), "RoundTripCandidate")));
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        BaseGridSnapshotPacket decoded;
        try {
            BaseGridSnapshotPacket.CODEC.encode(buffer, packet);
            decoded = BaseGridSnapshotPacket.CODEC.decode(buffer);
        } finally {
            buffer.release();
        }

        helper.assertTrue(decoded.selectedKey().equals(packet.selectedKey()),
                "Snapshot codec should preserve selected chunk key");
        helper.assertTrue(decoded.selectedReleaseAllowed() == packet.selectedReleaseAllowed(),
                "Snapshot codec should preserve release authority");
        helper.assertTrue(decoded.chunks().size() == 1 && decoded.chunks().getFirst().selected(),
                "Snapshot codec should preserve grid rows");
        helper.assertTrue(decoded.members().size() == 1 && "RoundTripMember".equals(decoded.members().getFirst().name()),
                "Snapshot codec should preserve member rows");
        helper.assertTrue(decoded.candidates().size() == 1
                        && "RoundTripCandidate".equals(decoded.candidates().getFirst().name()),
                "Snapshot codec should preserve candidate rows");
        helper.succeed();
    }

    @SuppressWarnings("unchecked")
    private static void screenCoreProviderRows(GameTestHelper helper) {
        BaseGridSnapshotPacket packet = new BaseGridSnapshotPacket(
                "minecraft:overworld",
                1,
                2,
                1,
                2,
                ClaimRecord.key("minecraft:overworld", 1, 2),
                "unclaimed",
                "Unclaimed",
                false,
                false,
                false,
                3,
                64,
                6,
                "Ready",
                List.of(new BaseGridSnapshotPacket.ChunkData(
                        ClaimRecord.key("minecraft:overworld", 1, 2),
                        "minecraft:overworld",
                        1,
                        2,
                        0,
                        0,
                        true,
                        true,
                        "unclaimed",
                        "",
                        "YOU")),
                List.of(),
                List.of(new BaseGridSnapshotPacket.PlayerData(UUID.randomUUID().toString(), "OnlineFriend")));
        BaseGridClientState.apply(packet);

        Map<String, Object> selected = (Map<String, Object>) BaseGridDataProviders.PROVIDER
                .resolve(EchoDataContext.empty(), List.of("selected"));
        List<Map<String, Object>> cells = (List<Map<String, Object>>) BaseGridDataProviders.PROVIDER
                .resolve(EchoDataContext.empty(), List.of("grid", "cells"));
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) BaseGridDataProviders.PROVIDER
                .resolve(EchoDataContext.empty(), List.of("candidates"));

        helper.assertTrue("1, 2".equals(selected.get("coordinates")),
                "Selected provider row should expose selected chunk coordinates");
        helper.assertTrue(Boolean.TRUE.equals(selected.get("claimButtonDisabled")) == false,
                "Unclaimed selected chunks should enable the claim action");
        helper.assertTrue("Claim Available".equals(selected.get("manageLabel"))
                        && "No claim controls this chunk.".equals(selected.get("summary")),
                "Unclaimed selected chunks should expose clear state copy");
        helper.assertTrue("Claim Required".equals(selected.get("candidateEmptyTitle")),
                "Unclaimed selected chunks should explain why add-member rows are unavailable");
        helper.assertTrue(cells.size() == 1 && Boolean.TRUE.equals(cells.getFirst().get("selected")),
                "Grid provider should expose selected chunk cell state");
        helper.assertTrue(candidates.size() == 1 && "OnlineFriend".equals(candidates.getFirst().get("name")),
                "Provider should expose online member candidates");
        helper.assertTrue(Boolean.TRUE.equals(candidates.getFirst().get("addDisabled")),
                "Candidate rows should be disabled when the selected claim is not manageable");

        UUID memberId = UUID.randomUUID();
        BaseGridClientState.apply(new BaseGridSnapshotPacket(
                "minecraft:overworld",
                4,
                4,
                4,
                4,
                ClaimRecord.key("minecraft:overworld", 4, 4),
                "mine",
                "Owner",
                true,
                true,
                true,
                1,
                64,
                6,
                "",
                List.of(new BaseGridSnapshotPacket.ChunkData(
                        ClaimRecord.key("minecraft:overworld", 4, 4),
                        "minecraft:overworld",
                        4,
                        4,
                        0,
                        0,
                        true,
                        true,
                        "mine",
                        "Owner",
                        "YOU")),
                List.of(new BaseGridSnapshotPacket.MemberData(
                        memberId.toString(),
                        "TrustedMember",
                        "MEMBER",
                        "Member",
                        false,
                        true,
                        true,
                        false,
                        true)),
                List.of(new BaseGridSnapshotPacket.PlayerData(UUID.randomUUID().toString(), "AddableFriend"))));
        selected = (Map<String, Object>) BaseGridDataProviders.PROVIDER
                .resolve(EchoDataContext.empty(), List.of("selected"));
        List<Map<String, Object>> members = (List<Map<String, Object>>) BaseGridDataProviders.PROVIDER
                .resolve(EchoDataContext.empty(), List.of("members"));
        candidates = (List<Map<String, Object>>) BaseGridDataProviders.PROVIDER
                .resolve(EchoDataContext.empty(), List.of("candidates"));

        helper.assertTrue("Owner Controls".equals(selected.get("manageLabel")),
                "Owned selected chunks should expose owner-management copy");
        helper.assertTrue(members.size() == 1 && Boolean.FALSE.equals(members.getFirst().get("actionDisabled")),
                "Member controls should stay enabled for manageable claims");
        helper.assertTrue(Boolean.FALSE.equals(candidates.getFirst().get("addDisabled")),
                "Candidate rows should be enabled for manageable claims");

        BaseGridClientState.apply(new BaseGridSnapshotPacket(
                "minecraft:overworld",
                8,
                8,
                8,
                8,
                ClaimRecord.key("minecraft:overworld", 8, 8),
                "occupied",
                "OtherOwner",
                false,
                false,
                false,
                1,
                64,
                6,
                "",
                List.of(new BaseGridSnapshotPacket.ChunkData(
                        ClaimRecord.key("minecraft:overworld", 8, 8),
                        "minecraft:overworld",
                        8,
                        8,
                        0,
                        0,
                        true,
                        true,
                        "occupied",
                        "OtherOwner",
                        "YOU")),
                List.of(new BaseGridSnapshotPacket.MemberData(
                        memberId.toString(),
                        "LockedMember",
                        "MANAGER",
                        "Manager",
                        true,
                        true,
                        true,
                        true,
                        false)),
                List.of()));
        members = (List<Map<String, Object>>) BaseGridDataProviders.PROVIDER
                .resolve(EchoDataContext.empty(), List.of("members"));
        helper.assertTrue(Boolean.TRUE.equals(members.getFirst().get("actionDisabled"))
                        && "Locked".equals(members.getFirst().get("manageLabel")),
                "Member controls should be disabled when the selected claim is not manageable");
        helper.succeed();
    }

    private static void commandRegistrationAndFlow(GameTestHelper helper) {
        ServerPlayer owner = makeServerPlayer(helper, "grid-cmd-owner", false);
        ServerLevel level = helper.getLevel();
        String dimension = BaseGridClaimService.dimension(level);
        int chunkX = owner.chunkPosition().x();
        int chunkZ = owner.chunkPosition().z();
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        try {
            BaseGridSavedData.get(level).remove(dimension, chunkX, chunkZ);
            BaseGridCommands.register(dispatcher, null);

            helper.assertTrue(dispatcher.getRoot().getChild("basegrid") != null
                            && dispatcher.getRoot().getChild("echo_basegrid") != null,
                    "Base Grid should register canonical and ECHO-prefixed commands");
            helper.assertTrue(dispatcher.execute("basegrid status", owner.createCommandSourceStack()) == 1,
                    "Base Grid status command should execute for players");
            helper.assertTrue(dispatcher.execute("basegrid claim", owner.createCommandSourceStack()) == 1,
                    "Base Grid claim command should claim the current chunk");
            helper.assertTrue(BaseGridSavedData.get(level).claim(dimension, chunkX, chunkZ).isPresent(),
                    "Claim command should persist a current-chunk claim");
            helper.assertTrue(dispatcher.execute("basegrid inspect", owner.createCommandSourceStack()) == 1,
                    "Base Grid inspect command should find the current claim");
            helper.assertTrue(dispatcher.execute("echo_basegrid unclaim", owner.createCommandSourceStack()) == 1,
                    "Base Grid alias should route to the same release command");
            helper.assertTrue(BaseGridSavedData.get(level).claim(dimension, chunkX, chunkZ).isEmpty(),
                    "Unclaim command should release the current chunk");
            helper.succeed();
        } catch (Exception exception) {
            helper.fail("Base Grid command flow failed: " + exception.getMessage());
        } finally {
            BaseGridSavedData.get(level).remove(dimension, chunkX, chunkZ);
            removePlayer(helper, owner);
        }
    }

    private static void releaseAuthority(GameTestHelper helper) {
        ServerPlayer owner = makeServerPlayer(helper, "grid-release-owner", false);
        ServerPlayer manager = makeServerPlayer(helper, "grid-release-manager", false);
        ServerPlayer op = makeServerPlayer(helper, "grid-release-op", true);
        ServerLevel level = helper.getLevel();
        String dimension = BaseGridClaimService.dimension(level);
        int chunkX = manager.chunkPosition().x();
        int chunkZ = manager.chunkPosition().z();
        try {
            BaseGridSavedData data = BaseGridSavedData.get(level);
            data.remove(dimension, chunkX, chunkZ);
            data.put(new ClaimRecord(
                    dimension,
                    chunkX,
                    chunkZ,
                    owner.getUUID(),
                    owner.getScoreboardName(),
                    Map.of(manager.getUUID(), new ClaimMember(manager.getUUID(), manager.getScoreboardName(),
                            ClaimRole.MANAGER, ClaimRole.MANAGER.defaultPermissions())),
                    10L,
                    10L));

            BaseGridSnapshotPacket managerSnapshot = BaseGridSnapshotPacket.create(manager, dimension, chunkX, chunkZ, "");
            helper.assertTrue(managerSnapshot.selectedManageable() && !managerSnapshot.selectedReleaseAllowed(),
                    "Managers should manage members but not release claims");

            ClaimActionResult managerRelease = BaseGridClaimService.unclaim(manager, dimension, chunkX, chunkZ);
            helper.assertTrue(!managerRelease.success() && data.claim(dimension, chunkX, chunkZ).isPresent(),
                    "Manager release attempts should be rejected and leave the claim intact");

            BaseGridSnapshotPacket opSnapshot = BaseGridSnapshotPacket.create(op, dimension, chunkX, chunkZ, "");
            helper.assertTrue(opSnapshot.selectedManageable() && opSnapshot.selectedReleaseAllowed(),
                    "Operator bypass should allow claim release");
            ClaimActionResult opRelease = BaseGridClaimService.unclaim(op, dimension, chunkX, chunkZ);
            helper.assertTrue(opRelease.success() && data.claim(dimension, chunkX, chunkZ).isEmpty(),
                    "Operator release should remove the claim");
            helper.succeed();
        } finally {
            BaseGridSavedData.get(level).remove(dimension, chunkX, chunkZ);
            removePlayer(helper, op);
            removePlayer(helper, manager);
            removePlayer(helper, owner);
        }
    }

    private static void holoMapClaimZonesRequireTerrain(GameTestHelper helper) {
        ServerPlayer owner = makeServerPlayer(helper, "grid-holomap-owner", false);
        ServerLevel level = helper.getLevel();
        String dimension = BaseGridClaimService.dimension(level);
        int chunkX = owner.chunkPosition().x() + 41;
        int chunkZ = owner.chunkPosition().z() + 43;
        try {
            BaseGridHoloMapIntegration.register();
            BaseGridSavedData claims = BaseGridSavedData.get(level);
            HoloMapTerrainSavedData terrain = HoloMapTerrainSavedData.get(level);
            claims.remove(dimension, chunkX, chunkZ);
            terrain.clear(owner.getUUID());
            claims.put(new ClaimRecord(dimension, chunkX, chunkZ, owner.getUUID(), owner.getScoreboardName(),
                    Map.of(), 12L, 12L));

            HoloMapQuery query = new HoloMapQuery(level.dimension(),
                    chunkX * 16.0D + 8.0D, 64.0D, chunkZ * 16.0D + 8.0D, 48);
            helper.assertTrue(HoloMapService.INSTANCE.richZones(owner, query).stream()
                            .noneMatch(zone -> BaseGridHoloMapIntegration.PROVIDER_ID.equals(zone.sourceId())),
                    "Base Grid HoloMap provider should hide claims until terrain is renderable for the player");

            terrain.putForTests(owner.getUUID().toString(), dimension, chunkX, chunkZ, 44L,
                    HoloMapTerrainTile.CURRENT_VERSION, HoloMapTerrainTile.DetailMode.SURFACE_SHADED,
                    filledPixels(0xFF2E6A5E));
            helper.assertTrue(HoloMapService.INSTANCE.richZones(owner, query).stream()
                            .anyMatch(zone -> BaseGridHoloMapIntegration.PROVIDER_ID.equals(zone.sourceId())
                                    && "mine".equals(BaseGridHoloMapIntegration.relationFromZoneId(zone.id()))),
                    "Base Grid HoloMap provider should emit owned claim zones for known real terrain chunks");
            helper.succeed();
        } finally {
            BaseGridSavedData.get(level).remove(dimension, chunkX, chunkZ);
            HoloMapTerrainSavedData.get(level).clear(owner.getUUID());
            removePlayer(helper, owner);
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

    private static ServerPlayer makeServerPlayer(GameTestHelper helper, String name, boolean opBypass) {
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), name), false);
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                cookie.gameProfile(), cookie.clientInformation()) {
            @Override
            public GameType gameMode() {
                return GameType.CREATIVE;
            }

            @Override
            public CommandSourceStack createCommandSourceStack() {
                if (!opBypass) {
                    return super.createCommandSourceStack();
                }
                return new CommandSourceStack(
                        CommandSource.NULL,
                        Vec3.ZERO,
                        Vec2.ZERO,
                        helper.getLevel(),
                        LevelBasedPermissionSet.GAMEMASTER,
                        getScoreboardName(),
                        Component.literal(getScoreboardName()),
                        helper.getLevel().getServer(),
                        this);
            }
        };
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        return player;
    }

    private static void removePlayer(GameTestHelper helper, ServerPlayer player) {
        if (player != null) {
            helper.getLevel().getServer().getPlayerList().remove(player);
        }
    }

    private static int[] filledPixels(int color) {
        int[] pixels = new int[HoloMapTerrainTile.PIXELS];
        java.util.Arrays.fill(pixels, color);
        return pixels;
    }

    private static boolean shouldRegisterTests() {
        String namespaces = System.getProperty("neoforge.enabledGameTestNamespaces", "");
        if (namespaces == null || namespaces.isBlank()) {
            return false;
        }
        for (String namespace : namespaces.split(",")) {
            String normalized = namespace.trim();
            if (EchoBaseGrid.MODID.equals(normalized) || "*".equals(normalized) || "all".equalsIgnoreCase(normalized)) {
                return true;
            }
        }
        return false;
    }

    private static Identifier id(String path) {
        return EchoBaseGrid.id(path);
    }
}
