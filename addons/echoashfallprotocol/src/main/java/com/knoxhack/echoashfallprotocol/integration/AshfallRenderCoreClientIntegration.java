package com.knoxhack.echoashfallprotocol.integration;

import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echoashfallprotocol.entity.ModEntities;
import com.knoxhack.echocore.client.model.EchoMobFamily;
import com.knoxhack.echorendercore.client.EchoRenderCoreMobFamilyRenderer;
import com.knoxhack.echorendercore.client.RenderCoreStaticSurfaceRegistry;
import java.lang.reflect.Method;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;

public final class AshfallRenderCoreClientIntegration {
    private static boolean staticSurfacesRegistered;

    private AshfallRenderCoreClientIntegration() {
    }

    public static void registerStaticSurfaces() {
        if (staticSurfacesRegistered) {
            return;
        }
        staticSurfacesRegistered = true;
        registerStaticSurface("echo_crystal");
        registerStaticSurface("ooze_crystal");
        registerStaticSurface("uranium_crystal");
        registerStaticSurface("blue_ice_crystal");
    }

    public static void registerEntityRenderers(Object event) {
        Method register = rendererRegistrationMethod(event);
        if (register == null) {
            return;
        }
        registerEntityRenderer(event, register, ModEntities.RAD_ZOMBIE.get(), renderer("rad_zombie", EchoMobFamily.HUMANOID, 1.0F, 0.5F));
        registerEntityRenderer(event, register, ModEntities.SCAVENGER_BANDIT.get(), renderer("scavenger_bandit", EchoMobFamily.HUMANOID, 1.0F, 0.5F));
        registerEntityRenderer(event, register, ModEntities.IRRADIATED_WOLF.get(), renderer("irradiated_wolf", EchoMobFamily.QUADRUPED, 1.0F, 0.5F));
        registerEntityRenderer(event, register, ModEntities.ECHO_DRONE.get(), renderer("echo_drone", EchoMobFamily.DRONE, 1.0F, 0.4F));
        registerEntityRenderer(event, register, ModEntities.SCOUT_DRONE.get(), renderer("scout_drone", EchoMobFamily.DRONE, 1.0F, 0.4F));
        registerEntityRenderer(event, register, ModEntities.GLOWING_GHOUL.get(), renderer("glowing_ghoul", EchoMobFamily.HUMANOID, 1.0F, 0.5F));
        registerEntityRenderer(event, register, ModEntities.ASH_WRAITH.get(), renderer("ash_wraith", EchoMobFamily.WRAITH, 1.0F, 0.5F));
        registerEntityRenderer(event, register, ModEntities.TOXIC_SLIME.get(), renderer("toxic_slime", EchoMobFamily.SLIME, 1.0F, 0.35F));
        registerEntityRenderer(event, register, ModEntities.GRIDBOUND_HUSK.get(), renderer("gridbound_husk", EchoMobFamily.HUMANOID, 1.0F, 0.55F));
        registerEntityRenderer(event, register, ModEntities.RELAY_WARDEN.get(), renderer("relay_warden", EchoMobFamily.HEAVY_BOSS, 1.0F, 0.85F));
        registerEntityRenderer(event, register, ModEntities.SIGNAL_LEECH.get(), renderer("signal_leech", EchoMobFamily.CRAWLER, 1.0F, 0.35F));
        registerEntityRenderer(event, register, ModEntities.NEXUS_NULLIFIER.get(), renderer("nexus_nullifier", EchoMobFamily.HUMANOID, 1.0F, 0.55F));
        registerEntityRenderer(event, register, ModEntities.CITY_STALKER.get(), renderer("city_stalker", EchoMobFamily.HUMANOID, 1.0F, 0.5F));
        registerEntityRenderer(event, register, ModEntities.RUST_WALKER.get(), renderer("rust_walker", EchoMobFamily.HEAVY_BOSS, 1.0F, 0.7F));
        registerEntityRenderer(event, register, ModEntities.STEAM_WRAITH.get(), renderer("steam_wraith", EchoMobFamily.WRAITH, 1.0F, 0.4F));
        registerEntityRenderer(event, register, ModEntities.MUTATED_CRAWLER.get(), renderer("mutated_crawler", EchoMobFamily.CRAWLER, 1.0F, 0.3F));
        registerEntityRenderer(event, register, ModEntities.ECHO_COMPANION_DRONE.get(), renderer("echo_companion_drone", EchoMobFamily.DRONE, 1.0F, 0.4F));
        registerEntityRenderer(event, register, ModEntities.WILD_DOG.get(), renderer("wild_dog", EchoMobFamily.QUADRUPED, 1.0F, 0.45F));
        registerEntityRenderer(event, register, ModEntities.FERAL_HUMAN.get(), renderer("feral_human", EchoMobFamily.HUMANOID, 1.0F, 0.5F));
        registerEntityRenderer(event, register, ModEntities.CRASH_SURVIVOR.get(), renderer("crash_survivor", EchoMobFamily.SURVIVOR_NPC, 1.0F, 0.5F));
        registerEntityRenderer(event, register, ModEntities.FACTION_NPC.get(), renderer("faction_npc", EchoMobFamily.SURVIVOR_NPC, 1.0F, 0.5F));
        registerEntityRenderer(event, register, ModEntities.WARDEN_BOSS.get(), renderer("warden_boss", EchoMobFamily.HEAVY_BOSS, 1.0F, 1.0F));
        registerEntityRenderer(event, register, ModEntities.WASTELAND_SENTINEL.get(), renderer("wasteland_sentinel", EchoMobFamily.HEAVY_BOSS, 1.0F, 0.9F));
        registerEntityRenderer(event, register, ModEntities.CRASH_ZONE_COLOSSUS.get(), renderer("crash_zone_colossus", EchoMobFamily.HEAVY_BOSS, 1.24F, 1.12F));
        registerEntityRenderer(event, register, ModEntities.CRYOGENIC_OVERSEER.get(), renderer("cryogenic_overseer", EchoMobFamily.HEAVY_BOSS, 1.04F, 0.9F));
        registerEntityRenderer(event, register, ModEntities.INDUSTRIAL_JUGGERNAUT.get(), renderer("industrial_juggernaut", EchoMobFamily.HEAVY_BOSS, 1.16F, 1.04F));
        registerEntityRenderer(event, register, ModEntities.NEXUS_SCAR_AVATAR.get(), renderer("nexus_scar_avatar", EchoMobFamily.HEAVY_BOSS, 1.18F, 1.08F));
        registerEntityRenderer(event, register, ModEntities.RADIATION_BEHEMOTH.get(), renderer("radiation_behemoth", EchoMobFamily.HEAVY_BOSS, 1.12F, 1.0F));
        registerEntityRenderer(event, register, ModEntities.CITY_RUIN_STALKER.get(), renderer("city_ruin_stalker", EchoMobFamily.HEAVY_BOSS, 0.92F, 0.68F));
        registerEntityRenderer(event, register, ModEntities.PLAINS_WARLORD.get(), renderer("plains_warlord", EchoMobFamily.HEAVY_BOSS, 1.02F, 0.88F));
        registerEntityRenderer(event, register, ModEntities.TOXIC_HIVE_MATRIARCH.get(), renderer("toxic_hive_matriarch", EchoMobFamily.HEAVY_BOSS, 1.05F, 0.92F));
        registerEntityRenderer(event, register, ModEntities.CORRUPTION_BLOOM.get(), renderer("corruption_bloom", EchoMobFamily.HEAVY_BOSS, 1.04F, 0.86F));
        registerEntityRenderer(event, register, ModEntities.SEVERANCE_ENGINE.get(), renderer("severance_engine", EchoMobFamily.HEAVY_BOSS, 1.14F, 0.86F));
        registerEntityRenderer(event, register, ModEntities.MIRROR_COMMAND.get(), renderer("mirror_command", EchoMobFamily.HEAVY_BOSS, 1.08F, 0.86F));
    }

