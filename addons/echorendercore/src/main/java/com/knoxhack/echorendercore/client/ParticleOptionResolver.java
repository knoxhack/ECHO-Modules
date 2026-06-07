package com.knoxhack.echorendercore.client;

import com.knoxhack.echorendercore.profile.ParticleEmitter;
import net.minecraft.core.particles.ParticleOptions;

@FunctionalInterface
public interface ParticleOptionResolver {
   ParticleOptions resolve(ParticleEmitter emitter);
}
