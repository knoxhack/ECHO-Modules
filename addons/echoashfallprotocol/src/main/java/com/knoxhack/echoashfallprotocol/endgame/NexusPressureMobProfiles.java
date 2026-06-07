package com.knoxhack.echoashfallprotocol.endgame;

import com.knoxhack.echoashfallprotocol.entity.ModEntities;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;

public final class NexusPressureMobProfiles {
    private static final Map<String, NexusPressureMobProfile> BY_ID = new LinkedHashMap<>();

    static {
        register(new NexusPressureMobProfile("gridbound_husk", "Gridbound Husk",
                36.0D, 6.0D, 4.0D, 0.24D, 3.0F, 0xFF9FE7FF,
                NexusPressureMobProfile.Ability.GRID_PRESSURE));
        register(new NexusPressureMobProfile("relay_warden", "Relay Warden",
                92.0D, 9.0D, 10.0D, 0.20D, 4.0F, 0xFFFFD06A,
                NexusPressureMobProfile.Ability.WARDEN_BULWARK));
        register(new NexusPressureMobProfile("signal_leech", "Signal Leech",
                28.0D, 5.0D, 1.0D, 0.31D, 2.0F, 0xFFA3FF77,
                NexusPressureMobProfile.Ability.SIGNAL_LEECH));
        register(new NexusPressureMobProfile("nexus_nullifier", "Nexus Nullifier",
                78.0D, 8.0D, 8.0D, 0.22D, 5.0F, 0xFFD8A8FF,
                NexusPressureMobProfile.Ability.NULL_FIELD));
    }

    private NexusPressureMobProfiles() {
    }

    public static Collection<NexusPressureMobProfile> all() {
        return BY_ID.values();
    }

    public static Optional<NexusPressureMobProfile> byEntityId(String entityId) {
        return Optional.ofNullable(BY_ID.get(normalize(entityId)));
    }

    public static Optional<NexusPressureMobProfile> byType(EntityType<?> type) {
        if (type == null) {
            return Optional.empty();
        }
        return byEntityId(BuiltInRegistries.ENTITY_TYPE.getKey(type).toString());
    }

    public static boolean isPressureMob(EntityType<?> type) {
        return byType(type).isPresent();
    }

    public static boolean registryMatchesEntities() {
        return byType(ModEntities.GRIDBOUND_HUSK.get()).isPresent()
                && byType(ModEntities.RELAY_WARDEN.get()).isPresent()
                && byType(ModEntities.SIGNAL_LEECH.get()).isPresent()
                && byType(ModEntities.NEXUS_NULLIFIER.get()).isPresent();
    }

    private static void register(NexusPressureMobProfile profile) {
        BY_ID.put(normalize(profile.entityId()), profile);
    }

    private static String normalize(String entityId) {
        return entityId == null ? "" : entityId.toLowerCase(Locale.ROOT);
    }
}
