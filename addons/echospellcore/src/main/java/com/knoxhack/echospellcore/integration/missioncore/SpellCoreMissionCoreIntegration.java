package com.knoxhack.echospellcore.integration.missioncore;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.mission.IMissionRegistry;
import com.echoplatform.echocore.api.mission.MissionChapterDefinition;
import com.echoplatform.echocore.api.mission.MissionDefinition;
import com.echoplatform.echocore.api.mission.MissionKind;
import com.echoplatform.echocore.api.mission.MissionObjectiveType;
import com.echoplatform.echocore.api.mission.MissionRewardClaimMode;
import com.echoplatform.echocore.api.mission.ObjectiveDefinition;
import com.echoplatform.echocore.api.mission.RewardDefinition;
import com.knoxhack.echospellcore.EchoSpellCore;
import com.knoxhack.echospellcore.api.SpellCoreApi;
import com.knoxhack.echospellcore.api.SpellCoreEvents;
import com.knoxhack.echospellcore.registry.ModItems;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public final class SpellCoreMissionCoreIntegration {
    private static final Identifier CHAPTER_ID = id("arcana_spellcore");

    private SpellCoreMissionCoreIntegration() {
    }

    public static void register() {
        EchoSpellCore.LOGGER.info("ECHO MissionCore integration loaded for SpellCore.");
        EchoCoreServices.registerMissionContent(EchoSpellCore.MODID, SpellCoreMissionCoreIntegration::registerContent);
        registerEventHooks();
        registerHookCoverage();
    }

    private static void registerContent(IMissionRegistry registry) {
        registry.registerChapter(EchoSpellCore.MODID, new MissionChapterDefinition(
                CHAPTER_ID,
                "ECHO: SpellCore",
                "Calibrate a Signal Focus, spend Aether Signal, and learn starter spells.",
                80,
                0x46E7FF));
        registerMission(registry, "craft_signal_focus", "Obtain Signal Focus",
                "Recover or craft the first shared spellcasting focus.",
                "Focus shell online.", MissionObjectiveType.OBTAIN_ITEM, id("signal_focus"), new ItemStack(ModItems.SIGNAL_FOCUS.get()));
        registerMission(registry, "carry_awakened_spell_core", "Carry Awakened Spell Core",
                "Use RitualCore's Spell Core Awakening output as the first casting authorization token.",
                "Spell core authorization present.", MissionObjectiveType.CUSTOM,
                Identifier.fromNamespaceAndPath("echoritualcore", "awakened_spell_core"),
                new ItemStack(ModItems.BLANK_SPELL_CORE.get()));
        registerMission(registry, "cast_signal_pulse", "Cast Signal Pulse",
                "Use Signal Focus to reveal and disrupt nearby signatures.",
                "Signal Pulse cast.", MissionObjectiveType.CUSTOM, SpellCoreApi.SIGNAL_PULSE,
                new ItemStack(ModItems.SIGNAL_CATALYST.get()));
        registerMission(registry, "cast_aether_bolt", "Cast Aether Bolt",
                "Cycle the Signal Focus and fire the first Aether projectile.",
                "Aether Bolt cast.", MissionObjectiveType.CUSTOM, SpellCoreApi.AETHER_BOLT,
                new ItemStack(ModItems.AETHER_CATALYST.get()));
        registerMission(registry, "cast_ash_veil", "Cast Ash Veil",
                "Cycle the Signal Focus and cloak yourself in ash signal static.",
                "Ash Veil cast.", MissionObjectiveType.CUSTOM, SpellCoreApi.ASH_VEIL,
                new ItemStack(ModItems.ASH_CATALYST.get()));
        registerMission(registry, "cast_void_step", "Cast Void Step",
                "Configure a Void-school slot and blink through a safe short vector.",
                "Void Step cast.", MissionObjectiveType.CUSTOM, SpellCoreApi.VOID_STEP,
                new ItemStack(ModItems.VOID_CATALYST.get()));
        registerMission(registry, "cast_storm_lance", "Cast Storm Lance",
                "Fire a Storm-school projectile through the synchronized projectile layer.",
                "Storm Lance cast.", MissionObjectiveType.CUSTOM, SpellCoreApi.STORM_LANCE,
                new ItemStack(ModItems.STORM_CATALYST.get()));
        registerMission(registry, "cast_crystal_wall", "Cast Crystal Wall",
                "Use Refined Aether to raise a Crystal defensive projection.",
                "Crystal Wall cast.", MissionObjectiveType.CUSTOM, SpellCoreApi.CRYSTAL_WALL,
                new ItemStack(ModItems.CRYSTAL_CATALYST.get()));
        registerMission(registry, "cast_blood_surge", "Cast Blood Surge",
                "Route damage and cursed aether through the first Blood-school amplifier.",
                "Blood Surge cast.", MissionObjectiveType.CUSTOM, SpellCoreApi.BLOOD_SURGE,
                new ItemStack(ModItems.BLOOD_CATALYST.get()));
        registerMission(registry, "cast_rift_blink", "Cast Rift Blink",
                "Use Rift Aether to move through a short calibrated exit trace.",
                "Rift Blink cast.", MissionObjectiveType.CUSTOM, SpellCoreApi.RIFT_BLINK,
                new ItemStack(ModItems.RIFT_CATALYST.get()));
        registerMission(registry, "cast_soul_thread", "Cast Soul Thread",
                "Latch a Soul-school thread for healing or hostile signal suppression.",
                "Soul Thread cast.", MissionObjectiveType.CUSTOM, SpellCoreApi.SOUL_THREAD,
                new ItemStack(ModItems.SOUL_CATALYST.get()));
        registerMission(registry, "cast_decay_touch", "Cast Decay Touch",
                "Apply the first Decay-school contact curse to a living target.",
                "Decay Touch cast.", MissionObjectiveType.CUSTOM, SpellCoreApi.DECAY_TOUCH,
                new ItemStack(ModItems.DECAY_CATALYST.get()));
        registerMission(registry, "cast_veil_trace", "Cast Veil Trace",
                "Spend Veil Resonance to expose a hidden signature or target trace.",
                "Veil Trace cast.", MissionObjectiveType.CUSTOM, SpellCoreApi.VEIL_TRACE,
                new ItemStack(ModItems.SIGNAL_CATALYST.get()));
        registerMission(registry, "cast_fracture_shear", "Cast Fracture Shear",
                "Fire a Fracture-school projectile and watch the stabilized trail.",
                "Fracture Shear cast.", MissionObjectiveType.CUSTOM, SpellCoreApi.FRACTURE_SHEAR,
                new ItemStack(ModItems.RIFT_CATALYST.get()));
        registerMission(registry, "configure_spell_deck", "Configure Spell Deck",
                "Open the Spell Deck and assign a spell or active slot.",
                "Loadout slot configured.", MissionObjectiveType.CUSTOM, id("loadout_configured"),
                new ItemStack(ModItems.SPELL_DECK.get()));
        registerMission(registry, "fire_spell_projectile", "Fire Spell Projectile",
                "Cast Aether Bolt or Dust Lance through the synchronized projectile layer.",
                "Projectile spell fired.", MissionObjectiveType.CUSTOM, id("spell_projectile_fired"),
                new ItemStack(ModItems.AETHER_CATALYST.get()));
        registerMission(registry, "install_spell_modifier", "Install Spell Modifier",
                "Toggle Range, Efficiency, or Overcharge on a Spell Deck slot.",
                "Modifier bus updated.", MissionObjectiveType.CUSTOM, id("loadout_configured"),
                new ItemStack(ModItems.OVERCHARGED_SPELL_CORE.get()));
        registerMission(registry, "manage_cooldown", "Check Spell Cooldown",
                "Attempt a spell while it is cooling down and read the HUD/Terminal feedback.",
                "Cooldown handled.", MissionObjectiveType.CUSTOM, id("cooldown_checked"),
                new ItemStack(ModItems.SPELL_DECK.get()));
    }

    private static void registerMission(IMissionRegistry registry, String path, String title, String briefing,
            String fieldGuide, MissionObjectiveType type, Identifier target, ItemStack icon) {
        Identifier missionId = id("arcana_spellcore/" + path);
        ObjectiveDefinition objective = new ObjectiveDefinition(
                id("arcana_spellcore/" + path + "_objective"),
                type,
                title,
                "",
                icon,
                1,
                false,
                Map.of("target", target.toString()));
        RewardDefinition reward = RewardDefinition.item(
                id("arcana_spellcore/" + path + "_reward"),
                MissionRewardClaimMode.CLAIMABLE,
                rewardStack(path));
        MissionDefinition mission = MissionDefinition.builder(missionId, CHAPTER_ID)
                .phase(phaseId(path), phaseTitle(path), phaseOrder(path), missionOrder(path))
                .text(title, briefing, fieldGuide)
                .category("SpellCore", "Arcana")
                .icon(icon)
                .objective(objective)
                .reward(reward)
                .kind(MissionKind.SIDE_OP)
                .build();
        registry.registerMission(EchoSpellCore.MODID, mission);
    }

    private static void registerEventHooks() {
        SpellCoreEvents.onCast(event -> EchoCoreServices.recordMissionObjective(
                event.player(),
                MissionObjectiveType.CUSTOM,
                event.spellId(),
                1,
                Map.of("source", EchoSpellCore.MODID, "action", "cast")));
    }

    private static void registerHookCoverage() {
        EchoCoreServices.registerMissionHookCoverage(EchoSpellCore.MODID, id("arcana_spellcore/craft_signal_focus"), id("signal_focus"));
        EchoCoreServices.registerMissionHookCoverage(EchoSpellCore.MODID, id("arcana_spellcore/carry_awakened_spell_core"), Identifier.fromNamespaceAndPath("echoritualcore", "awakened_spell_core"));
        EchoCoreServices.registerMissionHookCoverage(EchoSpellCore.MODID, id("arcana_spellcore/cast_signal_pulse"), SpellCoreApi.SIGNAL_PULSE);
        EchoCoreServices.registerMissionHookCoverage(EchoSpellCore.MODID, id("arcana_spellcore/cast_aether_bolt"), SpellCoreApi.AETHER_BOLT);
        EchoCoreServices.registerMissionHookCoverage(EchoSpellCore.MODID, id("arcana_spellcore/cast_ash_veil"), SpellCoreApi.ASH_VEIL);
        EchoCoreServices.registerMissionHookCoverage(EchoSpellCore.MODID, id("arcana_spellcore/cast_void_step"), SpellCoreApi.VOID_STEP);
        EchoCoreServices.registerMissionHookCoverage(EchoSpellCore.MODID, id("arcana_spellcore/cast_storm_lance"), SpellCoreApi.STORM_LANCE);
        EchoCoreServices.registerMissionHookCoverage(EchoSpellCore.MODID, id("arcana_spellcore/cast_crystal_wall"), SpellCoreApi.CRYSTAL_WALL);
        EchoCoreServices.registerMissionHookCoverage(EchoSpellCore.MODID, id("arcana_spellcore/cast_blood_surge"), SpellCoreApi.BLOOD_SURGE);
        EchoCoreServices.registerMissionHookCoverage(EchoSpellCore.MODID, id("arcana_spellcore/cast_rift_blink"), SpellCoreApi.RIFT_BLINK);
        EchoCoreServices.registerMissionHookCoverage(EchoSpellCore.MODID, id("arcana_spellcore/cast_soul_thread"), SpellCoreApi.SOUL_THREAD);
        EchoCoreServices.registerMissionHookCoverage(EchoSpellCore.MODID, id("arcana_spellcore/cast_decay_touch"), SpellCoreApi.DECAY_TOUCH);
        EchoCoreServices.registerMissionHookCoverage(EchoSpellCore.MODID, id("arcana_spellcore/cast_veil_trace"), SpellCoreApi.VEIL_TRACE);
        EchoCoreServices.registerMissionHookCoverage(EchoSpellCore.MODID, id("arcana_spellcore/cast_fracture_shear"), SpellCoreApi.FRACTURE_SHEAR);
        EchoCoreServices.registerMissionHookCoverage(EchoSpellCore.MODID, id("arcana_spellcore/configure_spell_deck"), id("loadout_configured"));
        EchoCoreServices.registerMissionHookCoverage(EchoSpellCore.MODID, id("arcana_spellcore/fire_spell_projectile"), id("spell_projectile_fired"));
        EchoCoreServices.registerMissionHookCoverage(EchoSpellCore.MODID, id("arcana_spellcore/install_spell_modifier"), id("loadout_configured"));
        EchoCoreServices.registerMissionHookCoverage(EchoSpellCore.MODID, id("arcana_spellcore/manage_cooldown"), id("cooldown_checked"));
    }

    private static String phaseId(String path) {
        return switch (path) {
            case "craft_signal_focus", "carry_awakened_spell_core" -> "focus_calibration";
            case "cast_signal_pulse", "cast_aether_bolt", "cast_ash_veil" -> "first_cast";
            case "configure_spell_deck", "install_spell_modifier", "manage_cooldown" -> "loadout_building";
            case "fire_spell_projectile", "cast_void_step", "cast_storm_lance", "cast_crystal_wall",
                    "cast_blood_surge", "cast_rift_blink", "cast_soul_thread", "cast_decay_touch",
                    "cast_veil_trace", "cast_fracture_shear" -> "school_mastery";
            default -> "spellcore";
        };
    }

    private static String phaseTitle(String path) {
        return switch (phaseId(path)) {
            case "focus_calibration" -> "Focus Calibration";
            case "first_cast" -> "First Cast";
            case "loadout_building" -> "Loadout Building";
            case "school_mastery" -> "School Mastery";
            default -> "SpellCore";
        };
    }

    private static int phaseOrder(String path) {
        return switch (phaseId(path)) {
            case "focus_calibration" -> 10;
            case "first_cast" -> 20;
            case "loadout_building" -> 30;
            case "school_mastery" -> 40;
            default -> 99;
        };
    }

    private static int missionOrder(String path) {
        return switch (path) {
            case "craft_signal_focus" -> 10;
            case "carry_awakened_spell_core" -> 20;
            case "cast_signal_pulse" -> 30;
            case "cast_aether_bolt" -> 40;
            case "cast_ash_veil" -> 50;
            case "configure_spell_deck" -> 60;
            case "install_spell_modifier" -> 70;
            case "fire_spell_projectile" -> 80;
            case "cast_void_step" -> 90;
            case "cast_storm_lance" -> 100;
            case "cast_crystal_wall" -> 110;
            case "cast_blood_surge" -> 120;
            case "cast_rift_blink" -> 130;
            case "cast_soul_thread" -> 140;
            case "cast_decay_touch" -> 150;
            case "cast_veil_trace" -> 160;
            case "cast_fracture_shear" -> 170;
            case "manage_cooldown" -> 180;
            default -> 99;
        };
    }

    private static ItemStack rewardStack(String path) {
        return switch (path) {
            case "craft_signal_focus" -> new ItemStack(ModItems.SIGNAL_CATALYST.get(), 2);
            case "carry_awakened_spell_core" -> new ItemStack(ModItems.BLANK_SPELL_CORE.get(), 1);
            case "cast_signal_pulse" -> new ItemStack(ModItems.AETHER_CATALYST.get(), 1);
            case "cast_aether_bolt" -> new ItemStack(ModItems.ASH_CATALYST.get(), 1);
            case "cast_ash_veil" -> new ItemStack(ModItems.ENGRAVED_SPELL_CORE.get(), 1);
            case "cast_void_step" -> new ItemStack(ModItems.VOID_CATALYST.get(), 1);
            case "cast_storm_lance" -> new ItemStack(ModItems.STORM_CATALYST.get(), 1);
            case "cast_crystal_wall" -> new ItemStack(ModItems.CRYSTAL_CATALYST.get(), 1);
            case "cast_blood_surge" -> new ItemStack(ModItems.BLOOD_CATALYST.get(), 1);
            case "cast_rift_blink" -> new ItemStack(ModItems.RIFT_CATALYST.get(), 1);
            case "cast_soul_thread" -> new ItemStack(ModItems.SOUL_CATALYST.get(), 1);
            case "cast_decay_touch" -> new ItemStack(ModItems.DECAY_CATALYST.get(), 1);
            case "cast_veil_trace" -> new ItemStack(ModItems.SIGNAL_CATALYST.get(), 1);
            case "cast_fracture_shear" -> new ItemStack(ModItems.RIFT_CATALYST.get(), 1);
            case "configure_spell_deck" -> new ItemStack(ModItems.BLANK_SPELL_CORE.get(), 1);
            case "install_spell_modifier" -> new ItemStack(ModItems.OVERCHARGED_SPELL_CORE.get(), 1);
            case "fire_spell_projectile" -> new ItemStack(ModItems.AETHER_CATALYST.get(), 2);
            default -> new ItemStack(ModItems.SIGNAL_CATALYST.get(), 1);
        };
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoSpellCore.MODID, path);
    }
}
