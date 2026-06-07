package com.knoxhack.echoashfallprotocol.client.renderer;

import com.knoxhack.echoashfallprotocol.entity.EchoCompanionDrone;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;

/**
 * Renderer for the ECHO-7 Companion Drone. Extracts mood/speech state for particles.
 * The hologram text and nametag are handled via vanilla name tag rendering.
 */
public class EchoCompanionDroneRenderer extends AshfallBoardDroneRenderer<EchoCompanionDrone> {
    public EchoCompanionDroneRenderer(EntityRendererProvider.Context context) {
        super(context, "echo_companion_drone", 0.4F);
    }

    @Override
    public void extractRenderState(EchoCompanionDrone entity, DroneRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.mood = entity.getMoodId();
        state.speechText = entity.getSpeechText();
        state.speechTicks = entity.getSpeechTicks();
        state.alertFlash = entity.getAlertFlash();
        state.modeName = entity.getCurrentMode() != null ? entity.getCurrentMode().getDisplayName() : "";
        state.repairLevel = entity.getRepairLevel();
        state.owned = entity.getOwnerUUID() != null;

        // Spawn mood-colored ambient particles around the drone when alerting.
        if (state.alertFlash > 0 && entity.level().isClientSide() && entity.tickCount % 2 == 0) {
            ParticleOptions particle = particleForMood(state.mood);
            var random = entity.level().getRandom();
            double cx = entity.getX();
            double cy = entity.getY() + 0.5;
            double cz = entity.getZ();
            for (int i = 0; i < 2; i++) {
                double ox = (random.nextDouble() - 0.5) * 0.6;
                double oy = (random.nextDouble() - 0.3) * 0.5;
                double oz = (random.nextDouble() - 0.5) * 0.6;
                entity.level().addParticle(particle, cx + ox, cy + oy, cz + oz, 0, 0.02, 0);
            }
        }
        // Soft idle glow particle (occasional) based on mood
        if (state.owned && entity.tickCount % 20 == 0 && entity.level().isClientSide()) {
            ParticleOptions idle = (state.mood == EchoCompanionDrone.MOOD_URGENT)
                    ? ParticleTypes.SMOKE
                    : ParticleTypes.END_ROD;
            entity.level().addParticle(idle,
                    entity.getX(), entity.getY() + 0.35, entity.getZ(),
                    0, 0.01, 0);
        }
    }

    @Override
    protected boolean shouldShowName(EchoCompanionDrone entity, double distanceToCameraSq) {
        // Always show the ECHO-7 tag when bonded to an owner, within reasonable range
        if (entity.getOwnerUUID() != null && distanceToCameraSq < 64.0 * 64.0) return true;
        return super.shouldShowName(entity, distanceToCameraSq);
    }

    private static ParticleOptions particleForMood(int moodId) {
        return switch (moodId) {
            case EchoCompanionDrone.MOOD_CHEERFUL   -> ParticleTypes.HAPPY_VILLAGER;
            case EchoCompanionDrone.MOOD_URGENT     -> ParticleTypes.ELECTRIC_SPARK;
            case EchoCompanionDrone.MOOD_CONCERNED  -> ParticleTypes.SMOKE;
            case EchoCompanionDrone.MOOD_SARCASTIC  -> ParticleTypes.WITCH;
            case EchoCompanionDrone.MOOD_REFLECTIVE -> ParticleTypes.ENCHANT;
            default                                  -> ParticleTypes.END_ROD;
        };
    }
}
