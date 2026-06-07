package com.knoxhack.echo.npcore.service;

import com.knoxhack.echo.npcore.data.NpcDataBridge;
import com.knoxhack.echo.npcore.data.NpcContactData;
import com.knoxhack.echo.npcore.dialogue.EchoNpcDialogue;
import com.knoxhack.echo.npcore.dialogue.EchoNpcDialogueManager;
import com.knoxhack.echo.npcore.dialogue.EchoNpcDialogueNode;
import com.knoxhack.echo.npcore.dialogue.EchoNpcDialogueOption;
import com.knoxhack.echo.npcore.dialogue.EchoNpcDialogueRuntime;
import com.knoxhack.echo.npcore.entity.EchoNpcEntity;
import com.knoxhack.echo.npcore.faction.EchoNpcFactionManager;
import com.knoxhack.echo.npcore.network.CloseNpcInteractionPacket;
import com.knoxhack.echo.npcore.network.EchoNpcScreenState;
import com.knoxhack.echo.npcore.network.OpenNpcScreenPacket;
import com.knoxhack.echo.npcore.network.RequestNpcServicePacket;
import com.knoxhack.echo.npcore.network.RequestNpcScreenRefreshPacket;
import com.knoxhack.echo.npcore.network.RequestNpcTradePacket;
import com.knoxhack.echo.npcore.network.SelectDialogueOptionPacket;
import com.knoxhack.echo.npcore.network.SyncNpcScreenStatePacket;
import com.knoxhack.echo.npcore.profile.EchoNpcProfile;
import com.knoxhack.echo.npcore.profile.EchoNpcProfileManager;
import com.knoxhack.echo.npcore.trade.EchoNpcTradeCost;
import com.knoxhack.echo.npcore.trade.EchoNpcTradeGroup;
import com.knoxhack.echo.npcore.trade.EchoNpcTradeManager;
import com.knoxhack.echo.npcore.trade.EchoNpcTradeOffer;
import com.knoxhack.echo.npcore.trade.EchoNpcTradeRuntime;
import com.knoxhack.echo.npcore.trade.EchoNpcTradeSet;
import com.knoxhack.echo.npcore.visual.EchoNpcVisualProfile;
import com.knoxhack.echo.npcore.visual.EchoNpcVisualProfileManager;
import com.knoxhack.echonetcore.api.EchoNetSend;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

public final class EchoNpcInteractionService {
    private EchoNpcInteractionService() {
    }

    public static void open(ServerPlayer player, EchoNpcEntity npc) {
        if (!valid(player, npc)) {
            return;
        }
        EchoNpcProfile profile = EchoNpcProfileManager.getOrFallback(npc.npcProfileId());
        recordContact(player, profile);
        npc.beginInteraction(player);
        EchoNpcDialogue dialogue = EchoNpcDialogueManager.getOrFallback(profile.dialogue());
        String start = EchoNpcDialogueRuntime.safeStart(dialogue);
        NpcDataBridge.setDialogueNode(player.getUUID(), npc.getUUID(), start);
        EchoNetSend.toPlayer(player, new OpenNpcScreenPacket(buildState(player, npc, "talk", start, "")));
    }

