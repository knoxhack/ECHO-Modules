package com.knoxhack.echoashfallprotocol.faction;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.EchoFactionActionResult;
import com.knoxhack.echocore.api.EchoFactionContract;
import com.knoxhack.echocore.api.EchoFactionContractState;
import com.knoxhack.echocore.api.EchoFactionProfile;
import com.knoxhack.echoashfallprotocol.echo.EchoIntel;
import com.knoxhack.echoashfallprotocol.echo.QuestData;
import com.knoxhack.echoashfallprotocol.research.ResearchData;
import com.knoxhack.echoashfallprotocol.world.ExplorationSiteRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Objective progress, validation, and rewards for Ashfall Echo Core contracts.
 */
public final class AshfallFactionContractProgression {
    private AshfallFactionContractProgression() {
    }

    public static EchoFactionContractState state(Player player, EchoFactionProfile profile, EchoFactionContract contract) {
        EchoFactionContractState base = EchoFactionContractState.fromProfile(profile, contract);
        if (player == null || profile == null || contract == null) {
            return base;
        }
        AshfallFactionContracts.Spec spec = AshfallFactionContracts.spec(contract.id()).orElse(null);
        if (spec == null) {
            return base;
        }
        boolean active = profile.activeContractId().filter(contract.id()::equals).isPresent();
        if (!active) {
            return new EchoFactionContractState(contract.id(), base.canAccept(), false, base.progressLine(), base.lockedReason());
        }
        String progress = progressLine(player, spec);
        boolean complete = isComplete(player, spec);
        return new EchoFactionContractState(contract.id(), false, complete, progress,
                complete ? "" : "Objective pending: " + progress);
    }

    public static EchoFactionActionResult onAccepted(ServerPlayer player, EchoFactionProfile profile,
            EchoFactionContract contract) {
        AshfallFactionContracts.Spec spec = AshfallFactionContracts.spec(contract.id()).orElse(null);
        if (spec == null) {
            return null;
        }
        AshfallFactionContractData data = AshfallFactionContractData.get(player);
        data.ensureContract(spec);
        AshfallFactionContractData.saveAndSync(player, data);
        EchoCoreServices.rememberFactionNpc(player, profile.definition().id(),
                "Contract opened: " + spec.title() + ". " + progressLine(player, spec));
        return EchoFactionActionResult.success("Contract Accepted", spec.title() + " active. " + progressLine(player, spec));
    }

    public static EchoFactionActionResult onCompleted(ServerPlayer player, EchoFactionProfile profile,
            EchoFactionContract contract) {
        AshfallFactionContracts.Spec spec = AshfallFactionContracts.spec(contract.id()).orElse(null);
        if (spec == null) {
            return null;
        }
        if (!isComplete(player, spec)) {
            return EchoFactionActionResult.failure("Contract Pending", "Objective pending: " + progressLine(player, spec));
        }
        if (!consumeDeliveryItems(player, spec)) {
            return EchoFactionActionResult.failure("Delivery Missing", "Required delivery items are no longer available.");
        }
        grantRewards(player, profile, spec);
        recordCompletion(player, profile, spec);
        return EchoFactionActionResult.success("Contract Complete",
                spec.title() + " resolved. " + spec.rewardLine());
    }

    public static boolean progressPoi(ServerPlayer player, String poiId) {
        String normalized = ExplorationSiteRegistry.normalize(poiId);
        return progress(player, AshfallFactionContracts.ObjectiveType.POI_DISCOVERY, normalized, 1);
    }

    public static boolean progressKill(ServerPlayer player, String entityId) {
        return progress(player, AshfallFactionContracts.ObjectiveType.KILL, normalize(entityId), 1);
    }

    public static boolean progressRepair(ServerPlayer player, String repairId) {
        return progress(player, AshfallFactionContracts.ObjectiveType.REPAIR, normalize(repairId), 1);
    }

    public static boolean progressRaidDefense(ServerPlayer player, Identifier factionId) {
        String target = factionId == null ? "" : factionId.getPath();
        return progress(player, AshfallFactionContracts.ObjectiveType.RAID_DEFENSE, target, 1);
    }

    private static boolean progress(ServerPlayer player, AshfallFactionContracts.ObjectiveType type,
            String targetId, int amount) {
        if (player == null || amount <= 0) {
            return false;
        }
        AshfallFactionContractData data = AshfallFactionContractData.get(player);
        boolean changed = false;
        for (EchoFactionProfile profile : EchoCoreServices.factionProfiles(player)) {
            if (!profile.definition().id().toString().startsWith("echoashfallprotocol:")) {
                continue;
            }
            Identifier active = profile.activeContractId().orElse(null);
            AshfallFactionContracts.Spec spec = AshfallFactionContracts.spec(active).orElse(null);
            if (spec == null) {
                continue;
            }
            for (int i = 0; i < spec.objectives().size(); i++) {
                AshfallFactionContracts.Objective objective = spec.objectives().get(i);
                if (objective.type() != type || !matches(objective, targetId)) {
                    continue;
                }
                int before = data.progress(spec.contractId(), i);
                int after = data.addProgress(spec.contractId(), i, amount, objective.requiredCount());
                if (after != before) {
                    changed = true;
                    player.sendSystemMessage(Component.literal("[ECHO-7] " + profile.definition().shortName()
                            + " contract: " + objective.displayText() + " (" + after + "/"
                            + objective.requiredCount() + ")"), true);
                }
            }
        }
        if (changed) {
            AshfallFactionContractData.saveAndSync(player, data);
        }
        return changed;
    }

