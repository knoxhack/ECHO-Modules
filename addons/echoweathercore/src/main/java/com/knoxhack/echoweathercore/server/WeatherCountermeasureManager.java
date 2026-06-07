package com.knoxhack.echoweathercore.server;

import com.knoxhack.echoweathercore.api.weather.WeatherEffectModifiers;
import com.knoxhack.echoweathercore.api.weather.WeatherType;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class WeatherCountermeasureManager {
    private static final Map<WeatherType, WeatherEffectModifiers> DEFAULT_COUNTERMEASURES = defaultCountermeasures();
    private static final Map<WeatherType, WeatherEffectModifiers> COUNTERMEASURES = new EnumMap<>(WeatherType.class);
    private static final Map<UUID, ShelterReport> SHELTER_REPORTS = new ConcurrentHashMap<>();

    static {
        resetForTests();
    }

    private WeatherCountermeasureManager() {}

    public static WeatherEffectModifiers getCountermeasureModifiers(WeatherType type) {
        return COUNTERMEASURES.getOrDefault(type, WeatherEffectModifiers.DEFAULT);
    }

    public static void registerCountermeasure(WeatherType type, WeatherEffectModifiers modifiers) {
        if (type != null && modifiers != null) {
            COUNTERMEASURES.put(type, modifiers);
        }
    }

    public static ShelterReport reportShelterEntered(ServerPlayer player, BlockPos shelterPos, String sourceReason) {
        if (player == null || shelterPos == null) {
            return null;
        }
        ShelterReport report = new ShelterReport(
                player.getUUID(),
                shelterPos.getX(),
                shelterPos.getY(),
                shelterPos.getZ(),
                player.level().getGameTime(),
                sourceReason == null ? "" : sourceReason);
        SHELTER_REPORTS.put(player.getUUID(), report);
        return report;
    }

    public static Optional<ShelterReport> lastShelterReport(ServerPlayer player) {
        return Optional.ofNullable(player == null ? null : SHELTER_REPORTS.get(player.getUUID()));
    }

    public static Map<UUID, ShelterReport> shelterReports() {
        return Map.copyOf(SHELTER_REPORTS);
    }

    public static boolean isCountermeasureItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return path.contains("filter") || path.contains("anchor") || path.contains("coil") || path.contains("cell");
    }

    public static void resetForTests() {
        COUNTERMEASURES.clear();
        COUNTERMEASURES.putAll(DEFAULT_COUNTERMEASURES);
        SHELTER_REPORTS.clear();
    }

    private static Map<WeatherType, WeatherEffectModifiers> defaultCountermeasures() {
        Map<WeatherType, WeatherEffectModifiers> defaults = new EnumMap<>(WeatherType.class);
        defaults.put(WeatherType.ASH_STORM, new WeatherEffectModifiers(
                1.0, 1.15, 1.15, 1.1, 0.85, 1.0, 1.0, 1.0, 1.0, 1.0,
                1.0, 1.0, 1.0, 1.0, 1.15, 1.0, 1.0, 1.0, 1.0, 0.85));
        defaults.put(WeatherType.TOXIC_RAIN, new WeatherEffectModifiers(
                1.0, 1.0, 1.0, 1.0, 0.7, 1.0, 0.6, 1.0, 1.0, 1.0,
                1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.75));
        defaults.put(WeatherType.RADIATION_STORM, new WeatherEffectModifiers(
                1.0, 1.0, 1.0, 1.0, 1.0, 0.5, 1.0, 1.0, 1.0, 1.0,
                1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.6));
        defaults.put(WeatherType.CRYO_FRONT, new WeatherEffectModifiers(
                1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.5, 1.0, 1.0,
                1.0, 1.0, 1.2, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.8));
        defaults.put(WeatherType.NEXUS_SIGNAL_STORM, new WeatherEffectModifiers(
                1.0, 1.2, 1.2, 1.2, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0,
                1.0, 1.0, 1.0, 1.0, 1.2, 1.0, 1.0, 1.0, 1.0, 0.9));
        defaults.put(WeatherType.ELECTROMAGNETIC_BLACKOUT, new WeatherEffectModifiers(
                1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0,
                1.0, 0.6, 1.15, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.85));
        return Map.copyOf(defaults);
    }

    public record ShelterReport(UUID playerId, int x, int y, int z, long gameTick, String sourceReason) {
    }
}