    private static <T extends Mob> EntityRendererProvider<T> renderer(String entityName, EchoMobFamily family,
            float scale, float shadow) {
        return context -> new EchoRenderCoreMobFamilyRenderer<>(context, EchoAshfallProtocol.MODID, entityName, family, scale, shadow);
    }

    private static Method rendererRegistrationMethod(Object event) {
        if (event == null) {
            return null;
        }
        try {
            return event.getClass().getMethod("registerEntityRenderer", EntityType.class, EntityRendererProvider.class);
        } catch (ReflectiveOperationException e) {
            EchoAshfallProtocol.LOGGER.debug("[RenderCore] Renderer registration event {} does not expose registerEntityRenderer.",
                    event.getClass().getName());
            return null;
        }
    }

    private static <T extends Mob> void registerEntityRenderer(Object event, Method register,
            EntityType<? extends T> entityType, EntityRendererProvider<T> provider) {
        try {
            register.invoke(event, entityType, provider);
        } catch (ReflectiveOperationException e) {
            EchoAshfallProtocol.LOGGER.error("[RenderCore] Failed to register renderer for {}", entityType, e);
        }
    }

    private static void registerStaticSurface(String blockId) {
        RenderCoreStaticSurfaceRegistry.register(
                Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, blockId),
                Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "static/" + blockId),
                "crystal_block"
        );
    }
}
