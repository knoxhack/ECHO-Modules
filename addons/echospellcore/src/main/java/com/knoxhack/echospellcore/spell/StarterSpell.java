package com.knoxhack.echospellcore.spell;

import com.knoxhack.echoarcanacore.api.AetherSignalType;
import com.knoxhack.echoarcanacore.api.SpellSchool;
import com.knoxhack.echoarcanacore.api.TargetingMode;
import com.knoxhack.echospellcore.EchoSpellCore;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public enum StarterSpell {
    SIGNAL_PULSE("signal_pulse", SpellSchool.SIGNAL, AetherSignalType.SIGNAL_AETHER,
            TargetingMode.CONE, 8.0D, 60, 10.0D, Items.AMETHYST_SHARD),
    ECHO_MARK("echo_mark", SpellSchool.SIGNAL, AetherSignalType.SIGNAL_AETHER,
            TargetingMode.RAYCAST, 5.0D, 50, 24.0D, Items.GLOW_INK_SAC),
    STATIC_BURST("static_burst", SpellSchool.SIGNAL, AetherSignalType.SIGNAL_AETHER,
            TargetingMode.AREA, 9.0D, 90, 6.0D, Items.REDSTONE),
    AETHER_BOLT("aether_bolt", SpellSchool.AETHER, AetherSignalType.RAW_AETHER,
            TargetingMode.PROJECTILE, 6.0D, 40, 20.0D, Items.ECHO_SHARD),
    AETHER_SHIELD("aether_shield", SpellSchool.AETHER, AetherSignalType.RAW_AETHER,
            TargetingMode.SELF, 9.0D, 120, 0.0D, Items.SHIELD),
    ARCANE_LIFT("arcane_lift", SpellSchool.AETHER, AetherSignalType.RAW_AETHER,
            TargetingMode.RAYCAST, 7.0D, 80, 16.0D, Items.FEATHER),
    ASH_VEIL("ash_veil", SpellSchool.ASH, AetherSignalType.RAW_AETHER,
            TargetingMode.SELF, 10.0D, 120, 0.0D, Items.GUNPOWDER),
    DUST_LANCE("dust_lance", SpellSchool.ASH, AetherSignalType.RAW_AETHER,
            TargetingMode.PROJECTILE, 7.0D, 55, 18.0D, Items.BLAZE_POWDER),
    CINDER_SKIN("cinder_skin", SpellSchool.ASH, AetherSignalType.RAW_AETHER,
            TargetingMode.SELF, 8.0D, 140, 0.0D, Items.MAGMA_CREAM),
    VOID_STEP("void_step", SpellSchool.VOID, AetherSignalType.RIFT_AETHER,
            TargetingMode.SELF, 8.0D, 90, 9.0D, Items.ENDER_PEARL),
    NULL_BOLT("null_bolt", SpellSchool.VOID, AetherSignalType.RIFT_AETHER,
            TargetingMode.PROJECTILE, 7.0D, 70, 20.0D, Items.OBSIDIAN),
    HOLLOW_CAGE("hollow_cage", SpellSchool.VOID, AetherSignalType.RIFT_AETHER,
            TargetingMode.RAYCAST, 10.0D, 150, 15.0D, Items.IRON_INGOT),
    STORM_LANCE("storm_lance", SpellSchool.STORM, AetherSignalType.SIGNAL_AETHER,
            TargetingMode.PROJECTILE, 8.0D, 55, 22.0D, Items.LIGHTNING_ROD),
    STATIC_DASH("static_dash", SpellSchool.STORM, AetherSignalType.SIGNAL_AETHER,
            TargetingMode.SELF, 6.0D, 75, 0.0D, Items.FEATHER),
    THUNDER_CAGE("thunder_cage", SpellSchool.STORM, AetherSignalType.SIGNAL_AETHER,
            TargetingMode.AREA, 11.0D, 150, 10.0D, Items.COPPER_INGOT),
    CRYSTAL_WALL("crystal_wall", SpellSchool.CRYSTAL, AetherSignalType.REFINED_AETHER,
            TargetingMode.SELF, 8.0D, 120, 0.0D, Items.GLASS),
    SHARD_BURST("shard_burst", SpellSchool.CRYSTAL, AetherSignalType.REFINED_AETHER,
            TargetingMode.CONE, 9.0D, 85, 8.0D, Items.AMETHYST_SHARD),
    RESONANT_ARMOR("resonant_armor", SpellSchool.CRYSTAL, AetherSignalType.REFINED_AETHER,
            TargetingMode.SELF, 10.0D, 170, 0.0D, Items.IRON_CHESTPLATE),
    BLOOD_SURGE("blood_surge", SpellSchool.BLOOD, AetherSignalType.CURSED_AETHER,
            TargetingMode.SELF, 8.0D, 115, 0.0D, Items.REDSTONE),
    RIFT_BLINK("rift_blink", SpellSchool.RIFT, AetherSignalType.RIFT_AETHER,
            TargetingMode.SELF, 9.0D, 95, 12.0D, Items.ENDER_EYE),
    SOUL_THREAD("soul_thread", SpellSchool.SOUL, AetherSignalType.SOUL_AETHER,
            TargetingMode.RAYCAST, 7.0D, 90, 12.0D, Items.GHAST_TEAR),
    DECAY_TOUCH("decay_touch", SpellSchool.DECAY, AetherSignalType.CURSED_AETHER,
            TargetingMode.RAYCAST, 8.0D, 100, 5.0D, Items.SPIDER_EYE),
    VEIL_TRACE("veil_trace", SpellSchool.VEIL, AetherSignalType.VEIL_RESONANCE,
            TargetingMode.RAYCAST, 7.0D, 85, 18.0D, Items.SPYGLASS),
    FRACTURE_SHEAR("fracture_shear", SpellSchool.FRACTURE, AetherSignalType.FRACTURE_ENERGY,
            TargetingMode.PROJECTILE, 9.0D, 95, 20.0D, Items.ECHO_SHARD);

    private final Identifier id;
    private final SpellSchool school;
    private final AetherSignalType aetherType;
    private final TargetingMode targetingMode;
    private final double cost;
    private final int cooldownTicks;
    private final double range;
    private final Item icon;

    StarterSpell(String path, SpellSchool school, AetherSignalType aetherType, TargetingMode targetingMode,
            double cost, int cooldownTicks, double range, Item icon) {
        this.id = EchoSpellCore.id("spell/" + path);
        this.school = school;
        this.aetherType = aetherType;
        this.targetingMode = targetingMode;
        this.cost = cost;
        this.cooldownTicks = cooldownTicks;
        this.range = range;
        this.icon = icon;
    }

    public Identifier id() {
        return id;
    }

    public SpellSchool school() {
        return school;
    }

    public AetherSignalType aetherType() {
        return aetherType;
    }

    public TargetingMode targetingMode() {
        return targetingMode;
    }

    public double cost() {
        return cost;
    }

    public int cooldownTicks() {
        return cooldownTicks;
    }

    public double range() {
        return range;
    }

    public Item icon() {
        return icon;
    }

    public String path() {
        return id.getPath().substring("spell/".length());
    }

    public String translationKey() {
        return "spell.echospellcore." + path();
    }

    public String title() {
        String text = path().replace('_', ' ').toLowerCase(Locale.ROOT);
        StringBuilder builder = new StringBuilder(text.length());
        boolean upper = true;
        for (char c : text.toCharArray()) {
            if (Character.isWhitespace(c)) {
                builder.append(c);
                upper = true;
            } else if (upper) {
                builder.append(Character.toUpperCase(c));
                upper = false;
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }

    public static List<StarterSpell> ordered() {
        return List.of(values());
    }

    public static Optional<StarterSpell> byId(Identifier id) {
        return Arrays.stream(values()).filter(spell -> spell.id.equals(id)).findFirst();
    }

    public static StarterSpell safe(Identifier id) {
        return byId(id).orElse(SIGNAL_PULSE);
    }
}