    private static boolean isComplete(Player player, AshfallFactionContracts.Spec spec) {
        AshfallFactionContractData data = AshfallFactionContractData.get(player);
        for (int i = 0; i < spec.objectives().size(); i++) {
            AshfallFactionContracts.Objective objective = spec.objectives().get(i);
            int progress = objective.type() == AshfallFactionContracts.ObjectiveType.ITEM_DELIVERY
                    ? countInventory(player, objective.targetIds().get(0))
                    : data.progress(spec.contractId(), i);
            if (progress < objective.requiredCount()) {
                return false;
            }
        }
        return !spec.objectives().isEmpty();
    }

    private static String progressLine(Player player, AshfallFactionContracts.Spec spec) {
        AshfallFactionContractData data = AshfallFactionContractData.get(player);
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < spec.objectives().size(); i++) {
            AshfallFactionContracts.Objective objective = spec.objectives().get(i);
            int progress = objective.type() == AshfallFactionContracts.ObjectiveType.ITEM_DELIVERY
                    ? countInventory(player, objective.targetIds().get(0))
                    : data.progress(spec.contractId(), i);
            lines.add(Math.min(progress, objective.requiredCount()) + "/" + objective.requiredCount()
                    + " " + objective.displayText());
        }
        return String.join(" | ", lines);
    }

    private static boolean consumeDeliveryItems(ServerPlayer player, AshfallFactionContracts.Spec spec) {
        for (AshfallFactionContracts.Objective objective : spec.objectives()) {
            if (objective.type() != AshfallFactionContracts.ObjectiveType.ITEM_DELIVERY) {
                continue;
            }
            String itemId = objective.targetIds().get(0);
            if (countInventory(player, itemId) < objective.requiredCount()) {
                return false;
            }
        }
        for (AshfallFactionContracts.Objective objective : spec.objectives()) {
            if (objective.type() == AshfallFactionContracts.ObjectiveType.ITEM_DELIVERY) {
                consumeItem(player, objective.targetIds().get(0), objective.requiredCount());
            }
        }
        return true;
    }

    private static void grantRewards(ServerPlayer player, EchoFactionProfile profile, AshfallFactionContracts.Spec spec) {
        for (String itemId : spec.rewardItems()) {
            BuiltInRegistries.ITEM.getOptional(Identifier.parse(itemId)).ifPresent(item -> give(player, item));
        }
        ResearchData research = ResearchData.get(player);
        research.addPoints(spec.researchReward());
        ResearchData.saveAndSync(player, research);
        EchoCoreServices.rememberFactionNpc(player, profile.definition().id(),
                "Contract complete: " + spec.title() + ". " + spec.rewardLine());
    }

    private static void recordCompletion(ServerPlayer player, EchoFactionProfile profile, AshfallFactionContracts.Spec spec) {
        QuestData quest = QuestData.get(player);
        quest.visitLocation("special", "faction:first_task_complete");
        quest.visitLocation("special", "faction:" + profile.definition().id().getPath() + ":contract_complete");
        QuestData.saveAndSync(player, quest);

        EchoIntel intel = EchoIntel.get(player);
        intel.addReconIntel("Faction Contract Complete",
                profile.definition().displayName() + " contract resolved: " + spec.title(),
                profile.definition().id(),
                EchoIntel.IntelPriority.MEDIUM);
        EchoIntel.saveAndSync(player, intel);
    }

    private static boolean matches(AshfallFactionContracts.Objective objective, String targetId) {
        String normalizedTarget = normalize(targetId);
        if (objective.targetIds().isEmpty()) {
            return true;
        }
        for (String target : objective.targetIds()) {
            String normalizedObjective = normalize(target);
            if (normalizedTarget.contains(normalizedObjective) || normalizedObjective.contains(normalizedTarget)) {
                return true;
            }
            if (aliasesOverlap(normalizedTarget, normalizedObjective)) {
                return true;
            }
        }
        return false;
    }

    private static boolean aliasesOverlap(String left, String right) {
        var leftAliases = ExplorationSiteRegistry.aliasesFor(left);
        var rightAliases = ExplorationSiteRegistry.aliasesFor(right);
        for (String alias : leftAliases) {
            if (rightAliases.contains(alias) || right.contains(alias)) {
                return true;
            }
        }
        for (String alias : rightAliases) {
            if (left.contains(alias)) {
                return true;
            }
        }
        return false;
    }

    private static int countInventory(Player player, String itemId) {
        if (player == null || itemId == null || itemId.isBlank()) {
            return 0;
        }
        Identifier id = Identifier.parse(itemId);
        int count = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(id)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static void consumeItem(Player player, String itemId, int count) {
        Identifier id = Identifier.parse(itemId);
        int remaining = count;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (remaining <= 0) {
                return;
            }
            if (!stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(id)) {
                int removed = Math.min(remaining, stack.getCount());
                stack.shrink(removed);
                remaining -= removed;
            }
        }
    }

    private static void give(ServerPlayer player, Item item) {
        ItemStack stack = new ItemStack(item);
        com.knoxhack.echoashfallprotocol.research.PerkEffectHandler.applyLootBonus(player, stack);
        if (!player.addItem(stack)) {
            player.drop(stack, false);
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }
}