    public static void selectDialogueOption(ServerPlayer player, SelectDialogueOptionPacket packet) {
        EchoNpcEntity npc = npc(player, packet.entityId());
        if (!valid(player, npc)) {
            return;
        }
        EchoNpcProfile profile = EchoNpcProfileManager.getOrFallback(npc.npcProfileId());
        EchoNpcDialogue dialogue = EchoNpcDialogueManager.getOrFallback(profile.dialogue());
        String nodeId = NpcDataBridge.dialogueNode(player.getUUID(), npc.getUUID());
        if (nodeId.isBlank()) {
            nodeId = EchoNpcDialogueRuntime.safeStart(dialogue);
        }
        EchoNpcDialogueNode node = dialogue.nodeOrFallback(nodeId);
        EchoNpcDialogueOption option = EchoNpcDialogueRuntime.findOption(node, packet.optionId());
        if (option == null) {
            sync(player, npc, "talk", nodeId, "Unknown dialogue option.");
            return;
        }
        NpcAvailability.Result availability = NpcAvailability.check(player, profile.faction(), option.requiresMission(),
                option.requiresFactionStanding(), option.disabledReason());
        if (!availability.allowed()) {
            sync(player, npc, "talk", nodeId, availability.message());
            return;
        }
        if (!option.next().isBlank()) {
            String next = dialogue.nodes().containsKey(option.next()) ? option.next() : EchoNpcDialogueRuntime.safeStart(dialogue);
            NpcDataBridge.setDialogueNode(player.getUUID(), npc.getUUID(), next);
            sync(player, npc, "talk", next, "");
            return;
        }
        switch (option.action()) {
            case "open_trade" -> sync(player, npc, "trade", nodeId, "");
            case "open_services" -> sync(player, npc, "services", nodeId, "");
            case "open_intel" -> sync(player, npc, "intel", nodeId, "");
            case "discover_contact" -> {
                recordContact(player, profile);
                sync(player, npc, "intel", nodeId, "Contact recorded: " + profile.displayName() + ".");
            }
            case "close" -> EchoNetSend.toPlayer(player, new SyncNpcScreenStatePacket(buildState(player, npc, "close", nodeId, "")));
            default -> sync(player, npc, "talk", nodeId,
                    option.action().isBlank() ? "No action configured." : "Action not wired yet: " + option.action());
        }
    }

    public static void requestTrade(ServerPlayer player, RequestNpcTradePacket packet) {
        EchoNpcEntity npc = npc(player, packet.entityId());
        if (!valid(player, npc)) {
            return;
        }
        EchoNpcProfile profile = EchoNpcProfileManager.getOrFallback(npc.npcProfileId());
        recordContact(player, profile);
        EchoNpcTradeSet tradeSet = EchoNpcTradeManager.getOrEmpty(profile.trades());
        EchoNpcTradeOffer offer = tradeSet.offer(packet.offerId());
        if (offer == null || offer.output() == null) {
            sync(player, npc, "trade", currentNode(player, npc, profile), "Trade unavailable.");
            return;
        }
        NpcAvailability.Result availability = NpcAvailability.check(player, profile.faction(), offer.requiresMission(),
                offer.requiresFactionStanding(), offer.disabledReason());
        if (!availability.allowed()) {
            sync(player, npc, "trade", currentNode(player, npc, profile), availability.message());
            return;
        }
        ServerLevel level = serverLevel(player);
        int stock = currentStock(level, npc.getUUID(), offer);
        if (offer.stock() > 0 && stock <= 0) {
            sync(player, npc, "trade", currentNode(player, npc, profile), "Out of stock.");
            return;
        }
        ItemStack output = EchoNpcTradeRuntime.stack(offer.output());
        if (output.isEmpty()) {
            sync(player, npc, "trade", currentNode(player, npc, profile), "Trade output is invalid.");
            return;
        }
        if (!EchoNpcTradeRuntime.consumeCosts(player, offer.input())) {
            sync(player, npc, "trade", currentNode(player, npc, profile), "Missing required items.");
            return;
        }
        if (!player.getInventory().add(output.copy())) {
            player.drop(output.copy(), false);
        }
        if (offer.stock() > 0) {
            int nextStock = Math.max(0, stock - 1);
            NpcDataBridge.setStock(level, npc.getUUID(), offer.id(), nextStock);
            if (offer.restockTime() > 0 && nextStock < offer.stock()) {
                long now = level == null ? player.level().getGameTime() : level.getGameTime();
                long restockAt = NpcDataBridge.tradeRestockAt(level, npc.getUUID(), offer.id());
                if (restockAt <= now) {
                    NpcDataBridge.setTradeRestockAt(level, npc.getUUID(), offer.id(), now + offer.restockTime());
                }
            }
        }
        sync(player, npc, "trade", currentNode(player, npc, profile), "Trade complete: " + offer.title());
    }

