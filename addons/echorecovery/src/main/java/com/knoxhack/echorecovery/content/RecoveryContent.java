package com.knoxhack.echorecovery.content;

import com.knoxhack.echorecovery.EchoRecovery;
import com.knoxhack.echorecovery.api.RecoveryItemRuleResult;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public final class RecoveryContent {
    public static final Identifier DEFAULT_GRAVE_TYPE = Identifier.fromNamespaceAndPath(EchoRecovery.MODID, "vanilla_grave");

    private static volatile Map<Identifier, RecoveryGraveType> graveTypes = defaultGraveTypes();
    private static volatile Map<Identifier, RecoveryRuleDefinition> rules = Map.of();
    private static volatile Map<Identifier, RecoveryPreset> presets = defaultPresets();

    private RecoveryContent() {
    }

    public static RecoveryGraveType graveType(Identifier id) {
        return graveTypes.getOrDefault(id, graveTypes.get(DEFAULT_GRAVE_TYPE));
    }

    public static Map<Identifier, RecoveryGraveType> graveTypes() {
        return graveTypes;
    }

    public static Map<Identifier, RecoveryRuleDefinition> rules() {
        return rules;
    }

    public static Map<Identifier, RecoveryPreset> presets() {
        return presets;
    }

    public static Optional<RecoveryItemRuleResult> evaluateDataRules(ItemStack stack) {
        return rules.values().stream()
                .sorted(Comparator.comparingInt(RecoveryRuleDefinition::priority).reversed()
                        .thenComparing(rule -> rule.id().toString()))
                .filter(rule -> rule.matches(stack))
                .map(RecoveryRuleDefinition::result)
                .findFirst();
    }

    public static void replaceJsonContent(LoadedContent loaded) {
        Map<Identifier, RecoveryGraveType> nextGraveTypes = new LinkedHashMap<>(defaultGraveTypes());
        nextGraveTypes.putAll(loaded.graveTypes());
        graveTypes = Map.copyOf(nextGraveTypes);

        Map<Identifier, RecoveryRuleDefinition> nextRules = new LinkedHashMap<>();
        loaded.rules().forEach((id, rule) -> {
            if (nextRules.containsKey(id)) {
                EchoRecovery.LOGGER.warn("Duplicate Recovery rule id {} ignored.", id);
            } else {
                nextRules.put(id, rule);
            }
        });
        rules = Map.copyOf(nextRules);

        Map<Identifier, RecoveryPreset> nextPresets = new LinkedHashMap<>(defaultPresets());
        nextPresets.putAll(loaded.presets());
        presets = Map.copyOf(nextPresets);

        EchoRecovery.LOGGER.info("Loaded Recovery content: {} grave types, {} rules, {} presets.",
                graveTypes.size(), rules.size(), presets.size());
    }

    private static Map<Identifier, RecoveryGraveType> defaultGraveTypes() {
        Map<Identifier, RecoveryGraveType> defaults = new LinkedHashMap<>();
        defaults.put(DEFAULT_GRAVE_TYPE, new RecoveryGraveType(
                DEFAULT_GRAVE_TYPE,
                "Stone Grave",
                Identifier.fromNamespaceAndPath(EchoRecovery.MODID, "grave"),
                Identifier.fromNamespaceAndPath("minecraft", "block/stone"),
                false,
                List.of("Protected grave. Recover all or move items out manually.")));
        defaults.put(Identifier.fromNamespaceAndPath(EchoRecovery.MODID, "ashfall_field_recovery_cache"),
                new RecoveryGraveType(
                        Identifier.fromNamespaceAndPath(EchoRecovery.MODID, "ashfall_field_recovery_cache"),
                        "Field Recovery Cache",
                        Identifier.fromNamespaceAndPath(EchoRecovery.MODID, "recovery_cache"),
                        Identifier.fromNamespaceAndPath("minecraft", "block/cracked_stone_bricks"),
                        true,
                        List.of("Signal integrity degraded by field conditions.", "Use Lens or Terminal for cache status.")));
        return Map.copyOf(defaults);
    }

    private static Map<Identifier, RecoveryPreset> defaultPresets() {
        Map<Identifier, RecoveryPreset> defaults = new LinkedHashMap<>();
        defaults.put(Identifier.fromNamespaceAndPath(EchoRecovery.MODID, "forgiving"), new RecoveryPreset(
                Identifier.fromNamespaceAndPath(EchoRecovery.MODID, "forgiving"),
                "Forgiving",
                Map.of(
                        "grave_expiration_minutes", "-1",
                        "remote_recovery_enabled", "false",
                        "grave_key_required", "false",
                        "recovery_compass_works_cross_dimension", "false")));
        return Map.copyOf(defaults);
    }

    public record LoadedContent(
            Map<Identifier, RecoveryGraveType> graveTypes,
            Map<Identifier, RecoveryRuleDefinition> rules,
            Map<Identifier, RecoveryPreset> presets) {
        public LoadedContent {
            graveTypes = Map.copyOf(graveTypes == null ? Map.of() : graveTypes);
            rules = Map.copyOf(rules == null ? Map.of() : rules);
            presets = Map.copyOf(presets == null ? Map.of() : presets);
        }
    }
}
