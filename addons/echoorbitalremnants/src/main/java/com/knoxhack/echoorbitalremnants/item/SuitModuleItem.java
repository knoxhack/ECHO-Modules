package com.knoxhack.echoorbitalremnants.item;

import com.knoxhack.echoorbitalremnants.progression.EchoTerminalProgress;
import com.knoxhack.echoorbitalremnants.suit.SuitEvents;
import com.knoxhack.echoorbitalremnants.suit.SuitState;
import com.knoxhack.echoorbitalremnants.world.ModDimensions;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class SuitModuleItem extends Item {
    private final Module module;

    public SuitModuleItem(Module module, Properties properties) {
        super(properties);
        this.module = module;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            SuitState state = SuitState.get(player);
            EchoTerminalProgress progress = EchoTerminalProgress.get(player);
            switch (module) {
                case OXYGEN_BOOSTER -> {
                    state.boostOxygen(25);
                    player.sendSystemMessage(Component.literal("ECHO-7 // Oxygen booster cycled. Reserve air flushed into suit loop."));
                }
                case RADIATION_VISOR -> {
                    state.reduceRadiation(20);
                    player.sendSystemMessage(Component.literal("ECHO-7 // Radiation visor recalibrated. Cosmic dose map narrowed."));
                }
                case THERMAL_REGULATOR -> {
                    state.applyThermalRecovery();
                    player.sendSystemMessage(Component.literal("ECHO-7 // Thermal regulator stabilized suit membrane temperature."));
                }
                case JET_BURST -> {
                    if (SuitEvents.isOrbitalExposure(player)) {
                        Vec3 impulse = player.getLookAngle().normalize().scale(1.15D).add(0.0D, 0.16D, 0.0D);
                        player.addDeltaMovement(impulse);
                        state.drainOxygen(6);
                        player.getCooldowns().addCooldown(player.getItemInHand(hand), 60);
                        player.sendSystemMessage(Component.literal("ECHO-7 // Jet burst fired. Oxygen reserve dipped."));
                    } else {
                        player.sendSystemMessage(Component.literal("ECHO-7 // Jet burst locked out outside low-pressure movement."));
                    }
                }
                case SCANNER -> {
                    player.sendSystemMessage(Component.literal("ECHO-7 // SCANNER // " + level.dimension().identifier()));
                    player.sendSystemMessage(Component.literal("Low Orbit: " + (progress.lowOrbitReached() ? "MAPPED" : "UNKNOWN")
                            + " | Lunar: " + (progress.lunarSignalUnlocked() ? "RESOLVED" : "WEAK")
                            + " | Mars: " + (progress.marsRouteUnlocked() ? "OPEN" : "LOCKED")
                            + " | Europa: " + (progress.europaRouteUnlocked() ? "OPEN" : "LOCKED")
                            + " | Deep Space: " + (progress.deepSpaceProtocolUnlocked() ? "OPEN" : "SEALED")));
                    if (ModDimensions.isSpaceLevel(level)) {
                        player.sendSystemMessage(Component.literal("Suit loop: O2 " + state.oxygen() + "% | Pressure " + state.pressure()
                                + "% | Radiation " + state.radiation() + "%"));
                    }
                }
            }
            state.save(player);
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    public enum Module {
        OXYGEN_BOOSTER,
        RADIATION_VISOR,
        THERMAL_REGULATOR,
        JET_BURST,
        SCANNER
    }
}
