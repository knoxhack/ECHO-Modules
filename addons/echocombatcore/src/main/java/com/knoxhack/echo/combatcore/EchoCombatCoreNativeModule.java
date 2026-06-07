package com.knoxhack.echo.combatcore;

import dev.echo.nativeplatform.contracts.EchoNativeSurfaceModuleEntrypoint;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class EchoCombatCoreNativeModule implements EchoNativeSurfaceModuleEntrypoint {
    public static final String MODULE_ID = EchoCombatConstants.MOD_ID;
    public static final String DAMAGE_ITEM_CONTRACT_ID = "echocombatcore:item/damage_weapon_trait_contract";
    public static final String ENTITY_SCALING_CONTRACT_ID = "echocombatcore:entity/enemy_scaling_boss_phase_contract";
    public static final String PLAYER_DEFENSE_CONTRACT_ID = "echocombatcore:player/armor_shield_telemetry_contract";
    public static final List<String> CONTRACT_IDS = List.of(
            DAMAGE_ITEM_CONTRACT_ID,
            ENTITY_SCALING_CONTRACT_ID,
            PLAYER_DEFENSE_CONTRACT_ID
    );

    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        Map<String, Object> referenceProbe = exerciseReferenceBehavior();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activated", true);
        result.put("activationStage", "combatcore_native_contract_active");
        result.put("adapterCoreUsed", true);
        result.put("nativeAdapterCodeExecuted", true);
        result.put("serviceCodeExecuted", true);
        result.put("moduleId", MODULE_ID);
        result.put("packId", context.getOrDefault("packId", "unknown"));
        result.put("registeredFeatureContracts", CONTRACT_IDS);
        result.put("logicalRegistrationCount", CONTRACT_IDS.size());
        result.put("adapterDomains", List.of("entities", "items", "player"));
        result.put("runtimeTargets", List.of("echo_native", "echo_runtime_standalone"));
        result.put("damageItemRoundTrip", referenceProbe.get("damageItemRoundTrip"));
        result.put("entityScalingRoundTrip", referenceProbe.get("entityScalingRoundTrip"));
        result.put("playerDefenseRoundTrip", referenceProbe.get("playerDefenseRoundTrip"));
        result.put("referenceProbe", referenceProbe);
        result.put("registryInjected", false);
        result.put("registryMutated", false);
        result.put("transformsPerformed", false);
        result.put("summary", "CombatCore native contract exercised damage/trait, enemy scaling/boss phase, and armor/shield/telemetry behavior.");
        return Map.copyOf(result);
    }

    public static void main(String[] args) {
        Map<String, Object> activation = new EchoCombatCoreNativeModule()
                .describeNativeSurfaces(Map.of("packId", "combatcore-smoke"));
        require(Boolean.TRUE.equals(activation.get("activated")),
                "CombatCore native adapter should activate");
        require(Boolean.TRUE.equals(activation.get("damageItemRoundTrip")),
                "CombatCore native adapter should exercise damage and weapon trait item behavior");
        require(Boolean.TRUE.equals(activation.get("entityScalingRoundTrip")),
                "CombatCore native adapter should exercise enemy scaling and boss phase behavior");
        require(Boolean.TRUE.equals(activation.get("playerDefenseRoundTrip")),
                "CombatCore native adapter should exercise armor, shield, and telemetry behavior");
        System.out.println("combatcore native adapter smoke PASS contracts=" + CONTRACT_IDS.size());
    }

    private Map<String, Object> exerciseReferenceBehavior() {
        EchoDamageTypeProfile damage = new EchoDamageTypeProfile(
                EchoDamageTypeId.of(" Signal_Burn "),
                null,
                " Signal Burn ",
                null,
                null,
                null,
                null,
                null,
                true,
                true,
                null,
                Map.of("school", "signal")
        );
        EchoWeaponTrait trait = new EchoWeaponTrait(
                EchoWeaponTraitId.of(" Overclocked_Blades "),
                " Overclocked Blades ",
                null,
                damage.id(),
                1.35D,
                0.75D,
                Set.of(EchoCombatConstants.FEATURE_COMBAT_DAMAGE),
                null,
                null,
                Map.of("slot", "melee")
        );
        EchoEnemyScalingProfile scaling = new EchoEnemyScalingProfile(
                "ashfall_elite",
                null,
                null,
                null,
                1.8D,
                1.25D,
                0.9D,
                1.1D,
                null,
                " late route ",
                Map.of("tier", "elite")
        );
        EchoBossPhase phase = new EchoBossPhase(
                EchoBossPhaseId.of(" Nexus_Phase_Two "),
                " Nexus Phase Two ",
                null,
                1.7D,
                null,
                null,
                null,
                null,
                " second wind ",
                " unlocks beam pattern ",
                Map.of("pattern", "beam")
        );
        EchoArmorProfile armor = new EchoArmorProfile(
                EchoArmorProfileId.of(" Signal_Ward "),
                " Signal Ward ",
                null,
                0.25D,
                Map.of(EchoCombatDamageKind.SIGNAL, 0.5D),
                null,
                null,
                null,
                Map.of("class", "support")
        );
        EchoShieldProfile shield = new EchoShieldProfile(
                "signal_shield",
                null,
                120.0D,
                6.0D,
                4.0D,
                List.of(EchoCombatDamageKind.TRUE_DAMAGE),
                null,
                null,
                Map.of("regen", "fast")
        );
        EchoCombatTelemetryEvent telemetry = new EchoCombatTelemetryEvent(
                "hit-1",
                null,
                null,
                null,
                null,
                damage.id(),
                42.5D,
                100L,
                Map.of("critical", "true")
        );
        EchoCombatRegistry registry = new EchoCombatRegistry(
                Map.of(damage.id(), damage),
                Map.of(armor.id(), armor),
                Map.of(trait.id(), trait),
                List.of(scaling),
                List.of(phase),
                List.of(shield),
                List.of(telemetry),
                null,
                null
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("damageItemRoundTrip", damage.id().value().equals("signal_burn")
                && damage.kind() == EchoCombatDamageKind.UNKNOWN
                && damage.displayName().equals("Signal Burn")
                && damage.bypassesArmor()
                && trait.id().value().equals("overclocked_blades")
                && trait.displayName().equals("Overclocked Blades")
                && trait.damageMultiplier() == 1.35D
                && trait.cooldownMultiplier() == 0.75D
                && trait.behaviorFeatures().contains(EchoCombatConstants.FEATURE_COMBAT_DAMAGE)
                && !trait.blocking());
        result.put("entityScalingRoundTrip", scaling.scalingId().equals("ashfall_elite")
                && scaling.healthMultiplier() == 1.8D
                && scaling.damageMultiplier() == 1.25D
                && scaling.speedMultiplier() == 0.9D
                && scaling.developerDetails().equals("late route")
                && phase.id().value().equals("nexus_phase_two")
                && phase.startsAtHealthRatio() == 1.0D
                && phase.playerSummary().equals("second wind")
                && phase.abilityReferences().isEmpty());
        result.put("playerDefenseRoundTrip", armor.id().value().equals("signal_ward")
                && armor.baseReduction() == 0.25D
                && armor.damageKindReduction().get(EchoCombatDamageKind.SIGNAL) == 0.5D
                && shield.shieldId().equals("signal_shield")
                && shield.capacity() == 120.0D
                && shield.vulnerableKinds().contains(EchoCombatDamageKind.TRUE_DAMAGE)
                && telemetry.kind() == EchoTelemetryKind.UNKNOWN
                && telemetry.amount() == 42.5D
                && telemetry.gameTime() == 100L
                && !registry.blocking());
        result.put("damageTypeId", damage.id().value());
        result.put("weaponTraitId", trait.id().value());
        result.put("bossPhaseRatio", phase.startsAtHealthRatio());
        result.put("shieldCapacity", shield.capacity());
        result.put("telemetryKind", telemetry.kind().serializedName());
        return Map.copyOf(result);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
