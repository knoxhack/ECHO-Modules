package com.knoxhack.echoorbitalremnants.faction;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.network.EchoPacketKind;
import com.knoxhack.echonetcore.api.EchoNetSend;
import com.knoxhack.echoorbitalremnants.entity.OrbitalFactionNpcEntity;
import com.knoxhack.echoorbitalremnants.item.FactionPledgeItem;
import com.knoxhack.echoorbitalremnants.network.OrbitalFactionDialogueOpenPayload;
import com.knoxhack.echoorbitalremnants.network.OrbitalFactionNpcActionPayload;
import com.knoxhack.echoorbitalremnants.progression.EchoTerminalProgress;
import com.knoxhack.echoorbitalremnants.registry.ModBlocks;
import com.knoxhack.echoorbitalremnants.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public final class OrbitalFactionDialogueService {
    public static final String ACTION_TALK = "talk";
    public static final String ACTION_SERVICE = "request_service";
    public static final String ACTION_ACCEPT_CHARTER = "accept_charter";
    public static final String ACTION_COMPLETE_CHARTER = "complete_charter";

    private static final double MAX_INTERACTION_DISTANCE_SQR = 64.0D;
    private static final long SERVICE_COOLDOWN_TICKS = 9600L;

    private OrbitalFactionDialogueService() {
    }

    public static void open(ServerPlayer player, OrbitalFactionNpcEntity npc) {
        if (player == null || npc == null || npc.isRemoved() || npc.distanceToSqr(player) > MAX_INTERACTION_DISTANCE_SQR) {
            return;
        }
        FactionPledgeItem.Faction faction = npc.faction();
        EchoTerminalProgress progress = EchoTerminalProgress.get(player);
        progress.recordOutpostContact(player, faction, player.level().getGameTime());
        EchoCoreServices.recordFactionInteraction(player, OrbitalOutpostProfiles.echoCoreFactionId(faction),
                npc.roleId(), player.level().getGameTime());
        EchoNetSend.toPlayer(player, packet(player, npc, progress), EchoPacketKind.CLIENTBOUND_SYNC);
    }

    public static void handleAction(OrbitalFactionNpcActionPayload payload, ServerPlayer player) {
        if (payload == null || player == null) {
            return;
        }
        Entity entity = player.level().getEntity(payload.entityId());
        if (!(entity instanceof OrbitalFactionNpcEntity npc) || npc.isRemoved()
                || npc.distanceToSqr(player) > MAX_INTERACTION_DISTANCE_SQR) {
            player.sendSystemMessage(Component.literal("ECHO-7 // Outpost contact signal out of range."));
            return;
        }
        FactionPledgeItem.Faction faction = npc.faction();
        if (player.level().dimension() != OrbitalOutpostProfiles.requiredDimension(faction)) {
            player.sendSystemMessage(Component.literal("ECHO-7 // Outpost charter rejected: wrong route dimension."));
            return;
        }

        String action = payload.actionId() == null ? "" : payload.actionId().trim();
        if (ACTION_TALK.equals(action)) {
            talk(player, npc, faction);
        } else if (ACTION_SERVICE.equals(action)) {
            requestService(player, npc, faction);
        } else if (ACTION_ACCEPT_CHARTER.equals(action)) {
            acceptCharter(player, npc, faction, payload.targetId());
        } else if (ACTION_COMPLETE_CHARTER.equals(action)) {
            completeCharter(player, npc, faction, payload.targetId());
        } else if (action.startsWith("barter_")) {
            barter(player, npc, faction, action);
        } else {
            player.sendSystemMessage(Component.literal("ECHO-7 // Outpost contact did not recognize that action."));
        }
    }

    private static OrbitalFactionDialogueOpenPayload packet(ServerPlayer player, OrbitalFactionNpcEntity npc,
            EchoTerminalProgress progress) {
        FactionPledgeItem.Faction faction = npc.faction();
        List<OrbitalFactionDialogueOpenPayload.ActionEntry> actions = new ArrayList<>();
        actions.add(new OrbitalFactionDialogueOpenPayload.ActionEntry(ACTION_TALK, "Talk",
                "Ask for local route context and update the terminal contact record.", false, true, ""));

        long now = player.level().getGameTime();
        boolean serviceReady = progress.outpostTier(faction) >= 1 && progress.outpostServiceReady(faction, now);
        String serviceLock = progress.outpostTier(faction) < 1
                ? "Complete this faction's Tier I charter first."
                : "Service cache cooling down.";
        actions.add(new OrbitalFactionDialogueOpenPayload.ActionEntry(ACTION_SERVICE, "Request Service",
                serviceDescription(faction), true, serviceReady, serviceReady ? "" : serviceLock));

        for (BarterOffer offer : barterOffers(faction)) {
            boolean enabled = offer.canPay(player);
            actions.add(new OrbitalFactionDialogueOpenPayload.ActionEntry(offer.id(), "Barter: " + offer.label(),
                    offer.description(), true, enabled, enabled ? "" : offer.lockedReason()));
        }

        String contractId = OrbitalOutpostProfiles.contractId(faction);
        boolean completed = progress.outpostTierOneComplete(faction);
        boolean active = progress.isOutpostCharterActive(faction);
        boolean canAccept = progress.canAcceptOutpostCharter(faction);
        CompletionCheck completion = canCompleteCharter(player, progress, faction, false);
        boolean canComplete = active && !completed && completion.ok();
        String locked = completed ? "Tier I charter archived."
                : !progress.outpostCharterUnlockReady(faction) ? "Resolve ECHO-0 before Sporebound will bind a Nexus charter."
                : !active && !canAccept ? "Another outpost charter is active."
                : active && !canComplete ? completion.message()
                : "";
        List<OrbitalFactionDialogueOpenPayload.ContractEntry> contracts = List.of(
                new OrbitalFactionDialogueOpenPayload.ContractEntry(
                        contractId,
                        OrbitalOutpostProfiles.contractTitle(faction),
                        "Tier I outpost charter required for the final ECHO-7 seal.",
                        OrbitalOutpostProfiles.objective(faction),
                        OrbitalOutpostProfiles.reward(faction),
                        OrbitalOutpostProfiles.route(faction),
                        progress.outpostTierOneComplete(faction) ? "Tier I complete."
                                : active ? completion.message()
                                : "Available at this outpost.",
                        locked,
                        active,
                        completed,
                        canAccept,
                        canComplete));
        return new OrbitalFactionDialogueOpenPayload(
                npc.getId(),
                OrbitalOutpostProfiles.factionId(faction),
                faction.displayName(),
                OrbitalOutpostProfiles.shortName(faction),
                npc.roleId(),
                OrbitalOutpostProfiles.roleName(faction),
                progress.outpostTier(faction),
                progress.outpostStanding(faction).name(),
                OrbitalOutpostProfiles.greeting(faction),
                OrbitalOutpostProfiles.localContext(faction) + " " + progress.outpostCharterStatus(),
                progress.activeOutpostCharterId(),
                actions,
                contracts);
    }

    private static void talk(ServerPlayer player, OrbitalFactionNpcEntity npc, FactionPledgeItem.Faction faction) {
        EchoTerminalProgress progress = EchoTerminalProgress.get(player);
        String report = OrbitalOutpostProfiles.shortName(faction) + " outpost contact logged. "
                + OrbitalOutpostProfiles.localContext(faction);
        progress.setLastTerminalReport(player, report);
        EchoCoreServices.rememberFactionNpc(player, OrbitalOutpostProfiles.echoCoreFactionId(faction), report);
        player.sendSystemMessage(Component.literal("ECHO-7 // " + report));
        open(player, npc);
    }

    private static void requestService(ServerPlayer player, OrbitalFactionNpcEntity npc, FactionPledgeItem.Faction faction) {
        EchoTerminalProgress progress = EchoTerminalProgress.get(player);
        long now = player.level().getGameTime();
        if (progress.outpostTier(faction) < 1) {
            player.sendSystemMessage(Component.literal("ECHO-7 // Complete the Tier I outpost charter before requesting service."));
            open(player, npc);
            return;
        }
        if (!progress.outpostServiceReady(faction, now)) {
            player.sendSystemMessage(Component.literal("ECHO-7 // Outpost service cache is cooling down."));
            open(player, npc);
            return;
        }
        grantService(player, faction);
        progress.markOutpostServiceUsed(player, faction, now + SERVICE_COOLDOWN_TICKS);
        String report = OrbitalOutpostProfiles.shortName(faction) + " service cache delivered.";
        progress.setLastTerminalReport(player, report);
        player.sendSystemMessage(Component.literal("ECHO-7 // " + report));
        open(player, npc);
    }

    private static void acceptCharter(ServerPlayer player, OrbitalFactionNpcEntity npc, FactionPledgeItem.Faction faction,
            String targetId) {
        if (!OrbitalOutpostProfiles.contractId(faction).equals(targetId)) {
            player.sendSystemMessage(Component.literal("ECHO-7 // Outpost charter target mismatch."));
            open(player, npc);
            return;
        }
        EchoTerminalProgress progress = EchoTerminalProgress.get(player);
        EchoTerminalProgress.OutpostCharterResult result = progress.acceptOutpostCharter(player, faction);
        player.sendSystemMessage(Component.literal("ECHO-7 // " + result.message()));
        open(player, npc);
    }

    private static void completeCharter(ServerPlayer player, OrbitalFactionNpcEntity npc, FactionPledgeItem.Faction faction,
            String targetId) {
        if (!OrbitalOutpostProfiles.contractId(faction).equals(targetId)) {
            player.sendSystemMessage(Component.literal("ECHO-7 // Outpost charter target mismatch."));
            open(player, npc);
            return;
        }
        EchoTerminalProgress progress = EchoTerminalProgress.get(player);
        CompletionCheck completion = canCompleteCharter(player, progress, faction, true);
        if (!completion.ok()) {
            player.sendSystemMessage(Component.literal("ECHO-7 // " + completion.message()));
            open(player, npc);
            return;
        }
        EchoTerminalProgress.OutpostCharterResult result = progress.completeOutpostCharter(player, faction);
        if (result.completed()) {
            grantCharterReward(player, faction);
        }
        player.sendSystemMessage(Component.literal("ECHO-7 // " + result.message()));
        open(player, npc);
    }

    private static void barter(ServerPlayer player, OrbitalFactionNpcEntity npc, FactionPledgeItem.Faction faction, String actionId) {
        BarterOffer offer = barterOffers(faction).stream()
                .filter(candidate -> candidate.id().equals(actionId))
                .findFirst()
                .orElse(null);
        if (offer == null) {
            player.sendSystemMessage(Component.literal("ECHO-7 // No such outpost barter."));
            return;
        }
        if (!offer.canPay(player)) {
            player.sendSystemMessage(Component.literal("ECHO-7 // " + offer.lockedReason()));
            open(player, npc);
            return;
        }
        offer.pay(player);
        offer.grant(player);
        EchoTerminalProgress progress = EchoTerminalProgress.get(player);
        String report = OrbitalOutpostProfiles.shortName(faction) + " barter complete: " + offer.label() + ".";
        progress.setLastTerminalReport(player, report);
        player.sendSystemMessage(Component.literal("ECHO-7 // " + report));
        open(player, npc);
    }

    private static CompletionCheck canCompleteCharter(Player player, EchoTerminalProgress progress,
            FactionPledgeItem.Faction faction, boolean consume) {
        if (progress.outpostTierOneComplete(faction)) {
            return new CompletionCheck(false, "Tier I charter already complete.");
        }
        if (!progress.isOutpostCharterActive(faction)) {
            return new CompletionCheck(false, "Accept this outpost charter first.");
        }
        if (!progress.outpostCharterUnlockReady(faction)) {
            return new CompletionCheck(false, "Resolve ECHO-0 before Sporebound Nexus charters unlock.");
        }
        if (player.level().dimension() != OrbitalOutpostProfiles.requiredDimension(faction)) {
            return new CompletionCheck(false, "Complete the charter at its matching outpost route.");
        }
        return switch (faction) {
            case VOID_SALVAGERS -> {
                if (nearbyAnyBlock(player, ModBlocks.SATURN_RING_RELAY.get())) {
                    yield new CompletionCheck(true, "Saturn Ring Relay scan proof ready.");
                }
                yield consumePair(player, ModItems.SATURN_RING_FRAGMENT.get(), 1, ModItems.VACUUM_CIRCUIT.get(), 1, consume,
                        "Need 1 Saturn Ring Fragment and 1 Vacuum Circuit, or scan a Saturn Ring Relay.");
            }
            case ORBITAL_REMNANT -> {
                if (nearbyAnyBlock(player, ModBlocks.TITAN_METHANE_PUMP.get())) {
                    yield new CompletionCheck(true, "Titan Methane Pump containment proof ready.");
                }
                yield consumePair(player, ModItems.TITAN_METHANE_CELL.get(), 1, ModItems.SUIT_SEALANT_PATCH.get(), 1, consume,
                        "Need 1 Titan Methane Cell and 1 Suit Sealant Patch, or scan a Titan Methane Pump.");
            }
            case NEXUS_CHOIR -> {
                if (nearbyAnyBlock(player, ModBlocks.NEXUS_ANCHOR.get(), ModBlocks.NEXUS_GROWTH.get())) {
                    yield new CompletionCheck(true, "Nexus Anchor/Growth interpretation proof ready.");
                }
                if (count(player, ModItems.NEXUS_STABILIZER_SHARD.get()) < 1) {
                    yield new CompletionCheck(false, "Need 1 Nexus Stabilizer Shard, or scan Nexus Anchor/Growth.");
                }
                if (consume && !player.hasInfiniteMaterials()) {
                    consume(player, ModItems.NEXUS_STABILIZER_SHARD.get(), 1);
                }
                yield new CompletionCheck(true, "Nexus Stabilizer Shard delivered.");
            }
        };
    }

    private static CompletionCheck consumePair(Player player, Item first, int firstCount, Item second, int secondCount,
            boolean consume, String missing) {
        if (count(player, first) < firstCount || count(player, second) < secondCount) {
            return new CompletionCheck(false, missing);
        }
        if (consume && !player.hasInfiniteMaterials()) {
            consume(player, first, firstCount);
            consume(player, second, secondCount);
        }
        return new CompletionCheck(true, "Delivery proof ready.");
    }

    private static String serviceDescription(FactionPledgeItem.Faction faction) {
        return switch (faction) {
            case VOID_SALVAGERS -> "Claim ring salvage support: alloy, hull repair, and navigation proof.";
            case ORBITAL_REMNANT -> "Claim containment support: oxygen, sealant, and pressure supplies.";
            case NEXUS_CHOIR -> "Claim anomaly support: cryo reserves and stabilization material.";
        };
    }

    private static void grantService(Player player, FactionPledgeItem.Faction faction) {
        switch (faction) {
            case VOID_SALVAGERS -> {
                give(player, ModItems.ORBITAL_ALLOY, 2);
                give(player, ModItems.VACUUM_CIRCUIT, 1);
                give(player, ModItems.HEAT_SHIELD_PLATE, 1);
            }
            case ORBITAL_REMNANT -> {
                give(player, ModItems.EMERGENCY_OXYGEN_CELL, 3);
                give(player, ModItems.SUIT_SEALANT_PATCH, 2);
                give(player, ModItems.OXYGEN_CANISTER, 1);
            }
            case NEXUS_CHOIR -> {
                give(player, ModItems.NEXUS_DUST, 4);
                give(player, ModItems.CRYO_BATTERY, 1);
                give(player, ModItems.NEXUS_STABILIZER_SHARD, 1);
            }
        }
    }

    private static void grantCharterReward(Player player, FactionPledgeItem.Faction faction) {
        switch (faction) {
            case VOID_SALVAGERS -> {
                give(player, ModItems.NAVIGATION_CHIP, 1);
                give(player, ModItems.ORBITAL_ALLOY, 3);
                give(player, ModItems.HEAT_SHIELD_PLATE, 1);
            }
            case ORBITAL_REMNANT -> {
                give(player, ModItems.EMERGENCY_OXYGEN_CELL, 4);
                give(player, ModItems.SUIT_SEALANT_PATCH, 3);
                give(player, ModItems.OXYGEN_CANISTER, 1);
            }
            case NEXUS_CHOIR -> {
                give(player, ModItems.NEXUS_DUST, 6);
                give(player, ModItems.CRYO_BATTERY, 1);
                give(player, ModItems.NEXUS_STABILIZER_SHARD, 1);
            }
        }
    }

    private static List<BarterOffer> barterOffers(FactionPledgeItem.Faction faction) {
        return switch (faction) {
            case VOID_SALVAGERS -> List.of(
                    new BarterOffer("barter_crashbreak_navigation", "Navigation Chip",
                            "1 Saturn Ring Fragment -> 1 Navigation Chip",
                            () -> ModItems.SATURN_RING_FRAGMENT.get(), 1,
                            () -> ModItems.NAVIGATION_CHIP.get(), 1),
                    new BarterOffer("barter_crashbreak_alloy", "Alloy Bundle",
                            "2 Vacuum Circuits -> 3 Orbital Alloy",
                            () -> ModItems.VACUUM_CIRCUIT.get(), 2,
                            () -> ModItems.ORBITAL_ALLOY.get(), 3));
            case ORBITAL_REMNANT -> List.of(
                    new BarterOffer("barter_radwarden_oxygen", "Oxygen Cache",
                            "1 Titan Methane Cell -> 2 Oxygen Canisters",
                            () -> ModItems.TITAN_METHANE_CELL.get(), 1,
                            () -> ModItems.OXYGEN_CANISTER.get(), 2),
                    new BarterOffer("barter_radwarden_sealant", "Sealant Cache",
                            "1 Saturn Relay Lens -> 3 Suit Sealant Patches",
                            () -> ModItems.SATURN_RELAY_LENS.get(), 1,
                            () -> ModItems.SUIT_SEALANT_PATCH.get(), 3));
            case NEXUS_CHOIR -> List.of(
                    new BarterOffer("barter_sporebound_cryo", "Cryo Battery",
                            "4 Nexus Dust -> 1 Cryo Battery",
                            () -> ModItems.NEXUS_DUST.get(), 4,
                            () -> ModItems.CRYO_BATTERY.get(), 1),
                    new BarterOffer("barter_sporebound_stabilizer", "Stabilizer Trade",
                            "1 Titan Survey Core -> 1 Nexus Stabilizer Shard",
                            () -> ModItems.TITAN_SURVEY_CORE.get(), 1,
                            () -> ModItems.NEXUS_STABILIZER_SHARD.get(), 1));
        };
    }

    private static boolean nearbyAnyBlock(Player player, Block... blocks) {
        BlockPos center = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-8, -4, -8), center.offset(8, 5, 8))) {
            Block current = player.level().getBlockState(pos).getBlock();
            for (Block block : blocks) {
                if (current == block) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int count(Player player, Item item) {
        int total = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static void consume(Player player, Item item, int amount) {
        int remaining = amount;
        for (int slot = 0; slot < player.getInventory().getContainerSize() && remaining > 0; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.is(item)) {
                continue;
            }
            int take = Math.min(remaining, stack.getCount());
            stack.shrink(take);
            remaining -= take;
        }
    }

    private static void give(Player player, Supplier<? extends Item> item, int count) {
        ItemStack stack = new ItemStack(item.get(), count);
        if (!player.addItem(stack)) {
            player.drop(stack, false);
        }
    }

    private record CompletionCheck(boolean ok, String message) {
    }

    private record BarterOffer(String id, String label, String description, Supplier<? extends Item> costItem,
            int costCount, Supplier<? extends Item> rewardItem, int rewardCount) {
        boolean canPay(Player player) {
            return count(player, costItem.get()) >= costCount;
        }

        String lockedReason() {
            Item item = costItem.get();
            return "Need " + costCount + " " + item.getName(new ItemStack(item)).getString() + ".";
        }

        void pay(Player player) {
            if (!player.hasInfiniteMaterials()) {
                consume(player, costItem.get(), costCount);
            }
        }

        void grant(Player player) {
            give(player, rewardItem, rewardCount);
        }
    }
}