    public static void requestService(ServerPlayer player, RequestNpcServicePacket packet) {
        EchoNpcEntity npc = npc(player, packet.entityId());
        if (!valid(player, npc)) {
            return;
        }
        EchoNpcProfile profile = EchoNpcProfileManager.getOrFallback(npc.npcProfileId());
        recordContact(player, profile);
        EchoNpcServiceSet set = EchoNpcServiceManager.getOrEmpty(profile.services());
        EchoNpcServiceDefinition service = set.service(packet.serviceId());
        if (service == null) {
            sync(player, npc, "services", currentNode(player, npc, profile), "Service unavailable.");
            return;
        }
        long now = player.level().getGameTime();
        long until = NpcDataBridge.serviceCooldownUntil(player, npc.getUUID(), service.id());
        if (until > now) {
            sync(player, npc, "services", currentNode(player, npc, profile), "Service cooling down: " + (until - now) + " ticks.");
            return;
        }
        NpcAvailability.Result availability = NpcAvailability.check(player, profile.faction(), service.requiresMission(),
                service.requiresFactionStanding(), service.disabledReason());
        if (!availability.allowed()) {
            sync(player, npc, "services", currentNode(player, npc, profile), availability.message());
            return;
        }
        if (!EchoNpcTradeRuntime.consumeCosts(player, service.cost())) {
            sync(player, npc, "services", currentNode(player, npc, profile), "Missing service cost.");
            return;
        }
        String result = EchoNpcServiceRuntime.apply(player, npc, profile, service);
        if (service.cooldown() > 0) {
            NpcDataBridge.setServiceCooldownUntil(player, npc.getUUID(), service.id(), now + service.cooldown());
        }
        sync(player, npc, "services", currentNode(player, npc, profile), result);
    }

    public static void refresh(ServerPlayer player, RequestNpcScreenRefreshPacket packet) {
        EchoNpcEntity npc = npc(player, packet.entityId());
        if (!valid(player, npc)) {
            return;
        }
        EchoNpcProfile profile = EchoNpcProfileManager.getOrFallback(npc.npcProfileId());
        sync(player, npc, cleanTab(packet.tab()), currentNode(player, npc, profile), "Screen state refreshed.");
    }

    public static void close(ServerPlayer player, CloseNpcInteractionPacket packet) {
        EchoNpcEntity npc = npc(player, packet.entityId());
        if (npc != null) {
            NpcDataBridge.setDialogueNode(player.getUUID(), npc.getUUID(), "");
            npc.endInteraction();
        }
    }

    public static EchoNpcScreenState buildState(ServerPlayer player, EchoNpcEntity npc, String tab, String nodeId, String status) {
        EchoNpcProfile profile = EchoNpcProfileManager.getOrFallback(npc.npcProfileId());
        EchoNpcVisualProfile visual = EchoNpcVisualProfileManager.getOrFallback(profile.visualProfile());
        EchoNpcDialogue dialogue = EchoNpcDialogueManager.getOrFallback(profile.dialogue());
        String effectiveNode = nodeId == null || nodeId.isBlank() ? EchoNpcDialogueRuntime.safeStart(dialogue) : nodeId;
        EchoNpcDialogueNode node = dialogue.nodeOrFallback(effectiveNode);
        List<EchoNpcScreenState.DialogueOptionState> options = node.options().stream()
                .map(option -> {
                    NpcAvailability.Result availability = NpcAvailability.check(player, profile.faction(),
                            option.requiresMission(), option.requiresFactionStanding(), option.disabledReason());
                    return new EchoNpcScreenState.DialogueOptionState(option.id(), option.label(), option.action(), option.next(),
                            availability.allowed(), availability.message(), option.target(), option.actionId());
                })
                .toList();
        String factionName = EchoNpcFactionManager.get(profile.faction())
                .map(faction -> faction.displayName())
                .orElse(profile.faction().toString());
        String relationship = NpcFactionBridge.relationshipLabel(player, profile.faction());
        return new EchoNpcScreenState(
                npc.getId(),
                profile.id().toString(),
                npc.hasCustomName() ? npc.getCustomName().getString() : profile.displayName(),
                profile.role(),
                factionName,
                relationship,
                idString(visual.portrait()),
                idString(visual.factionBadge()),
                idString(visual.screenFrame()),
                visual.theme().toString(),
                tab,
                effectiveNode,
                node.text(),
                options,
                tradeGroups(player, npc, profile),
                services(player, npc, profile),
                status);
    }

