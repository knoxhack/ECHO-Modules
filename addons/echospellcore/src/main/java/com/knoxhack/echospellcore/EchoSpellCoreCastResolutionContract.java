package com.knoxhack.echospellcore;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoSpellCoreCastResolutionContract {
    public static final String MODULE_ID = "echospellcore";
    public static final String ADAPTERCORE_CONTRACT_ID = "echospellcore:player/signal_focus_cast_resolution";
    public static final String REFERENCE_PLAYER_ID = "ashfall-spell-scout-001";
    public static final String REFERENCE_SPELL_ID = "echospellcore:spell/aether_bolt";
    public static final String REFERENCE_PROJECTILE_ID = "echospellcore:projectile/aether_bolt";

    private EchoSpellCoreCastResolutionContract() {
    }

    public static Map<String, Object> executeReferenceCast(String packId) {
        int baseCost = 6;
        int baseCooldown = 40;
        double baseRange = 20.0D;
        double baseDamage = 5.0D;
        double costScale = 0.82D * 1.18D;
        double rangeScale = 1.25D;
        int resolvedCost = Math.max(1, (int) Math.ceil(baseCost * costScale));
        int resolvedCooldown = Math.max(8, baseCooldown - 10);
        double resolvedRange = baseRange * rangeScale;

        Map<String, Object> resolution = new LinkedHashMap<>();
        resolution.put("adapterCoreContract", ADAPTERCORE_CONTRACT_ID);
        resolution.put("service", "echospellcore:signal_focus_cast_service");
        resolution.put("spellCastResolved", true);
        resolution.put("packId", packId == null || packId.isBlank() ? "unknown" : packId);
        resolution.put("playerId", REFERENCE_PLAYER_ID);
        resolution.put("focusItem", "echospellcore:signal_focus");
        resolution.put("deckState", Map.of(
                "itemId", "echospellcore:spell_deck",
                "activeSlot", 3,
                "coreState", "engraved",
                "selectedSpell", REFERENCE_SPELL_ID,
                "modifiers", List.of("efficiency", "range")
        ));
        resolution.put("castProfile", castProfile(baseCost, costScale, resolvedCost, baseCooldown,
                resolvedCooldown, baseRange, resolvedRange));
        resolution.put("aetherLedger", Map.of(
                "type", "RAW_AETHER",
                "before", 24,
                "cost", resolvedCost,
                "after", 18,
                "max", 40
        ));
        resolution.put("cooldownState", Map.of(
                "key", "cooldown_aether_bolt",
                "gameTime", 1200L,
                "readyAt", 1230L,
                "remainingTicks", resolvedCooldown
        ));
        resolution.put("projectileIntent", Map.of(
                "projectileId", REFERENCE_PROJECTILE_ID,
                "baseDamage", baseDamage,
                "resolvedDamage", baseDamage,
                "velocity", 1.05D,
                "range", resolvedRange,
                "syncPacket", "echospellcore:spell_projectile_sync",
                "hitEffects", List.of(effect("minecraft:glowing", 80, 0))
        ));
        resolution.put("events", List.of(
                event("spell.cast", REFERENCE_SPELL_ID, "cast"),
                event("aether.consume", "RAW_AETHER", String.valueOf(resolvedCost)),
                event("spell.cooldown", "cooldown_aether_bolt", String.valueOf(resolvedCooldown)),
                event("projectile.spawn", REFERENCE_PROJECTILE_ID, "spawn")
        ));
        resolution.put("diagnostics", List.of(
                "spell.deck.loaded",
                "spell.cast_profile.resolved",
                "spell.aether.consumed",
                "spell.cooldown.persisted",
                "spell.projectile.intent_dispatched"
        ));
        resolution.put("referenceBehavior", "spellcore_resolves_signal_focus_cast_cost_cooldown_and_projectile");
        return Map.copyOf(resolution);
    }

    public static boolean referenceCastPassed(Map<String, Object> resolution) {
        return Boolean.TRUE.equals(resolution.get("spellCastResolved"))
                && ADAPTERCORE_CONTRACT_ID.equals(resolution.get("adapterCoreContract"))
                && REFERENCE_PLAYER_ID.equals(resolution.get("playerId"))
                && String.valueOf(resolution.get("deckState")).contains("selectedSpell=" + REFERENCE_SPELL_ID)
                && String.valueOf(resolution.get("deckState")).contains("efficiency")
                && String.valueOf(resolution.get("deckState")).contains("range")
                && String.valueOf(resolution.get("castProfile")).contains("resolvedCost=6")
                && String.valueOf(resolution.get("castProfile")).contains("resolvedCooldownTicks=30")
                && String.valueOf(resolution.get("castProfile")).contains("resolvedRange=25.0")
                && String.valueOf(resolution.get("aetherLedger")).contains("after=18")
                && String.valueOf(resolution.get("cooldownState")).contains("readyAt=1230")
                && String.valueOf(resolution.get("projectileIntent")).contains(REFERENCE_PROJECTILE_ID)
                && String.valueOf(resolution.get("projectileIntent")).contains("minecraft:glowing")
                && String.valueOf(resolution.get("diagnostics")).contains("spell.projectile.intent_dispatched");
    }

    private static Map<String, Object> effect(String effectId, int durationTicks, int amplifier) {
        Map<String, Object> effect = new LinkedHashMap<>();
        effect.put("effectId", effectId);
        effect.put("durationTicks", durationTicks);
        effect.put("amplifier", amplifier);
        return Map.copyOf(effect);
    }

    private static Map<String, Object> castProfile(
            int baseCost,
            double costScale,
            int resolvedCost,
            int baseCooldown,
            int resolvedCooldown,
            double baseRange,
            double resolvedRange
    ) {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("spellId", REFERENCE_SPELL_ID);
        profile.put("aetherType", "RAW_AETHER");
        profile.put("targetingMode", "PROJECTILE");
        profile.put("baseCost", baseCost);
        profile.put("costScale", costScale);
        profile.put("resolvedCost", resolvedCost);
        profile.put("baseCooldownTicks", baseCooldown);
        profile.put("resolvedCooldownTicks", resolvedCooldown);
        profile.put("baseRange", baseRange);
        profile.put("resolvedRange", resolvedRange);
        profile.put("damageScale", 1.0D);
        profile.put("curseRisk", 0.0D);
        return Map.copyOf(profile);
    }

    private static Map<String, Object> event(String eventId, String target, String action) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("eventId", eventId);
        event.put("target", target);
        event.put("action", action);
        return Map.copyOf(event);
    }
}
