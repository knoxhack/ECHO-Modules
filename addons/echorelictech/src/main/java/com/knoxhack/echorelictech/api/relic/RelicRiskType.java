package com.knoxhack.echorelictech.api.relic;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import java.util.Locale;

public enum RelicRiskType implements StringRepresentable {
    NEXUS_INSTABILITY,
    RADIATION_EXPOSURE,
    BATTERY_DRAIN,
    ITEM_DAMAGE,
    COOLDOWN_TRAUMA,
    MALFUNCTION,
    FACTION_DISTRUST,
    MEMORY_CORRUPTION,
    WORLD_HAZARD_SPIKE,
    HOSTILE_SIGNAL_ATTRACTION,
    PLAYER_DEBUFF,
    AI_WHISPER,
    PERMANENT_REPAIR_COST,
    POWERGRID_OVERLOAD,
    FALSE_GUIDANCE,
    DRONE_DISOBEDIENCE,
    CLIMATE_BACKFIRE;

    public static final Codec<RelicRiskType> CODEC = StringRepresentable.fromEnum(RelicRiskType::values);

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
