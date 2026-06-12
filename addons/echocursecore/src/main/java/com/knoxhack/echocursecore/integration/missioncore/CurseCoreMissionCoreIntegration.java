package com.knoxhack.echocursecore.integration.missioncore;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.mission.IMissionRegistry;
import com.echoplatform.echocore.api.mission.MissionChapterDefinition;
import com.echoplatform.echocore.api.mission.MissionDefinition;
import com.echoplatform.echocore.api.mission.MissionKind;
import com.echoplatform.echocore.api.mission.MissionObjectiveType;
import com.echoplatform.echocore.api.mission.MissionRewardClaimMode;
import com.echoplatform.echocore.api.mission.ObjectiveDefinition;
import com.echoplatform.echocore.api.mission.RewardDefinition;
import com.knoxhack.echocursecore.EchoCurseCore;
import com.knoxhack.echocursecore.api.CurseCoreApi;
import com.knoxhack.echocursecore.api.CurseCoreEvents;
import com.knoxhack.echocursecore.registry.ModItems;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public final class CurseCoreMissionCoreIntegration {
    private static final Identifier CHAPTER_ID = id("arcana_cursecore");

    private CurseCoreMissionCoreIntegration() {
    }

    public static void register() {
        EchoCurseCore.LOGGER.info("ECHO MissionCore integration loaded for CurseCore.");
        EchoCoreServices.registerMissionContent(EchoCurseCore.MODID, CurseCoreMissionCoreIntegration::registerContent);
        registerEventHooks();
        registerHookCoverage();
    }

    private static void registerContent(IMissionRegistry registry) {
        registry.registerChapter(EchoCurseCore.MODID, new MissionChapterDefinition(
                CHAPTER_ID,
                "ECHO: CurseCore",
                "Identify symptoms, survive a controlled curse, and cleanse it through ritual engineering.",
                90,
                0xE05A7A));
        registerMission(registry, "gain_echo_rot", "Gain Echo Rot",
                "Use Echo Rot Sample or trigger signal backlash to create a live curse target.",
                "Echo Rot signature recorded.", MissionObjectiveType.CUSTOM, CurseCoreApi.ECHO_ROT,
                new ItemStack(ModItems.ECHO_ROT_SAMPLE.get()));
        registerMission(registry, "cleanse_minor_curse", "Cleanse Minor Curse",
                "Reduce an active curse stage with RitualCore Curse Cleansing I.",
                "Curse stage reduced.", MissionObjectiveType.CUSTOM, CurseCoreApi.CURSE_CLEANSED,
                new ItemStack(ModItems.PURIFIED_CURSE_ASH.get()));
    }

    private static void registerMission(IMissionRegistry registry, String path, String title, String briefing,
            String fieldGuide, MissionObjectiveType type, Identifier target, ItemStack icon) {
        Identifier missionId = id("arcana_cursecore/" + path);
        ObjectiveDefinition objective = new ObjectiveDefinition(
                id("arcana_cursecore/" + path + "_objective"),
                type,
                title,
                "",
                icon,
                1,
                false,
                Map.of("target", target.toString()));
        RewardDefinition reward = RewardDefinition.item(
                id("arcana_cursecore/" + path + "_reward"),
                MissionRewardClaimMode.CLAIMABLE,
                rewardStack(path));
        MissionDefinition mission = MissionDefinition.builder(missionId, CHAPTER_ID)
                .phase(phaseId(path), phaseTitle(path), phaseOrder(path), missionOrder(path))
                .text(title, briefing, fieldGuide)
                .category("CurseCore", "Arcana")
                .icon(icon)
                .objective(objective)
                .reward(reward)
                .kind(MissionKind.SIDE_OP)
                .build();
        registry.registerMission(EchoCurseCore.MODID, mission);
    }

    private static void registerEventHooks() {
        CurseCoreEvents.onGained(event -> EchoCoreServices.recordMissionObjective(
                event.player(), MissionObjectiveType.CUSTOM, event.curseId(), 1,
                Map.of("source", EchoCurseCore.MODID, "stage", Integer.toString(event.stage()))));
        CurseCoreEvents.onCleansed(event -> EchoCoreServices.recordMissionObjective(
                event.player(), MissionObjectiveType.CUSTOM, CurseCoreApi.CURSE_CLEANSED, 1,
                Map.of("source", EchoCurseCore.MODID, "curse", event.curseId().toString())));
    }

    private static void registerHookCoverage() {
        EchoCoreServices.registerMissionHookCoverage(EchoCurseCore.MODID, id("arcana_cursecore/gain_echo_rot"), CurseCoreApi.ECHO_ROT);
        EchoCoreServices.registerMissionHookCoverage(EchoCurseCore.MODID, id("arcana_cursecore/cleanse_minor_curse"), CurseCoreApi.CURSE_CLEANSED);
    }

    private static String phaseId(String path) {
        return switch (path) {
            case "gain_echo_rot" -> "first_symptom";
            case "cleanse_minor_curse" -> "cleansing";
            default -> "cursecore";
        };
    }

    private static String phaseTitle(String path) {
        return switch (phaseId(path)) {
            case "first_symptom" -> "First Symptom";
            case "cleansing" -> "Cleansing";
            default -> "CurseCore";
        };
    }

    private static int phaseOrder(String path) {
        return switch (phaseId(path)) {
            case "first_symptom" -> 10;
            case "cleansing" -> 30;
            default -> 99;
        };
    }

    private static int missionOrder(String path) {
        return switch (path) {
            case "gain_echo_rot" -> 10;
            case "cleanse_minor_curse" -> 20;
            default -> 99;
        };
    }

    private static ItemStack rewardStack(String path) {
        return switch (path) {
            case "gain_echo_rot" -> new ItemStack(ModItems.CURSE_DIAGNOSTIC_SLIP.get(), 1);
            case "cleanse_minor_curse" -> new ItemStack(ModItems.PURIFIED_CURSE_ASH.get(), 1);
            default -> new ItemStack(ModItems.CURSE_DIAGNOSTIC_SLIP.get(), 1);
        };
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoCurseCore.MODID, path);
    }
}
