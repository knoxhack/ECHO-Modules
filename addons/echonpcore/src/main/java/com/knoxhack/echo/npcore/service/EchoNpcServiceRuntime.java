package com.knoxhack.echo.npcore.service;

import com.knoxhack.echo.npcore.data.NpcContactData;
import com.knoxhack.echo.npcore.entity.EchoNpcEntity;
import com.knoxhack.echo.npcore.profile.EchoNpcProfile;
import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.EchoFactionActionResult;
import com.knoxhack.echocore.api.WorldContextSnapshot;
import com.knoxhack.echocore.api.WorldMarker;
import com.knoxhack.echocore.api.WorldMarkerType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class EchoNpcServiceRuntime {
    private EchoNpcServiceRuntime() {
    }

    public static String apply(ServerPlayer player, EchoNpcEntity npc, EchoNpcProfile profile, EchoNpcServiceDefinition service) {
        return switch (service.action()) {
            case "heal" -> heal(player, service.amount());
            case "feed" -> feed(player, service.amount());
            case "intel_hint", "world_intel" -> intelHint(player, profile);
            case "repair_held_item" -> repairHeldItem(player, service.amount());
            case "discover_contact" -> discoverContact(player, profile);
            case "start_mission" -> startMission(player, service);
            case "mission_action" -> missionAction(player, service);
            case "faction_action" -> factionAction(player, profile, service);
            case "reveal_marker" -> revealMarker(player, npc, profile, service);
            case "map_refresh" -> EchoCoreServices.refreshMapMarkers(player, "npcore:" + service.id())
                    ? "Map markers refreshed." : "No map provider refreshed markers.";
            default -> "Service acknowledged. No first-pass action is wired for " + service.action() + ".";
        };
    }

    private static String heal(ServerPlayer player, int amount) {
        if (amount <= 0 || player.getHealth() >= player.getMaxHealth()) {
            return "No treatment needed.";
        }
        player.heal(amount);
        return "Field treatment applied.";
    }

    private static String feed(ServerPlayer player, int amount) {
        if (amount <= 0) {
            return "No ration effect configured.";
        }
        player.getFoodData().eat(amount, 0.35F);
        return "Field rations shared.";
    }

    private static String intelHint(ServerPlayer player, EchoNpcProfile profile) {
        WorldContextSnapshot context = EchoCoreServices.worldRegions().worldContext(player);
        String configured = profile == null ? "" : profile.integrations().intelSummary();
        String region = context.currentRegionOptional()
                .map(current -> " Current region: " + current.displayName() + ".")
                .orElse("");
        String hazard = context.hazard().summary().isBlank() ? "" : " Hazard: " + context.hazard().summary() + ".";
        String message = configured.isBlank()
                ? "Local intel: survey smoke columns, relay towers, and old roads." + region + hazard
                : configured + region + hazard;
        player.sendSystemMessage(Component.literal("[NPCore] " + message).withStyle(ChatFormatting.AQUA), true);
        EchoCoreServices.mirrorIntel(player, "echonpcore",
                profile == null ? "npc_intel" : profile.id().toString(), "NPC field intel", message);
        return "Local intel uploaded.";
    }

    private static String repairHeldItem(ServerPlayer player, int amount) {
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty() || !stack.isDamaged()) {
            return "Hold a damaged item for field repair.";
        }
        stack.setDamageValue(Math.max(0, stack.getDamageValue() - Math.max(1, amount)));
        return "Held item stabilized.";
    }

    private static String discoverContact(ServerPlayer player, EchoNpcProfile profile) {
        if (profile == null) {
            return "Contact unavailable.";
        }
        NpcContactData.discover(player, profile, player.level().getGameTime());
        NpcFactionBridge.recordContact(player, profile.faction(), profile.role());
        return "Contact recorded: " + profile.displayName() + ".";
    }

    private static String startMission(ServerPlayer player, EchoNpcServiceDefinition service) {
        Identifier missionId = target(service);
        if (missionId == null) {
            return "Mission target is not configured.";
        }
        return EchoCoreServices.startMission(player, missionId)
                ? "Mission started: " + missionId + "."
                : "Mission could not be started: " + missionId + ".";
    }

    private static String missionAction(ServerPlayer player, EchoNpcServiceDefinition service) {
        Identifier missionId = target(service);
        if (missionId == null) {
            return "Mission target is not configured.";
        }
        String actionId = service.actionId().isBlank() ? service.id() : service.actionId();
        return EchoCoreServices.handleMissionAction(player, missionId, actionId)
                ? "Mission action accepted: " + actionId + "."
                : "Mission action rejected: " + actionId + ".";
    }

    private static String factionAction(ServerPlayer player, EchoNpcProfile profile, EchoNpcServiceDefinition service) {
        Identifier actionId = target(service);
        if (profile == null || actionId == null) {
            return "Faction action is not configured.";
        }
        Identifier targetId = parse(service.actionId());
        EchoFactionActionResult result = NpcFactionBridge.perform(player, profile.faction(), profile.role(), actionId, targetId);
        return result.title() + ": " + result.message();
    }

    private static String revealMarker(ServerPlayer player, EchoNpcEntity npc, EchoNpcProfile profile, EchoNpcServiceDefinition service) {
        Identifier markerId = target(service);
        if (markerId == null && profile != null) {
            markerId = Identifier.fromNamespaceAndPath(profile.id().getNamespace(), "npc/" + profile.id().getPath());
        }
        if (markerId == null) {
            return "Marker target is not configured.";
        }
        BlockPos pos = npc == null ? player.blockPosition() : npc.homePos();
        String name = profile == null ? "NPC Contact" : profile.displayName();
        WorldMarker marker = new WorldMarker(markerId, null, WorldMarkerType.OUTPOST, name,
                "NPC contact marker revealed by NPCore.", player.level().dimension(), pos, 24, true,
                player.level().getGameTime());
        WorldMarker revealed = EchoCoreServices.worldRegions().revealMarker((Level) player.level(), marker);
        EchoCoreServices.refreshMapMarkers(player, "npcore_marker_revealed");
        return revealed == null ? "Marker provider unavailable." : "Marker revealed: " + name + ".";
    }

    private static Identifier target(EchoNpcServiceDefinition service) {
        Identifier explicit = parse(service.target());
        return explicit != null ? explicit : parse(service.actionId());
    }

    private static Identifier parse(String value) {
        return value == null || value.isBlank() ? null : Identifier.tryParse(value.trim());
    }
}
