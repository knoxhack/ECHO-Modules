package com.knoxhack.echoashfallprotocol.integration;

import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echoashfallprotocol.faction.AshfallBiomeFactions;
import com.knoxhack.echoashfallprotocol.faction.AshfallFactionMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;

/**
 * Optional bridge into NPCore. Reflection keeps Ashfall compile-independent
 * while still invoking the real NPCore runtime when the addon is loaded.
 */
public final class AshfallNpcoreBridge {
    private static final String MODID = EchoAshfallProtocol.MODID;

    private AshfallNpcoreBridge() {
    }

    public static boolean trySpawnContact(ServerPlayer player, Identifier factionId, String roleId, BlockPos pos) {
        if (player == null || !(player.level() instanceof ServerLevel level) || pos == null) {
            return false;
        }
        Identifier profileId = profileFor(factionId, roleId);
        if (profileId == null) {
            return false;
        }
        Entity npc = createNpcoreEntity(level);
        if (npc == null || !invokeNpcMethod(npc, "configureProfile", new Class<?>[]{Identifier.class}, profileId)
                || !invokeNpcMethod(npc, "setHome", new Class<?>[]{BlockPos.class}, pos)) {
            return false;
        }
        npc.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        npc.setYRot(level.getRandom().nextFloat() * 360.0F);
        npc.setXRot(0.0F);
        if (npc instanceof Mob mob) {
            mob.setPersistenceRequired();
        }
        return level.addFreshEntity(npc);
    }

    public static int countNearby(ServerLevel level, ServerPlayer player, Identifier factionId, int radius) {
        if (level == null || player == null) {
            return 0;
        }
        Class<? extends Entity> npcClass = npcClass();
        if (npcClass == null) {
            return 0;
        }
        Identifier canonical = AshfallFactionMap.canonicalFaction(factionId);
        return level.getEntitiesOfClass(npcClass, player.getBoundingBox().inflate(radius),
                npc -> npc.isAlive() && isAshfallContact(npc, canonical)).size();
    }

    private static boolean isAshfallContact(Entity npc, Identifier factionId) {
        Identifier profileId = invokeNpcIdentifier(npc, "npcProfileId");
        Identifier contactFactionId = invokeNpcIdentifier(npc, "factionId");
        if (profileId == null || !MODID.equals(profileId.getNamespace())) {
            return false;
        }
        return factionId == null || factionId.equals(AshfallFactionMap.canonicalFaction(contactFactionId));
    }

    private static Entity createNpcoreEntity(ServerLevel level) {
        try {
            Class<?> registryClass = Class.forName("com.knoxhack.echo.npcore.registry.ModEntities");
            Object holder = registryClass.getField("ECHO_NPC").get(null);
            Object type = holder.getClass().getMethod("get").invoke(holder);
            if (!(type instanceof EntityType<?> entityType)) {
                return null;
            }
            return entityType.create(level, EntitySpawnReason.EVENT);
        } catch (ReflectiveOperationException | LinkageError exception) {
            return null;
        }
    }

    private static Class<? extends Entity> npcClass() {
        try {
            return Class.forName("com.knoxhack.echo.npcore.entity.EchoNpcEntity").asSubclass(Entity.class);
        } catch (ReflectiveOperationException | LinkageError exception) {
            return null;
        }
    }

    private static boolean invokeNpcMethod(Entity npc, String methodName, Class<?>[] parameterTypes, Object... arguments) {
        try {
            npc.getClass().getMethod(methodName, parameterTypes).invoke(npc, arguments);
            return true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            return false;
        }
    }

    private static Identifier invokeNpcIdentifier(Entity npc, String methodName) {
        try {
            Object value = npc.getClass().getMethod(methodName).invoke(npc);
            return value instanceof Identifier id ? id : null;
        } catch (ReflectiveOperationException | LinkageError exception) {
            return null;
        }
    }

    private static Identifier profileFor(Identifier factionId, String roleId) {
        Identifier canonical = AshfallFactionMap.canonicalFaction(factionId);
        if (canonical == null) {
            return null;
        }
        String role = roleId == null ? "" : roleId.trim();
        if (AshfallBiomeFactions.RADWARDEN_COMPACT.equals(canonical)) {
            return id(role.contains("guard") ? "radwarden_guard" : "radwarden_quartermaster");
        }
        if (AshfallBiomeFactions.CRASHBREAK_SALVAGE.equals(canonical)) {
            return id(role.contains("broker") || role.contains("route") ? "crashbreak_broker" : "crashbreak_scout");
        }
        if (AshfallBiomeFactions.SPOREBOUND_SANCTUM.equals(canonical)) {
            return id(role.contains("medic") || role.contains("brewer") ? "sporebound_medic" : "sporebound_elder");
        }
        return null;
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
