package com.knoxhack.echoashfallprotocol.event;

import com.knoxhack.echoashfallprotocol.endgame.NexusCampaignActions;
import com.knoxhack.echoashfallprotocol.world.NexusCampaignData;
import com.knoxhack.echoashfallprotocol.world.NexusWorldData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Monster;

/**
 * Applies ongoing world effects based on Nexus Core choice.
 * Runs periodically to maintain the chosen world's state.
 */
public class NexusWorldEffects {

    private static final int EFFECT_INTERVAL = 6000; // Every 5 minutes (6000 ticks)

    public static void onWorldTick(Object event) {
        if (!(eventValue(event, "getLevel") instanceof ServerLevel level)) return;
        if (level.dimension() != Level.OVERWORLD) return;

        NexusWorldData data = NexusWorldData.get(level);
        NexusCampaignData campaign = NexusCampaignData.get(level);
        if (level.getGameTime() % 1200L == 0L && campaign.tickInstability(level, data.hasChoiceBeenMade())) {
            NexusCampaignActions.syncCampaignState(level);
        }
        if (data.getState() == NexusWorldData.WorldState.NORMAL && campaign.isAwakened()
                && level.getGameTime() % EFFECT_INTERVAL == 0L) {
            applyInstabilityPressure(level, campaign);
        }
        if (level.getGameTime() % EFFECT_INTERVAL != 0) return;
        if (data.getState() == NexusWorldData.WorldState.NORMAL) return;

        applyPeriodicEffects(level, data.getState());
    }

    private static void applyInstabilityPressure(ServerLevel level, NexusCampaignData campaign) {
        for (ServerPlayer player : level.players()) {
            if (campaign.getInstability() >= 70) {
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 300, 0, false, false));
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§5[NEXUS INSTABILITY]§r Core pressure is interfering with scanner and combat telemetry."), true);
            } else if (campaign.getInstability() >= 40) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§d[NEXUS INSTABILITY]§r Relay activity rising. Resolve relays or prepare for surge pressure."), true);
            }
        }
    }

    private static void applyPeriodicEffects(ServerLevel level, NexusWorldData.WorldState state) {
        for (ServerPlayer player : level.players()) {
            switch (state) {
                case RESTORED -> {
                    // Restored world: players get gentle regeneration
                    if (player.getHealth() < player.getMaxHealth()) {
                        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0, false, false));
                    }
                    // Resistance to damage in restored zones
                    player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 600, 0, false, false));
                    player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 600, 0, false, false));
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§a[NEXUS RESTORE]§r Grid harmonics stabilizing local survival pressure."), true);
                }
                case DESTROYED -> {
                    // Destroyed world: constant environmental stress
                    player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 600, 0, false, false));
                    // But also strength from surviving chaos
                    if (level.getRandom().nextFloat() < 0.3f) {
                        player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 400, 0, false, false));
                    }
                    long timeOfDay = level.getOverworldClockTime() % 24000L;
                    if (timeOfDay >= 13000L || level.isThundering()) {
                        player.addEffect(new MobEffectInstance(MobEffects.SPEED, 300, 0, false, false));
                    }
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§c[NEXUS DESTROY]§r Free wasteland conditions rising. Move with supplies."), true);
                }
                case CONTROLLED -> {
                    // Controlled world: night vision and efficiency
                    player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1200, 0, false, false));
                    player.addEffect(new MobEffectInstance(MobEffects.HASTE, 600, 0, false, false));
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§5[NEXUS CONTROL]§r Command signal boosting vision, work speed, and machine control."), true);
                    
                    // Controlled world suppresses nearby monsters periodically
                    if (level.getRandom().nextFloat() < 0.2f) {
                        suppressNearbyMonsters(level, player);
                    }
                }
                default -> {}
            }
        }
    }

    private static void suppressNearbyMonsters(ServerLevel level, ServerPlayer player) {
        var monsters = level.getEntitiesOfClass(Monster.class, 
                player.getBoundingBox().inflate(32.0));
        
        for (Monster monster : monsters) {
            // Weakness effect from Nexus control field
            monster.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 300, 1, false, false));
            monster.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 300, 0, false, false));
        }
        
        if (!monsters.isEmpty() && level.getRandom().nextFloat() < 0.1f) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§5[NEXUS FIELD]§r Hostile entities suppressed in your domain."));
        }
    }

    private static Object eventValue(Object event, String methodName) {
        if (event == null) {
            return null;
        }
        try {
            return event.getClass().getMethod(methodName).invoke(event);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return null;
        }
    }
}
