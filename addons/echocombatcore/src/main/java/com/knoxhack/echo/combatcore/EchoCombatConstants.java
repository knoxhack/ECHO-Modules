package com.knoxhack.echo.combatcore;

import com.knoxhack.echo.platformcore.EchoFeatureId;

import java.util.Set;

public final class EchoCombatConstants {
    public static final String MOD_ID = "echocombatcore";
    public static final String MOD_NAME = "ECHO: CombatCore";

    public static final EchoFeatureId FEATURE_COMBAT_DAMAGE = EchoFeatureId.of("combat.damage");
    public static final EchoFeatureId FEATURE_COMBAT_ARMOR = EchoFeatureId.of("combat.armor_profiles");
    public static final EchoFeatureId FEATURE_COMBAT_WEAPON_TRAITS = EchoFeatureId.of("combat.weapon_traits");
    public static final EchoFeatureId FEATURE_COMBAT_TELEMETRY = EchoFeatureId.of("combat.telemetry");

    public static final Set<EchoFeatureId> PROVIDED_FEATURES = Set.of(
            FEATURE_COMBAT_DAMAGE,
            FEATURE_COMBAT_ARMOR,
            FEATURE_COMBAT_WEAPON_TRAITS,
            FEATURE_COMBAT_TELEMETRY
    );

    private EchoCombatConstants() {
    }
}