    private static List<EchoNpcScreenState.TradeGroupState> tradeGroups(ServerPlayer player, EchoNpcEntity npc, EchoNpcProfile profile) {
        EchoNpcTradeSet set = EchoNpcTradeManager.getOrEmpty(profile.trades());
        List<EchoNpcScreenState.TradeGroupState> groups = new ArrayList<>();
        for (EchoNpcTradeGroup group : set.groups()) {
            List<EchoNpcScreenState.TradeOfferState> offers = new ArrayList<>();
            for (EchoNpcTradeOffer offer : group.offers()) {
                ServerLevel level = serverLevel(player);
                int stock = currentStock(level, npc.getUUID(), offer);
                NpcMissionBridge.RequirementResult missionRequirement =
                        NpcMissionBridge.checkMissionRequirement(player, offer.requiresMission());
                NpcFactionBridge.RequirementResult factionRequirement =
                        NpcFactionBridge.checkStanding(player, profile.faction(), offer.requiresFactionStanding());
                long restockRemaining = restockRemaining(level, npc.getUUID(), offer);
                offers.add(new EchoNpcScreenState.TradeOfferState(
                        offer.id(),
                        offer.title(),
                        costs(offer.input()),
                        cost(offer.output()),
                        offer.stock() == 0 ? 0 : stock,
                        offer.stock() > 0,
                        offer.requiresMission(),
                        missionRequirement.allowed(),
                        missionRequirement.message(),
                        offer.requiresFactionStanding(),
                        factionRequirement.allowed(),
                        factionRequirement.message(),
                        offer.disabledReason(),
                        offer.restockTime(),
                        restockRemaining));
            }
            groups.add(new EchoNpcScreenState.TradeGroupState(group.id(), group.title(), offers));
        }
        return groups;
    }

    private static List<EchoNpcScreenState.ServiceState> services(ServerPlayer player, EchoNpcEntity npc, EchoNpcProfile profile) {
        EchoNpcServiceSet set = EchoNpcServiceManager.getOrEmpty(profile.services());
        long now = player.level().getGameTime();
        List<EchoNpcScreenState.ServiceState> states = new ArrayList<>();
        for (EchoNpcServiceDefinition service : set.services()) {
            long until = NpcDataBridge.serviceCooldownUntil(player, npc.getUUID(), service.id());
            states.add(new EchoNpcScreenState.ServiceState(
                    service.id(),
                    service.title(),
                    service.description(),
                    costs(service.cost()),
                    service.action(),
                    service.amount(),
                    service.cooldown(),
                    Math.max(0L, until - now),
                    service.requiresMission(),
                    NpcMissionBridge.checkMissionRequirement(player, service.requiresMission()).allowed(),
                    NpcMissionBridge.checkMissionRequirement(player, service.requiresMission()).message(),
                    service.requiresFactionStanding(),
                    NpcFactionBridge.checkStanding(player, profile.faction(), service.requiresFactionStanding()).allowed(),
                    service.disabledReason(),
                    service.target(),
                    service.actionId()));
        }
        return states;
    }

    private static List<EchoNpcScreenState.CostState> costs(List<EchoNpcTradeCost> costs) {
        return costs.stream().map(EchoNpcInteractionService::cost).toList();
    }

