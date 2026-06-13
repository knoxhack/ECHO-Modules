package com.knoxhack.echo.equipmentcore;

import java.util.List;

public final class EchoEquipmentCore {
    public static final String MODID = "echoequipmentcore";
    public static final List<String> REQUIRES = List.of(
            "echocore",
            "echoadaptercore",
            "echoarmory",
            "echocombatcore",
            "echotoolcore"
        );
    public static final List<String> PROVIDES = List.of(
            "equipment.slots",
            "equipment.durability",
            "equipment.upgrades",
            "equipment.loadout_validation"
        );
    public static final List<String> MVP_CONTRACTS = List.of(
            "gear_slot_contract",
            "durability_rules",
            "upgrade_modifiers",
            "loadout_validation"
        );

    public EchoEquipmentCore() {
        bootstrap();
    }

    public void bootstrap() {
    }

    public String moduleId() {
        return MODID;
    }

    public List<String> provides() {
        return PROVIDES;
    }
}
