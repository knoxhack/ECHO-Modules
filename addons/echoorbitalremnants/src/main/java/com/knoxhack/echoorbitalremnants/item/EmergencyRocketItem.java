package com.knoxhack.echoorbitalremnants.item;

import com.knoxhack.echoorbitalremnants.entity.EmergencyRocketEntity;
import com.knoxhack.echoorbitalremnants.progression.EchoTerminalProgress;
import com.knoxhack.echoorbitalremnants.progression.LaunchReadiness;
import com.knoxhack.echoorbitalremnants.progression.LaunchPadLocator;
import com.knoxhack.echoorbitalremnants.registry.ModEntities;
import com.knoxhack.echoorbitalremnants.suit.SuitEvents;
import com.knoxhack.echoorbitalremnants.world.ModDimensions;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class EmergencyRocketItem extends Item {
    public EmergencyRocketItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            EchoTerminalProgress progress = EchoTerminalProgress.get(player);
            if (SuitEvents.isOrbitalExposure(player) && progress.hasEarthReturnPoint() && player instanceof ServerPlayer serverPlayer) {
                ServerLevel returnLevel = serverPlayer.level().getServer().getLevel(ModDimensions.keyFromString(progress.earthReturnDimension()));
                if (returnLevel == null) {
                    returnLevel = serverPlayer.level().getServer().overworld();
                }
                serverPlayer.teleportTo(returnLevel, progress.earthReturnX(), progress.earthReturnY(), progress.earthReturnZ(), Set.of(), player.getYRot(), player.getXRot(), false);
                sendFeedback(player, "Emergency re-entry vector accepted.");
                return InteractionResult.SUCCESS_SERVER;
            }
            if (SuitEvents.isOrbitalExposure(player) && !progress.hasEarthReturnPoint()) {
                sendFeedback(player, "Re-entry denied. Launch from Earth once to save an Earth return vector.");
                return InteractionResult.CONSUME;
            }

            boolean bypassesReadiness = LaunchReadiness.bypassesReadiness(player);
            if (!bypassesReadiness) {
                LaunchReadiness readiness = LaunchReadiness.evaluateForLaunch(player);
                if (!readiness.ready()) {
                    sendFeedback(player, "Launch hold. Complete " + readiness.missing().size() + " checks before staging the rocket.");
                    readiness.missing().stream().limit(8).forEach(player::sendSystemMessage);
                    return InteractionResult.CONSUME;
                }
            }

            if (!(level instanceof ServerLevel serverLevel)) {
                return InteractionResult.CONSUME;
            }

            Optional<BlockPos> padCenter = LaunchPadLocator.findNearbyPlatformCenter(player);
            EmergencyRocketEntity rocket = ModEntities.EMERGENCY_ROCKET_VEHICLE.get().create(serverLevel, EntitySpawnReason.EVENT);
            if (rocket == null) {
                sendFeedback(player, "Rocket placement failed. Vehicle registry offline.");
                return InteractionResult.CONSUME;
            }

            double rocketX = player.getX();
            double rocketY = player.getY();
            double rocketZ = player.getZ();
            if (padCenter.isPresent()) {
                BlockPos base = padCenter.get();
                rocketX = base.getX() + 0.5D;
                rocketY = base.getY() + 2.0D;
                rocketZ = base.getZ() + 0.5D;
            }
            rocket.setLaunchPadPosition(rocketX, rocketY, rocketZ, player.getYRot());
            if (!serverLevel.noBlockCollision(rocket, rocket.getBoundingBox()) || !serverLevel.noBorderCollision(rocket, rocket.getBoundingBox())) {
                sendFeedback(player, "Rocket placement blocked. Clear blocks or entities from the rocket volume above the pad.");
                return InteractionResult.CONSUME;
            }
            if (!serverLevel.getEntities((Entity) null, rocket.getBoundingBox().inflate(2.0D, 1.0D, 2.0D),
                    entity -> entity.getType() == ModEntities.EMERGENCY_ROCKET_VEHICLE.get()).isEmpty()) {
                sendFeedback(player, "Rocket placement blocked. Break or launch the existing staged vehicle first.");
                return InteractionResult.CONSUME;
            }

            if (!serverLevel.addFreshEntity(rocket)) {
                sendFeedback(player, "Rocket placement failed. Move to a loaded, clear staging area and try again.");
                return InteractionResult.CONSUME;
            }
            rocket.playStagedFeedback();
            ItemStack stack = player.getItemInHand(hand);
            if (!bypassesReadiness) {
                stack.shrink(1);
            }
            sendFeedback(player, "Emergency Rocket staged on pad center. Board the cabin and start countdown.");
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    private static void sendFeedback(Player player, String message) {
        Component component = Component.literal("ECHO-7 // " + message);
        player.sendSystemMessage(component);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(component, true);
        }
    }
}
