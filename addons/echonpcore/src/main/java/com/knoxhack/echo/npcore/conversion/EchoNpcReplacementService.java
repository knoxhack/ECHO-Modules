package com.knoxhack.echo.npcore.conversion;

import com.knoxhack.echo.adaptercore.EchoBackendWorldEventBridge;
import com.knoxhack.echo.npcore.EchoNpcCore;
import com.knoxhack.echo.npcore.config.EchoNpcCoreConfig;
import com.knoxhack.echo.npcore.data.NpcConversionRecord;
import com.knoxhack.echo.npcore.data.NpcDataBridge;
import com.knoxhack.echo.npcore.entity.EchoNpcEntity;
import com.knoxhack.echo.npcore.registry.ModEntities;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.phys.AABB;

public final class EchoNpcReplacementService {
    private static final Identifier NONE = Identifier.fromNamespaceAndPath("minecraft", "none");
    private static final Identifier WANDERING_TRADER = Identifier.fromNamespaceAndPath("minecraft", "wandering_trader");

    private EchoNpcReplacementService() {
    }

    public static void onEntityJoinLevel(Object event) {
        if (!EchoNpcCoreConfig.bool(EchoNpcCoreConfig.REPLACE_ON_SPAWN, true)) {
            return;
        }
        String mode = EchoNpcCoreConfig.conversionMode();
        if ("off".equals(mode) || "convert_on_first_interact".equals(mode)) {
            return;
        }
        if (EchoBackendWorldEventBridge.entityJoinLevel(event) instanceof ServerLevel serverLevel) {
            convertCandidate(serverLevel, EchoBackendWorldEventBridge.entityJoinEntity(event), "spawn");
        }
    }

    public static void onEntityInteract(Object event) {
        if (!EchoNpcCoreConfig.bool(EchoNpcCoreConfig.REPLACE_ON_FIRST_INTERACT, true)) {
            return;
        }
        ServerPlayer player = EchoBackendWorldEventBridge.entityInteractServerPlayer(event);
        if (player == null || !(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Entity target = EchoBackendWorldEventBridge.entityInteractTarget(event);
        EchoNpcEntity converted = convertCandidate(serverLevel, target, "first_interact");
        if (converted != null) {
            EchoBackendWorldEventBridge.cancelEntityInteract(event, InteractionResult.SUCCESS);
            converted.mobInteract(player, EchoBackendWorldEventBridge.entityInteractHand(event));
        }
    }

    public static int convertNearbyVillagers(ServerPlayer player, int radius) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return 0;
        }
        AABB area = player.getBoundingBox().inflate(radius);
        List<Entity> candidates = serverLevel.getEntities(player, area, entity ->
                entity instanceof Villager || entity instanceof WanderingTrader ||
                        (entity instanceof ZombieVillager && EchoNpcCoreConfig.bool(EchoNpcCoreConfig.REPLACE_ZOMBIE_VILLAGERS, false)));
        int converted = 0;
        for (Entity candidate : candidates) {
            if (convertCandidate(serverLevel, candidate, "command") != null) {
                converted++;
            }
        }
        return converted;
    }

    public static EchoNpcEntity convertCandidate(ServerLevel level, Entity entity, String reason) {
        if (entity == null || entity instanceof EchoNpcEntity || entity.isRemoved()) {
            return null;
        }
        Replacement replacement = replacementFor(entity).orElse(null);
        if (replacement == null) {
            return null;
        }
        EchoNpcEntity npc = ModEntities.ECHO_NPC.get().create(level, EntitySpawnReason.EVENT);
        if (npc == null) {
            return null;
        }
        npc.setPos(entity.getX(), entity.getY(), entity.getZ());
        npc.setYRot(entity.getYRot());
        npc.setXRot(entity.getXRot());
        npc.setDeltaMovement(entity.getDeltaMovement());
        npc.configureProfile(replacement.profileId());
        npc.configureConvertedSource(replacement.sourceType().toString(), replacement.sourceProfession().toString());
        if (EchoNpcCoreConfig.bool(EchoNpcCoreConfig.PRESERVE_CUSTOM_NAME, true) && entity.hasCustomName()) {
            npc.setCustomName(entity.getCustomName());
            npc.setCustomNameVisible(entity.isCustomNameVisible());
        }
        if (!level.addFreshEntity(npc)) {
            return null;
        }
        NpcDataBridge.recordConversion(level, new NpcConversionRecord(entity.getUUID(), npc.getUUID(),
                replacement.sourceType().toString(), replacement.sourceProfession().toString(),
                replacement.profileId(), level.getGameTime()));
        entity.discard();
        if (EchoNpcCoreConfig.bool(EchoNpcCoreConfig.DEBUG_REPLACEMENT_LOGS, true)) {
            EchoNpcCore.LOGGER.info("NPCore converted {} ({}) to profile {} because {}.",
                    entity.getType().toShortString(), replacement.sourceProfession(), replacement.profileId(), reason);
        }
        return npc;
    }

    private static Optional<Replacement> replacementFor(Entity entity) {
        if (entity instanceof Villager villager) {
            if (!EchoNpcCoreConfig.bool(EchoNpcCoreConfig.REPLACE_VANILLA_VILLAGERS, true)) {
                return Optional.empty();
            }
            Identifier profession = professionId(villager);
            Optional<Identifier> profile = EchoNpcReplacementManager.profileForProfession(profession);
            if (profile.isEmpty() && !profession.equals(NONE)) {
                profile = EchoNpcReplacementManager.profileForProfession(NONE);
            }
            return profile.map(id -> new Replacement(entityTypeId(entity), profession, id));
        }
        if (entity instanceof WanderingTrader) {
            if (!EchoNpcCoreConfig.bool(EchoNpcCoreConfig.REPLACE_WANDERING_TRADER, true)) {
                return Optional.empty();
            }
            return EchoNpcReplacementManager.profileForEntityType(WANDERING_TRADER)
                    .map(id -> new Replacement(WANDERING_TRADER, NONE, id));
        }
        if (entity instanceof ZombieVillager && EchoNpcCoreConfig.bool(EchoNpcCoreConfig.REPLACE_ZOMBIE_VILLAGERS, false)) {
            return EchoNpcReplacementManager.profileForProfession(NONE)
                    .map(id -> new Replacement(entityTypeId(entity), NONE, id));
        }
        return Optional.empty();
    }

    private static Identifier professionId(Villager villager) {
        try {
            return villager.getVillagerData().profession().unwrapKey()
                    .map(ResourceKey::identifier)
                    .orElse(NONE);
        } catch (RuntimeException exception) {
            return NONE;
        }
    }

    private static Identifier entityTypeId(Entity entity) {
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return id == null ? Identifier.fromNamespaceAndPath("minecraft", "unknown") : id;
    }

    private record Replacement(Identifier sourceType, Identifier sourceProfession, Identifier profileId) {
    }
}
