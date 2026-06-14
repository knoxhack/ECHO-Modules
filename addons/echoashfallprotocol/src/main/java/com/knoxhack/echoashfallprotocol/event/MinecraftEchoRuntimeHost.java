package com.knoxhack.echoashfallprotocol.event;

import com.knoxhack.echo.adaptercore.EchoCanonicalContentIds;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeBlockEntitySnapshot;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeBlockRef;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeBlockState;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeCapabilityRequest;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeEvent;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeItemStack;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeMutationContext;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeMutationLedgerEntry;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeMutationTarget;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativePacket;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativePlayerRef;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativePosition;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResult;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeSaveData;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeStructurePlacement;
import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echoashfallprotocol.block.entity.AtmosphericScrubberBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.AutofeedHopperBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.BatteryBankBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.ContaminantCondenserBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.CrystallineSynthesizerBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.DeepCoreMinerBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.FactoryControllerBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.FieldMedBayBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.FilterWorkbenchBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.HandRecyclerBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.HopperHandler;
import com.knoxhack.echoashfallprotocol.block.entity.IsotopeRefinerBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.ItemPipeBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.LoadDistributorBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.MachineInventory;
import com.knoxhack.echoashfallprotocol.block.entity.MicroGeneratorBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.NexusCapacitorBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.OreGrinderBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.PowerCableBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.PowerNodeBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.RadiationCleanserBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.RainCollectorBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.ScrapDynamoBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.ScrapPressBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.SignalScannerBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.ThermalArrayBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.ThermalBurnerBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.WaterPurifierBlockEntity;
import com.knoxhack.echoashfallprotocol.capability.IEnergyStorage;
import com.knoxhack.echoashfallprotocol.echo.MissionRegistry;
import com.knoxhack.echoashfallprotocol.echo.QuestData;
import com.knoxhack.echoashfallprotocol.network.WelcomeScreenPacket;
import com.knoxhack.echoashfallprotocol.registry.ModAttachments;
import com.knoxhack.echoashfallprotocol.registry.ModItems;
import com.knoxhack.echoashfallprotocol.survival.SurvivalData;
import com.knoxhack.echoashfallprotocol.world.POIScannerService;
import com.knoxhack.echoashfallprotocol.world.StartingDropPodData;
import com.knoxhack.echoashfallprotocol.worldgen.ProceduralStructureGenerator;
import com.knoxhack.echoashfallprotocol.worldgen.StructureType;
import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.mission.MissionObjectiveType;
import com.echoplatform.echocore.api.network.EchoPacketKind;
import com.knoxhack.echonetcore.api.EchoNetSend;
import com.knoxhack.echonetcore.network.EchoSyncPayload;
import com.knoxhack.echonetcore.network.EchoSyncType;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.LevelData;

