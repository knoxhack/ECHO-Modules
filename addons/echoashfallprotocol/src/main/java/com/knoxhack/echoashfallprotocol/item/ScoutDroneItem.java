package com.knoxhack.echoashfallprotocol.item;

import com.knoxhack.echoashfallprotocol.entity.EchoCompanionDrone;
import com.knoxhack.echoashfallprotocol.entity.ScoutDrone;
import com.knoxhack.echoashfallprotocol.entity.drone.DroneCommandService;
import com.knoxhack.echoashfallprotocol.event.AshfallAdapterCoreExplorationRuntime;
import com.knoxhack.echoashfallprotocol.event.AshfallAdapterCoreLateRuntime;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResult;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/**
 * Scout Drone Item — deploys a player-owned ScoutDrone entity into the world.
 * Right-click air: deploy drone.
 * Shift+Right-click: cycle drone mode (if already deployed).
 */
public class ScoutDroneItem extends Item {

    public ScoutDroneItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            // If sneaking, try to find and toggle owner's existing drone
            if (player.isShiftKeyDown()) {
                ScoutDrone ownedDrone = findOwnedDrone(serverLevel, player);
                if (ownedDrone != null && player instanceof ServerPlayer serverPlayer) {
                    NativeResult result = AshfallAdapterCoreExplorationRuntime.scoutDroneModeCycle(
                            serverPlayer,
                            ownedDrone,
                            "scout_drone_item");
                    if (result.terminalFailure()) {
                        return InteractionResult.FAIL;
                    }
                    return result.mutated() ? InteractionResult.CONSUME : InteractionResult.SUCCESS;
                } else if (player instanceof ServerPlayer serverPlayer) {
                    DroneCommandService.execute(serverPlayer, "recall");
                    return InteractionResult.SUCCESS;
                } else {
                    player.sendSystemMessage(Component.literal(
                            "§c[ECHO-7 // DRONE]§r No deployed Scout Drone found. Deploy one first."));
                    return InteractionResult.FAIL;
                }
            }

            if (!(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.FAIL;
            }
            NativeResult result = AshfallAdapterCoreLateRuntime.scoutDroneItemUsed(serverPlayer, hand);
            if (result.terminalFailure()) {
                return InteractionResult.FAIL;
            }
            return result.mutated() ? InteractionResult.CONSUME : InteractionResult.SUCCESS;
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player,
            net.minecraft.world.entity.LivingEntity target, InteractionHand hand) {
        // Right-click on the drone itself to cycle its mode
        if (target instanceof ScoutDrone drone && player instanceof ServerPlayer serverPlayer) {
            if (drone.getOwnerUUID() != null && drone.getOwnerUUID().equals(player.getUUID())) {
                NativeResult result = AshfallAdapterCoreExplorationRuntime.scoutDroneModeCycle(
                        serverPlayer,
                        drone,
                        "scout_drone_entity_interact");
                if (result.terminalFailure()) {
                    return InteractionResult.FAIL;
                }
                return result.mutated() ? InteractionResult.CONSUME : InteractionResult.SUCCESS;
            }
        } else if (target instanceof EchoCompanionDrone drone && player instanceof ServerPlayer serverPlayer) {
            if (drone.getOwnerUUID() != null && drone.getOwnerUUID().equals(player.getUUID())) {
                DroneCommandService.execute(serverPlayer, player.isShiftKeyDown() ? "recall" : "status");
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    private ScoutDrone findOwnedDrone(ServerLevel level, Player player) {
        for (ScoutDrone drone : level.getEntitiesOfClass(ScoutDrone.class,
                player.getBoundingBox().inflate(64.0))) {
            if (drone.getOwnerUUID() != null && drone.getOwnerUUID().equals(player.getUUID())) {
                return drone;
            }
        }
        return null;
    }
}
