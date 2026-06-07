package com.knoxhack.echobasegrid.event;

import com.knoxhack.echobasegrid.EchoBaseGrid;
import com.knoxhack.echobasegrid.api.ClaimPermission;
import com.knoxhack.echobasegrid.api.ClaimRecord;
import com.knoxhack.echobasegrid.config.BaseGridConfig;
import com.knoxhack.echobasegrid.service.BaseGridClaimService;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import com.knoxhack.echo.adaptercore.EchoBackendWorldEventBridge;

public final class BaseGridEvents {
    private static final Map<UUID, Long> LAST_DENIAL_TICK = new HashMap<>();
    private static final long DENIAL_COOLDOWN_TICKS = 40L;

    private BaseGridEvents() {
    }

    public static void onBlockBreak(Object event) {
        if (!BaseGridConfig.ENABLED.get()) {
            return;
        }
        Level level = EchoBackendWorldEventBridge.blockEventLevel(event);
        BlockPos eventPos = EchoBackendWorldEventBridge.blockEventPos(event);
        if (level == null || eventPos == null) {
            return;
        }
        ServerPlayer player = EchoBackendWorldEventBridge.blockBreakServerPlayer(event);
        if (player != null) {
            if (!BaseGridClaimService.can(player, level, eventPos, ClaimPermission.BUILD)) {
                EchoBackendWorldEventBridge.cancel(event);
                deny(player, "Base Grid denied block breaking in a protected claim.");
            }
            return;
        }
        if (BaseGridClaimService.claimAt(level, eventPos).isPresent()) {
            EchoBackendWorldEventBridge.cancel(event);
        }
    }

    public static void onBlockPlace(Object event) {
        Level level = EchoBackendWorldEventBridge.blockEventLevel(event);
        BlockPos eventPos = EchoBackendWorldEventBridge.blockEventPos(event);
        if (!BaseGridConfig.ENABLED.get() || level == null || eventPos == null) {
            return;
        }
        Entity entity = EchoBackendWorldEventBridge.blockEventEntity(event);
        if (entity instanceof ServerPlayer player) {
            if (!BaseGridClaimService.can(player, level, eventPos, ClaimPermission.BUILD)) {
                EchoBackendWorldEventBridge.cancel(event);
                deny(player, "Base Grid denied block placement in a protected claim.");
            }
            return;
        }
        if (BaseGridClaimService.claimAt(level, eventPos).isPresent()) {
            EchoBackendWorldEventBridge.cancel(event);
        }
    }

    public static void onRightClickBlock(Object event) {
        if (!BaseGridConfig.ENABLED.get()
                || EchoBackendWorldEventBridge.entityInteractHand(event) != InteractionHand.MAIN_HAND
                || EchoBackendWorldEventBridge.rightClickBlockServerPlayer(event) == null) {
            return;
        }
        ServerPlayer player = EchoBackendWorldEventBridge.rightClickBlockServerPlayer(event);
        BlockPos pos = EchoBackendWorldEventBridge.rightClickBlockPos(event);
        if (player == null || pos == null) {
            return;
        }
        ClaimPermission permission = interactionPermission(player.level(), pos);
        if (BaseGridClaimService.can(player, player.level(), pos, permission)) {
            return;
        }
        EchoBackendWorldEventBridge.cancelRightClickBlock(event, InteractionResult.FAIL);
        deny(player, "Base Grid denied access to this protected claim.");
    }

    public static void onExplosionDetonate(Object event) {
        if (!BaseGridConfig.ENABLED.get() || !BaseGridConfig.PROTECT_EXPLOSIONS.get()) {
            return;
        }
        Level level = EchoBackendWorldEventBridge.explosionLevel(event);
        var affectedBlocks = EchoBackendWorldEventBridge.explosionAffectedBlocks(event);
        if (level == null || affectedBlocks.isEmpty()) {
            return;
        }
        affectedBlocks.removeIf(pos -> BaseGridClaimService.claimAt(level, pos).isPresent());
    }

    private static ClaimPermission interactionPermission(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity instanceof Container ? ClaimPermission.CONTAINERS : ClaimPermission.INTERACT;
    }

    private static void deny(ServerPlayer player, String message) {
        if (player == null) {
            return;
        }
        long gameTime = player.level().getGameTime();
        Long previous = LAST_DENIAL_TICK.get(player.getUUID());
        if (previous != null && gameTime - previous < DENIAL_COOLDOWN_TICKS) {
            return;
        }
        LAST_DENIAL_TICK.put(player.getUUID(), gameTime);
        player.sendSystemMessage(Component.literal("[ECHO-7] " + message), true);
    }
}