public class MinecraftEchoRuntimeHost implements EchoNativeRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echoashfallprotocol:minecraft_runtime_host";
    private static final String FIRST_JOIN_FLAG = "ashes_of_tomorrow.received_kit";
    private static final String QUEST_DROP_POD_INITIALIZED = "quest.dropPodInitialized";
    private static final String FIRST_MISSION_ID = "secure_crash_outpost";
    private static final String SAVE_ROOT = "echoashfallprotocol.runtime_host.save_data";
    private static final Identifier RUNTIME_HUD_CHANNEL =
            Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "runtime_hud_notification");

    private final MinecraftRuntimeHostContext hostContext;
    private final PlayerInventory playerInventory = new MinecraftPlayerInventory();
    private final PlayerState playerState = new MinecraftPlayerState();
    private final WorldBlocks worldBlocks = new MinecraftWorldBlocks();
    private final WorldState worldState = new MinecraftWorldState();
    private final Structures structures = new MinecraftStructures();
    private final BlockEntities blockEntities = new MinecraftBlockEntities();
    private final Capabilities capabilities = new MinecraftCapabilities();
    private final Events events = new MinecraftEvents();
    private final Packets packets = new MinecraftPackets();
    private final Hud hud = new MinecraftHud();
    private final SaveData saveData = new MinecraftSaveData();
    private final RuntimeSurfaces runtimeSurfaces = new MinecraftRuntimeSurfaces();

    public MinecraftEchoRuntimeHost(MinecraftRuntimeHostContext hostContext) {
        if (hostContext == null) {
            throw new IllegalArgumentException("Minecraft runtime host context must not be null.");
        }
        this.hostContext = hostContext;
    }

    public String runtimeHostId() {
        return hostContext.runtimeHostId();
    }

    public String compatibilityDelegateId() {
        return "";
    }

    public String runtimeLane() {
        return "Minecraft";
    }

    public NativePlayerRef playerRef() {
        return hostContext.playerRef();
    }

    public String dimensionId() {
        return hostContext.dimensionId();
    }

    public NativeMutationContext context(String idempotencyKey, String nativeInterface, String nativeMethod) {
        return hostContext.context(idempotencyKey, nativeInterface, nativeMethod);
    }

    @Override
    public PlayerInventory playerInventory() {
        return playerInventory;
    }

    @Override
    public PlayerState playerState() {
        return playerState;
    }

    @Override
    public WorldBlocks worldBlocks() {
        return worldBlocks;
    }

    @Override
    public WorldState worldState() {
        return worldState;
    }

    @Override
    public Structures structures() {
        return structures;
    }

    @Override
    public BlockEntities blockEntities() {
        return blockEntities;
    }

    @Override
    public Capabilities capabilities() {
        return capabilities;
    }

    @Override
    public Events events() {
        return events;
    }

    @Override
    public Packets packets() {
        return packets;
    }

    @Override
    public Hud hud() {
        return hud;
    }

    @Override
    public SaveData saveData() {
        return saveData;
    }

    @Override
    public RuntimeSurfaces runtimeSurfaces() {
        return runtimeSurfaces;
    }

    private final class MinecraftPlayerInventory implements PlayerInventory {
        @Override
        public NativeResult grant(NativePlayerRef playerRef, NativeItemStack stack, NativeMutationContext context) {
            ServerPlayer player = hostContext.resolvePlayer(playerRef);
            if (player == null) {
                return result(false, "INVALID_PLAYER", "Native player reference did not match an online player.", context,
                        Map.of("item", stack.itemId()), NativeMutationTarget.none(), Map.of(), Map.of(), false, false);
            }
            Identifier itemId = Identifier.tryParse(stack.itemId());
            if (itemId == null) {
                return result(false, "FAIL_INVALID_ITEM_ID", "Item id could not be parsed.", context,
                        Map.of("item", stack.itemId()), target(playerRef), Map.of(), Map.of(), false, false);
            }
            Item item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(Items.AIR);
            if (item == Items.AIR) {
                return result(false, "SKIPPED_ITEM_UNAVAILABLE", "Item was not registered in the current runtime.", context,
                        Map.of("item", stack.itemId()), target(playerRef), Map.of(), Map.of(), false, false);
            }
            if (Boolean.TRUE.equals(stack.components().get("dedupe")) && hasItem(player, item)) {
                return result(false, "SKIPPED_ALREADY_PRESENT", "Player already has the requested item.", context,
                        Map.of("item", stack.itemId()), target(playerRef), inventorySummary(player), inventorySummary(player), false, false);
            }

            Map<String, Object> before = inventorySummary(player);
            ItemStack itemStack = buildItemStack(stack, item);
            boolean dropped = !player.getInventory().add(itemStack);
            if (dropped) {
                player.drop(itemStack, false);
            }
            player.getInventory().setChanged();
            Map<String, Object> after = inventorySummary(player);
            String missionRecordFailure = "";
            boolean missionAdvanced = false;
            try {
                missionAdvanced = recordItemCollected(player, Map.of(
                        "itemId", stack.itemId(),
                        "count", stack.count(),
                        "source", "native_inventory_grant"));
            } catch (Throwable failure) {
                missionRecordFailure = failureSummary(failure);
            }
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("item", stack.itemId());
            details.put("count", stack.count());
            details.put("dropped", dropped);
            details.put("missionAdvanced", missionAdvanced);
            details.put("missionRecordFailure", missionRecordFailure);
            details.put("runtimeInventoryTouched", true);
            details.put("runtimeInventoryMutated", true);
            return result(true, dropped ? "MUTATED_DROPPED" : "MUTATED", "Granted item stack to player inventory.", context,
                    details,
                    target(playerRef),
                    before,
                    after,
                    true,
                    missionAdvanced);
        }

        @Override
        public NativeResult remove(NativePlayerRef playerRef, String itemId, int count, NativeMutationContext context) {
            ServerPlayer player = hostContext.resolvePlayer(playerRef);
            if (player == null) {
                return result(false, "INVALID_PLAYER", "Native player reference did not match an online player.", context,
                        Map.of("item", itemId, "count", count), NativeMutationTarget.none(), Map.of(), Map.of(), false, false);
            }
            if (count < 1) {
                return result(false, "FAIL_INVALID_COUNT", "Inventory removal count must be positive.", context,
                        Map.of("item", itemId, "count", count), target(playerRef), inventorySummary(player), inventorySummary(player), false, false);
            }
            Identifier id = Identifier.tryParse(itemId);
            if (id == null) {
                return result(false, "FAIL_INVALID_ITEM_ID", "Item id could not be parsed.", context,
                        Map.of("item", itemId, "count", count), target(playerRef), inventorySummary(player), inventorySummary(player), false, false);
            }
            Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(Items.AIR);
            if (item == Items.AIR) {
                return result(false, "SKIPPED_ITEM_UNAVAILABLE", "Item was not registered in the current runtime.", context,
                        Map.of("item", itemId, "count", count), target(playerRef), inventorySummary(player), inventorySummary(player), false, false);
            }

            Map<String, Object> before = inventorySummary(player);
            int remaining = count;
            for (int slot = 0; slot < player.getInventory().getContainerSize() && remaining > 0; slot++) {
                ItemStack stack = player.getInventory().getItem(slot);
                if (stack.isEmpty() || !stack.is(item)) {
                    continue;
                }
                int removed = Math.min(remaining, stack.getCount());
                stack.shrink(removed);
                remaining -= removed;
                if (stack.isEmpty()) {
                    player.getInventory().setItem(slot, ItemStack.EMPTY);
                }
            }
            int removed = count - remaining;
            if (removed > 0) {
                player.getInventory().setChanged();
            }
            Map<String, Object> after = inventorySummary(player);
            return result(removed > 0, removed == count ? "MUTATED" : removed > 0 ? "MUTATED_PARTIAL" : "SKIPPED_ITEM_NOT_PRESENT",
                    removed > 0 ? "Removed matching item stacks from player inventory." : "No matching item stacks were present.",
                    context,
                    Map.of(
                            "item", itemId,
                            "requested", count,
                            "removed", removed,
                            "runtimeInventoryTouched", removed > 0,
                            "runtimeInventoryMutated", removed > 0),
                    target(playerRef),
                    before,
                    after,
                    removed > 0,
                    false);
        }

        @Override
        public List<NativeItemStack> snapshot(NativePlayerRef playerRef, NativeMutationContext context) {
            ServerPlayer player = hostContext.resolvePlayer(playerRef);
            if (player == null) {
                return List.of();
            }
            List<NativeItemStack> stacks = new ArrayList<>();
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                ItemStack stack = player.getInventory().getItem(slot);
                if (!stack.isEmpty()) {
                    stacks.add(new NativeItemStack(
                            BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),
                            stack.getCount(),
                            Map.of("slot", slot)));
                }
            }
            return List.copyOf(stacks);
        }
    }

    private final class MinecraftPlayerState implements PlayerState {
        @Override
        public NativeResult teleport(NativePlayerRef playerRef, NativePosition position, NativeMutationContext context) {
            ServerPlayer player = hostContext.resolvePlayer(playerRef);
            if (player == null) {
                return result(false, "INVALID_PLAYER", "Native player reference did not match an online player.", context,
                        Map.of(), NativeMutationTarget.none(), Map.of(), Map.of(), false, false);
            }
            ServerLevel level = hostContext.resolveLevel(position.dimensionId());
            if (level == null) {
                return result(false, "FAIL_DIMENSION_UNAVAILABLE", "Target dimension was not loaded.", context,
                        positionSnapshot(position), target(playerRef), playerPosition(player), playerPosition(player), false, false);
            }
            Map<String, Object> before = playerPosition(player);
            if (level == player.level()) {
                player.teleportTo(position.x(), position.y(), position.z());
            } else {
                player.teleportTo(level, position.x(), position.y(), position.z(),
                        Set.of(), position.yaw(), position.pitch(), false);
            }
            player.setYRot(position.yaw());
            player.setXRot(position.pitch());
            return result(true, "MUTATED", "Teleported player to native position.", context,
                    Map.of("position", positionSnapshot(position)),
                    new NativeMutationTarget(playerRef, position.dimensionId(), position, null),
                    before,
                    playerPosition(player),
                    true,
                    false);
        }

        @Override
        public NativeResult bindRespawn(
                NativePlayerRef playerRef,
                NativePosition position,
                boolean forced,
                NativeMutationContext context) {
            ServerPlayer player = hostContext.resolvePlayer(playerRef);
            if (player == null) {
                return result(false, "INVALID_PLAYER", "Native player reference did not match an online player.", context,
                        Map.of(), NativeMutationTarget.none(), Map.of(), Map.of(), false, false);
            }
            ServerLevel level = hostContext.resolveLevel(position.dimensionId());
            if (level == null) {
                return result(false, "FAIL_DIMENSION_UNAVAILABLE", "Respawn dimension was not loaded.", context,
                        positionSnapshot(position), target(playerRef), Map.of(), Map.of(), false, false);
            }
            if (!forced && player.getRespawnConfig() != null) {
                return result(false, "SKIPPED_RESPAWN_ALREADY_BOUND", "Player already has a respawn binding.", context,
                        Map.of("forced", false), target(playerRef), respawnSummary(player), respawnSummary(player), false, false);
            }
            Map<String, Object> before = respawnSummary(player);
            BlockPos respawn = new BlockPos((int) position.x(), (int) position.y(), (int) position.z());
            player.setRespawnPosition(
                    new ServerPlayer.RespawnConfig(LevelData.RespawnData.of(level.dimension(), respawn, 0.0F, 0.0F), forced),
                    false);
            return result(true, "MUTATED", "Bound player respawn to a Minecraft position.", context,
                    Map.of("respawn", positionSnapshot(respawn), "forced", forced),
                    new NativeMutationTarget(playerRef, position.dimensionId(), position, null),
                    before,
                    respawnSummary(player),
                    true,
                    false);
        }

        @Override
        public NativeResult grantAdvancement(
                NativePlayerRef playerRef,
                String advancementId,
                String criterion,
                NativeMutationContext context) {
            ServerPlayer player = hostContext.resolvePlayer(playerRef);
            if (player == null) {
                return result(false, "INVALID_PLAYER", "Native player reference did not match an online player.", context,
                        Map.of(), NativeMutationTarget.none(), Map.of(), Map.of(), false, false);
            }
            Identifier id = Identifier.tryParse(advancementId);
            if (id == null) {
                return result(false, "FAIL_INVALID_ADVANCEMENT", "Advancement id could not be parsed.", context,
                        Map.of("advancement", advancementId), target(playerRef), Map.of(), Map.of(), false, false);
            }
            var holder = hostContext.level().getServer().getAdvancements().get(id);
            if (holder == null) {
                return result(false, "SKIPPED_ADVANCEMENT_MISSING", "Advancement was not registered.", context,
                        Map.of("advancement", advancementId, "criterion", criterion), target(playerRef), Map.of(), Map.of(), false, false);
            }
            boolean awarded = player.getAdvancements().award(holder, criterion);
            return result(awarded, awarded ? "MUTATED" : "SKIPPED_ADVANCEMENT_ALREADY_GRANTED",
                    awarded ? "Granted advancement criterion." : "Advancement criterion was already granted.",
                    context,
                    Map.of("advancement", advancementId, "criterion", criterion),
                    target(playerRef),
                    Map.of(),
                    Map.of("awarded", awarded),
                    awarded,
                    false);
        }

        @Override
        public NativeResult writePersistentState(
                NativePlayerRef playerRef,
                String key,
                Object value,
                NativeMutationContext context) {
            ServerPlayer player = hostContext.resolvePlayer(playerRef);
            if (player == null) {
                return result(false, "INVALID_PLAYER", "Native player reference did not match an online player.", context,
                        Map.of("key", key), NativeMutationTarget.none(), Map.of(), Map.of(), false, false);
            }
            Map<String, Object> before = Map.of("key", key, "persistentDataTouched", player.getPersistentData() != null);
            writePersistentValue(player.getPersistentData(), key, value);

            boolean syncedQuest = syncQuestState(player, key, value);
            boolean syncedSurvival = syncSurvivalState(player, key, value);
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("key", key);
            details.put("value", safeValue(value));
            details.put("questDataSynced", syncedQuest);
            details.put("survivalDataSynced", syncedSurvival);
            details.put("runtimePlayerStateTouched", true);
            details.put("runtimePlayerStateMutated", true);
            if (key != null && key.startsWith("mission.")) {
                details.put("runtimeMissionStateTouched", true);
                details.put("runtimeMissionStateMutated", true);
            }
            return result(true, "MUTATED", "Wrote player persistent state through the Minecraft host.", context,
                    details,
                    target(playerRef),
                    before,
                    Map.of("key", key, "value", safeValue(value)),
                    true,
                    false);
        }
    }

    private final class MinecraftWorldBlocks implements WorldBlocks {
        @Override
        public NativeResult setBlock(NativeBlockRef block, NativeBlockState state, NativeMutationContext context) {
            ServerLevel level = hostContext.resolveLevel(block.dimensionId());
            BlockPos pos = blockPos(block);
            if (level == null) {
                return result(false, "FAIL_DIMENSION_UNAVAILABLE", "Block dimension was not loaded.", context,
                        Map.of("block", blockSnapshot(block), "state", state.blockId()), blockTarget(block), Map.of(), Map.of(), false, false);
            }
            if (!level.hasChunkAt(pos)) {
                return result(false, "SKIPPED_CHUNK_UNLOADED", "Target block chunk was not loaded.", context,
                        Map.of("block", blockSnapshot(block), "state", state.blockId()), blockTarget(block), Map.of(), Map.of(), false, false);
            }
            BlockState beforeState = level.getBlockState(pos);
            BlockState desired = resolveBlockState(state);
            if (desired == null) {
                return result(false, "FAIL_INVALID_BLOCK_STATE", "Block id or state properties could not be resolved.", context,
                        Map.of("block", blockSnapshot(block), "state", state.blockId()), blockTarget(block),
                        nativeBlockState(beforeState).properties(), nativeBlockState(beforeState).properties(), false, false);
            }
            boolean changed = level.setBlock(pos, desired, 3);
            boolean placementRecorded = false;
            String placementRecordFailure = "";
            if (changed) {
                try {
                    recordBlockPlacement(state.blockId(), pos);
                    placementRecorded = true;
                } catch (Throwable failure) {
                    placementRecordFailure = failureSummary(failure);
                }
            }
            NativeBlockState afterState = nativeBlockState(level.getBlockState(pos));
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("block", blockSnapshot(block));
            details.put("state", afterState.blockId());
            details.put("properties", afterState.properties());
            details.put("placementRecorded", placementRecorded);
            details.put("placementRecordFailure", placementRecordFailure);
            details.put("runtimeWorldBlockTouched", changed);
            details.put("runtimeWorldBlockMutated", changed);
            return result(changed, changed ? "MUTATED" : "SKIPPED_BLOCK_UNCHANGED",
                    changed ? "Set Minecraft block state." : "Minecraft block state was unchanged.",
                    context,
                    details,
                    blockTarget(block),
                    Map.of("state", nativeBlockState(beforeState).blockId(), "properties", nativeBlockState(beforeState).properties()),
                    Map.of("state", afterState.blockId(), "properties", afterState.properties()),
                    changed,
                    false);
        }

        @Override
        public NativeResult clearBlock(NativeBlockRef block, NativeMutationContext context) {
            ServerLevel level = hostContext.resolveLevel(block.dimensionId());
            BlockPos pos = blockPos(block);
            if (level == null) {
                return result(false, "FAIL_DIMENSION_UNAVAILABLE", "Block dimension was not loaded.", context,
                        Map.of("block", blockSnapshot(block)), blockTarget(block), Map.of(), Map.of(), false, false);
            }
            if (!level.hasChunkAt(pos)) {
                return result(false, "SKIPPED_CHUNK_UNLOADED", "Target block chunk was not loaded.", context,
                        Map.of("block", blockSnapshot(block)), blockTarget(block), Map.of(), Map.of(), false, false);
            }
            BlockState beforeState = level.getBlockState(pos);
            if (beforeState.isAir()) {
                return result(false, "SKIPPED_ALREADY_AIR", "Target block was already air.", context,
                        Map.of("block", blockSnapshot(block)), blockTarget(block),
                        Map.of("state", nativeBlockState(beforeState).blockId()), Map.of("state", nativeBlockState(beforeState).blockId()), false, false);
            }
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity != null) {
                level.removeBlockEntity(pos);
            }
            boolean changed = level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            MissionRegistry.invalidateBlockProbeCache(hostContext.player());
            return result(changed, changed ? "MUTATED" : "FAIL_BLOCK_CLEAR_UNCHANGED",
                    changed ? "Cleared Minecraft block to air." : "Minecraft block clear did not change the world.",
                    context,
                    Map.of(
                            "block", blockSnapshot(block),
                            "removedBlockEntity", blockEntity != null,
                            "runtimeWorldBlockTouched", changed,
                            "runtimeWorldBlockMutated", changed),
                    blockTarget(block),
                    Map.of("state", nativeBlockState(beforeState).blockId(), "properties", nativeBlockState(beforeState).properties()),
                    Map.of("state", nativeBlockState(level.getBlockState(pos)).blockId()),
                    changed,
                    false);
        }

        @Override
        public NativeBlockState blockState(NativeBlockRef block, NativeMutationContext context) {
            ServerLevel level = hostContext.resolveLevel(block.dimensionId());
            if (level == null || !level.hasChunkAt(blockPos(block))) {
                return new NativeBlockState("minecraft:air", Map.of(
                        "loaded", false,
                        "dimensionId", block.dimensionId()));
            }
            return nativeBlockState(level.getBlockState(blockPos(block)));
        }

        @Override
        public boolean isLoaded(NativeBlockRef block, NativeMutationContext context) {
            ServerLevel level = hostContext.resolveLevel(block.dimensionId());
            return level != null && level.hasChunkAt(blockPos(block));
        }
    }

    private final class MinecraftWorldState implements WorldState {
        @Override
        public NativeResult writeMarker(String markerId, Map<String, Object> payload, NativeMutationContext context) {
            return saveData.write(new NativeSaveData("world_markers", markerId, payload), context);
        }

        @Override
        public NativeResult writeWeatherState(String stateId, Map<String, Object> payload, NativeMutationContext context) {
            return saveData.write(new NativeSaveData("weather_state", stateId, payload), context);
        }

        @Override
        public NativeResult writeRouteState(String routeId, Map<String, Object> payload, NativeMutationContext context) {
            return saveData.write(new NativeSaveData("route_state", routeId, payload), context);
        }
    }

    private final class MinecraftStructures implements Structures {
        @Override
        public NativeResult placeStructure(NativeStructurePlacement placement, NativeMutationContext context) {
            ServerLevel level = hostContext.resolveLevel(placement.dimensionId());
            if (level == null) {
                return result(false, "FAIL_DIMENSION_UNAVAILABLE", "Structure dimension was not loaded.", context,
                        structurePayload(placement), structureTarget(placement), Map.of(), Map.of(), false, false);
            }
            BlockPos origin = new BlockPos(placement.originX(), placement.originY(), placement.originZ());
            Map<String, Object> before = Map.of("originLoaded", level.hasChunkAt(origin));
            if (!level.hasChunkAt(origin)) {
                return result(false, "SKIPPED_CHUNK_UNLOADED", "Structure origin chunk was not loaded.", context,
                        structurePayload(placement), structureTarget(placement), before, before, false, false);
            }
            if ("echoashfallprotocol:drop_pod".equals(placement.structureId()) || "drop_pod".equals(placement.structureId())) {
                BlockPos interior = ProceduralStructureGenerator.placeStartingDropPod(level, origin, level.getRandom());
                if (interior == null) {
                    return result(false, "FAIL_STRUCTURE_NOT_PLACED", "Personal drop pod structure placement returned no interior.", context,
                            structurePayload(placement), structureTarget(placement), before, Map.of(), false, false);
                }
                StartingDropPodData.get(level).addOrReplace(hostContext.player().getUUID(), origin, interior);
                Map<String, Object> marker = new LinkedHashMap<>();
                marker.put("structure", "echoashfallprotocol:drop_pod");
                marker.put("origin", positionSnapshot(origin));
                marker.put("interior", positionSnapshot(interior));
                marker.put("playerId", hostContext.player().getUUID().toString());
                NativeResult saveResult =
                        saveData.write(new NativeSaveData("structure_placements", structureMarkerId(placement, origin), marker), context);
                marker.put("runtimeStructurePlaced", true);
                marker.put("runtimeStructureMutated", true);
                marker.put("runtimeSaveDataTouched", saveResult.mutated());
                marker.put("liveSaveDataFileTouched", Boolean.TRUE.equals(saveResult.snapshot().get("liveSaveDataFileTouched")));
                marker.put("runtimeSaveDataBackend", saveResult.snapshot().getOrDefault("runtimeSaveDataBackend", ""));
                marker.put("saveFile", saveResult.snapshot().getOrDefault("saveFile", ""));
                return result(true, "MUTATED", "Placed personal drop pod structure.", context,
                        marker,
                        structureTarget(placement),
                        before,
                        marker,
                        true,
                        false);
            }

            StructureType type = structureType(placement.structureId());
            if (type == null) {
                return result(false, "UNSUPPORTED_STRUCTURE", "Structure id was not registered in the procedural structure path.", context,
                        structurePayload(placement), structureTarget(placement), before, before, false, false);
            }
            String bossBiomeOverride = stringValue(placement.constraints(), "bossBiomeOverride");
            ProceduralStructureGenerator.generateStructure(level, origin, type, level.getRandom(), bossBiomeOverride.isBlank() ? null : bossBiomeOverride);
            Map<String, Object> marker = new LinkedHashMap<>();
            marker.put("structure", canonicalStructureId(type));
            marker.put("origin", positionSnapshot(origin));
            marker.put("anchor", placement.anchor());
            marker.put("playerId", hostContext.player().getUUID().toString());
            NativeResult saveResult =
                    saveData.write(new NativeSaveData("structure_placements", structureMarkerId(placement, origin), marker), context);
            markQuestLocation(hostContext.player(), "structure", structureMarkerId(placement, origin));
            marker.put("runtimeStructurePlaced", true);
            marker.put("runtimeStructureMutated", true);
            marker.put("runtimeSaveDataTouched", saveResult.mutated());
            marker.put("liveSaveDataFileTouched", Boolean.TRUE.equals(saveResult.snapshot().get("liveSaveDataFileTouched")));
            marker.put("runtimeSaveDataBackend", saveResult.snapshot().getOrDefault("runtimeSaveDataBackend", ""));
            marker.put("saveFile", saveResult.snapshot().getOrDefault("saveFile", ""));
            return result(true, "MUTATED", "Placed procedural Minecraft structure.", context,
                    marker,
                    structureTarget(placement),
                    before,
                    marker,
                    true,
                    false);
        }
    }

    private final class MinecraftBlockEntities implements BlockEntities {
        @Override
        public NativeResult tick(NativeBlockRef block, NativeMutationContext context) {
            ServerLevel level = hostContext.resolveLevel(block.dimensionId());
            BlockPos pos = blockPos(block);
            if (level == null || !level.hasChunkAt(pos)) {
                return result(false, "SKIPPED_CHUNK_UNLOADED", "Block entity chunk is not loaded.", context,
                        Map.of("block", blockSnapshot(block)), blockTarget(block), Map.of("loaded", false), Map.of("loaded", false), false, false);
            }
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity == null) {
                return result(false, "SKIPPED_NO_BLOCK_ENTITY", "No live block entity exists at the target.", context,
                        Map.of("block", blockSnapshot(block)), blockTarget(block), Map.of(), Map.of(), false, false);
            }
            Map<String, Object> before = blockEntityState(level, pos, level.getBlockState(pos), entity);
            boolean supported = tickBlockEntity(level, pos, level.getBlockState(pos), entity);
            Map<String, Object> after = blockEntityState(level, pos, level.getBlockState(pos), entity);
            if (!supported) {
                return result(false, "UNSUPPORTED_BLOCK_ENTITY_TICK", "Block entity type has no host-wired Minecraft tick path.", context,
                        after, blockTarget(block), before, after, false, false);
            }
            boolean changed = !before.equals(after);
            return result(changed, changed ? "MUTATED" : "NOOP", changed
                            ? "Ticked live Minecraft block entity and mutated state."
                            : "Ticked live Minecraft block entity without a state change.",
                    context,
                    copyPayload(after, Map.of(
                            "runtimeBlockEntityTouched", changed,
                            "runtimeBlockEntityMutated", changed)),
                    blockTarget(block),
                    before,
                    after,
                    changed,
                    false);
        }

        @Override
        public NativeBlockEntitySnapshot snapshot(NativeBlockRef block, NativeMutationContext context) {
            ServerLevel level = hostContext.resolveLevel(block.dimensionId());
            BlockPos pos = blockPos(block);
            if (level == null || !level.hasChunkAt(pos)) {
                return new NativeBlockEntitySnapshot("minecraft:missing", block, Map.of("loaded", false));
            }
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity == null) {
                return new NativeBlockEntitySnapshot("minecraft:empty", block, Map.of("blockState", nativeBlockState(level.getBlockState(pos)).blockId()));
            }
            return new NativeBlockEntitySnapshot(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(entity.getType()).toString(),
                    block,
                    blockEntityState(level, pos, level.getBlockState(pos), entity));
        }

        @Override
        public NativeResult applySnapshot(NativeBlockEntitySnapshot snapshot, NativeMutationContext context) {
            ServerLevel level = hostContext.resolveLevel(snapshot.block().dimensionId());
            BlockPos pos = blockPos(snapshot.block());
            if (level == null || !level.hasChunkAt(pos)) {
                return result(false, "SKIPPED_CHUNK_UNLOADED", "Block entity chunk is not loaded for snapshot application.", context,
                        snapshot.state(), blockTarget(snapshot.block()), Map.of("loaded", false), Map.of("loaded", false), false, false);
            }
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity == null) {
                return result(false, "SKIPPED_NO_BLOCK_ENTITY", "No live block entity exists for snapshot application.", context,
                        snapshot.state(), blockTarget(snapshot.block()), Map.of(), Map.of(), false, false);
            }
            Map<String, Object> before = blockEntityState(level, pos, level.getBlockState(pos), entity);
            boolean changed = applyBlockEntitySnapshot(entity, snapshot.state());
            if (changed) {
                entity.setChanged();
                level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
            }
            Map<String, Object> after = blockEntityState(level, pos, level.getBlockState(pos), entity);
            return result(changed, changed ? "MUTATED" : "NOOP", changed
                            ? "Applied snapshot to live Minecraft block entity state."
                            : "Snapshot matched live Minecraft block entity state.",
                    context,
                    copyPayload(after, Map.of(
                            "requestedState", snapshot.state(),
                            "runtimeBlockEntityTouched", changed,
                            "runtimeBlockEntityMutated", changed)),
                    blockTarget(snapshot.block()),
                    before,
                    after,
                    changed,
                    false);
        }
    }

    private final class MinecraftCapabilities implements Capabilities {
        @Override
        public NativeResult insertItem(NativeCapabilityRequest request, NativeItemStack stack, NativeMutationContext context) {
            BlockEntity entity = blockEntity(request.block());
            Container inventory = inventory(entity);
            if (entity == null || inventory == null) {
                return result(false, "UNSUPPORTED_CAPABILITY", "Target block entity exposes no live item inventory capability.", context,
                        Map.of("capability", request.capabilityId(), "item", stack.itemId()), blockTarget(request.block()), Map.of(), Map.of(), false, false);
            }
            ItemStack nativeStack = nativeStack(stack);
            if (nativeStack.isEmpty()) {
                return result(false, "FAIL_INVALID_ITEM", "Capability insertion item was not registered.", context,
                        Map.of("capability", request.capabilityId(), "item", stack.itemId()), blockTarget(request.block()), capabilityState(request), capabilityState(request), false, false);
            }
            Map<String, Object> before = capabilityState(request);
            int inserted = insertIntoInventory(entity, inventory, nativeStack, side(request.side()));
            if (inserted > 0) {
                entity.setChanged();
            }
            Map<String, Object> after = capabilityState(request);
            return result(inserted > 0, inserted > 0 ? "MUTATED" : "NOOP",
                    inserted > 0 ? "Inserted item into live Minecraft block capability." : "No capability slot accepted the item.",
                    context,
                    copyPayload(after, Map.of(
                            "inserted", inserted,
                            "requested", stack.count(),
                            "item", stack.itemId(),
                            "runtimeCapabilityTouched", inserted > 0,
                            "runtimeCapabilityMutated", inserted > 0)),
                    blockTarget(request.block()),
                    before,
                    after,
                    inserted > 0,
                    false);
        }

        @Override
        public NativeResult extractItem(NativeCapabilityRequest request, String itemId, int count, NativeMutationContext context) {
            BlockEntity entity = blockEntity(request.block());
            Container inventory = inventory(entity);
            if (entity == null || inventory == null) {
                return result(false, "UNSUPPORTED_CAPABILITY", "Target block entity exposes no live item inventory capability.", context,
                        Map.of("capability", request.capabilityId(), "item", itemId, "count", count), blockTarget(request.block()), Map.of(), Map.of(), false, false);
            }
            Map<String, Object> before = capabilityState(request);
            ItemStack extracted = extractFromInventory(entity, inventory, itemId, Math.max(1, count), side(request.side()));
            if (!extracted.isEmpty()) {
                entity.setChanged();
            }
            Map<String, Object> after = capabilityState(request);
            return result(!extracted.isEmpty(), !extracted.isEmpty() ? "MUTATED" : "NOOP",
                    !extracted.isEmpty() ? "Extracted item from live Minecraft block capability." : "No matching item was available to extract.",
                    context,
                    copyPayload(after, Map.of(
                            "extracted", extracted.getCount(),
                            "item", itemId(extracted),
                            "runtimeCapabilityTouched", !extracted.isEmpty(),
                            "runtimeCapabilityMutated", !extracted.isEmpty())),
                    blockTarget(request.block()),
                    before,
                    after,
                    !extracted.isEmpty(),
                    false);
        }

        @Override
        public NativeResult receiveEnergy(NativeCapabilityRequest request, int amount, NativeMutationContext context) {
            BlockEntity entity = blockEntity(request.block());
            if (!(entity instanceof IEnergyStorage energy)) {
                return result(false, "UNSUPPORTED_CAPABILITY", "Target block entity exposes no live energy capability.", context,
                        Map.of("capability", request.capabilityId(), "amount", amount), blockTarget(request.block()), Map.of(), Map.of(), false, false);
            }
            Map<String, Object> before = capabilityState(request);
            int received = energy.receiveEnergy(Math.max(1, amount), false);
            if (received > 0) {
                entity.setChanged();
            }
            Map<String, Object> after = capabilityState(request);
            return result(received > 0, received > 0 ? "MUTATED" : "NOOP",
                    received > 0 ? "Received energy into live Minecraft block capability." : "Energy capability rejected the receive request.",
                    context,
                    copyPayload(after, Map.of(
                            "received", received,
                            "requested", amount,
                            "runtimeCapabilityTouched", received > 0,
                            "runtimeCapabilityMutated", received > 0)),
                    blockTarget(request.block()),
                    before,
                    after,
                    received > 0,
                    false);
        }

        @Override
        public NativeResult extractEnergy(NativeCapabilityRequest request, int amount, NativeMutationContext context) {
            BlockEntity entity = blockEntity(request.block());
            if (!(entity instanceof IEnergyStorage energy)) {
                return result(false, "UNSUPPORTED_CAPABILITY", "Target block entity exposes no live energy capability.", context,
                        Map.of("capability", request.capabilityId(), "amount", amount), blockTarget(request.block()), Map.of(), Map.of(), false, false);
            }
            Map<String, Object> before = capabilityState(request);
            int extracted = energy.extractEnergy(Math.max(1, amount), false);
            if (extracted > 0) {
                entity.setChanged();
            }
            Map<String, Object> after = capabilityState(request);
            return result(extracted > 0, extracted > 0 ? "MUTATED" : "NOOP",
                    extracted > 0 ? "Extracted energy from live Minecraft block capability." : "Energy capability rejected the extract request.",
                    context,
                    copyPayload(after, Map.of(
                            "extracted", extracted,
                            "requested", amount,
                            "runtimeCapabilityTouched", extracted > 0,
                            "runtimeCapabilityMutated", extracted > 0)),
                    blockTarget(request.block()),
                    before,
                    after,
                    extracted > 0,
                    false);
        }

        @Override
        public Map<String, Object> readCapability(NativeCapabilityRequest request, NativeMutationContext context) {
            return capabilityState(request);
        }
    }

    private final class MinecraftEvents implements Events {
        @Override
        public NativeResult publish(NativeEvent event, NativeMutationContext context) {
            ServerPlayer player = event.player() == null ? hostContext.player() : hostContext.resolvePlayer(event.player());
            if (player == null) {
                return result(false, "INVALID_PLAYER", "Native event player did not match an online player.", context,
                        Map.of("event", event.eventId()), NativeMutationTarget.none(), Map.of(), Map.of(), false, true);
            }
            String eventId = event.eventId();
            Map<String, Object> payload = event.payload();
            if ("client_tick".equals(eventId) || "world_tick".equals(eventId)) {
                boolean recorded = AshfallAdapterCoreMissionTriggerRuntime.worldTick(player);
                return finishGameplayEvent(player, event, context, recorded,
                        recorded ? "Published native UI tick into MissionCore." : "Native UI tick was saved without matching mission progress.",
                        Map.of("ticks", intValue(payload.get("ticks"), 1), "missionAdvanced", recorded));
            }
            if ("ashfall.special_marker".equals(eventId)) {
                String marker = firstNonBlank(stringValue(payload, "marker"), stringValue(payload, "target"));
                boolean marked = markQuestLocation(player, "special", marker);
                if (marked) {
                    marked |= recordMission(player, MissionObjectiveType.CUSTOM, marker, 1, payload);
                }
                return finishGameplayEvent(player, event, context, marked,
                        marked ? "Published special marker into QuestData and MissionCore." : "Special marker was already recorded.",
                        Map.of("marker", marker, "markerRecorded", marked));
            }
            if (MinecraftEchoRuntimeHost.isItemCollectedEvent(eventId)) {
                boolean recorded = recordItemCollected(player, payload);
                return finishGameplayEvent(player, event, context, recorded,
                        recorded ? "Published item collection into MissionCore." : "Item collection event was saved without matching mission progress.",
                        Map.of("item", MinecraftEchoRuntimeHost.itemTarget(payload), "missionAdvanced", recorded));
            }
            if (MinecraftEchoRuntimeHost.isItemUsedEvent(eventId)) {
                boolean recorded = recordItemUsed(player, payload);
                return finishGameplayEvent(player, event, context, recorded,
                        recorded ? "Published item use into MissionCore." : "Item use event was saved without matching mission progress.",
                        Map.of("item", MinecraftEchoRuntimeHost.itemTarget(payload), "missionAdvanced", recorded));
            }
            if ("player.recipe_crafted".equals(eventId)) {
                boolean recorded = recordRecipeCrafted(player, payload);
                return finishGameplayEvent(player, event, context, recorded,
                        recorded ? "Published crafted item into MissionCore." : "Crafted-item event was saved without matching mission progress.",
                        Map.of("item", MinecraftEchoRuntimeHost.itemTarget(payload), "missionAdvanced", recorded));
            }
            if (MinecraftEchoRuntimeHost.isBlockPlacedEvent(eventId)) {
                String blockId = firstNonBlank(
                        firstNonBlank(stringValue(payload, "block"), stringValue(payload, "blockId")),
                        stringValue(payload, "target"));
                boolean recorded = recordBlockPlacement(player, blockId, payload);
                return finishGameplayEvent(player, event, context, recorded,
                        recorded ? "Published block placement into QuestData and MissionCore." : "Block placement event did not include a block id.",
                        Map.of("block", blockId, "blockPlacementRecorded", recorded));
            }
            if ("player.shelter_slept".equals(eventId)) {
                boolean recorded = markQuestLocation(player, "special", "shelter:slept");
                recorded |= recordMission(player, MissionObjectiveType.CUSTOM, "ashfall:sleep_shelter", 1, payload);
                return finishGameplayEvent(player, event, context, recorded,
                        recorded ? "Published shelter rest into QuestData and MissionCore." : "Shelter rest event was saved without matching mission progress.",
                        Map.of("marker", "shelter:slept", "missionAdvanced", recorded));
            }
            if (MinecraftEchoRuntimeHost.isScannerUsedEvent(eventId)) {
                boolean recorded = recordScannerUsed(player, payload);
                return finishGameplayEvent(player, event, context, recorded,
                        recorded ? "Published scanner use into exploration state and MissionCore." : "Scanner use event was saved without matching mission progress.",
                        Map.of("scanTarget", firstNonBlank(stringValue(payload, "scanTarget"), stringValue(payload, "target")),
                                "missionAdvanced", recorded));
            }
            if (MinecraftEchoRuntimeHost.isRegionEnteredEvent(eventId)) {
                boolean recorded = recordRegionEntered(player, payload);
                return finishGameplayEvent(player, event, context, recorded,
                        recorded ? "Published region or POI discovery into QuestData and MissionCore." : "Region event was saved without matching mission progress.",
                        Map.of("target", firstNonBlank(stringValue(payload, "siteId"), stringValue(payload, "target")),
                                "missionAdvanced", recorded));
            }
            if (MinecraftEchoRuntimeHost.isTerminalOpenedEvent(eventId)) {
                boolean recorded = recordTerminalOpened(player, payload);
                return finishGameplayEvent(player, event, context, recorded,
                        recorded ? "Published terminal or recovery cache opening into QuestData and MissionCore." : "Terminal event was saved without matching mission progress.",
                        Map.of("terminalId", MinecraftEchoRuntimeHost.terminalTarget(payload), "missionAdvanced", recorded));
            }
            if ("command_execution".equals(eventId)) {
                boolean recorded = recordTerminalCommand(player, payload);
                return finishGameplayEvent(player, event, context, recorded,
                        recorded ? "Published terminal command into QuestData and MissionCore." : "Terminal command was saved without matching mission progress.",
                        Map.of(
                                "command", stringValue(payload, "command"),
                                "terminalId", MinecraftEchoRuntimeHost.terminalTarget(payload),
                                "missionAdvanced", recorded));
            }
            if (MinecraftEchoRuntimeHost.isMachinePoweredEvent(eventId)) {
                boolean recorded = recordMachinePowered(player, payload);
                return finishGameplayEvent(player, event, context, recorded,
                        recorded ? "Published machine power event into QuestData and MissionCore." : "Machine power event was saved without matching mission progress.",
                        Map.of("machineId", MinecraftEchoRuntimeHost.machineTarget(payload), "missionAdvanced", recorded));
            }
            if (EchoCanonicalContentIds.EVENT_MACHINE_OUTPUT_CREATED.equals(eventId)) {
                boolean recorded = recordMission(player, MissionObjectiveType.CUSTOM, eventId, 1, payload);
                return finishGameplayEvent(player, event, context, recorded,
                        recorded ? "Published machine output event into MissionCore." : "Machine output event was saved without matching mission progress.",
                        Map.of("target", firstNonBlank(stringValue(payload, "output"), stringValue(payload, "target")),
                                "missionAdvanced", recorded));
            }
            if (MinecraftEchoRuntimeHost.isNativeUiActionEvent(eventId)) {
                boolean recorded = recordNativeUiAction(player, eventId, payload);
                return finishGameplayEvent(player, event, context, recorded,
                        recorded ? "Published native UI action into QuestData and MissionCore." : "Native UI action was saved without matching mission progress.",
                        Map.of(
                                "action", firstNonBlank(stringValue(payload, "action"), eventId),
                                "target", MinecraftEchoRuntimeHost.eventTarget(payload),
                                "missionAdvanced", recorded));
            }
            if (EchoCanonicalContentIds.EVENT_MISSION_OBJECTIVE_COMPLETED.equals(eventId)
                    || EchoCanonicalContentIds.EVENT_MISSION_COMPLETED.equals(eventId)) {
                return finishGameplayEvent(player, event, context, true,
                        "Published canonical mission feedback event.",
                        Map.of("target", stringValue(payload, "target"), "missionAdvanced", true));
            }
            return result(false, "UNSUPPORTED_EVENT", "Native event id is not wired through this host slice yet.", context,
                    Map.of("event", eventId), target(new NativePlayerRef(player.getUUID().toString())), Map.of(), Map.of(), false, true);
        }
    }

    private final class MinecraftPackets implements Packets {
        @Override
        public NativeResult sendToPlayer(NativePacket packet, NativeMutationContext context) {
            ServerPlayer player = hostContext.resolvePlayer(packet.player());
            if (player == null) {
                return result(false, "INVALID_PLAYER", "Native packet player did not match an online player.", context,
                        Map.of("packet", packet.packetId()), NativeMutationTarget.none(), Map.of(), Map.of(), false, true);
            }
            boolean nativeLoaderProcess = nativeLoaderProcess();
            boolean sent = false;
            String packetFailure = "";
            if (!nativeLoaderProcess) {
                try {
                    CustomPacketPayload payload = packetPayload(packet);
                    sent = EchoNetSend.toPlayer(player, payload, packetKind(packet.channel()));
                } catch (Throwable failure) {
                    packetFailure = failureSummary(failure);
                }
            }
            boolean fallbackShown = sent ? false : sendPacketFallback(player, packet);
            return result(sent || fallbackShown,
                    sent ? "MUTATED" : fallbackShown ? "MUTATED_PACKET_FALLBACK" : "FAILED_PACKET_SEND",
                    sent
                            ? "Sent native packet through EchoNetSend.toPlayer."
                            : fallbackShown
                                    ? nativeLoaderProcess
                                            ? "Published native packet through Native Loader visible host fallback."
                                            : "EchoNetSend.toPlayer rejected the packet; published visible host fallback."
                                    : nativeLoaderProcess
                                            ? "Native Loader packet fallback had no visible payload."
                                            : "EchoNetSend.toPlayer rejected the packet and no fallback payload was available.",
                    context,
                    Map.of(
                            "packet", packet.packetId(),
                            "channel", packet.channel(),
                            "packetSent", sent,
                            "runtimePacketSent", sent || fallbackShown,
                            "runtimePacketMutated", sent || fallbackShown,
                            "runtimePacketChannel", packet.channel(),
                            "packetFailure", packetFailure,
                            "nativeLoaderPacketFallback", nativeLoaderProcess,
                            "fallbackShown", fallbackShown),
                    target(packet.player()),
                    Map.of(),
                    Map.of("sent", sent, "fallbackShown", fallbackShown),
                    false,
                    true);
        }

        @Override
        public NativeResult broadcast(NativePacket packet, NativeMutationContext context) {
            boolean nativeLoaderProcess = nativeLoaderProcess();
            int sent = 0;
            String packetFailure = "";
            if (!nativeLoaderProcess) {
                try {
                    CustomPacketPayload payload = packetPayload(packet);
                    sent = EchoNetSend.toAllPlayers(hostContext.level().getServer(), payload, packetKind(packet.channel()));
                } catch (Throwable failure) {
                    packetFailure = failureSummary(failure);
                }
            }
            int fallbackShown = sent > 0 ? 0 : sendPacketFallbackToAllPlayers(packet);
            return result(sent > 0 || fallbackShown > 0,
                    sent > 0 ? "MUTATED" : fallbackShown > 0 ? "MUTATED_PACKET_FALLBACK" : "FAILED_PACKET_BROADCAST",
                    sent > 0
                            ? "Broadcast native packet through EchoNetSend."
                            : fallbackShown > 0
                                    ? nativeLoaderProcess
                                            ? "Broadcast native packet through Native Loader visible host fallback."
                                            : "Broadcast native packet through visible host fallback."
                                    : "No players received the native packet and no fallback payload was available.",
                    context,
                    Map.of(
                            "packet", packet.packetId(),
                            "channel", packet.channel(),
                            "packetFailure", packetFailure,
                            "nativeLoaderPacketFallback", nativeLoaderProcess,
                            "sentPlayers", sent,
                            "fallbackPlayers", fallbackShown,
                            "runtimePacketSent", sent > 0 || fallbackShown > 0,
                            "runtimePacketMutated", sent > 0 || fallbackShown > 0,
                            "runtimePacketChannel", packet.channel()),
                    NativeMutationTarget.none(),
                    Map.of(),
                    Map.of("sentPlayers", sent, "fallbackPlayers", fallbackShown),
                    false,
                    true);
        }
    }

    private final class MinecraftHud implements Hud {
        @Override
        public NativeResult publishNotification(NativePlayerRef playerRef, Map<String, Object> payload, NativeMutationContext context) {
            ServerPlayer player = hostContext.resolvePlayer(playerRef);
            if (player == null) {
                return result(false, "INVALID_PLAYER", "Native HUD player did not match an online player.", context,
                        payload, NativeMutationTarget.none(), Map.of(), Map.of(), false, true);
            }
            boolean packetSent = false;
            String packetFailure = "";
            boolean nativeLoaderProcess = nativeLoaderProcess();
            if (!nativeLoaderProcess) {
                try {
                    CompoundTag tag = compoundFromMap(payload);
                    packetSent = EchoNetSend.toPlayer(
                            player,
                            new EchoSyncPayload(EchoSyncType.VISUAL_STATE, RUNTIME_HUD_CHANNEL, null, tag),
                            EchoPacketKind.CLIENTBOUND_SYNC);
                } catch (Throwable failure) {
                    packetFailure = failureSummary(failure);
                }
            }
            boolean chatShown = sendChatFallback(player, payload);
            return result(packetSent || chatShown, packetSent || chatShown ? "MUTATED" : "FAILED_HUD_NOTIFICATION",
                    packetSent
                            ? "Published native HUD notification packet."
                            : nativeLoaderProcess
                                    ? "Published native HUD notification through Native Loader chat fallback."
                                    : "Published native HUD notification through chat fallback.",
                    context,
                    copyPayload(payload, Map.of(
                            "packetSent", packetSent,
                            "packetFailure", packetFailure,
                            "nativeLoaderHudFallback", nativeLoaderProcess,
                            "chatFallbackShown", chatShown,
                            "runtimeHudNotificationPublished", packetSent || chatShown,
                            "runtimeHudNotificationMutated", packetSent || chatShown)),
                    target(playerRef),
                    Map.of(),
                    Map.of("packetSent", packetSent, "chatFallbackShown", chatShown),
                    false,
                    true);
        }
    }

    private final class MinecraftSaveData implements SaveData {
        @Override
        public NativeResult write(NativeSaveData data, NativeMutationContext context) {
            Map<String, Object> before = read(data.scope(), data.key(), context);
            Map<String, Object> after = writeNativeSaveData(hostContext.player(), data);
            return result(true, "MUTATED", "Wrote native host save data into the live world save file.", context,
                    copyPayload(after, Map.of(
                            "payload", data.payload(),
                            "runtimeSaveDataTouched", true,
                            "liveSaveDataFileTouched", true,
                            "runtimeSaveDataBackend", "world_save_file")),
                    target(hostContext.playerRef()),
                    before,
                    after,
                    true,
                    false);
        }

        @Override
        public Map<String, Object> read(String scope, String key, NativeMutationContext context) {
            String prefix = nativeSaveArchivePrefix(scope, key);
            String snapshot = "";
            Path file = nativeSaveDataFile(hostContext.player());
            try {
                if (Files.exists(file)) {
                    for (String entry : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                        if (entry.startsWith(prefix)) {
                            snapshot = entry;
                        }
                    }
                }
            } catch (Exception ignored) {
                snapshot = "";
            }
            return Map.of(
                    "scope", scope == null ? "" : scope,
                    "key", key == null ? "" : key,
                    "present", !snapshot.isBlank(),
                    "snapshot", snapshot,
                    "saveFile", file.toString());
        }

        @Override
        public NativeResult delete(String scope, String key, NativeMutationContext context) {
            String prefix = nativeSaveArchivePrefix(scope, key);
            Path file = nativeSaveDataFile(hostContext.player());
            boolean removed = false;
            try {
                if (Files.exists(file)) {
                    List<String> lines = new ArrayList<>(Files.readAllLines(file, StandardCharsets.UTF_8));
                    removed = lines.removeIf(entry -> entry.startsWith(prefix));
                    if (removed) {
                        Files.write(file, lines, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
                    }
                }
            } catch (Exception ignored) {
                removed = false;
            }
            return result(removed, removed ? "MUTATED" : "SKIPPED_SAVE_KEY_MISSING",
                    removed ? "Deleted native host save data from the live world save file." : "Native host save key was not present.",
                    context,
                    Map.of(
                            "scope", scope == null ? "" : scope,
                            "key", key == null ? "" : key,
                            "saveFile", file.toString(),
                            "runtimeSaveDataTouched", removed,
                            "liveSaveDataFileTouched", removed,
                            "runtimeSaveDataBackend", "world_save_file"),
                    target(hostContext.playerRef()),
                    Map.of("present", removed),
                    Map.of("present", false),
                    removed,
                    false);
        }
    }

    private final class MinecraftRuntimeSurfaces implements RuntimeSurfaces {
        @Override
        public NativeResult clientTick(String phase, Map<String, Object> payload, NativeMutationContext context) {
            return runtimeSurfaceMutation("client_tick", phase, payload, context, true, false);
        }

        @Override
        public NativeResult renderLayer(String layerId, Map<String, Object> payload, NativeMutationContext context) {
            return runtimeSurfaceMutation("render_layers", layerId, payload, context, true, false);
        }

        @Override
        public NativeResult screenEvent(String screenId, String eventType, Map<String, Object> payload, NativeMutationContext context) {
            Map<String, Object> details = new LinkedHashMap<>(payload == null ? Map.of() : payload);
            details.put("eventType", eventType == null ? "" : eventType);
            return runtimeSurfaceMutation("screen_events", screenId, details, context, true, false);
        }

        @Override
        public NativeResult keybind(String keybindId, String action, Map<String, Object> payload, NativeMutationContext context) {
            Map<String, Object> details = new LinkedHashMap<>(payload == null ? Map.of() : payload);
            details.put("action", action == null ? "" : action);
            return runtimeSurfaceMutation("keybinds", keybindId, details, context, true, false);
        }

        @Override
        public NativeResult registerCommand(
                String moduleId,
                String commandId,
                String targetSurface,
                String targetBridge,
                Map<String, Object> evidence,
                NativeMutationContext context) {
            Map<String, Object> details = new LinkedHashMap<>(evidence == null ? Map.of() : evidence);
            details.put("moduleId", moduleId == null ? "" : moduleId);
            details.put("targetSurface", targetSurface == null ? "" : targetSurface);
            details.put("targetBridge", targetBridge == null ? "" : targetBridge);
            return runtimeSurfaceMutation("commands", commandId, details, context, false, false);
        }

        @Override
        public NativeResult registerNetworkPacket(
                String moduleId,
                String packetId,
                String surface,
                String sourceRuntimeTarget,
                List<String> consumers,
                Map<String, Object> evidence,
                NativeMutationContext context) {
            Map<String, Object> details = new LinkedHashMap<>(evidence == null ? Map.of() : evidence);
            details.put("moduleId", moduleId == null ? "" : moduleId);
            details.put("surface", surface == null ? "" : surface);
            details.put("sourceRuntimeTarget", sourceRuntimeTarget == null ? "" : sourceRuntimeTarget);
            details.put("consumers", consumers == null ? List.of() : List.copyOf(consumers));
            return runtimeSurfaceMutation("network_channels", packetId, details, context, false, true);
        }

        @Override
        public NativeResult reloadConfig(String moduleId, String configId, String scope, Map<String, Object> evidence, NativeMutationContext context) {
            Map<String, Object> details = new LinkedHashMap<>(evidence == null ? Map.of() : evidence);
            details.put("moduleId", moduleId == null ? "" : moduleId);
            details.put("scope", scope == null ? "" : scope);
            return runtimeSurfaceMutation("config_reloads", configId, details, context, false, false);
        }

        @Override
        public NativeResult reloadResources(String moduleId, String resourceId, String scope, Map<String, Object> evidence, NativeMutationContext context) {
            Map<String, Object> details = new LinkedHashMap<>(evidence == null ? Map.of() : evidence);
            details.put("moduleId", moduleId == null ? "" : moduleId);
            details.put("scope", scope == null ? "" : scope);
            return runtimeSurfaceMutation("resource_reloads", resourceId, details, context, false, false);
        }

        @Override
        public NativeResult saveHook(String hookId, Map<String, Object> payload, NativeMutationContext context) {
            return runtimeSurfaceMutation("save_hooks", hookId, payload, context, false, false);
        }

        @Override
        public NativeResult lifecyclePhase(String moduleId, String phaseId, Map<String, Object> evidence, NativeMutationContext context) {
            Map<String, Object> details = new LinkedHashMap<>(evidence == null ? Map.of() : evidence);
            details.put("moduleId", moduleId == null ? "" : moduleId);
            return runtimeSurfaceMutation("lifecycle_phases", phaseId, details, context, false, false);
        }

        @Override
        public NativeResult publishRuntimeEvent(
                String sourceModule,
                String eventId,
                Map<String, Object> payload,
                String status,
                NativeMutationContext context) {
            Map<String, Object> details = new LinkedHashMap<>(payload == null ? Map.of() : payload);
            details.put("sourceModule", sourceModule == null ? "" : sourceModule);
            details.put("status", status == null ? "" : status);
            return runtimeSurfaceMutation("events", eventId, details, context, true, false);
        }

        @Override
        public NativeResult syncServerClient(String channel, String payload, NativeMutationContext context) {
            return runtimeSurfaceMutation("server_client_sync", channel, Map.of("payload", payload == null ? "" : payload), context, true, true);
        }

        private NativeResult runtimeSurfaceMutation(
                String surface,
                String targetId,
                Map<String, Object> payload,
                NativeMutationContext context,
                boolean publishEvent,
                boolean sendPacket) {
            String safeSurface = safeId(surface);
            String safeTarget = safeId(targetId == null || targetId.isBlank() ? "default" : targetId);
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("surface", surface == null ? "" : surface);
            details.put("targetId", targetId == null ? "" : targetId);
            details.put("payload", payload == null ? Map.of() : Map.copyOf(payload));
            details.put("minecraftRuntimeAccessed", true);
            details.put("liveRuntimeMutationSupported", true);
            NativeMutationContext saveContext = context == null
                    ? MinecraftEchoRuntimeHost.this.context(
                            "minecraft_runtime_surface:" + safeSurface + ":" + safeTarget,
                            "EchoNativeRuntimeHost.RuntimeSurfaces",
                            "runtimeSurfaceMutation")
                    : context;
            NativeResult saveResult = saveData().write(
                    new NativeSaveData("native_loader_runtime_hooks", safeSurface + "/" + safeTarget, Map.copyOf(details)),
                    saveContext);
            NativeResult eventResult = null;
            NativeResult packetResult = null;
            if (saveResult.completedWithMutation() && publishEvent) {
                eventResult = events().publish(
                        new NativeEvent("native_loader." + safeSurface, playerRef(), Map.copyOf(details)),
                        MinecraftEchoRuntimeHost.this.context(
                                "minecraft_runtime_surface_event:" + safeSurface + ":" + safeTarget,
                                "EchoNativeRuntimeHost.Events",
                                "publish"));
            }
            if (saveResult.completedWithMutation() && sendPacket) {
                packetResult = packets().sendToPlayer(
                        new NativePacket("native_loader." + safeSurface, playerRef(), surface == null ? "" : surface, Map.copyOf(details)),
                        MinecraftEchoRuntimeHost.this.context(
                                "minecraft_runtime_surface_packet:" + safeSurface + ":" + safeTarget,
                                "EchoNativeRuntimeHost.Packets",
                                "sendToPlayer"));
            }
            boolean eventMutated = eventResult != null && eventResult.mutated();
            boolean packetMutated = packetResult != null && packetResult.mutated();
            boolean saveTouched = saveResult.mutated();
            boolean requiredEventSatisfied = !publishEvent || eventMutated;
            boolean requiredPacketSatisfied = !sendPacket || packetMutated;
            boolean mutated = saveTouched && requiredEventSatisfied && requiredPacketSatisfied;
            Map<String, Object> resultDetails = new LinkedHashMap<>(details);
            resultDetails.put("runtimeSurfaceSaveStatus", saveResult.status());
            resultDetails.put("runtimeSurfaceSaveMutated", saveTouched);
            resultDetails.put("runtimeSurfaceSaveTouched", saveTouched);
            resultDetails.put("runtimeSaveDataTouched", saveTouched);
            resultDetails.put("liveSaveDataFileTouched", saveTouched);
            resultDetails.put("runtimeSaveDataBackend", "world_save_file");
            resultDetails.put("saveFile", String.valueOf(saveResult.snapshot().getOrDefault("saveFile", "")));
            resultDetails.put("runtimeSurfaceRequiresEvent", publishEvent);
            resultDetails.put("runtimeSurfaceRequiresPacket", sendPacket);
            resultDetails.put("runtimeSurfaceEventPublished", eventMutated);
            resultDetails.put("runtimeSurfacePacketSent", packetMutated);
            resultDetails.put("runtimeSurfaceRequiredEventSatisfied", requiredEventSatisfied);
            resultDetails.put("runtimeSurfaceRequiredPacketSatisfied", requiredPacketSatisfied);
            resultDetails.put("runtimeSurfaceLiveProofSatisfied", mutated);
            resultDetails.put("runtimeSurfaceSaveSnapshot", saveResult.snapshot());
            if (eventResult != null) {
                resultDetails.put("runtimeSurfaceEventStatus", eventResult.status());
                resultDetails.put("runtimeSurfaceEventMutated", eventResult.mutated());
                resultDetails.put("runtimeSurfaceEventSnapshot", eventResult.snapshot());
            }
            if (packetResult != null) {
                resultDetails.put("runtimeSurfacePacketStatus", packetResult.status());
                resultDetails.put("runtimeSurfacePacketMutated", packetResult.mutated());
                resultDetails.put("runtimeSurfacePacketSnapshot", packetResult.snapshot());
            }
            stampSubsystemRuntimeEvidence(
                    resultDetails,
                    surface,
                    targetId,
                    payload,
                    saveTouched,
                    eventMutated,
                    packetMutated,
                    requiredEventSatisfied,
                    requiredPacketSatisfied);
            Map<String, Object> before = Map.of(
                    "savePresentBefore", saveResult.snapshot().getOrDefault("present", false));
            Map<String, Object> after = Map.of(
                    "saveMutated", saveTouched,
                    "saveTouched", saveTouched,
                    "eventMutated", eventMutated,
                    "eventPublished", eventMutated,
                    "packetMutated", packetMutated,
                    "packetSent", packetMutated,
                    "requiredEventSatisfied", requiredEventSatisfied,
                    "requiredPacketSatisfied", requiredPacketSatisfied,
                    "liveProofSatisfied", mutated);
            String status = mutated
                    ? "MUTATED"
                    : saveTouched && (!requiredEventSatisfied || !requiredPacketSatisfied)
                    ? "FAILED_REQUIRED_RUNTIME_SURFACE_EVIDENCE"
                    : saveResult.status();
            return result(
                    mutated,
                    status,
                    mutated
                            ? "Mutated Minecraft runtime surface and recorded all required save/event/packet evidence."
                            : "Minecraft runtime surface did not record all required save/event/packet evidence.",
                    saveContext,
                    Map.copyOf(resultDetails),
                    target(playerRef()),
                    before,
                    after,
                    saveTouched,
                    eventMutated || packetMutated);
        }

        private void stampSubsystemRuntimeEvidence(
                Map<String, Object> resultDetails,
                String surface,
                String targetId,
                Map<String, Object> payload,
                boolean saveTouched,
                boolean eventMutated,
                boolean packetMutated,
                boolean requiredEventSatisfied,
                boolean requiredPacketSatisfied) {
            String safeTarget = targetId == null ? "" : targetId;
            Map<String, Object> safePayload = payload == null ? Map.of() : payload;
            switch (surface == null ? "" : surface) {
                case "commands" -> {
                    resultDetails.put("runtimeCommandRegistryTouched", saveTouched);
                    resultDetails.put("runtimeCommandRegistryMutated", saveTouched);
                    resultDetails.put("runtimeCommandId", safeTarget);
                    resultDetails.put("runtimeCommandModuleId", String.valueOf(safePayload.getOrDefault("moduleId", "")));
                    resultDetails.put("runtimeCommandTargetSurface", String.valueOf(safePayload.getOrDefault("targetSurface", "")));
                    resultDetails.put("runtimeCommandTargetBridge", String.valueOf(safePayload.getOrDefault("targetBridge", "")));
                }
                case "network_channels" -> {
                    resultDetails.put("runtimeNetworkChannelTouched", saveTouched);
                    resultDetails.put("runtimeNetworkChannelMutated", saveTouched && packetMutated && requiredPacketSatisfied);
                    resultDetails.put("runtimeNetworkPacketSent", packetMutated);
                    resultDetails.put("runtimeNetworkChannelId", safeTarget);
                    resultDetails.put("runtimeNetworkModuleId", String.valueOf(safePayload.getOrDefault("moduleId", "")));
                    resultDetails.put("runtimeNetworkConsumers", safePayload.getOrDefault("consumers", List.of()));
                }
                case "config_reloads" -> {
                    resultDetails.put("runtimeConfigReloadTouched", saveTouched);
                    resultDetails.put("runtimeConfigReloadMutated", saveTouched);
                    resultDetails.put("runtimeConfigId", safeTarget);
                    resultDetails.put("runtimeConfigModuleId", String.valueOf(safePayload.getOrDefault("moduleId", "")));
                    resultDetails.put("runtimeConfigScope", String.valueOf(safePayload.getOrDefault("scope", "")));
                }
                case "resource_reloads" -> {
                    resultDetails.put("runtimeResourceReloadTouched", saveTouched);
                    resultDetails.put("runtimeResourceReloadMutated", saveTouched);
                    resultDetails.put("runtimeResourceId", safeTarget);
                    resultDetails.put("runtimeResourceModuleId", String.valueOf(safePayload.getOrDefault("moduleId", "")));
                    resultDetails.put("runtimeResourceScope", String.valueOf(safePayload.getOrDefault("scope", "")));
                }
                case "save_hooks" -> {
                    resultDetails.put("runtimeSaveHookTouched", saveTouched);
                    resultDetails.put("runtimeSaveHookMutated", saveTouched);
                    resultDetails.put("runtimeSaveHookId", safeTarget);
                }
                case "lifecycle_phases" -> {
                    resultDetails.put("runtimeLifecyclePhaseTouched", saveTouched);
                    resultDetails.put("runtimeLifecyclePhaseMutated", saveTouched);
                    resultDetails.put("runtimeLifecyclePhaseId", safeTarget);
                    resultDetails.put("runtimeLifecycleModuleId", String.valueOf(safePayload.getOrDefault("moduleId", "")));
                }
                case "events" -> {
                    resultDetails.put("runtimeEventTouched", saveTouched);
                    resultDetails.put("runtimeEventMutated", saveTouched && eventMutated && requiredEventSatisfied);
                    resultDetails.put("runtimeEventPublished", eventMutated);
                    resultDetails.put("runtimeEventId", safeTarget);
                    resultDetails.put("runtimeEventSourceModule", String.valueOf(safePayload.getOrDefault("sourceModule", "")));
                }
                case "server_client_sync" -> {
                    resultDetails.put("runtimeServerClientSyncTouched", saveTouched);
                    resultDetails.put("runtimeServerClientSyncMutated", saveTouched && eventMutated && packetMutated
                            && requiredEventSatisfied && requiredPacketSatisfied);
                    resultDetails.put("runtimeServerClientSyncChannel", safeTarget);
                    resultDetails.put("runtimeServerClientSyncPacketSent", packetMutated);
                    resultDetails.put("runtimeServerClientSyncEventPublished", eventMutated);
                }
                default -> {
                    // Generic runtime surfaces still carry the shared save/event/packet evidence above.
                }
            }
        }

        private String safeId(String value) {
            if (value == null || value.isBlank()) {
                return "unknown";
            }
            StringBuilder builder = new StringBuilder();
            for (char character : value.toCharArray()) {
                if (Character.isLetterOrDigit(character)
                        || character == ':'
                        || character == '.'
                        || character == '_'
                        || character == '-') {
                    builder.append(character);
                } else {
                    builder.append('_');
                }
            }
            return builder.toString();
        }
    }

    private NativeResult finishGameplayEvent(
            ServerPlayer player,
            NativeEvent event,
            NativeMutationContext context,
            boolean gameplayStateChanged,
            String message,
            Map<String, Object> details) {
        NativePlayerRef playerRef = new NativePlayerRef(player.getUUID().toString());
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("event", event.eventId());
        snapshot.put("eventId", event.eventId());
        snapshot.put("gameplayStateChanged", gameplayStateChanged);
        if (details != null) {
            snapshot.putAll(details);
        }

        Map<String, Object> savePayload = new LinkedHashMap<>();
        savePayload.putAll(event.payload());
        savePayload.putAll(snapshot);
        savePayload.put("gameTime", context.gameTime());
        NativeResult saveResult = saveData.write(
                new NativeSaveData("gameplay_events", MinecraftEchoRuntimeHost.gameplaySaveKey(event.eventId(), event.payload()), savePayload),
                context);

        Map<String, Object> hudPayload = new LinkedHashMap<>();
        hudPayload.put("eventId", event.eventId());
        hudPayload.put("target", MinecraftEchoRuntimeHost.eventTarget(event.payload()));
        hudPayload.put("message", MinecraftEchoRuntimeHost.gameplayHudMessage(event.eventId(), event.payload(), gameplayStateChanged));
        hudPayload.put("missionAdvanced", gameplayStateChanged);
        NativeResult hudResult = hud.publishNotification(playerRef, hudPayload, context);

        boolean saveTouched = saveResult.mutated();
        boolean hudOrEventEmitted = hudResult.mutated();
        boolean mutated = gameplayStateChanged || saveTouched || hudOrEventEmitted;
        snapshot.put("hostSaveTouched", saveTouched);
        snapshot.put("hudOrEventEmitted", hudOrEventEmitted);
        snapshot.put("runtimeEventTouched", saveTouched || hudOrEventEmitted);
        snapshot.put("runtimeEventPublished", hudOrEventEmitted);
        snapshot.put("runtimeEventMutated", mutated);
        snapshot.put("runtimeEventId", event.eventId());
        return result(mutated, mutated ? "MUTATED" : "NOOP", message, context,
                snapshot,
                target(playerRef),
                Map.of(),
                snapshot,
                saveTouched,
                hudOrEventEmitted);
    }

    private NativeResult result(
            boolean mutated,
            String status,
            String message,
            NativeMutationContext context,
            Map<String, Object> snapshot,
            NativeMutationTarget target,
            Map<String, Object> beforeSummary,
            Map<String, Object> afterSummary,
            boolean saveTouched,
            boolean hudOrEventEmitted) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("operationId", context.idempotencyKey());
        data.put("moduleId", context.moduleId());
        data.put("dimensionId", context.dimensionId());
        data.put("logicalSide", context.logicalSide());
        data.put("nativeInterface", context.metadata().getOrDefault("nativeInterface", ""));
        data.put("nativeMethod", context.metadata().getOrDefault("nativeMethod", ""));
        data.put("runtimeHostId", runtimeHostId());
        data.put("compatibilityDelegate", compatibilityDelegateId());
        data.put("realNativeStateMutated", mutated);
        if (snapshot != null) {
            data.putAll(snapshot);
        }
        data.put("hostSaveTouched", saveTouched || Boolean.TRUE.equals(data.get("hostSaveTouched")));
        data.put("saveTouched", saveTouched || Boolean.TRUE.equals(data.get("saveTouched")));
        data.put("hudOrEventEmitted", hudOrEventEmitted || Boolean.TRUE.equals(data.get("hudOrEventEmitted")));
        data.put("feedbackEmitted", hudOrEventEmitted || Boolean.TRUE.equals(data.get("feedbackEmitted")));
        NativeResult result = new NativeResult(mutated, status, message, data);
        try {
            hostContext.ledgerSink().record(new NativeMutationLedgerEntry(
                    context.idempotencyKey(),
                    runtimeHostId(),
                    snapshot,
                    target,
                    beforeSummary,
                    afterSummary,
                    result.resultStatus(),
                    result.failureReason(),
                    saveTouched,
                    hudOrEventEmitted));
        } catch (Throwable ignored) {
            // Optional compatibility ledger storage must not obscure a completed live state mutation.
        }
        return result;
    }

    private NativeMutationTarget target(NativePlayerRef playerRef) {
        return new NativeMutationTarget(playerRef, hostContext.dimensionId(), null, null);
    }

    private NativeMutationTarget blockTarget(NativeBlockRef block) {
        return new NativeMutationTarget(null, block.dimensionId(), null, block);
    }

    private NativeMutationTarget structureTarget(NativeStructurePlacement placement) {
        NativePosition position = new NativePosition(
                placement.dimensionId(),
                placement.originX(),
                placement.originY(),
                placement.originZ(),
                0.0F,
                0.0F);
        return new NativeMutationTarget(hostContext.playerRef(), placement.dimensionId(), position, null);
    }

    private static ItemStack buildItemStack(NativeItemStack stack, Item item) {
        ItemStack itemStack = new ItemStack(item, stack.count());
        Object customNameKey = stack.components().get("customNameKey");
        if (customNameKey instanceof String key && !key.isBlank()) {
            itemStack.set(DataComponents.CUSTOM_NAME, Component.translatable(key));
        }
        Object message = stack.components().get("message");
        if (message instanceof String text && !text.isBlank()) {
            CompoundTag tag = new CompoundTag();
            tag.putString("message", text);
            itemStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
        return itemStack;
    }

    private static boolean hasItem(ServerPlayer player, Item item) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty() && stack.is(item)) {
                return true;
            }
        }
        return false;
    }

    private static Map<String, Object> inventorySummary(ServerPlayer player) {
        int occupiedSlots = 0;
        int itemCount = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty()) {
                occupiedSlots++;
                itemCount += stack.getCount();
            }
        }
        return Map.of(
                "occupiedSlots", occupiedSlots,
                "itemCount", itemCount);
    }

    private static Map<String, Object> playerPosition(ServerPlayer player) {
        return Map.of(
                "dimensionId", player.level().dimension().identifier().toString(),
                "x", player.getX(),
                "y", player.getY(),
                "z", player.getZ(),
                "yaw", player.getYRot(),
                "pitch", player.getXRot());
    }

    private static Map<String, Object> respawnSummary(ServerPlayer player) {
        return Map.of("bound", player.getRespawnConfig() != null);
    }

    private static BlockPos blockPos(NativeBlockRef block) {
        return new BlockPos(block.x(), block.y(), block.z());
    }

    private static Map<String, Object> blockSnapshot(NativeBlockRef block) {
        return Map.of(
                "dimensionId", block.dimensionId(),
                "x", block.x(),
                "y", block.y(),
                "z", block.z());
    }

    private static Map<String, Object> positionSnapshot(BlockPos pos) {
        return Map.of(
                "x", pos.getX(),
                "y", pos.getY(),
                "z", pos.getZ());
    }

    private static Map<String, Object> positionSnapshot(NativePosition position) {
        return Map.of(
                "dimensionId", position.dimensionId(),
                "x", position.x(),
                "y", position.y(),
                "z", position.z(),
                "yaw", position.yaw(),
                "pitch", position.pitch());
    }

    private static BlockState resolveBlockState(NativeBlockState nativeState) {
        Identifier id = Identifier.tryParse(nativeState.blockId());
        if (id == null) {
            return null;
        }
        Block block = BuiltInRegistries.BLOCK.getOptional(id).orElse(Blocks.AIR);
        if (block == Blocks.AIR && !"minecraft:air".equals(nativeState.blockId())) {
            return null;
        }
        BlockState state = block.defaultBlockState();
        for (Map.Entry<String, Object> property : nativeState.properties().entrySet()) {
            state = withProperty(state, property.getKey(), String.valueOf(property.getValue()));
        }
        return state;
    }

    private static Property<?> findProperty(BlockState state, String name) {
        for (Property<?> property : state.getProperties()) {
            if (property.getName().equals(name)) {
                return property;
            }
        }
        return null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static BlockState withProperty(BlockState state, String name, String value) {
        Property property = findProperty(state, name);
        if (property == null) {
            return state;
        }
        Optional parsed = property.getValue(value);
        if (parsed.isEmpty()) {
            return state;
        }
        return state.setValue(property, (Comparable) parsed.get());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static NativeBlockState nativeBlockState(BlockState state) {
        Map<String, Object> properties = new LinkedHashMap<>();
        for (Property property : state.getProperties()) {
            properties.put(property.getName(), property.getName((Comparable) state.getValue(property)));
        }
        return new NativeBlockState(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString(), properties);
    }

    private BlockEntity blockEntity(NativeBlockRef block) {
        ServerLevel level = hostContext.resolveLevel(block.dimensionId());
        BlockPos pos = blockPos(block);
        if (level == null || !level.hasChunkAt(pos)) {
            return null;
        }
        return level.getBlockEntity(pos);
    }

    private static Map<String, Object> blockEntityState(ServerLevel level, BlockPos pos, BlockState state, BlockEntity entity) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("loaded", true);
        snapshot.put("hasBlockEntity", entity != null);
        snapshot.put("blockState", nativeBlockState(state).blockId());
        snapshot.put("position", positionSnapshot(pos));
        if (entity == null) {
            return Map.copyOf(snapshot);
        }
        snapshot.put("blockEntityId", BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(entity.getType()).toString());
        Container inventory = inventory(entity);
        if (inventory != null) {
            snapshot.put("inventorySlots", inventorySlots(inventory));
            snapshot.put("occupiedInventorySlots", occupiedSlots(inventory));
            snapshot.put("totalInventoryItems", totalInventoryItems(inventory));
        }
        if (entity instanceof IEnergyStorage energy) {
            snapshot.put("energyStored", energy.getEnergyStored());
            snapshot.put("energyCapacity", energy.getMaxEnergyStored());
            snapshot.put("canReceiveEnergy", energy.canReceive());
            snapshot.put("canExtractEnergy", energy.canExtract());
        }
        snapshot.put("saveTag", entity.saveWithFullMetadata(level.registryAccess()).toString());
        return Map.copyOf(snapshot);
    }

    private static boolean tickBlockEntity(ServerLevel level, BlockPos pos, BlockState state, BlockEntity entity) {
        if (entity instanceof AtmosphericScrubberBlockEntity typed) {
            AtmosphericScrubberBlockEntity.serverTick(level, pos, state, typed);
        } else if (entity instanceof AutofeedHopperBlockEntity typed) {
            AutofeedHopperBlockEntity.serverTick(level, pos, state, typed);
        } else if (entity instanceof BatteryBankBlockEntity typed) {
            BatteryBankBlockEntity.serverTick(level, pos, state, typed);
        } else if (entity instanceof ContaminantCondenserBlockEntity typed) {
            ContaminantCondenserBlockEntity.serverTick(level, pos, state, typed);
        } else if (entity instanceof CrystallineSynthesizerBlockEntity typed) {
            CrystallineSynthesizerBlockEntity.serverTick(level, pos, state, typed);
        } else if (entity instanceof DeepCoreMinerBlockEntity typed) {
            DeepCoreMinerBlockEntity.serverTick(level, pos, state, typed);
        } else if (entity instanceof FactoryControllerBlockEntity typed) {
            FactoryControllerBlockEntity.serverTick(level, pos, state, typed);
        } else if (entity instanceof FieldMedBayBlockEntity typed) {
            FieldMedBayBlockEntity.serverTick(level, pos, state, typed);
        } else if (entity instanceof FilterWorkbenchBlockEntity typed) {
            FilterWorkbenchBlockEntity.serverTick(level, pos, state, typed);
        } else if (entity instanceof HandRecyclerBlockEntity typed) {
            HandRecyclerBlockEntity.serverTick(level, pos, state, typed);
        } else if (entity instanceof IsotopeRefinerBlockEntity typed) {
            IsotopeRefinerBlockEntity.serverTick(level, pos, state, typed);
        } else if (entity instanceof ItemPipeBlockEntity typed) {
            ItemPipeBlockEntity.serverTick(level, pos, state, typed);
        } else if (entity instanceof LoadDistributorBlockEntity typed) {
            LoadDistributorBlockEntity.serverTick(level, pos, state, typed);
        } else if (entity instanceof MicroGeneratorBlockEntity typed) {
            MicroGeneratorBlockEntity.serverTick(level, pos, state, typed);
        } else if (entity instanceof NexusCapacitorBlockEntity typed) {
            NexusCapacitorBlockEntity.serverTick(level, pos, state, typed);
        } else if (entity instanceof OreGrinderBlockEntity typed) {
            OreGrinderBlockEntity.serverTick(level, pos, state, typed);
        } else if (entity instanceof PowerCableBlockEntity typed) {
            PowerCableBlockEntity.serverTick(level, pos, state, typed);
        } else if (entity instanceof PowerNodeBlockEntity typed) {
            PowerNodeBlockEntity.serverTick(level, pos, state, typed);
        } else if (entity instanceof RadiationCleanserBlockEntity typed) {
            RadiationCleanserBlockEntity.serverTick(level, pos, state, typed);
        } else if (entity instanceof RainCollectorBlockEntity typed) {
            RainCollectorBlockEntity.serverTick(level, pos, state, typed);
        } else if (entity instanceof ScrapDynamoBlockEntity typed) {
            ScrapDynamoBlockEntity.serverTick(level, pos, state, typed);
        } else if (entity instanceof ScrapPressBlockEntity typed) {
            ScrapPressBlockEntity.serverTick(level, pos, state, typed);
        } else if (entity instanceof SignalScannerBlockEntity typed) {
            SignalScannerBlockEntity.serverTick(level, pos, state, typed);
        } else if (entity instanceof ThermalArrayBlockEntity typed) {
            ThermalArrayBlockEntity.serverTick(level, pos, state, typed);
        } else if (entity instanceof ThermalBurnerBlockEntity typed) {
            ThermalBurnerBlockEntity.serverTick(level, pos, state, typed);
        } else if (entity instanceof WaterPurifierBlockEntity typed) {
            WaterPurifierBlockEntity.serverTick(level, pos, state, typed);
        } else {
            return false;
        }
        entity.setChanged();
        return true;
    }

    private static boolean applyBlockEntitySnapshot(BlockEntity entity, Map<String, Object> state) {
        boolean changed = false;
        Container inventory = inventory(entity);
        if (inventory != null && state.containsKey("inventorySlots")) {
            changed |= applyInventorySlots(inventory, state.get("inventorySlots"));
        }
        if (entity instanceof IEnergyStorage energy && state.containsKey("energyStored")) {
            int requested = clamp(intValue(state.get("energyStored"), energy.getEnergyStored()), 0, energy.getMaxEnergyStored());
            if (requested != energy.getEnergyStored()) {
                energy.setEnergyStored(requested);
                changed = true;
            }
        }
        return changed;
    }

    private Map<String, Object> capabilityState(NativeCapabilityRequest request) {
        ServerLevel level = hostContext.resolveLevel(request.block().dimensionId());
        BlockPos pos = blockPos(request.block());
        if (level == null || !level.hasChunkAt(pos)) {
            return Map.of(
                    "status", "UNLOADED",
                    "capability", request.capabilityId(),
                    "block", blockSnapshot(request.block()));
        }
        BlockEntity entity = level.getBlockEntity(pos);
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("status", entity == null ? "EMPTY" : "LIVE");
        state.put("capability", request.capabilityId());
        state.put("block", blockSnapshot(request.block()));
        state.put("side", request.side());
        if (entity != null) {
            state.put("blockEntityId", BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(entity.getType()).toString());
        }
        Container inventory = inventory(entity);
        if (inventory != null) {
            state.put("inventorySlots", inventorySlots(inventory));
            state.put("inputSlots", Arrays.stream(inputSlots(entity, side(request.side()))).boxed().toList());
            state.put("outputSlots", Arrays.stream(outputSlots(entity, side(request.side()))).boxed().toList());
        }
        if (entity instanceof IEnergyStorage energy) {
            state.put("energyStored", energy.getEnergyStored());
            state.put("energyCapacity", energy.getMaxEnergyStored());
            state.put("canReceiveEnergy", energy.canReceive());
            state.put("canExtractEnergy", energy.canExtract());
        }
        return Map.copyOf(state);
    }

    private static Container inventory(BlockEntity entity) {
        if (entity instanceof HopperHandler handler) {
            return handler.getInventory();
        }
        if (entity instanceof Container container) {
            return container;
        }
        Object reflected = invokeNoArg(entity, "getInventory");
        return reflected instanceof Container container ? container : null;
    }

    private static Object invokeNoArg(Object target, String methodName) {
        if (target == null) {
            return null;
        }
        try {
            Method method = target.getClass().getMethod(methodName);
            method.setAccessible(true);
            return method.invoke(target);
        } catch (ReflectiveOperationException | SecurityException ignored) {
            return null;
        }
    }

    private static List<Map<String, Object>> inventorySlots(Container inventory) {
        List<Map<String, Object>> slots = new ArrayList<>();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            slots.add(slotSnapshot(slot, inventory.getItem(slot)));
        }
        return List.copyOf(slots);
    }

    private static Map<String, Object> slotSnapshot(int slot, ItemStack stack) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("slot", slot);
        snapshot.put("item", itemId(stack));
        snapshot.put("count", stack == null || stack.isEmpty() ? 0 : stack.getCount());
        snapshot.put("empty", stack == null || stack.isEmpty());
        return Map.copyOf(snapshot);
    }

    private static int occupiedSlots(Container inventory) {
        int occupied = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (!inventory.getItem(slot).isEmpty()) {
                occupied++;
            }
        }
        return occupied;
    }

    private static int totalInventoryItems(Container inventory) {
        int count = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            count += inventory.getItem(slot).getCount();
        }
        return count;
    }

    private static boolean applyInventorySlots(Container inventory, Object rawSlots) {
        if (!(rawSlots instanceof Iterable<?> slots)) {
            return false;
        }
        boolean changed = false;
        for (Object rawSlot : slots) {
            if (!(rawSlot instanceof Map<?, ?> slotMap)) {
                continue;
            }
            int slot = intValue(slotMap.get("slot"), -1);
            if (slot < 0 || slot >= inventory.getContainerSize()) {
                continue;
            }
            Object rawItemId = slotMap.get("itemId");
            Object rawItem = slotMap.get("item");
            String itemId = firstNonBlank(rawItemId == null ? "" : String.valueOf(rawItemId),
                    rawItem == null ? "" : String.valueOf(rawItem));
            int count = Math.max(0, intValue(slotMap.get("count"), 0));
            ItemStack stack = count <= 0 ? ItemStack.EMPTY : stackFromId(itemId, count);
            ItemStack before = inventory.getItem(slot);
            if (!ItemStack.isSameItemSameComponents(before, stack) || before.getCount() != stack.getCount()) {
                inventory.setItem(slot, stack);
                changed = true;
            }
        }
        if (changed) {
            inventory.setChanged();
        }
        return changed;
    }

    private static ItemStack nativeStack(NativeItemStack stack) {
        return stackFromId(stack.itemId(), stack.count());
    }

    private static ItemStack stackFromId(String itemId, int count) {
        Identifier id = Identifier.tryParse(itemId);
        if (id == null) {
            return ItemStack.EMPTY;
        }
        Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(Items.AIR);
        if (item == Items.AIR) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(item, Math.max(1, count));
    }

    private static int insertIntoInventory(BlockEntity entity, Container inventory, ItemStack stack, Direction side) {
        int remaining = stack.getCount();
        for (int slot : inputSlots(entity, side)) {
            if (remaining <= 0) {
                break;
            }
            if (!canInsert(entity, inventory, slot, stack, side)) {
                continue;
            }
            ItemStack existing = inventory.getItem(slot);
            if (!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing, stack)) {
                continue;
            }
            int limit = Math.min(inventory.getMaxStackSize(), stack.getMaxStackSize());
            int moved = Math.min(remaining, existing.isEmpty() ? limit : limit - existing.getCount());
            if (moved <= 0) {
                continue;
            }
            ItemStack updated = existing.isEmpty() ? stack.copyWithCount(moved) : existing.copy();
            if (!existing.isEmpty()) {
                updated.grow(moved);
            }
            inventory.setItem(slot, updated);
            remaining -= moved;
        }
        if (remaining != stack.getCount()) {
            inventory.setChanged();
        }
        return stack.getCount() - remaining;
    }

    private static ItemStack extractFromInventory(BlockEntity entity, Container inventory, String itemId, int count, Direction side) {
        int remaining = Math.max(1, count);
        ItemStack extracted = ItemStack.EMPTY;
        for (int slot : outputSlots(entity, side)) {
            if (remaining <= 0) {
                break;
            }
            if (!canExtract(entity, slot, side)) {
                continue;
            }
            ItemStack existing = inventory.getItem(slot);
            if (existing.isEmpty() || !matchesItem(existing, itemId)) {
                continue;
            }
            int moved = Math.min(remaining, existing.getCount());
            if (extracted.isEmpty()) {
                extracted = existing.copyWithCount(moved);
            } else if (ItemStack.isSameItemSameComponents(extracted, existing)) {
                extracted.grow(moved);
            } else {
                break;
            }
            existing.shrink(moved);
            if (existing.isEmpty()) {
                inventory.setItem(slot, ItemStack.EMPTY);
            }
            remaining -= moved;
        }
        if (!extracted.isEmpty()) {
            inventory.setChanged();
        }
        return extracted;
    }

    private static int[] inputSlots(BlockEntity entity, Direction side) {
        if (entity instanceof HopperHandler handler) {
            return uniqueSlots(handler.getInputSlots(side == null ? Direction.UP : side));
        }
        Container inventory = inventory(entity);
        return allSlots(inventory);
    }

    private static int[] outputSlots(BlockEntity entity, Direction side) {
        if (entity instanceof HopperHandler handler) {
            return uniqueSlots(handler.getOutputSlots(side == null ? Direction.DOWN : side));
        }
        Container inventory = inventory(entity);
        return allSlots(inventory);
    }

    private static int[] allSlots(Container inventory) {
        if (inventory == null) {
            return new int[0];
        }
        int[] slots = new int[inventory.getContainerSize()];
        for (int i = 0; i < slots.length; i++) {
            slots[i] = i;
        }
        return slots;
    }

    private static int[] uniqueSlots(int[] slots) {
        LinkedHashSet<Integer> unique = new LinkedHashSet<>();
        for (int slot : slots) {
            unique.add(slot);
        }
        return unique.stream().mapToInt(Integer::intValue).toArray();
    }

    private static boolean canInsert(BlockEntity entity, Container inventory, int slot, ItemStack stack, Direction side) {
        if (slot < 0 || slot >= inventory.getContainerSize()) {
            return false;
        }
        if (entity instanceof HopperHandler handler && !handler.canInsertItem(slot, stack)) {
            return false;
        }
        return inventory.canPlaceItem(slot, stack);
    }

    private static boolean canExtract(BlockEntity entity, int slot, Direction side) {
        if (entity instanceof HopperHandler handler) {
            return handler.canExtractItem(slot);
        }
        return true;
    }

    private static boolean matchesItem(ItemStack stack, String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return true;
        }
        Identifier id = Identifier.tryParse(itemId);
        return id != null && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(id);
    }

    private static String itemId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    private static Direction side(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Direction.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private void recordBlockPlacement(String blockId, BlockPos pos) {
        recordBlockPlacement(hostContext.player(), blockId, Map.of("x", pos.getX(), "y", pos.getY(), "z", pos.getZ()));
    }

    private static boolean recordItemCollected(ServerPlayer player, Map<String, Object> payload) {
        if (nativeLoaderProcess()) {
            return false;
        }
        String target = MinecraftEchoRuntimeHost.itemTarget(payload);
        int count = Math.max(1, intValue(payload.get("count"), 1));
        boolean changed = AshfallAdapterCoreMissionTriggerRuntime.itemCollected(player, target);
        changed |= recordMission(player, MissionObjectiveType.OBTAIN_ITEM, target, count, payload);
        changed |= recordMission(player, MissionObjectiveType.DELIVER_ITEM, target, count, payload);
        changed |= recordEarlyInventoryPredicates(player, payload);
        return changed;
    }

    private static boolean recordItemUsed(ServerPlayer player, Map<String, Object> payload) {
        if (nativeLoaderProcess()) {
            return false;
        }
        String target = MinecraftEchoRuntimeHost.itemTarget(payload);
        boolean changed = recordMission(player, MissionObjectiveType.CUSTOM, target, 1, payload);
        changed |= recordMission(player, MissionObjectiveType.CUSTOM, "consume/" + sanitizePath(target), 1, payload);
        if (EchoCanonicalContentIds.ITEM_CLEAN_WATER_BOTTLE.equals(target)) {
            changed |= recordMission(player, MissionObjectiveType.OBTAIN_ITEM, target, 1, payload);
        }
        return changed;
    }

    private static boolean recordRecipeCrafted(ServerPlayer player, Map<String, Object> payload) {
        if (nativeLoaderProcess()) {
            return false;
        }
        String target = MinecraftEchoRuntimeHost.itemTarget(payload);
        int count = Math.max(1, intValue(payload.get("count"), 1));
        boolean changed = recordMission(player, MissionObjectiveType.CRAFT_ITEM, target, count, payload);
        changed |= recordMission(player, MissionObjectiveType.OBTAIN_ITEM, target, count, payload);
        changed |= recordMission(player, MissionObjectiveType.DELIVER_ITEM, target, count, payload);
        changed |= recordEarlyInventoryPredicates(player, payload);
        return changed;
    }

    private static boolean recordScannerUsed(ServerPlayer player, Map<String, Object> payload) {
        boolean changed = markQuestLocation(player, "special", "scanner:used");
        changed |= recordMission(player, MissionObjectiveType.CUSTOM, "scanner/used", 1, payload);
        if (Boolean.TRUE.equals(payload.get("signalFound"))) {
            String siteId = stringValue(payload, "siteId");
            changed |= recordPoiState(player, siteId, QuestData.POIObjectiveState.SCANNED);
            changed |= recordMission(player, MissionObjectiveType.SCAN_BLOCK, "echoashfallprotocol:scan_first_poi", 1, payload);
            if (!siteId.isBlank()) {
                changed |= recordMission(player, MissionObjectiveType.SCAN_BLOCK, MinecraftEchoRuntimeHost.namespacedAshfallTarget(siteId), 1, payload);
            }
            changed |= AshfallAdapterCoreMissionTriggerRuntime.lensScanned(
                    player,
                    firstNonBlank(stringValue(payload, "scanId"), "echoashfallprotocol:crash_perimeter"));
        }
        return changed;
    }

    private static boolean recordRegionEntered(ServerPlayer player, Map<String, Object> payload) {
        String siteId = firstNonBlank(stringValue(payload, "siteId"), MinecraftEchoRuntimeHost.stripPrefix(stringValue(payload, "target"), "poi/"));
        boolean changed = false;
        if (!siteId.isBlank()) {
            changed |= markQuestLocation(player, "poi", siteId);
            changed |= recordPoiState(player, siteId, QuestData.POIObjectiveState.SCANNED);
            changed |= recordMission(player, MissionObjectiveType.DISCOVER_STRUCTURE, MinecraftEchoRuntimeHost.namespacedAshfallTarget(siteId), 1, payload);
        }
        if (Boolean.TRUE.equals(payload.get("newlyDiscovered"))) {
            changed |= recordMission(player, MissionObjectiveType.CUSTOM, "echoashfallprotocol:poi_explorer", 1, payload);
        }
        changed |= recordMission(player, MissionObjectiveType.ENTER_REGION, firstNonBlank(siteId, stringValue(payload, "target")), 1, payload);
        return changed;
    }

    private static boolean recordTerminalOpened(ServerPlayer player, Map<String, Object> payload) {
        String terminalId = MinecraftEchoRuntimeHost.terminalTarget(payload);
        String target = firstNonBlank(stringValue(payload, "target"), terminalId);
        boolean changed = AshfallAdapterCoreMissionTriggerRuntime.terminalOpened(player, terminalId);
        if (MinecraftEchoRuntimeHost.isRecoveryCacheTarget(terminalId) || MinecraftEchoRuntimeHost.isRecoveryCacheTarget(target)) {
            changed |= markQuestLocation(player, "special", "cache:opened");
            changed |= recordNearestPoiState(player, QuestData.POIObjectiveState.CACHE_LOOTED);
            changed |= recordMission(player, MissionObjectiveType.CUSTOM, "cache/opened", 1, payload);
            changed |= recordMission(player, MissionObjectiveType.CUSTOM, "echoashfallprotocol:loot_survivor_cache", 1, payload);
        }
        changed |= recordMission(player, MissionObjectiveType.CUSTOM, target, 1, payload);
        return changed;
    }

    private static boolean recordTerminalCommand(ServerPlayer player, Map<String, Object> payload) {
        String command = stringValue(payload, "command").strip().toLowerCase(Locale.ROOT);
        if ("status".equals(command)) {
            command = "mission status";
        }
        boolean changed = AshfallAdapterCoreMissionTriggerRuntime.terminalMissionCommand(player, command);
        String terminalId = firstNonBlank(MinecraftEchoRuntimeHost.terminalTarget(payload), "echoterminal:ashfall_first_steps");
        changed |= recordTerminalOpened(player, copyPayload(payload, Map.of("terminalId", terminalId, "target", terminalId)));
        changed |= recordMission(player, MissionObjectiveType.CUSTOM, "terminal/command/" + sanitizePath(command), 1, payload);
        return changed;
    }

    private static boolean recordNativeUiAction(ServerPlayer player, String eventId, Map<String, Object> payload) {
        String target = firstNonBlank(MinecraftEchoRuntimeHost.eventTarget(payload), eventId);
        String action = firstNonBlank(
                stringValue(payload, "action"),
                firstNonBlank(stringValue(payload, "effect"), eventId));
        boolean changed = markQuestLocation(player, "native_ui",
                sanitizePath(eventId) + "/" + sanitizePath(target));
        changed |= recordMission(player, MissionObjectiveType.CUSTOM, eventId, 1, payload);
        changed |= recordMission(player, MissionObjectiveType.CUSTOM,
                "native_ui/" + sanitizePath(eventId) + "/" + sanitizePath(action), 1, payload);
        if ("native.ui.index_bookmark".equals(eventId)) {
            changed |= recordMission(player, MissionObjectiveType.CUSTOM,
                    "native_ui/index_bookmark/" + sanitizePath(stringValue(payload, "entryId")), 1, payload);
        }
        if ("native.ui.ashfall_drone_command".equals(eventId)) {
            changed |= recordMission(player, MissionObjectiveType.CUSTOM,
                    "native_ui/drone_command/" + sanitizePath(stringValue(payload, "command")), 1, payload);
        }
        return changed;
    }

    private static boolean recordMachinePowered(ServerPlayer player, Map<String, Object> payload) {
        String machineId = MinecraftEchoRuntimeHost.machineTarget(payload);
        boolean active = !payload.containsKey("active") || Boolean.TRUE.equals(payload.get("active"));
        boolean changed = false;
        if (active) {
            changed |= markQuestLocation(player, "special", "machine_powered:" + sanitizePath(machineId));
            changed |= AshfallAdapterCoreMissionTriggerRuntime.machinePowered(player, machineId);
            if ("echoashfallprotocol:power_node".equals(machineId)) {
                changed |= markQuestLocation(player, "special", "power_node:activated");
                changed |= recordMission(player, MissionObjectiveType.PLACE_BLOCK, "echoashfallprotocol:power_node", 1, payload);
                changed |= recordMission(player, MissionObjectiveType.CUSTOM, "echoashfallprotocol:activate_power_node", 1, payload);
                changed |= AshfallAdapterCoreMissionTriggerRuntime.machinePowered(player, "echoashfallprotocol:recovery_cache");
                if (intValue(payload.get("activeNodeCount"), 0) >= 5) {
                    changed |= recordMission(player, MissionObjectiveType.CUSTOM, "echoashfallprotocol:stabilize_nexus_grid", 1, payload);
                }
            }
            changed |= recordMission(player, MissionObjectiveType.CUSTOM, machineId, 1, payload);
        }
        return changed;
    }

    private static boolean recordBlockPlacement(ServerPlayer player, String blockId, Map<String, Object> payload) {
        if (nativeLoaderProcess()) {
            return false;
        }
        if (blockId == null || blockId.isBlank()) {
            return false;
        }
        Identifier id = Identifier.tryParse(blockId);
        if (id == null) {
            id = Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, sanitizePath(blockId));
        }
        QuestData quest = player.getData(ModAttachments.QUEST_DATA.get());
        quest.recordBlockPlacement(id.toString());
        QuestData.saveAndSync(player, quest);
        AshfallAdapterCoreRuntimeGuards.ensureMissionContentReady(player, "gameplay_event");
        EchoCoreServices.recordMissionObjective(
                player,
                MissionObjectiveType.PLACE_BLOCK,
                id,
                1,
                missionPayload(payload, Map.of("source", RUNTIME_HOST_ID)));
        MissionRegistry.invalidateBlockProbeCache(player);
        return true;
    }

    private static boolean recordMission(
            ServerPlayer player,
            MissionObjectiveType type,
            String target,
            int count,
            Map<String, Object> payload) {
        if (nativeLoaderProcess()) {
            return false;
        }
        Identifier targetId = MinecraftEchoRuntimeHost.targetId(target);
        if (targetId == null) {
            return false;
        }
        AshfallAdapterCoreRuntimeGuards.ensureMissionContentReady(player, "gameplay_event");
        return EchoCoreServices.recordMissionObjective(
                player,
                type,
                targetId,
                Math.max(1, count),
                missionPayload(payload, Map.of(
                        "source", RUNTIME_HOST_ID,
                        "adapterCoreEvent", stringValue(payload, "source"))));
    }

    private static boolean recordEarlyInventoryPredicates(ServerPlayer player, Map<String, Object> payload) {
        boolean changed = false;
        if (MinecraftEchoRuntimeHost.hasAny(player, ModItems.EMERGENCY_RATION.get(), ModItems.WILD_BERRY.get())) {
            changed |= recordMission(player, MissionObjectiveType.CUSTOM, "ashfall:food_buffer", 1, payload);
        }
        if (MinecraftEchoRuntimeHost.countItem(player, ModItems.EMERGENCY_RATION.get()) >= 4
                || MinecraftEchoRuntimeHost.countItem(player, ModItems.WILD_BERRY.get()) >= 12) {
            changed |= recordMission(player, MissionObjectiveType.CUSTOM, "ashfall:ration_buffer", 1, payload);
        }
        if (MinecraftEchoRuntimeHost.hasAll(player, ModItems.ASHBONE_SHIV.get(), ModItems.SCAVENGER_SPEAR.get(), ModItems.HIDE_WRAP.get())) {
            changed |= recordMission(player, MissionObjectiveType.CUSTOM, "ashfall:wasteland_field_kit", 1, payload);
        }
        return changed;
    }

    private static boolean recordPoiState(ServerPlayer player, String siteId, QuestData.POIObjectiveState state) {
        if (siteId == null || siteId.isBlank() || state == null) {
            return false;
        }
        QuestData quest = QuestData.get(player);
        boolean alreadyRecorded = quest.hasPOIState(siteId, state);
        quest.recordPOIState(siteId, state);
        quest.visitLocation("poi", siteId);
        QuestData.saveAndSync(player, quest);
        return !alreadyRecorded;
    }

    private static boolean recordNearestPoiState(ServerPlayer player, QuestData.POIObjectiveState state) {
        POIScannerService.ScanHit hit = POIScannerService.scan(player);
        if (hit == null || hit.distance() > POIScannerService.DISCOVERY_RADIUS * 1.5D) {
            return false;
        }
        return recordPoiState(player, hit.id(), state);
    }

    private static boolean isItemCollectedEvent(String eventId) {
        return EchoCanonicalContentIds.EVENT_PLAYER_ITEM_COLLECTED.equals(eventId)
                || "player.item_obtained".equals(eventId);
    }

    private static boolean isItemUsedEvent(String eventId) {
        return EchoCanonicalContentIds.EVENT_PLAYER_ITEM_USED.equals(eventId)
                || "player.item_consumed".equals(eventId);
    }

    private static boolean isBlockPlacedEvent(String eventId) {
        return EchoCanonicalContentIds.EVENT_PLAYER_BLOCK_PLACED.equals(eventId)
                || "player.place_block".equals(eventId);
    }

    private static boolean isScannerUsedEvent(String eventId) {
        return EchoCanonicalContentIds.EVENT_PLAYER_SCANNER_USED.equals(eventId)
                || "ashfall.scanner_used".equals(eventId);
    }

    private static boolean isRegionEnteredEvent(String eventId) {
        return EchoCanonicalContentIds.EVENT_PLAYER_REGION_ENTERED.equals(eventId)
                || "ashfall.poi_discovered".equals(eventId);
    }

    private static boolean isTerminalOpenedEvent(String eventId) {
        return EchoCanonicalContentIds.EVENT_PLAYER_TERMINAL_OPENED.equals(eventId)
                || "ashfall.cache_opened".equals(eventId);
    }

    private static boolean isMachinePoweredEvent(String eventId) {
        return EchoCanonicalContentIds.EVENT_PLAYER_MACHINE_POWERED.equals(eventId)
                || "ashfall.power_node_state".equals(eventId);
    }

    private static boolean isNativeUiActionEvent(String eventId) {
        return switch (eventId) {
            case "native.ui.surface_open",
                 "native.ui.index_bookmark",
                 "native.ui.holomap_state",
                 "native.ui.signalos_terminal",
                 "native.ui.ashfall_drone_command" -> true;
            default -> false;
        };
    }

    private static String itemTarget(Map<String, Object> payload) {
        return firstNonBlank(
                firstNonBlank(stringValue(payload, "itemId"), stringValue(payload, "item")),
                stringValue(payload, "target"));
    }

    private static String terminalTarget(Map<String, Object> payload) {
        return firstNonBlank(
                firstNonBlank(stringValue(payload, "terminalId"), stringValue(payload, "cacheId")),
                stringValue(payload, "target"));
    }

    private static String machineTarget(Map<String, Object> payload) {
        return firstNonBlank(
                firstNonBlank(stringValue(payload, "machineId"), stringValue(payload, "blockId")),
                stringValue(payload, "target"));
    }

    private static String eventTarget(Map<String, Object> payload) {
        return firstNonBlank(
                firstNonBlank(
                        firstNonBlank(stringValue(payload, "target"), stringValue(payload, "itemId")),
                        firstNonBlank(stringValue(payload, "blockId"), stringValue(payload, "machineId"))),
                firstNonBlank(stringValue(payload, "terminalId"), stringValue(payload, "siteId")));
    }

    private static String gameplaySaveKey(String eventId, Map<String, Object> payload) {
        String target = eventTarget(payload);
        String suffix = target.isBlank() ? "" : "." + sanitizePath(target).replace('/', '.');
        return sanitizePath(eventId).replace('/', '.') + suffix;
    }

    private static String gameplayHudMessage(String eventId, Map<String, Object> payload, boolean missionAdvanced) {
        String target = eventTarget(payload);
        String state = missionAdvanced ? "advanced" : "recorded";
        return "[ECHO] " + eventId + " " + state + (target.isBlank() ? "." : ": " + target);
    }

    private static String namespacedAshfallTarget(String target) {
        if (target == null || target.isBlank()) {
            return "";
        }
        return target.contains(":") ? target : EchoAshfallProtocol.MODID + ":" + sanitizePath(target);
    }

    private static String stripPrefix(String value, String prefix) {
        if (value == null || prefix == null || !value.startsWith(prefix)) {
            return "";
        }
        return value.substring(prefix.length());
    }

    private static boolean isRecoveryCacheTarget(String target) {
        String normalized = target == null ? "" : target.toLowerCase(Locale.ROOT);
        return normalized.contains("recovery_cache")
                || normalized.contains("loot_survivor_cache")
                || normalized.equals("cache/opened");
    }

    private static boolean hasAny(ServerPlayer player, Item... items) {
        for (Item item : items) {
            if (countItem(player, item) > 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasAll(ServerPlayer player, Item... items) {
        for (Item item : items) {
            if (countItem(player, item) <= 0) {
                return false;
            }
        }
        return true;
    }

    private static int countItem(ServerPlayer player, Item item) {
        int count = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty() && stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static Identifier targetId(String target) {
        if (target == null || target.isBlank()) {
            return null;
        }
        Identifier parsed = Identifier.tryParse(target);
        if (parsed != null) {
            return parsed;
        }
        return Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, sanitizePath(target));
    }

    private static boolean markQuestLocation(ServerPlayer player, String category, String marker) {
        if (marker == null || marker.isBlank()) {
            return false;
        }
        QuestData quest = player.getData(ModAttachments.QUEST_DATA.get());
        if (quest.hasVisitedLocation(category, marker)) {
            return false;
        }
        quest.visitLocation(category, marker);
        QuestData.saveAndSync(player, quest);
        return true;
    }

    private static boolean syncQuestState(ServerPlayer player, String key, Object value) {
        if (!QUEST_DROP_POD_INITIALIZED.equals(key)) {
            return false;
        }
        QuestData quest = player.getData(ModAttachments.QUEST_DATA.get());
        boolean initialized = Boolean.TRUE.equals(value);
        boolean changed = quest.isDropPodInitialized() != initialized;
        quest.setDropPodInitialized(initialized);
        if (initialized) {
            boolean missionAlreadyAvailable = quest.isMissionUnlocked(FIRST_MISSION_ID)
                    || quest.isMissionCompleted(FIRST_MISSION_ID);
            if (!missionAlreadyAvailable) {
                quest.unlockMission(FIRST_MISSION_ID);
                changed = true;
            }
            quest.setSelectedMissionId(FIRST_MISSION_ID);
            changed |= quest.repairMissionState(player);
        }
        if (changed || initialized) {
            QuestData.saveAndSync(player, quest);
        }
        return true;
    }

    private static boolean syncSurvivalState(ServerPlayer player, String key, Object value) {
        if (key == null || !key.startsWith("survival.")) {
            return false;
        }
        SurvivalData survival = player.getData(ModAttachments.SURVIVAL_DATA.get());
        String field = key.substring("survival.".length());
        boolean changed = true;
        switch (field) {
            case "hydration" -> survival.setHydration(intValue(value, survival.getHydration()));
            case "radiationLevel" -> survival.setRadiationLevel(floatValue(value, survival.getRadiationLevel()));
            case "airFilterLife" -> survival.setAirFilterLife(intValue(value, survival.getAirFilterLife()));
            case "hasMask" -> survival.setHasMask(Boolean.TRUE.equals(value));
            case "filterTier" -> survival.setFilterTier(intValue(value, survival.getFilterTier()));
            default -> changed = false;
        }
        if (changed) {
            player.setData(ModAttachments.SURVIVAL_DATA.get(), survival);
            player.syncData(ModAttachments.SURVIVAL_DATA.get());
        }
        return changed;
    }

    private static void writePersistentValue(CompoundTag tag, String key, Object value) {
        if (FIRST_JOIN_FLAG.equals(key)) {
            tag.putBoolean(FIRST_JOIN_FLAG, Boolean.TRUE.equals(value));
            return;
        }
        if (value instanceof Boolean bool) {
            tag.putBoolean(key, bool);
        } else if (value instanceof Integer integer) {
            tag.putInt(key, integer);
        } else if (value instanceof Long longValue) {
            tag.putLong(key, longValue);
        } else if (value instanceof Float floatValue) {
            tag.putFloat(key, floatValue);
        } else if (value instanceof Double doubleValue) {
            tag.putDouble(key, doubleValue);
        } else {
            tag.putString(key, value == null ? "" : String.valueOf(value));
        }
    }

    private static StructureType structureType(String structureId) {
        String normalized = structureId == null ? "" : structureId.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains(":")) {
            normalized = normalized.substring(normalized.indexOf(':') + 1);
        }
        return StructureType.byName(normalized);
    }

    private static String canonicalStructureId(StructureType type) {
        return EchoAshfallProtocol.MODID + ":" + type.getName();
    }

    private static String structureMarkerId(NativeStructurePlacement placement, BlockPos origin) {
        String structure = placement.structureId().contains(":")
                ? placement.structureId().substring(placement.structureId().indexOf(':') + 1)
                : placement.structureId();
        return sanitizePath(structure) + "@" + origin.getX() + "," + origin.getY() + "," + origin.getZ();
    }

    private static Map<String, Object> structurePayload(NativeStructurePlacement placement) {
        return Map.of(
                "structure", placement.structureId(),
                "dimensionId", placement.dimensionId(),
                "origin", Map.of("x", placement.originX(), "y", placement.originY(), "z", placement.originZ()),
                "anchor", placement.anchor(),
                "constraints", placement.constraints());
    }

    private static CustomPacketPayload packetPayload(NativePacket packet) {
        if ("echoashfallprotocol:welcome_screen".equals(packet.packetId())) {
            return new WelcomeScreenPacket();
        }
        Identifier channel = Identifier.tryParse(firstNonBlank(packet.channel(), packet.packetId()));
        if (channel == null) {
            channel = Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, sanitizePath(packet.packetId()));
        }
        return new EchoSyncPayload(syncType(packet.payload()), channel, null, compoundFromMap(packet.payload()));
    }

    private static EchoPacketKind packetKind(String channel) {
        if (channel == null || channel.isBlank()) {
            return EchoPacketKind.CLIENTBOUND_SYNC;
        }
        try {
            return EchoPacketKind.valueOf(channel.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return EchoPacketKind.CLIENTBOUND_SYNC;
        }
    }

    private static EchoSyncType syncType(Map<String, Object> payload) {
        String type = stringValue(payload, "syncType");
        if (type.isBlank()) {
            return EchoSyncType.VISUAL_STATE;
        }
        try {
            return EchoSyncType.valueOf(type.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return EchoSyncType.VISUAL_STATE;
        }
    }

    private int sendPacketFallbackToAllPlayers(NativePacket packet) {
        int sent = 0;
        for (ServerPlayer player : hostContext.level().getServer().getPlayerList().getPlayers()) {
            if (sendPacketFallback(player, packet)) {
                sent++;
            }
        }
        return sent;
    }

    private static boolean sendPacketFallback(ServerPlayer player, NativePacket packet) {
        if (sendChatFallback(player, packet.payload())) {
            return true;
        }
        if ("echoashfallprotocol:welcome_screen".equals(packet.packetId())) {
            player.sendSystemMessage(Component.literal(""));
            player.sendSystemMessage(Component.translatable("message.EchoAshfallProtocol.starting.line"));
            player.sendSystemMessage(Component.translatable("message.EchoAshfallProtocol.starting.kit"));
            player.sendSystemMessage(Component.translatable("message.EchoAshfallProtocol.starting.buffer"));
            player.sendSystemMessage(Component.literal(""));
            return true;
        }
        String packetId = firstNonBlank(packet.packetId(), packet.channel());
        if (!packetId.isBlank()) {
            player.sendSystemMessage(Component.literal("[ECHO] " + packetId));
            return true;
        }
        return false;
    }

    private static boolean sendChatFallback(ServerPlayer player, Map<String, Object> payload) {
        Object lineKeys = payload.get("lineKeys");
        if (lineKeys instanceof Iterable<?> keys) {
            player.sendSystemMessage(Component.literal(""));
            for (Object key : keys) {
                String translationKey = key == null ? "" : String.valueOf(key);
                if (!translationKey.isBlank()) {
                    player.sendSystemMessage(Component.translatable(translationKey));
                }
            }
            player.sendSystemMessage(Component.literal(""));
            return true;
        }
        String message = firstNonBlank(stringValue(payload, "message"), stringValue(payload, "text"));
        if (!message.isBlank()) {
            player.sendSystemMessage(Component.literal(message));
            return true;
        }
        return false;
    }

    private static Map<String, Object> writeNativeSaveData(ServerPlayer player, NativeSaveData data) {
        Path file = nativeSaveDataFile(player);
        String prefix = nativeSaveArchivePrefix(data.scope(), data.key());
        String snapshot = prefix
                + " writtenAtGameTime=" + player.level().getGameTime()
                + " payload=" + String.valueOf(data.payload());
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(
                    file,
                    snapshot + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to write native runtime save file " + file, exception);
        }
        return Map.of(
                "scope", data.scope(),
                "key", data.key(),
                "present", true,
                "saveBackend", "world_save_file",
                "saveFile", file.toString(),
                "snapshot", snapshot);
    }

    private static String nativeSaveArchivePrefix(String scope, String key) {
        return SAVE_ROOT + ":" + firstNonBlank(scope, "default") + "/" + firstNonBlank(key, "default") + "=";
    }

    private static Path nativeSaveDataFile(ServerPlayer player) {
        try {
            Object server = player.level().getServer();
            Class<?> levelResourceClass = Class.forName("net.minecraft.world.level.storage.LevelResource");
            Object root = levelResourceClass.getField("ROOT").get(null);
            Object path = server.getClass().getMethod("getWorldPath", levelResourceClass).invoke(server, root);
            if (path instanceof Path worldPath) {
                return worldPath.resolve("echo-native").resolve("adaptercore-save-data.properties");
            }
        } catch (Throwable ignored) {
            // Fall through to the process working directory when the world path API is unavailable.
        }
        return Path.of("echo-native", "adaptercore-save-data.properties").toAbsolutePath();
    }

    private static String failureSummary(Throwable failure) {
        if (failure == null) {
            return "";
        }
        Throwable current = failure;
        if (current instanceof java.lang.reflect.InvocationTargetException invocation
                && invocation.getTargetException() != null) {
            current = invocation.getTargetException();
        }
        String message = current.getMessage();
        return current.getClass().getName() + (message == null || message.isBlank() ? "" : ": " + message);
    }

    private static boolean nativeLoaderProcess() {
        return dev.echo.nativeplatform.contracts.EchoNativeClientRuntimeEnvironment.isNativeLoaderActive();
    }

    private static CompoundTag compoundFromMap(Map<String, Object> payload) {
        CompoundTag tag = new CompoundTag();
        if (payload == null) {
            return tag;
        }
        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Boolean bool) {
                tag.putBoolean(entry.getKey(), bool);
            } else if (value instanceof Integer integer) {
                tag.putInt(entry.getKey(), integer);
            } else if (value instanceof Long longValue) {
                tag.putLong(entry.getKey(), longValue);
            } else if (value instanceof Float floatValue) {
                tag.putFloat(entry.getKey(), floatValue);
            } else if (value instanceof Double doubleValue) {
                tag.putDouble(entry.getKey(), doubleValue);
            } else {
                tag.putString(entry.getKey(), value == null ? "" : String.valueOf(value));
            }
        }
        return tag;
    }

    private static Map<String, Object> copyPayload(Map<String, Object> base, Map<String, Object> extra) {
        Map<String, Object> copy = new LinkedHashMap<>();
        if (base != null) {
            copy.putAll(base);
        }
        if (extra != null) {
            copy.putAll(extra);
        }
        return Map.copyOf(copy);
    }

    private static Map<String, String> missionPayload(Map<String, Object> base, Map<String, Object> extra) {
        Map<String, String> copy = new LinkedHashMap<>();
        if (base != null) {
            for (Map.Entry<String, Object> entry : base.entrySet()) {
                copy.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }
        if (extra != null) {
            for (Map.Entry<String, Object> entry : extra.entrySet()) {
                copy.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }
        return Map.copyOf(copy);
    }

    private static String stringValue(Map<String, Object> payload, String key) {
        if (payload == null) {
            return "";
        }
        Object value = payload.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private static String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second == null ? "" : second;
    }

    private static String sanitizePath(String raw) {
        String value = raw == null ? "unknown" : raw.trim().toLowerCase(Locale.ROOT);
        if (value.contains(":")) {
            value = value.substring(value.indexOf(':') + 1);
        }
        value = value.replaceAll("[^a-z0-9_./-]", "_");
        return value.isBlank() ? "unknown" : value;
    }

    private static Object safeValue(Object value) {
        if (value == null || value instanceof Number || value instanceof Boolean || value instanceof String) {
            return value == null ? "" : value;
        }
        return String.valueOf(value);
    }

    private static int intValue(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static float floatValue(Object value, float fallback) {
        if (value instanceof Number number) {
            return number.floatValue();
        }
        try {
            return Float.parseFloat(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}

