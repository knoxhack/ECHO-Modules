package com.knoxhack.echoarcanacore.integration;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.mission.IMissionRegistry;
import com.echoplatform.echocore.api.mission.MissionChapterDefinition;
import com.echoplatform.echocore.api.mission.MissionDefinition;
import com.echoplatform.echocore.api.mission.MissionHookTargets;
import com.echoplatform.echocore.api.mission.MissionKind;
import com.echoplatform.echocore.api.mission.MissionObjectiveType;
import com.echoplatform.echocore.api.mission.ObjectiveDefinition;
import com.knoxhack.echoarcanacore.EchoArcanaCore;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class ArcanaCoreMissionIntegration {
    private static final Identifier CHAPTER = id("arcana_division");

    private ArcanaCoreMissionIntegration() {
    }

    public static void register() {
        EchoCoreServices.registerMissionContent(EchoArcanaCore.MODID, ArcanaCoreMissionIntegration::registerContent);
        registerHookCoverage();
    }

    private static void registerContent(IMissionRegistry registry) {
        registry.registerChapter(EchoArcanaCore.MODID, new MissionChapterDefinition(
                CHAPTER,
                "ECHO: Arcana Division",
                "Decode the hidden operating system behind reality through signal science, relics, rituals, and forbidden archives.",
                90,
                0x7DE6D1));
        mission(registry, "first_signal", "A1", "First Signal", 10, "First Arcane Signal",
                "Detect the first Aether Signal through Lens, Index, Grimoire, or Field Journal handoff.",
                MissionObjectiveType.UNLOCK_RESEARCH, "first_arcane_scan", new ItemStack(Items.SPYGLASS));
        mission(registry, "veilbound_studies", "A2", "Veilbound Studies", 20, "Open Veilbound Bridge",
                "ARCANA: Veilbound Studies remains its own campaign and reports Veil research into the division when present.",
                MissionObjectiveType.UNLOCK_RESEARCH, "veilbound_bridge", new ItemStack(Items.WRITABLE_BOOK));
        mission(registry, "relic_recovery", "A3", "Relic Recovery", 30, "Recover Unknown Relics",
                "Route RelicTech discoveries through Arcana Core without making JEI the knowledge layer.",
                MissionObjectiveType.OBTAIN_ITEM, "unknown_relic", new ItemStack(Items.ECHO_SHARD));
        mission(registry, "ritual_engineering", "A4", "Ritual Engineering", 40, "Engineer First Ritual Circuit",
                "Prepare the shared RitualCore route while preserving Veilbound ritual ownership.",
                MissionObjectiveType.CUSTOM, "ritual_engineering", new ItemStack(Items.CRYING_OBSIDIAN));
        mission(registry, "spell_focus", "A5", "Spell Focus", 50, "Calibrate Spell Focus",
                "Reserve SpellCore progression for focus casting, loadouts, and corruption risk.",
                MissionObjectiveType.CUSTOM, "spell_focus", new ItemStack(Items.AMETHYST_SHARD));
        mission(registry, "curse_consequences", "A6", "Curse Consequences", 60, "Document First Curse Consequence",
                "Route curses through official Index and Grimoire warnings before contracts or cleansing.",
                MissionObjectiveType.CUSTOM, "curse_consequence", new ItemStack(Items.SCULK));
        mission(registry, "aether_automation", "A7", "Aether Automation", 70, "Prototype Aether Machine",
                "Reserve AetherWorks progression for aether storage, transfer, contamination, and overload.",
                MissionObjectiveType.CUSTOM, "aether_machine", new ItemStack(Items.REDSTONE));
        mission(registry, "rift_exploration", "A8", "Rift Exploration", 80, "Find First Rift Signal",
                "Prepare RiftWorlds as a Lens, HoloMap, ritual, and relic-driven exploration route.",
                MissionObjectiveType.DISCOVER_STRUCTURE, "rift_signal", new ItemStack(Items.RECOVERY_COMPASS));
        mission(registry, "familiar_bond", "A9", "Familiar Bond", 90, "Prepare Familiar Bond",
                "Reserve FamiliarCore progression for companions, commands, bond state, and curse contamination.",
                MissionObjectiveType.CUSTOM, "familiar_bond", new ItemStack(Items.NAME_TAG));
        mission(registry, "forbidden_mastery", "A10", "Forbidden Mastery", 100, "Read Forbidden Warning",
                "Keep forbidden knowledge warning-gated through Grimoire and Arcane Index.",
                MissionObjectiveType.CUSTOM, "forbidden_warning", new ItemStack(Items.ENCHANTED_BOOK));
    }

    private static void mission(IMissionRegistry registry, String path, String phaseId, String phaseTitle, int order,
            String title, String briefing, MissionObjectiveType type, String targetPath, ItemStack icon) {
        Identifier mission = id(path);
        Identifier target = MissionHookTargets.objectiveTarget(EchoArcanaCore.MODID, mission, targetPath);
        registry.registerMission(EchoArcanaCore.MODID, MissionDefinition.builder(mission, CHAPTER)
                .phase(phaseId.toLowerCase(java.util.Locale.ROOT), phaseTitle, order, order)
                .text(title, briefing, "Arcana Division records official progression through Index, Terminal, Grimoire, Lens, HoloMap, and addon bridges.")
                .category("Arcana Division", "Main")
                .icon(icon)
                .kind(MissionKind.MAIN)
                .metadata("terminal_route_role", "OPTIONAL")
                .metadata("terminal_route_visible", "false")
                .objective(new ObjectiveDefinition(
                        id(path + "/objective"),
                        type,
                        title,
                        "",
                        icon,
                        1,
                        false,
                        Map.of("target", target.toString())))
                .build());
    }

    private static void registerHookCoverage() {
        EchoCoreServices.registerMissionHookCoverage(EchoArcanaCore.MODID, id("first_signal"),
                MissionHookTargets.objectiveTarget(EchoArcanaCore.MODID, id("first_signal"), "first_arcane_scan"));
        EchoCoreServices.registerMissionHookCoverage(EchoArcanaCore.MODID, id("veilbound_studies"),
                MissionHookTargets.objectiveTarget(EchoArcanaCore.MODID, id("veilbound_studies"), "veilbound_bridge"));
        EchoCoreServices.registerMissionHookCoverage(EchoArcanaCore.MODID, id("relic_recovery"),
                MissionHookTargets.objectiveTarget(EchoArcanaCore.MODID, id("relic_recovery"), "unknown_relic"));
        EchoCoreServices.registerMissionHookCoverage(EchoArcanaCore.MODID, id("ritual_engineering"),
                MissionHookTargets.objectiveTarget(EchoArcanaCore.MODID, id("ritual_engineering"), "ritual_engineering"));
        EchoCoreServices.registerMissionHookCoverage(EchoArcanaCore.MODID, id("spell_focus"),
                MissionHookTargets.objectiveTarget(EchoArcanaCore.MODID, id("spell_focus"), "spell_focus"));
        EchoCoreServices.registerMissionHookCoverage(EchoArcanaCore.MODID, id("curse_consequences"),
                MissionHookTargets.objectiveTarget(EchoArcanaCore.MODID, id("curse_consequences"), "curse_consequence"));
        EchoCoreServices.registerMissionHookCoverage(EchoArcanaCore.MODID, id("aether_automation"),
                MissionHookTargets.objectiveTarget(EchoArcanaCore.MODID, id("aether_automation"), "aether_machine"));
        EchoCoreServices.registerMissionHookCoverage(EchoArcanaCore.MODID, id("rift_exploration"),
                MissionHookTargets.objectiveTarget(EchoArcanaCore.MODID, id("rift_exploration"), "rift_signal"));
        EchoCoreServices.registerMissionHookCoverage(EchoArcanaCore.MODID, id("familiar_bond"),
                MissionHookTargets.objectiveTarget(EchoArcanaCore.MODID, id("familiar_bond"), "familiar_bond"));
        EchoCoreServices.registerMissionHookCoverage(EchoArcanaCore.MODID, id("forbidden_mastery"),
                MissionHookTargets.objectiveTarget(EchoArcanaCore.MODID, id("forbidden_mastery"), "forbidden_warning"));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoArcanaCore.MODID, path);
    }
}
