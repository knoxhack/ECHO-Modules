package com.knoxhack.echoashfallprotocol.endgame;

import com.knoxhack.echoashfallprotocol.entity.ModEntities;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;

public final class NexusFinalBossProfiles {
    private static final Map<PostNexusData.NexusPath, NexusFinalBossProfile> BY_PATH =
            new EnumMap<>(PostNexusData.NexusPath.class);
    private static final Map<String, NexusFinalBossProfile> BY_ID = new LinkedHashMap<>();

    static {
        register(new NexusFinalBossProfile(PostNexusData.NexusPath.RESTORE,
                "corruption_bloom", "Corruption Bloom",
                "Purification overload / growth rupture",
                "The Bloom roots into every relay you saved. Cut the growth without poisoning the lattice.",
                "Purification is cascading. Break the core bloom before it rewrites the cure.",
                "Corruption Bloom dissolved. Restore lattice can breathe again.",
                "Clear growth pulses and keep medicine ready",
                240.0D, 10.0D, 8.0D, 0.22D, 7.0F, 0xFF7CFF8A,
                ModEntities.CORRUPTION_BLOOM::get));
        register(new NexusFinalBossProfile(PostNexusData.NexusPath.DESTROY,
                "severance_engine", "Severance Engine",
                "Fallout surge / command collapse",
                "The Engine turns collapse into shrapnel. Do not let the dead signal take the room with it.",
                "Dead signal overpressure rising. Finish it before the blast loop stabilizes.",
                "Severance Engine destroyed. The command chain has no spine left.",
                "Push through blast pulses and clear defenders",
                260.0D, 12.0D, 10.0D, 0.20D, 8.0F, 0xFFFF6969,
                ModEntities.SEVERANCE_ENGINE::get));
        register(new NexusFinalBossProfile(PostNexusData.NexusPath.CONTROL,
                "mirror_command", "Mirror Command",
                "Command reflection / obedience field",
                "Mirror Command answers every order with a better one. Keep your signal yours.",
                "Reflection lattice online. Break the mirror before it learns your route.",
                "Mirror Command bound. Control has a voice, and the terminal knows its cost.",
                "Stay mobile through null pulses and pressure adds",
                250.0D, 11.0D, 9.0D, 0.24D, 7.5F, 0xFFD6A2FF,
                ModEntities.MIRROR_COMMAND::get));
    }

    private NexusFinalBossProfiles() {
    }

    public static Collection<NexusFinalBossProfile> all() {
        return BY_PATH.values();
    }

    public static Optional<NexusFinalBossProfile> byPath(PostNexusData.NexusPath path) {
        return Optional.ofNullable(BY_PATH.get(path));
    }

    public static Optional<NexusFinalBossProfile> byEntityId(String entityId) {
        return Optional.ofNullable(BY_ID.get(normalize(entityId)));
    }

    public static Optional<NexusFinalBossProfile> byType(EntityType<?> type) {
        if (type == null) {
            return Optional.empty();
        }
        return byEntityId(BuiltInRegistries.ENTITY_TYPE.getKey(type).toString());
    }

    public static boolean hasCoverage() {
        return BY_PATH.containsKey(PostNexusData.NexusPath.RESTORE)
                && BY_PATH.containsKey(PostNexusData.NexusPath.DESTROY)
                && BY_PATH.containsKey(PostNexusData.NexusPath.CONTROL)
                && BY_PATH.size() == 3;
    }

    private static void register(NexusFinalBossProfile profile) {
        BY_PATH.put(profile.path(), profile);
        BY_ID.put(normalize(profile.entityId()), profile);
    }

    private static String normalize(String entityId) {
        return entityId == null ? "" : entityId.toLowerCase(Locale.ROOT);
    }
}
