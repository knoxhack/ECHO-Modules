package com.knoxhack.echoashfallprotocol.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echoashfallprotocol.effect.AllianceEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;

import java.util.function.Supplier;

/**
 * Registry for custom mob effects.
 */
public class ModEffects {
    public static final Object EFFECTS = EchoBackendRegistryBridge.create(Registries.MOB_EFFECT, EchoAshfallProtocol.MODID);

    // === NEXUS PATH EFFECTS ===
    public static final Supplier<MobEffect> ALLIANCE = EchoBackendRegistryBridge.register(EFFECTS, "alliance",
            AllianceEffect::new);

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(EFFECTS, eventBus);
    }
}
