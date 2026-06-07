package com.knoxhack.echoorbitalremnants.integration;

import com.knoxhack.echo.adaptercore.EchoBackendClientBridge;
import com.knoxhack.echocore.client.model.EchoMobFamily;
import com.knoxhack.echorendercore.client.EchoRenderCoreMobFamilyRenderer;
import com.knoxhack.echorendercore.client.RenderCoreStaticSurfaceRegistry;
import com.knoxhack.echoorbitalremnants.EchoOrbitalRemnants;
import com.knoxhack.echoorbitalremnants.client.RenderCoreEmergencyRocketRenderer;
import com.knoxhack.echoorbitalremnants.entity.EchoDefenseDroneEntity;
import com.knoxhack.echoorbitalremnants.entity.EmergencyRocketEntity;
import com.knoxhack.echoorbitalremnants.registry.ModEntities;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;

public final class OrbitalRenderCoreClientIntegration {
    private static boolean staticSurfacesRegistered;

    private OrbitalRenderCoreClientIntegration() {
    }

    public static void registerStaticSurfaces() {
        if (staticSurfacesRegistered) {
            return;
        }
        staticSurfacesRegistered = true;
        RenderCoreStaticSurfaceRegistry.register(
                Identifier.fromNamespaceAndPath(EchoOrbitalRemnants.MODID, "cryo_crystal_block"),
                Identifier.fromNamespaceAndPath(EchoOrbitalRemnants.MODID, "static/cryo_crystal_block"),
                "crystal_block"
        );
    }

    public static void registerEntityRenderers(Object event) {
        EntityRendererProvider<EmergencyRocketEntity> rocketRenderer = RenderCoreEmergencyRocketRenderer::new;
        EntityRendererProvider<EchoDefenseDroneEntity> droneRenderer = RenderCoreEchoDefenseDroneRenderer::new;
        EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.EMERGENCY_ROCKET_VEHICLE.get(), rocketRenderer);
        EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.ECHO_DEFENSE_DRONE.get(), droneRenderer);
        EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.VACUUM_WRAITH.get(), renderer("vacuum_wraith", EchoMobFamily.WRAITH, 1.15F, 0.25F));
        EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.CORRUPTED_DOCKING_AI.get(), renderer("corrupted_docking_ai", EchoMobFamily.DRONE, 1.35F, 0.44F));
        EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.BROKEN_ASTRONAUT.get(), renderer("broken_astronaut", EchoMobFamily.STATION_SUIT, 1.0F, 0.52F));
        EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.NEXUS_HUSK.get(), renderer("nexus_husk", EchoMobFamily.HUMANOID, 1.05F, 0.56F));
        EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.LUNAR_NEXUS_HUSK.get(), renderer("lunar_nexus_husk", EchoMobFamily.STATION_SUIT, 1.22F, 0.68F));
        EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.ABANDONED_CAPTAIN.get(), renderer("abandoned_captain", EchoMobFamily.STATION_SUIT, 1.18F, 0.72F));
        EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.ECHO_ZERO.get(), renderer("echo_zero", EchoMobFamily.HEAVY_BOSS, 1.35F, 0.9F));
        EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.EUROPA_CRYO_WARDEN.get(), renderer("europa_cryo_warden", EchoMobFamily.DRONE, 1.45F, 0.58F));
        EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.SATURN_RELAY_SENTINEL.get(), renderer("saturn_relay_sentinel", EchoMobFamily.DRONE, 1.55F, 0.6F));
        EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.TITAN_METHANE_STALKER.get(), renderer("titan_methane_stalker", EchoMobFamily.HUMANOID, 1.18F, 0.66F));
        EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.ORBITAL_FACTION_NPC.get(), renderer("orbital_faction_npc", EchoMobFamily.SURVIVOR_NPC, 1.0F, 0.5F));
    }

    private static <T extends Mob> EntityRendererProvider<T> renderer(String entityName, EchoMobFamily family,
            float scale, float shadow) {
        return context -> new EchoRenderCoreMobFamilyRenderer<>(context, EchoOrbitalRemnants.MODID, entityName, family, scale, shadow);
    }
}