    private static int currentStock(ServerLevel level, UUID npcId, EchoNpcTradeOffer offer) {
        if (offer == null || offer.stock() <= 0) {
            return 0;
        }
        int stock = NpcDataBridge.stock(level, npcId, offer.id(), offer.stock());
        if (stock >= offer.stock()) {
            NpcDataBridge.setTradeRestockAt(level, npcId, offer.id(), 0L);
            return offer.stock();
        }
        if (offer.restockTime() <= 0) {
            return stock;
        }
        long now = level == null ? 0L : level.getGameTime();
        long restockAt = NpcDataBridge.tradeRestockAt(level, npcId, offer.id());
        if (restockAt > 0L && now >= restockAt) {
            NpcDataBridge.setStock(level, npcId, offer.id(), offer.stock());
            NpcDataBridge.setTradeRestockAt(level, npcId, offer.id(), 0L);
            return offer.stock();
        }
        if (restockAt <= 0L && now > 0L) {
            NpcDataBridge.setTradeRestockAt(level, npcId, offer.id(), now + offer.restockTime());
        }
        return stock;
    }

    private static long restockRemaining(ServerLevel level, UUID npcId, EchoNpcTradeOffer offer) {
        if (offer == null || offer.stock() <= 0 || offer.restockTime() <= 0) {
            return 0L;
        }
        long restockAt = NpcDataBridge.tradeRestockAt(level, npcId, offer.id());
        long now = level == null ? 0L : level.getGameTime();
        return restockAt <= now ? 0L : restockAt - now;
    }

    private static EchoNpcScreenState.CostState cost(EchoNpcTradeCost cost) {
        return cost == null ? new EchoNpcScreenState.CostState("", 0)
                : new EchoNpcScreenState.CostState(cost.item().toString(), cost.count());
    }

    private static void sync(ServerPlayer player, EchoNpcEntity npc, String tab, String nodeId, String status) {
        npc.beginInteraction(player);
        EchoNetSend.toPlayer(player, new SyncNpcScreenStatePacket(buildState(player, npc, tab, nodeId, status)));
        if (status != null && !status.isBlank()) {
            player.sendSystemMessage(Component.literal("[NPCore] " + status).withStyle(ChatFormatting.GRAY), true);
        }
    }

    private static String currentNode(ServerPlayer player, EchoNpcEntity npc, EchoNpcProfile profile) {
        String node = NpcDataBridge.dialogueNode(player.getUUID(), npc.getUUID());
        if (!node.isBlank()) {
            return node;
        }
        return EchoNpcDialogueRuntime.safeStart(EchoNpcDialogueManager.getOrFallback(profile.dialogue()));
    }

    private static void recordContact(ServerPlayer player, EchoNpcProfile profile) {
        if (player == null || profile == null) {
            return;
        }
        NpcFactionBridge.recordContact(player, profile.faction(), profile.role());
        if (profile.integrations().discoverOnInteract()) {
            NpcContactData.discover(player, profile, player.level().getGameTime());
        }
    }

    private static boolean valid(ServerPlayer player, EchoNpcEntity npc) {
        if (player == null || npc == null || npc.isRemoved() || !npc.isAlive()) {
            return false;
        }
        if (player.level() != npc.level()) {
            return false;
        }
        double range = npc.interactionRange();
        return player.distanceToSqr(npc) <= range * range;
    }

    private static EchoNpcEntity npc(ServerPlayer player, int entityId) {
        Entity entity = player.level().getEntity(entityId);
        return entity instanceof EchoNpcEntity npc ? npc : null;
    }

    private static String cleanTab(String tab) {
        return switch (tab == null ? "" : tab.trim().toLowerCase(Locale.ROOT)) {
            case "trade" -> "trade";
            case "services" -> "services";
            case "intel" -> "intel";
            default -> "talk";
        };
    }

    private static ServerLevel serverLevel(ServerPlayer player) {
        return player.level() instanceof ServerLevel level ? level : null;
    }

    private static String idString(Object id) {
        return id == null ? "" : id.toString();
    }
}
