package com.knoxhack.echomultiblockcore.runtime;

import com.knoxhack.echomultiblockcore.EchoMultiblockCore;
import com.knoxhack.echomultiblockcore.api.CapabilityDiagnostic;
import com.knoxhack.echomultiblockcore.api.CapabilityNode;
import com.knoxhack.echomultiblockcore.api.CapabilityRequirement;
import com.knoxhack.echomultiblockcore.api.CapabilityThroughput;
import com.knoxhack.echomultiblockcore.api.InstalledMultiblockUpgrade;
import com.knoxhack.echomultiblockcore.api.MultiblockAutomationRecipe;
import com.knoxhack.echomultiblockcore.api.MultiblockCapability;
import com.knoxhack.echomultiblockcore.api.MultiblockCapabilityRuntime;
import com.knoxhack.echomultiblockcore.api.MultiblockDefinition;
import com.knoxhack.echomultiblockcore.api.MultiblockUpgradeDefinition;
import com.knoxhack.echomultiblockcore.api.MultiblockUpgradeRegistry;
import com.knoxhack.echomultiblockcore.api.UpgradeModifier;
import com.knoxhack.echomultiblockcore.registry.ModBlocks;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class MultiblockCapabilityService {
    private static final Identifier NODE_DATA = MultiblockCapability.DATA.id();
    private static final TagKey<Block> POWER_BUS_NODES = TagKey.create(Registries.BLOCK,
            Identifier.fromNamespaceAndPath(EchoMultiblockCore.MODID, "power_bus_nodes"));

    private MultiblockCapabilityService() {
    }

    public static MultiblockCapabilityRuntime evaluate(
            MultiblockDefinition definition,
            MultiblockAutomationRecipe recipe,
            List<BlockPos> matchedBlocks,
            Level level,
            List<InstalledMultiblockUpgrade> upgrades) {
        if (level == null) {
            return MultiblockCapabilityRuntime.EMPTY;
        }
        List<CapabilityNode> nodes = discoverNodes(matchedBlocks, level);
        List<CapabilityRequirement> requirements = new ArrayList<>();
        if (definition != null) {
            requirements.addAll(definition.capabilityRequirements());
        }
        if (recipe != null) {
            requirements.addAll(recipe.capabilityCosts());
        }
        if (requirements.isEmpty()) {
            return new MultiblockCapabilityRuntime(nodes, List.of(), List.of());
        }

        Map<Identifier, Capacity> capacities = new LinkedHashMap<>();
        for (CapabilityNode node : nodes) {
            capacities.computeIfAbsent(node.capabilityId(), ignored -> new Capacity())
                    .add(node.capacity(), node.throughput());
        }
        int upgradeBonus = capabilityBonus(upgrades);
        List<CapabilityThroughput> throughput = new ArrayList<>();
        List<CapabilityDiagnostic> diagnostics = new ArrayList<>();
        for (CapabilityRequirement requirement : requirements) {
            Capacity capacity = capacities.getOrDefault(requirement.capabilityId(), Capacity.EMPTY);
            int available = capacity.capacity + upgradeBonus;
            int availableThroughput = capacity.throughput + upgradeBonus;
            throughput.add(new CapabilityThroughput(requirement.capabilityId(), requirement.amount(), available,
                    availableThroughput, requirement.unit()));
            if (requirement.required()
                    && (available < requirement.amount() || availableThroughput < requirement.throughput())) {
                diagnostics.add(new CapabilityDiagnostic(requirement.capabilityId(),
                        "Missing " + requirement.capabilityId() + " capacity "
                                + available + "/" + requirement.amount()
                                + " and throughput " + availableThroughput + "/" + requirement.throughput() + ".",
                        true));
            }
        }
        return new MultiblockCapabilityRuntime(nodes, throughput, diagnostics);
    }

    public static List<CapabilityNode> discoverNodes(List<BlockPos> matchedBlocks, Level level) {
        if (matchedBlocks == null || matchedBlocks.isEmpty() || level == null) {
            return List.of();
        }
        List<CapabilityNode> nodes = new ArrayList<>();
        for (BlockPos pos : matchedBlocks) {
            if (!level.hasChunkAt(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            CapabilityNode node = nodeFor(state, pos);
            if (node != null) {
                nodes.add(node);
            }
        }
        return List.copyOf(nodes);
    }

    public static String summarize(MultiblockCapabilityRuntime runtime) {
        if (runtime == null || runtime.throughput().isEmpty()) {
            return "No capability costs";
        }
        return runtime.throughput().stream()
                .map(value -> value.capabilityId() + " " + value.available() + "/" + value.required())
                .reduce((left, right) -> left + ", " + right)
                .orElse("No capability costs");
    }

    private static CapabilityNode nodeFor(BlockState state, BlockPos pos) {
        Block block = state.getBlock();
        if (block == ModBlocks.POWER_BUS.get() || state.is(POWER_BUS_NODES)) {
            return node(pos, MultiblockCapability.POWER_INPUT.id(), 1000, 128);
        }
        if (block == ModBlocks.DATA_BUS.get()) {
            return node(pos, NODE_DATA, 1000, 128);
        }
        if (block == ModBlocks.SIGNAL_CONDUIT.get() || block == ModBlocks.SIGNAL_TOWER_CORE.get()) {
            return node(pos, NODE_DATA, 250, 32);
        }
        if (block == ModBlocks.INPUT_CRATE.get()) {
            return node(pos, MultiblockCapability.ITEM_INPUT.id(), 512, 64);
        }
        if (block == ModBlocks.OUTPUT_CRATE.get()) {
            return node(pos, MultiblockCapability.ITEM_OUTPUT.id(), 512, 64);
        }
        if (block == ModBlocks.ROBOTIC_ARM.get()) {
            return node(pos, MultiblockCapability.ROBOTICS.id(), 1, 1);
        }
        if (block == ModBlocks.AUTO_BUILDER.get()) {
            return node(pos, MultiblockCapability.AUTO_BUILDER.id(), 1, 1);
        }
        return null;
    }

    private static CapabilityNode node(BlockPos pos, Identifier capabilityId, int capacity, int throughput) {
        return new CapabilityNode(
                Identifier.fromNamespaceAndPath(EchoMultiblockCore.MODID, "node_" + Long.toUnsignedString(pos.asLong())),
                capabilityId,
                pos,
                capacity,
                throughput,
                true);
    }

    private static int capabilityBonus(List<InstalledMultiblockUpgrade> upgrades) {
        if (upgrades == null || upgrades.isEmpty()) {
            return 0;
        }
        int bonus = 0;
        for (InstalledMultiblockUpgrade upgrade : upgrades) {
            MultiblockUpgradeDefinition definition = MultiblockUpgradeRegistry.byId(upgrade.upgradeId()).orElse(null);
            if (definition == null) {
                continue;
            }
            for (UpgradeModifier modifier : definition.modifiers()) {
                if (modifier.type() == UpgradeModifier.Type.CAPABILITY_BONUS
                        || modifier.type() == UpgradeModifier.Type.STORAGE_BONUS) {
                    bonus += (int) Math.round(modifier.value() * Math.max(1, upgrade.tier()));
                }
            }
        }
        return bonus;
    }

    private static final class Capacity {
        private static final Capacity EMPTY = new Capacity();
        private int capacity;
        private int throughput;

        private void add(int capacity, int throughput) {
            this.capacity += Math.max(0, capacity);
            this.throughput += Math.max(0, throughput);
        }
    }
}
