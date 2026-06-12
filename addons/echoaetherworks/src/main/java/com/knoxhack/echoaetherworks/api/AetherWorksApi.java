package com.knoxhack.echoaetherworks.api;

import com.knoxhack.echoaetherworks.EchoAetherWorks;
import com.knoxhack.echoaetherworks.block.entity.AetherCellBlockEntity;
import com.knoxhack.echoaetherworks.block.entity.AetherCondenserBlockEntity;
import com.knoxhack.echoaetherworks.block.entity.AetherConduitBlockEntity;
import com.knoxhack.echoaetherworks.block.entity.AetherStorageBlockEntity;
import com.knoxhack.echoaetherworks.registry.ModItems;
import com.knoxhack.echoarcanacore.api.AetherSignalType;
import com.knoxhack.echoarcanacore.api.AetherStorage;
import com.knoxhack.echoarcanacore.api.AetherStorageTarget;
import com.knoxhack.echoarcanacore.api.ArcanaCoreServices;
import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.mission.MissionObjectiveType;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class AetherWorksApi {
    public static final Identifier AETHER_CONDENSER = EchoAetherWorks.id("aether_condenser");
    public static final Identifier AETHER_CELL = EchoAetherWorks.id("aether_cell");
    public static final Identifier AETHER_CONDUIT = EchoAetherWorks.id("aether_conduit");
    private static final int MAX_TOPOLOGY_NODES = 64;
    private static final int MAX_TOPOLOGY_DEPTH = 16;

    public record AetherTopologySnapshot(int nodeCount, int acceptingNodes, int pushNodes, int routeDepth,
            int storedAmount, int capacity, int automationRecipes, int completedRecipes) {
        public static final AetherTopologySnapshot EMPTY = new AetherTopologySnapshot(0, 0, 0, 0, 0, 0, 0, 0);
    }

    public record AetherAutomationRecipe(String id, String family, int inputSlots, int outputSlots, int aetherCost) {
        public AetherAutomationRecipe(String id, int inputSlots, int outputSlots, int aetherCost) {
            this(id, familyFor(id), inputSlots, outputSlots, aetherCost);
        }
    }

    private AetherWorksApi() {
    }

    public static Identifier machineIdFor(AetherStorageTarget target) {
        if (target instanceof AetherCondenserBlockEntity) {
            return AETHER_CONDENSER;
        }
        if (target instanceof AetherConduitBlockEntity) {
            return AETHER_CONDUIT;
        }
        if (target instanceof AetherCellBlockEntity) {
            return AETHER_CELL;
        }
        return AETHER_CELL;
    }

    public static boolean drawToPlayer(ServerPlayer player, AetherStorageTarget source, Identifier sourceId) {
        if (player == null || source == null) {
            return false;
        }
        AetherStorage storage = source.aetherStorage();
        if (storage == null || storage.storedAmount() <= 0.0D) {
            player.sendSystemMessage(Component.translatable("message.echoaetherworks.storage_empty"));
            return false;
        }
        AetherSignalType type = storage.outputType();
        double amount = Math.min(16.0D, storage.storedAmount());
        double extracted = ArcanaCoreServices.aether().extractAether(source, amount, type);
        double accepted = ArcanaCoreServices.aether().addAether(player, extracted, type);
        if (accepted < extracted) {
            ArcanaCoreServices.aether().insertAether(source, extracted - accepted, type);
        }
        if (accepted <= 0.0D) {
            player.sendSystemMessage(Component.translatable("message.echoaetherworks.player_full"));
            return false;
        }
        player.sendSystemMessage(Component.translatable("message.echoaetherworks.draw",
                Math.round(accepted), type.serializedName()));
        record(player, sourceId == null ? AETHER_CELL : sourceId, "draw");
        return true;
    }

    public static AetherTopologySnapshot describeTopology(Level level, BlockPos source) {
        Map<BlockPos, Integer> nodes = scanTopology(level, source);
        if (nodes.isEmpty()) {
            return AetherTopologySnapshot.EMPTY;
        }
        int accepting = 0;
        int pushing = 0;
        int routeDepth = 0;
        double stored = 0.0D;
        double capacity = 0.0D;
        int recipes = 0;
        int completed = 0;
        for (Map.Entry<BlockPos, Integer> entry : nodes.entrySet()) {
            routeDepth = Math.max(routeDepth, entry.getValue());
            BlockEntity blockEntity = level.getBlockEntity(entry.getKey());
            if (!(blockEntity instanceof AetherStorageTarget target)) {
                continue;
            }
            AetherStorage storage = target.aetherStorage();
            stored += storage.storedAmount();
            capacity += storage.maxStoredAmount();
            if (blockEntity instanceof AetherStorageBlockEntity storageNode) {
                accepting += storageNode.acceptsNetworkInput() ? 1 : 0;
                pushing += storageNode.canPushNetwork() && storageNode.automationActive() ? 1 : 0;
                recipes += automationRecipeSlots(storageNode);
                completed += storageNode.automationCycles();
            } else {
                accepting++;
            }
        }
        return new AetherTopologySnapshot(nodes.size(), accepting, pushing, routeDepth,
                (int) Math.round(stored), (int) Math.round(capacity), recipes, completed);
    }

    public static double routeFromNetwork(Level level, AetherStorageBlockEntity source, double requestedAmount) {
        if (level == null || level.isClientSide() || source == null || requestedAmount <= 0.0D
                || !source.automationActive() || !source.canPushNetwork() || source.storedAmount() <= 0.0D) {
            return 0.0D;
        }
        Map<BlockPos, Integer> nodes = scanTopology(level, source.getBlockPos());
        if (nodes.size() <= 1) {
            return 0.0D;
        }
        AetherSignalType type = source.aetherStorage().outputType();
        double remaining = Math.min(requestedAmount, source.aetherStorage().transferRate());
        double moved = 0.0D;
        for (boolean allowRelayTargets : new boolean[] {false, true}) {
            for (Map.Entry<BlockPos, Integer> entry : nodes.entrySet()) {
                if (remaining <= 0.0D || entry.getKey().equals(source.getBlockPos())) {
                    continue;
                }
                BlockEntity blockEntity = level.getBlockEntity(entry.getKey());
                if (!allowRelayTargets && blockEntity instanceof AetherConduitBlockEntity) {
                    continue;
                }
                if (!(blockEntity instanceof AetherStorageTarget target)) {
                    continue;
                }
                if (blockEntity instanceof AetherStorageBlockEntity storageTarget && !storageTarget.acceptsNetworkInput()) {
                    continue;
                }
                AetherStorage targetStorage = target.aetherStorage();
                double room = Math.max(0.0D, targetStorage.maxStoredAmount() - targetStorage.storedAmount());
                if (room <= 0.0D) {
                    continue;
                }
                double extracted = ArcanaCoreServices.aether().extractAether(source, Math.min(remaining, room), type);
                if (extracted <= 0.0D) {
                    continue;
                }
                double accepted = ArcanaCoreServices.aether().insertAether(target, extracted, type);
                if (accepted < extracted) {
                    ArcanaCoreServices.aether().insertAether(source, extracted - accepted, type);
                }
                moved += accepted;
                remaining -= accepted;
            }
        }
        return moved;
    }

    private static Map<BlockPos, Integer> scanTopology(Level level, BlockPos source) {
        if (level == null || source == null) {
            return Map.of();
        }
        Map<BlockPos, Integer> result = new LinkedHashMap<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        Map<BlockPos, Integer> depth = new HashMap<>();
        BlockPos start = source.immutable();
        queue.add(start);
        visited.add(start);
        depth.put(start, 0);
        while (!queue.isEmpty() && result.size() < MAX_TOPOLOGY_NODES) {
            BlockPos pos = queue.remove();
            int nodeDepth = depth.getOrDefault(pos, 0);
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (!(blockEntity instanceof AetherStorageTarget)) {
                continue;
            }
            result.put(pos, nodeDepth);
            if (!(blockEntity instanceof AetherStorageBlockEntity) || nodeDepth >= MAX_TOPOLOGY_DEPTH) {
                continue;
            }
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction).immutable();
                if (!visited.add(next)) {
                    continue;
                }
                BlockEntity neighbor = level.getBlockEntity(next);
                if (neighbor instanceof AetherStorageTarget) {
                    queue.add(next);
                    depth.put(next, nodeDepth + 1);
                }
            }
        }
        return result;
    }

    private static int automationRecipeSlots(AetherStorageBlockEntity node) {
        return readyAutomationRecipeCount(node);
    }

    private static int readyAutomationRecipeCount(AetherStorageBlockEntity node) {
        if (node == null || !node.automationActive()) {
            return 0;
        }
        AetherStorage storage = node.aetherStorage();
        int count = 0;
        if (node instanceof AetherCondenserBlockEntity && storage.storedAmount() >= 152.0D
                && storage.contaminationLevel() > 0.3D
                && node.hasAutomationInput(ModItems.AETHER_CAPACITOR.get(), 1)
                && node.hasAutomationInput(ModItems.PURITY_CATALYST.get(), 1)
                && node.canAcceptAutomationOutput(new ItemStack(ModItems.AETHER_COIL.get(), 3))) {
            count++;
        }
        if (node instanceof AetherCellBlockEntity && storage.storedAmount() >= 192.0D
                && node.hasAutomationInput(ModItems.AETHER_CAPACITOR.get(), 1)
                && node.hasAutomationInput(ModItems.AETHER_COIL.get(), 1)
                && node.canAcceptAutomationOutput(new ItemStack(ModItems.PURITY_CATALYST.get(), 2))) {
            count++;
        }
        if (node instanceof AetherConduitBlockEntity && storage.storedAmount() >= 88.0D
                && node.redstoneControlEnabled()
                && node.hasAutomationInput(ModItems.AETHER_COIL.get(), 1)
                && node.hasAutomationInput(ModItems.AETHER_CAPACITOR.get(), 1)
                && node.canAcceptAutomationOutput(new ItemStack(ModItems.PURITY_CATALYST.get(), 2))) {
            count++;
        }
        if (node instanceof AetherCondenserBlockEntity && storage.storedAmount() >= 116.0D
                && node.hasAutomationInput(ModItems.AETHER_COIL.get(), 2)
                && node.canAcceptAutomationOutput(new ItemStack(ModItems.PURITY_CATALYST.get()))) {
            count++;
        }
        if (node instanceof AetherCellBlockEntity && storage.storedAmount() >= 260.0D
                && storage.contaminationLevel() > 0.2D
                && node.hasAutomationInput(ModItems.AETHER_CAPACITOR.get(), 2)
                && node.canAcceptAutomationOutput(new ItemStack(ModItems.AETHER_COIL.get(), 2))) {
            count++;
        }
        if (node instanceof AetherConduitBlockEntity && storage.storedAmount() >= 72.0D
                && node.redstoneMode() == AetherStorageBlockEntity.REDSTONE_PULSE
                && node.hasAutomationInput(ModItems.AETHER_COIL.get(), 1)
                && node.hasAutomationInput(ModItems.PURITY_CATALYST.get(), 1)
                && node.canAcceptAutomationOutput(new ItemStack(ModItems.AETHER_CAPACITOR.get()))) {
            count++;
        }
        if (node instanceof AetherCellBlockEntity && storage.storedAmount() >= 160.0D
                && node.hasAutomationInput(ModItems.AETHER_COIL.get(), 1)
                && node.hasAutomationInput(ModItems.PURITY_CATALYST.get(), 1)
                && node.canAcceptAutomationOutput(new ItemStack(ModItems.AETHER_CAPACITOR.get(), 2))) {
            count++;
        }
        if (node instanceof AetherCondenserBlockEntity && storage.storedAmount() >= 144.0D
                && node.hasAutomationInput(ModItems.AETHER_COIL.get(), 1)
                && node.hasAutomationInput(ModItems.PURITY_CATALYST.get(), 1)
                && node.canAcceptAutomationOutput(new ItemStack(ModItems.AETHER_CAPACITOR.get(), 3))) {
            count++;
        }
        if (node instanceof AetherConduitBlockEntity && storage.storedAmount() >= 64.0D
                && storage.contaminationLevel() > 0.05D
                && node.hasAutomationInput(ModItems.AETHER_CAPACITOR.get(), 1)
                && node.hasAutomationInput(ModItems.PURITY_CATALYST.get(), 1)
                && node.canAcceptAutomationOutput(new ItemStack(ModItems.AETHER_COIL.get(), 2))) {
            count++;
        }
        if (node instanceof AetherCondenserBlockEntity && storage.storedAmount() >= 128.0D
                && node.hasAutomationInput(ModItems.AETHER_COIL.get(), 1)
                && node.canAcceptAutomationOutput(new ItemStack(ModItems.AETHER_CAPACITOR.get(), 2))) {
            count++;
        }
        if (node instanceof AetherCondenserBlockEntity && storage.storedAmount() >= 96.0D) {
            count++;
        }
        if (node instanceof AetherCondenserBlockEntity && storage.storedAmount() >= 80.0D
                && node.hasAutomationInput(ModItems.PURITY_CATALYST.get(), 1)
                && node.canAcceptAutomationOutput(new ItemStack(ModItems.AETHER_COIL.get(), 2))) {
            count++;
        }
        if (node instanceof AetherCellBlockEntity && storage.storedAmount() >= 112.0D
                && node.hasAutomationInput(ModItems.AETHER_CAPACITOR.get(), 1)
                && node.canAcceptAutomationOutput(new ItemStack(ModItems.PURITY_CATALYST.get()))) {
            count++;
        }
        if (node instanceof AetherCellBlockEntity && storage.storedAmount() >= 64.0D) {
            count++;
        }
        if (node instanceof AetherCellBlockEntity && storage.storedAmount() >= 96.0D
                && node.hasAutomationInput(ModItems.AETHER_COIL.get(), 1)
                && node.canAcceptAutomationOutput(new ItemStack(ModItems.AETHER_CAPACITOR.get()))) {
            count++;
        }
        if (node instanceof AetherConduitBlockEntity && storage.storedAmount() >= 16.0D
                && storage.contaminationLevel() > 0.15D
                && node.hasAutomationInput(ModItems.PURITY_CATALYST.get(), 1)
                && node.canAcceptAutomationOutput(new ItemStack(ModItems.AETHER_COIL.get()))) {
            count++;
        }
        if (node instanceof AetherConduitBlockEntity && storage.storedAmount() >= 40.0D
                && storage.contaminationLevel() > 0.05D
                && node.hasAutomationInput(ModItems.AETHER_CAPACITOR.get(), 1)
                && node.canAcceptAutomationOutput(new ItemStack(ModItems.PURITY_CATALYST.get()))) {
            count++;
        }
        if (node instanceof AetherConduitBlockEntity
                && (storage.contaminationLevel() > 0.05D || storage.storedAmount() >= 16.0D)) {
            count++;
        }
        return count;
    }

    private static int automationRecipeScore(AetherStorageBlockEntity node) {
        if (!node.automationActive()) {
            return 0;
        }
        if (node instanceof AetherCondenserBlockEntity && node.aetherStorage().storedAmount() >= 152.0D
                && node.aetherStorage().contaminationLevel() > 0.3D
                && node.hasAutomationInput(ModItems.AETHER_CAPACITOR.get(), 1)
                && node.hasAutomationInput(ModItems.PURITY_CATALYST.get(), 1)) {
            return node.canAcceptAutomationOutput(new ItemStack(ModItems.AETHER_COIL.get(), 3))
                    ? 96 + (int) Math.round(node.aetherStorage().contaminationLevel() * 40.0D)
                    : 0;
        }
        if (node instanceof AetherCellBlockEntity && node.aetherStorage().storedAmount() >= 192.0D
                && node.hasAutomationInput(ModItems.AETHER_CAPACITOR.get(), 1)
                && node.hasAutomationInput(ModItems.AETHER_COIL.get(), 1)) {
            return node.canAcceptAutomationOutput(new ItemStack(ModItems.PURITY_CATALYST.get(), 2))
                    ? 92 + node.automationInputStock()
                    : 0;
        }
        if (node instanceof AetherConduitBlockEntity && node.aetherStorage().storedAmount() >= 88.0D
                && node.redstoneControlEnabled()
                && node.hasAutomationInput(ModItems.AETHER_COIL.get(), 1)
                && node.hasAutomationInput(ModItems.AETHER_CAPACITOR.get(), 1)) {
            return node.canAcceptAutomationOutput(new ItemStack(ModItems.PURITY_CATALYST.get(), 2))
                    ? 88 + (node.redstonePowered() ? 8 : 0)
                    : 0;
        }
        if (node instanceof AetherCellBlockEntity && node.aetherStorage().storedAmount() >= 260.0D
                && node.aetherStorage().contaminationLevel() > 0.2D
                && node.hasAutomationInput(ModItems.AETHER_CAPACITOR.get(), 2)) {
            return node.canAcceptAutomationOutput(new ItemStack(ModItems.AETHER_COIL.get(), 2))
                    ? 86 + (int) Math.round(node.aetherStorage().contaminationLevel() * 30.0D)
                    : 0;
        }
        if (node instanceof AetherConduitBlockEntity && node.aetherStorage().storedAmount() >= 72.0D
                && node.redstoneMode() == AetherStorageBlockEntity.REDSTONE_PULSE
                && node.hasAutomationInput(ModItems.AETHER_COIL.get(), 1)
                && node.hasAutomationInput(ModItems.PURITY_CATALYST.get(), 1)) {
            return node.canAcceptAutomationOutput(new ItemStack(ModItems.AETHER_CAPACITOR.get()))
                    ? 84 + (node.redstonePowered() ? 6 : 0)
                    : 0;
        }
        if (node instanceof AetherCondenserBlockEntity && node.aetherStorage().storedAmount() >= 116.0D
                && node.hasAutomationInput(ModItems.AETHER_COIL.get(), 2)) {
            return node.canAcceptAutomationOutput(new ItemStack(ModItems.PURITY_CATALYST.get()))
                    ? 76 + node.automationInputStock()
                    : 0;
        }
        if (node instanceof AetherCellBlockEntity && node.aetherStorage().storedAmount() >= 160.0D
                && node.hasAutomationInput(ModItems.AETHER_COIL.get(), 1)
                && node.hasAutomationInput(ModItems.PURITY_CATALYST.get(), 1)) {
            return node.canAcceptAutomationOutput(new ItemStack(ModItems.AETHER_CAPACITOR.get(), 2))
                    ? 82 + node.automationInputStock()
                    : 0;
        }
        if (node instanceof AetherCondenserBlockEntity && node.aetherStorage().storedAmount() >= 144.0D
                && node.hasAutomationInput(ModItems.AETHER_COIL.get(), 1)
                && node.hasAutomationInput(ModItems.PURITY_CATALYST.get(), 1)) {
            return node.canAcceptAutomationOutput(new ItemStack(ModItems.AETHER_CAPACITOR.get(), 3))
                    ? 78 + node.automationInputStock()
                    : 0;
        }
        if (node instanceof AetherConduitBlockEntity && node.aetherStorage().storedAmount() >= 64.0D
                && node.aetherStorage().contaminationLevel() > 0.05D
                && node.hasAutomationInput(ModItems.AETHER_CAPACITOR.get(), 1)
                && node.hasAutomationInput(ModItems.PURITY_CATALYST.get(), 1)) {
            return node.canAcceptAutomationOutput(new ItemStack(ModItems.AETHER_COIL.get(), 2))
                    ? 72 + (int) Math.round(node.aetherStorage().contaminationLevel() * 30.0D)
                    : 0;
        }
        if (node instanceof AetherCondenserBlockEntity && node.aetherStorage().storedAmount() >= 128.0D
                && node.hasAutomationInput(ModItems.AETHER_COIL.get(), 1)) {
            return node.canAcceptAutomationOutput(new ItemStack(ModItems.AETHER_CAPACITOR.get(), 2))
                    ? 60 + node.automationInputStock()
                    : 0;
        }
        if (node instanceof AetherCondenserBlockEntity && node.aetherStorage().storedAmount() >= 80.0D
                && node.hasAutomationInput(ModItems.PURITY_CATALYST.get(), 1)) {
            return node.canAcceptAutomationOutput(new ItemStack(ModItems.AETHER_COIL.get(), 2))
                    ? 42 + node.automationInputStock()
                    : 0;
        }
        if (node instanceof AetherCondenserBlockEntity && node.aetherStorage().storedAmount() >= 96.0D) {
            return 30 + (int) Math.min(24.0D, node.aetherStorage().storedAmount() / 8.0D);
        }
        if (node instanceof AetherCellBlockEntity && node.aetherStorage().storedAmount() >= 112.0D
                && node.hasAutomationInput(ModItems.AETHER_CAPACITOR.get(), 1)) {
            return node.canAcceptAutomationOutput(new ItemStack(ModItems.PURITY_CATALYST.get()))
                    ? 45 + node.automationInputStock()
                    : 0;
        }
        if (node instanceof AetherCellBlockEntity && node.aetherStorage().storedAmount() >= 96.0D
                && node.hasAutomationInput(ModItems.AETHER_COIL.get(), 1)) {
            return node.canAcceptAutomationOutput(new ItemStack(ModItems.AETHER_CAPACITOR.get()))
                    ? 34 + node.automationInputStock()
                    : 0;
        }
        if (node instanceof AetherConduitBlockEntity && node.aetherStorage().storedAmount() >= 16.0D
                && node.aetherStorage().contaminationLevel() > 0.15D
                && node.hasAutomationInput(ModItems.PURITY_CATALYST.get(), 1)) {
            return node.canAcceptAutomationOutput(new ItemStack(ModItems.AETHER_COIL.get()))
                    ? 50 + (int) Math.round(node.aetherStorage().contaminationLevel() * 40.0D)
                    : 0;
        }
        if (node instanceof AetherConduitBlockEntity && node.aetherStorage().storedAmount() >= 40.0D
                && node.aetherStorage().contaminationLevel() > 0.05D
                && node.hasAutomationInput(ModItems.AETHER_CAPACITOR.get(), 1)) {
            return node.canAcceptAutomationOutput(new ItemStack(ModItems.PURITY_CATALYST.get()))
                    ? 38 + (int) Math.round(node.aetherStorage().contaminationLevel() * 25.0D)
                    : 0;
        }
        if (node instanceof AetherConduitBlockEntity && node.aetherStorage().contaminationLevel() > 0.2D) {
            return 20 + (int) Math.round(node.aetherStorage().contaminationLevel() * 30.0D);
        }
        if (node instanceof AetherCellBlockEntity && node.aetherStorage().storedAmount() >= 64.0D) {
            return 10 + (int) Math.min(24.0D, node.aetherStorage().storedAmount() / 12.0D);
        }
        return 0;
    }

    public static AetherAutomationRecipe bestAutomationRecipe(AetherStorageBlockEntity node) {
        if (node == null || !node.automationActive()) {
            return null;
        }
        AetherStorage storage = node.aetherStorage();
        if (node instanceof AetherCondenserBlockEntity && storage.storedAmount() >= 152.0D
                && storage.contaminationLevel() > 0.3D
                && node.hasAutomationInput(ModItems.AETHER_CAPACITOR.get(), 1)
                && node.hasAutomationInput(ModItems.PURITY_CATALYST.get(), 1)) {
            return node.canAcceptAutomationOutput(new ItemStack(ModItems.AETHER_COIL.get(), 3))
                    ? new AetherAutomationRecipe("condenser_overload_scrubber", "generator_safety", 2, 3, 52)
                    : null;
        }
        if (node instanceof AetherCellBlockEntity && storage.storedAmount() >= 192.0D
                && node.hasAutomationInput(ModItems.AETHER_CAPACITOR.get(), 1)
                && node.hasAutomationInput(ModItems.AETHER_COIL.get(), 1)) {
            return node.canAcceptAutomationOutput(new ItemStack(ModItems.PURITY_CATALYST.get(), 2))
                    ? new AetherAutomationRecipe("cell_storage_bus", "storage_bus", 2, 2, 44)
                    : null;
        }
        if (node instanceof AetherConduitBlockEntity && storage.storedAmount() >= 88.0D
                && node.redstoneControlEnabled()
                && node.hasAutomationInput(ModItems.AETHER_COIL.get(), 1)
                && node.hasAutomationInput(ModItems.AETHER_CAPACITOR.get(), 1)) {
            return node.canAcceptAutomationOutput(new ItemStack(ModItems.PURITY_CATALYST.get(), 2))
                    ? new AetherAutomationRecipe("conduit_redstone_relay", "network_control", 2, 2, 34)
                    : null;
        }
        if (node instanceof AetherCellBlockEntity && storage.storedAmount() >= 260.0D
                && storage.contaminationLevel() > 0.2D
                && node.hasAutomationInput(ModItems.AETHER_CAPACITOR.get(), 2)) {
            return node.canAcceptAutomationOutput(new ItemStack(ModItems.AETHER_COIL.get(), 2))
                    ? new AetherAutomationRecipe("cell_overflow_shunt", "storage_safety", 2, 2, 64)
                    : null;
        }
        if (node instanceof AetherConduitBlockEntity && storage.storedAmount() >= 72.0D
                && node.redstoneMode() == AetherStorageBlockEntity.REDSTONE_PULSE
                && node.hasAutomationInput(ModItems.AETHER_COIL.get(), 1)
                && node.hasAutomationInput(ModItems.PURITY_CATALYST.get(), 1)) {
            return node.canAcceptAutomationOutput(new ItemStack(ModItems.AETHER_CAPACITOR.get()))
                    ? new AetherAutomationRecipe("conduit_pulse_damper", "network_control", 2, 1, 30)
                    : null;
        }
        if (node instanceof AetherCondenserBlockEntity && storage.storedAmount() >= 116.0D
                && node.hasAutomationInput(ModItems.AETHER_COIL.get(), 2)) {
            return node.canAcceptAutomationOutput(new ItemStack(ModItems.PURITY_CATALYST.get()))
                    ? new AetherAutomationRecipe("condenser_signal_rectifier", "generator_signal", 2, 1, 30)
                    : null;
        }
        if (node instanceof AetherCellBlockEntity && storage.storedAmount() >= 160.0D
                && node.hasAutomationInput(ModItems.AETHER_COIL.get(), 1)
                && node.hasAutomationInput(ModItems.PURITY_CATALYST.get(), 1)) {
            return node.canAcceptAutomationOutput(new ItemStack(ModItems.AETHER_CAPACITOR.get(), 2))
                    ? new AetherAutomationRecipe("stabilized_capacitor_array", 2, 2, 36)
                    : null;
        }
        if (node instanceof AetherCondenserBlockEntity && storage.storedAmount() >= 144.0D
                && node.hasAutomationInput(ModItems.AETHER_COIL.get(), 1)
                && node.hasAutomationInput(ModItems.PURITY_CATALYST.get(), 1)) {
            return node.canAcceptAutomationOutput(new ItemStack(ModItems.AETHER_CAPACITOR.get(), 3))
                    ? new AetherAutomationRecipe("condenser_reactor_seed", 2, 3, 40)
                    : null;
        }
        if (node instanceof AetherConduitBlockEntity && storage.storedAmount() >= 64.0D
                && storage.contaminationLevel() > 0.05D
                && node.hasAutomationInput(ModItems.AETHER_CAPACITOR.get(), 1)
                && node.hasAutomationInput(ModItems.PURITY_CATALYST.get(), 1)) {
            return node.canAcceptAutomationOutput(new ItemStack(ModItems.AETHER_COIL.get(), 2))
                    ? new AetherAutomationRecipe("conduit_filter_bundle", 2, 2, 28)
                    : null;
        }
        if (node instanceof AetherCondenserBlockEntity && storage.storedAmount() >= 128.0D
                && node.hasAutomationInput(ModItems.AETHER_COIL.get(), 1)) {
            return node.canAcceptAutomationOutput(new ItemStack(ModItems.AETHER_CAPACITOR.get(), 2))
                    ? new AetherAutomationRecipe("crystallize_aether", 1, 2, 32)
                    : null;
        }
        if (node instanceof AetherCellBlockEntity && storage.storedAmount() >= 112.0D
                && node.hasAutomationInput(ModItems.AETHER_CAPACITOR.get(), 1)) {
            return node.canAcceptAutomationOutput(new ItemStack(ModItems.PURITY_CATALYST.get()))
                    ? new AetherAutomationRecipe("capacitor_batch", 1, 1, 24)
                    : null;
        }
        if (node instanceof AetherCondenserBlockEntity && storage.storedAmount() >= 80.0D
                && node.hasAutomationInput(ModItems.PURITY_CATALYST.get(), 1)) {
            return node.canAcceptAutomationOutput(new ItemStack(ModItems.AETHER_COIL.get(), 2))
                    ? new AetherAutomationRecipe("purity_distillation", 1, 2, 24)
                    : null;
        }
        if (node instanceof AetherCellBlockEntity && storage.storedAmount() >= 96.0D
                && node.hasAutomationInput(ModItems.AETHER_COIL.get(), 1)) {
            return node.canAcceptAutomationOutput(new ItemStack(ModItems.AETHER_CAPACITOR.get()))
                    ? new AetherAutomationRecipe("coil_winding", 1, 1, 20)
                    : null;
        }
        if (node instanceof AetherConduitBlockEntity && storage.storedAmount() >= 16.0D
                && storage.contaminationLevel() > 0.15D
                && node.hasAutomationInput(ModItems.PURITY_CATALYST.get(), 1)) {
            return node.canAcceptAutomationOutput(new ItemStack(ModItems.AETHER_COIL.get()))
                    ? new AetherAutomationRecipe("purity_line_scrub", 1, 1, 16)
                    : null;
        }
        if (node instanceof AetherConduitBlockEntity && storage.storedAmount() >= 40.0D
                && storage.contaminationLevel() > 0.05D
                && node.hasAutomationInput(ModItems.AETHER_CAPACITOR.get(), 1)) {
            return node.canAcceptAutomationOutput(new ItemStack(ModItems.PURITY_CATALYST.get()))
                    ? new AetherAutomationRecipe("capacitor_bleed", 1, 1, 20)
                    : null;
        }
        if (node instanceof AetherCondenserBlockEntity && storage.storedAmount() >= 96.0D) {
            return new AetherAutomationRecipe("refine", 0, 0, 16);
        }
        if (node instanceof AetherCellBlockEntity && storage.storedAmount() >= 64.0D) {
            return new AetherAutomationRecipe("capacity_tune", 0, 0, 16);
        }
        if (node instanceof AetherConduitBlockEntity
                && (storage.contaminationLevel() > 0.05D || storage.storedAmount() >= 16.0D)) {
            return new AetherAutomationRecipe("line_purge", 0, 0, 8);
        }
        return null;
    }

    public static boolean runNetworkAutomationRecipe(ServerPlayer player, Level level, AetherStorageBlockEntity source) {
        if (level == null || level.isClientSide() || source == null || !source.automationActive()) {
            return false;
        }
        Map<BlockPos, Integer> nodes = scanTopology(level, source.getBlockPos());
        AetherStorageBlockEntity best = null;
        int bestScore = 0;
        int bestDepth = Integer.MAX_VALUE;
        for (Map.Entry<BlockPos, Integer> entry : nodes.entrySet()) {
            BlockEntity blockEntity = level.getBlockEntity(entry.getKey());
            if (!(blockEntity instanceof AetherStorageBlockEntity node)) {
                continue;
            }
            int score = automationRecipeScore(node);
            if (score <= 0) {
                continue;
            }
            int depth = entry.getValue();
            if (score > bestScore || score == bestScore && depth < bestDepth) {
                best = node;
                bestScore = score;
                bestDepth = depth;
            }
        }
        if (best == null) {
            if (player != null) {
                player.sendSystemMessage(Component.translatable("message.echoaetherworks.no_recipe"));
            }
            return false;
        }
        boolean ran = runBestAutomationRecipe(player, best);
        if (ran && best != source) {
            source.consumeRedstonePulse();
        }
        if (ran && best != source && player != null) {
            record(player, machineIdFor(best), "network_automation_route");
        }
        return ran;
    }

    public static boolean runBestAutomationRecipe(ServerPlayer player, AetherStorageBlockEntity node) {
        if (node == null || !node.automationActive()) {
            return false;
        }
        AetherStorage storage = node.aetherStorage();
        AetherAutomationRecipe catalogRecipe = bestAutomationRecipe(node);
        if (catalogRecipe == null) {
            if (player != null) {
                player.sendSystemMessage(Component.translatable("message.echoaetherworks.no_recipe"));
            }
            return false;
        }
        boolean ran = false;
        String recipe = catalogRecipe.id();
        if ("condenser_overload_scrubber".equals(recipe)
                && node.canAcceptAutomationOutput(new ItemStack(ModItems.AETHER_COIL.get(), 3))
                && consumeAutomationInputs(node, ModItems.AETHER_CAPACITOR.get(), 1, ModItems.PURITY_CATALYST.get(), 1)) {
            node.setAetherStorage(new AetherStorage(
                    storage.storedAmount() - 52.0D,
                    Math.min(1024.0D, storage.maxStoredAmount() + 16.0D),
                    storage.acceptedTypes(),
                    AetherSignalType.REFINED_AETHER,
                    Math.min(40.0D, storage.transferRate() + 1.5D),
                    Math.max(0.0D, storage.contaminationLevel() - 0.28D)));
            node.addAutomationOutput(new ItemStack(ModItems.AETHER_COIL.get(), 3));
            ran = true;
        } else if ("cell_storage_bus".equals(recipe)
                && node.canAcceptAutomationOutput(new ItemStack(ModItems.PURITY_CATALYST.get(), 2))
                && consumeAutomationInputs(node, ModItems.AETHER_CAPACITOR.get(), 1, ModItems.AETHER_COIL.get(), 1)) {
            node.setAetherStorage(new AetherStorage(
                    storage.storedAmount() - 44.0D,
                    Math.min(1200.0D, storage.maxStoredAmount() + 72.0D),
                    storage.acceptedTypes(),
                    storage.outputType(),
                    Math.min(44.0D, storage.transferRate() + 2.5D),
                    Math.max(0.0D, storage.contaminationLevel() - 0.06D)));
            node.addAutomationOutput(new ItemStack(ModItems.PURITY_CATALYST.get(), 2));
            ran = true;
        } else if ("conduit_redstone_relay".equals(recipe)
                && node.canAcceptAutomationOutput(new ItemStack(ModItems.PURITY_CATALYST.get(), 2))
                && consumeAutomationInputs(node, ModItems.AETHER_COIL.get(), 1, ModItems.AETHER_CAPACITOR.get(), 1)) {
            node.setAetherStorage(new AetherStorage(
                    storage.storedAmount() - 34.0D,
                    storage.maxStoredAmount(),
                    storage.acceptedTypes(),
                    storage.outputType(),
                    Math.min(44.0D, storage.transferRate() + 3.0D),
                    Math.max(0.0D, storage.contaminationLevel() - 0.1D)));
            node.addAutomationOutput(new ItemStack(ModItems.PURITY_CATALYST.get(), 2));
            ran = true;
        } else if ("cell_overflow_shunt".equals(recipe)
                && node.canAcceptAutomationOutput(new ItemStack(ModItems.AETHER_COIL.get(), 2))
                && consumeAutomationInputs(node, ModItems.AETHER_CAPACITOR.get(), 1, ModItems.AETHER_CAPACITOR.get(), 1)) {
            node.setAetherStorage(new AetherStorage(
                    storage.storedAmount() - 64.0D,
                    Math.min(1400.0D, storage.maxStoredAmount() + 128.0D),
                    storage.acceptedTypes(),
                    storage.outputType(),
                    Math.min(48.0D, storage.transferRate() + 1.0D),
                    Math.max(0.0D, storage.contaminationLevel() - 0.18D)));
            node.addAutomationOutput(new ItemStack(ModItems.AETHER_COIL.get(), 2));
            ran = true;
        } else if ("conduit_pulse_damper".equals(recipe)
                && node.canAcceptAutomationOutput(new ItemStack(ModItems.AETHER_CAPACITOR.get()))
                && consumeAutomationInputs(node, ModItems.AETHER_COIL.get(), 1, ModItems.PURITY_CATALYST.get(), 1)) {
            node.setAetherStorage(new AetherStorage(
                    storage.storedAmount() - 30.0D,
                    storage.maxStoredAmount(),
                    storage.acceptedTypes(),
                    storage.outputType(),
                    Math.min(48.0D, storage.transferRate() + 4.0D),
                    Math.max(0.0D, storage.contaminationLevel() - 0.18D)));
            node.addAutomationOutput(new ItemStack(ModItems.AETHER_CAPACITOR.get()));
            ran = true;
        } else if ("condenser_signal_rectifier".equals(recipe)
                && node.canAcceptAutomationOutput(new ItemStack(ModItems.PURITY_CATALYST.get()))
                && consumeAutomationInputs(node, ModItems.AETHER_COIL.get(), 1, ModItems.AETHER_COIL.get(), 1)) {
            node.setAetherStorage(new AetherStorage(
                    storage.storedAmount() - 30.0D,
                    Math.min(1024.0D, storage.maxStoredAmount() + 12.0D),
                    storage.acceptedTypes(),
                    AetherSignalType.SIGNAL_AETHER,
                    Math.min(42.0D, storage.transferRate() + 1.0D),
                    Math.min(1.0D, storage.contaminationLevel() + 0.03D)));
            node.addAutomationOutput(new ItemStack(ModItems.PURITY_CATALYST.get()));
            ran = true;
        } else if ("stabilized_capacitor_array".equals(recipe)
                && node.canAcceptAutomationOutput(new ItemStack(ModItems.AETHER_CAPACITOR.get(), 2))
                && consumeAutomationInputs(node, ModItems.AETHER_COIL.get(), 1, ModItems.PURITY_CATALYST.get(), 1)) {
            node.setAetherStorage(new AetherStorage(
                    storage.storedAmount() - 36.0D,
                    Math.min(1024.0D, storage.maxStoredAmount() + 48.0D),
                    storage.acceptedTypes(),
                    storage.outputType(),
                    Math.min(40.0D, storage.transferRate() + 2.0D),
                    Math.max(0.0D, storage.contaminationLevel() - 0.08D)));
            node.addAutomationOutput(new ItemStack(ModItems.AETHER_CAPACITOR.get(), 2));
            ran = true;
        } else if ("condenser_reactor_seed".equals(recipe)
                && node.canAcceptAutomationOutput(new ItemStack(ModItems.AETHER_CAPACITOR.get(), 3))
                && consumeAutomationInputs(node, ModItems.AETHER_COIL.get(), 1, ModItems.PURITY_CATALYST.get(), 1)) {
            node.setAetherStorage(new AetherStorage(
                    storage.storedAmount() - 40.0D,
                    Math.min(1024.0D, storage.maxStoredAmount() + 24.0D),
                    storage.acceptedTypes(),
                    AetherSignalType.REFINED_AETHER,
                    Math.min(40.0D, storage.transferRate() + 1.0D),
                    Math.max(0.0D, storage.contaminationLevel() - 0.06D)));
            node.addAutomationOutput(new ItemStack(ModItems.AETHER_CAPACITOR.get(), 3));
            ran = true;
        } else if ("conduit_filter_bundle".equals(recipe)
                && node.canAcceptAutomationOutput(new ItemStack(ModItems.AETHER_COIL.get(), 2))
                && consumeAutomationInputs(node, ModItems.AETHER_CAPACITOR.get(), 1, ModItems.PURITY_CATALYST.get(), 1)) {
            node.setAetherStorage(new AetherStorage(
                    Math.max(0.0D, storage.storedAmount() - 28.0D),
                    storage.maxStoredAmount(),
                    storage.acceptedTypes(),
                    storage.outputType(),
                    Math.min(40.0D, storage.transferRate() + 2.0D),
                    Math.max(0.0D, storage.contaminationLevel() - 0.32D)));
            node.addAutomationOutput(new ItemStack(ModItems.AETHER_COIL.get(), 2));
            ran = true;
        } else if ("crystallize_aether".equals(recipe)
                && node.canAcceptAutomationOutput(new ItemStack(ModItems.AETHER_CAPACITOR.get(), 2))
                && node.consumeAutomationInput(ModItems.AETHER_COIL.get(), 1)) {
            node.setAetherStorage(new AetherStorage(
                    storage.storedAmount() - 32.0D,
                    storage.maxStoredAmount(),
                    storage.acceptedTypes(),
                    AetherSignalType.REFINED_AETHER,
                    storage.transferRate(),
                    storage.contaminationLevel() + 0.02D));
            node.addAutomationOutput(new ItemStack(ModItems.AETHER_CAPACITOR.get(), 2));
            ran = true;
        } else if ("capacitor_batch".equals(recipe)
                && node.canAcceptAutomationOutput(new ItemStack(ModItems.PURITY_CATALYST.get()))
                && node.consumeAutomationInput(ModItems.AETHER_CAPACITOR.get(), 1)) {
            node.setAetherStorage(new AetherStorage(
                    storage.storedAmount() - 24.0D,
                    Math.min(1024.0D, storage.maxStoredAmount() + 16.0D),
                    storage.acceptedTypes(),
                    storage.outputType(),
                    Math.min(40.0D, storage.transferRate() + 1.0D),
                    storage.contaminationLevel()));
            node.addAutomationOutput(new ItemStack(ModItems.PURITY_CATALYST.get()));
            ran = true;
        } else if ("purity_distillation".equals(recipe)
                && node.canAcceptAutomationOutput(new ItemStack(ModItems.AETHER_COIL.get(), 2))
                && node.consumeAutomationInput(ModItems.PURITY_CATALYST.get(), 1)) {
            node.setAetherStorage(new AetherStorage(
                    storage.storedAmount() - 24.0D,
                    storage.maxStoredAmount(),
                    storage.acceptedTypes(),
                    AetherSignalType.REFINED_AETHER,
                    storage.transferRate(),
                    Math.max(0.0D, storage.contaminationLevel() - 0.12D)));
            node.addAutomationOutput(new ItemStack(ModItems.AETHER_COIL.get(), 2));
            ran = true;
        } else if ("coil_winding".equals(recipe)
                && node.canAcceptAutomationOutput(new ItemStack(ModItems.AETHER_CAPACITOR.get()))
                && node.consumeAutomationInput(ModItems.AETHER_COIL.get(), 1)) {
            node.setAetherStorage(new AetherStorage(
                    storage.storedAmount() - 20.0D,
                    Math.min(1024.0D, storage.maxStoredAmount() + 8.0D),
                    storage.acceptedTypes(),
                    storage.outputType(),
                    Math.min(40.0D, storage.transferRate() + 0.5D),
                    storage.contaminationLevel() + 0.01D));
            node.addAutomationOutput(new ItemStack(ModItems.AETHER_CAPACITOR.get()));
            ran = true;
        } else if ("purity_line_scrub".equals(recipe)
                && node.canAcceptAutomationOutput(new ItemStack(ModItems.AETHER_COIL.get()))
                && node.consumeAutomationInput(ModItems.PURITY_CATALYST.get(), 1)) {
            node.setAetherStorage(new AetherStorage(
                    Math.max(0.0D, storage.storedAmount() - 16.0D),
                    storage.maxStoredAmount(),
                    storage.acceptedTypes(),
                    storage.outputType(),
                    Math.min(40.0D, storage.transferRate() + 1.0D),
                    Math.max(0.0D, storage.contaminationLevel() - 0.35D)));
            node.addAutomationOutput(new ItemStack(ModItems.AETHER_COIL.get()));
            ran = true;
        } else if ("capacitor_bleed".equals(recipe)
                && node.canAcceptAutomationOutput(new ItemStack(ModItems.PURITY_CATALYST.get()))
                && node.consumeAutomationInput(ModItems.AETHER_CAPACITOR.get(), 1)) {
            node.setAetherStorage(new AetherStorage(
                    Math.max(0.0D, storage.storedAmount() - 20.0D),
                    storage.maxStoredAmount(),
                    storage.acceptedTypes(),
                    storage.outputType(),
                    Math.min(40.0D, storage.transferRate() + 1.5D),
                    Math.max(0.0D, storage.contaminationLevel() - 0.2D)));
            node.addAutomationOutput(new ItemStack(ModItems.PURITY_CATALYST.get()));
            ran = true;
        } else if (node instanceof AetherCondenserBlockEntity && storage.storedAmount() >= 96.0D) {
            node.setAetherStorage(new AetherStorage(
                    storage.storedAmount() - 16.0D,
                    storage.maxStoredAmount(),
                    storage.acceptedTypes(),
                    AetherSignalType.REFINED_AETHER,
                    storage.transferRate(),
                    storage.contaminationLevel() + 0.04D));
            ran = true;
        } else if (node instanceof AetherCellBlockEntity && storage.storedAmount() >= 64.0D) {
            node.setAetherStorage(new AetherStorage(
                    storage.storedAmount() - 16.0D,
                    Math.min(960.0D, storage.maxStoredAmount() + 32.0D),
                    storage.acceptedTypes(),
                    storage.outputType(),
                    Math.min(36.0D, storage.transferRate() + 2.0D),
                    storage.contaminationLevel()));
            ran = true;
        } else if (node instanceof AetherConduitBlockEntity
                && (storage.contaminationLevel() > 0.05D || storage.storedAmount() >= 16.0D)) {
            node.setAetherStorage(new AetherStorage(
                    Math.max(0.0D, storage.storedAmount() - 8.0D),
                    storage.maxStoredAmount(),
                    storage.acceptedTypes(),
                    storage.outputType(),
                    storage.transferRate(),
                    Math.max(0.0D, storage.contaminationLevel() - 0.18D)));
            ran = true;
        }
        if (!ran) {
            if (player != null) {
                player.sendSystemMessage(Component.translatable("message.echoaetherworks.no_recipe"));
            }
            return false;
        }
        node.incrementAutomationCycles();
        node.consumeRedstonePulse();
        if (node.getLevel() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            node.checkOverloadSafety(serverLevel);
        }
        if (player != null) {
            player.sendSystemMessage(Component.translatable("message.echoaetherworks.recipe_ran", recipe));
            record(player, machineIdFor(node), "automation_" + recipe);
        }
        return true;
    }

    private static String familyFor(String id) {
        if (id == null) {
            return "general";
        }
        return switch (id) {
            case "condenser_overload_scrubber" -> "generator_safety";
            case "cell_storage_bus" -> "storage_bus";
            case "conduit_redstone_relay" -> "network_control";
            case "condenser_signal_rectifier" -> "generator_signal";
            case "cell_overflow_shunt" -> "storage_safety";
            case "conduit_pulse_damper" -> "network_control";
            case "stabilized_capacitor_array", "capacitor_batch", "coil_winding", "capacity_tune" -> "storage";
            case "condenser_reactor_seed", "crystallize_aether", "purity_distillation", "refine" -> "generator";
            case "conduit_filter_bundle", "purity_line_scrub", "capacitor_bleed", "line_purge" -> "network";
            default -> "general";
        };
    }

    private static boolean consumeAutomationInputs(AetherStorageBlockEntity node, net.minecraft.world.item.Item first,
            int firstAmount, net.minecraft.world.item.Item second, int secondAmount) {
        if (!node.hasAutomationInput(first, firstAmount) || !node.hasAutomationInput(second, secondAmount)) {
            return false;
        }
        return node.consumeAutomationInput(first, firstAmount)
                && node.consumeAutomationInput(second, secondAmount);
    }

    public static void record(ServerPlayer player, Identifier subject, String action) {
        if (player == null || subject == null) {
            return;
        }
        EchoCoreServices.recordMissionObjective(player, MissionObjectiveType.CUSTOM, subject, 1,
                Map.of("source", EchoAetherWorks.MODID, "action", action == null ? "" : action));
    }
}
