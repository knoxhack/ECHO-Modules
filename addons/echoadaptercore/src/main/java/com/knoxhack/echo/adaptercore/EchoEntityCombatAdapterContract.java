package com.knoxhack.echo.adaptercore;

import java.util.List;

public final class EchoEntityCombatAdapterContract {
    public static final String CONTRACT_ID = "adaptercore.entity_combat.v1";
    public static final String SPAWN_ENTITY = "adaptercore.entity.spawn";
    public static final String DAMAGE_ENTITY = "adaptercore.entity.damage";
    public static final String DAMAGE_PLAYER = "adaptercore.player.damage";
    public static final String ENTITY_AI_TICK = "adaptercore.entity.ai_tick";
    public static final String NPC_INTERACTION = "adaptercore.npc.interaction";
    public static final String ENCOUNTER_START = "adaptercore.encounter.start";
    public static final String ENCOUNTER_END = "adaptercore.encounter.end";
    public static final String COMBAT_REWARD = "adaptercore.combat.reward";
    public static final String MISSION_TRIGGER = "adaptercore.mission.trigger";

    public static final List<String> REQUIRED_RECORDS = List.of(
            "EchoEntityType",
            "EchoEntityInstance",
            "EchoNpcProfile",
            "EchoCreatureBrain",
            "EchoEncounter",
            "EchoDamageSource",
            "EchoCombatStats",
            "EchoWeaponProfile",
            "EchoArmorProfile",
            "EchoFactionRelation",
            "EchoInteractionOption");

    private EchoEntityCombatAdapterContract() {
    }
}
