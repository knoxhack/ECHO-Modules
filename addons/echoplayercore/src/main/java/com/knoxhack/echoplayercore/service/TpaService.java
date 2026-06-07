package com.knoxhack.echoplayercore.service;

import com.knoxhack.echoplayercore.config.PlayerCoreConfig;
import com.knoxhack.echoplayercore.data.TpaRequest;
import com.knoxhack.echoplayercore.event.PlayerTpaTeleportEvent;
import com.knoxhack.echoplayercore.teleport.TeleportAction;
import com.knoxhack.echoplayercore.data.TeleportLocation;
import com.knoxhack.echoplayercore.teleport.TeleportReason;
import com.knoxhack.echoplayercore.teleport.TeleportService;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class TpaService {
    private static final Map<UUID, TpaRequest> PENDING = new ConcurrentHashMap<>();

    private TpaService() {
    }

    public static boolean request(ServerPlayer requester, ServerPlayer target, boolean here) {
        if (requester == null || target == null) {
            return false;
        }
        if (requester.getUUID().equals(target.getUUID())) {
            requester.sendSystemMessage(Component.translatable("echoplayercore.message.tpa_self")
                    .withStyle(ChatFormatting.RED));
            return false;
        }
        long timeout = PlayerCoreConfig.tpaTimeoutSeconds() * 1000L;
        TpaRequest existing = PENDING.get(target.getUUID());
        if (existing != null && !existing.isExpired(timeout) && existing.requesterId().equals(requester.getUUID())) {
            requester.sendSystemMessage(Component.translatable("echoplayercore.message.tpa_already_sent")
                    .withStyle(ChatFormatting.YELLOW));
            return false;
        }
        TpaRequest req = new TpaRequest(requester.getUUID(), target.getUUID(), System.currentTimeMillis(), here);
        PENDING.put(target.getUUID(), req);
        String key = here ? "echoplayercore.message.tpa_here_request" : "echoplayercore.message.tpa_request";
        requester.sendSystemMessage(Component.translatable("echoplayercore.message.tpa_sent", target.getScoreboardName())
                .withStyle(ChatFormatting.GREEN));
        target.sendSystemMessage(Component.translatable(key, requester.getScoreboardName())
                .withStyle(ChatFormatting.AQUA));
        return true;
    }

    public static boolean accept(ServerPlayer target) {
        if (target == null) {
            return false;
        }
        long timeout = PlayerCoreConfig.tpaTimeoutSeconds() * 1000L;
        TpaRequest req = PENDING.remove(target.getUUID());
        if (req == null || req.isExpired(timeout)) {
            target.sendSystemMessage(Component.translatable("echoplayercore.message.tpa_no_request")
                    .withStyle(ChatFormatting.RED));
            return false;
        }
        ServerPlayer requester = target.level().getServer().getPlayerList().getPlayer(req.requesterId());
        if (requester == null) {
            target.sendSystemMessage(Component.translatable("echoplayercore.message.tpa_requester_offline")
                    .withStyle(ChatFormatting.RED));
            return false;
        }

        ServerPlayer toTeleport = req.here() ? target : requester;
        ServerPlayer destination = req.here() ? requester : target;

        var tpaEvent = new PlayerTpaTeleportEvent(toTeleport, destination, req.here());
        if (tpaEvent.isCanceled()) {
            return false;
        }

        TeleportLocation loc = TeleportLocation.fromPlayer(destination, "tpa");
        if (TeleportService.teleportTo(toTeleport, loc, TeleportReason.TPA)) {
            CooldownService.applyCooldown(requester, req.here() ? TeleportAction.TPA_HERE : TeleportAction.TPA);
            requester.sendSystemMessage(Component.translatable("echoplayercore.message.tpa_accepted")
                    .withStyle(ChatFormatting.GREEN));
            target.sendSystemMessage(Component.translatable("echoplayercore.message.tpa_accepted")
                    .withStyle(ChatFormatting.GREEN));
            return true;
        }
        target.sendSystemMessage(Component.translatable("echoplayercore.message.teleport_failed")
                .withStyle(ChatFormatting.RED));
        return false;
    }

    public static boolean deny(ServerPlayer target) {
        if (target == null) {
            return false;
        }
        long timeout = PlayerCoreConfig.tpaTimeoutSeconds() * 1000L;
        TpaRequest req = PENDING.remove(target.getUUID());
        if (req == null || req.isExpired(timeout)) {
            target.sendSystemMessage(Component.translatable("echoplayercore.message.tpa_no_request")
                    .withStyle(ChatFormatting.RED));
            return false;
        }
        ServerPlayer requester = target.level().getServer().getPlayerList().getPlayer(req.requesterId());
        target.sendSystemMessage(Component.translatable("echoplayercore.message.tpa_denied")
                .withStyle(ChatFormatting.RED));
        if (requester != null) {
            requester.sendSystemMessage(Component.translatable("echoplayercore.message.tpa_denied_target",
                    target.getScoreboardName()).withStyle(ChatFormatting.RED));
        }
        return true;
    }

    public static Optional<TpaRequest> getPending(UUID targetId) {
        long timeout = PlayerCoreConfig.tpaTimeoutSeconds() * 1000L;
        TpaRequest req = PENDING.get(targetId);
        if (req == null || req.isExpired(timeout)) {
            PENDING.remove(targetId);
            return Optional.empty();
        }
        return Optional.of(req);
    }
}
