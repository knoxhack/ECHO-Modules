package com.knoxhack.echorecovery.integration;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.mission.IMissionRegistry;
import com.knoxhack.echocore.api.mission.MissionChapterDefinition;
import com.knoxhack.echocore.api.mission.MissionDefinition;
import com.knoxhack.echocore.api.mission.MissionKind;
import com.knoxhack.echocore.api.mission.MissionObjectiveType;
import com.knoxhack.echocore.api.mission.ObjectiveDefinition;
import com.knoxhack.echocore.api.mission.RewardDefinition;
import com.knoxhack.echorecovery.EchoRecovery;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class RecoveryMissionCoreIntegration {
    private static boolean registered;
    private static final Identifier CHAPTER = id("chapter/recovery");
    private static final Identifier FIRST_RECOVERY = id("mission/first_recovery");
    private static final Identifier COMPASS = id("mission/recovery_compass");
    private static final Identifier REMOTE = id("mission/remote_recovery");
    private static final Identifier TEAM = id("mission/team_recovery");
    private static final Identifier ASHFALL_OUTPOST =
            Identifier.fromNamespaceAndPath("echoashfallprotocol", "secure_crash_outpost");
    private static final Identifier ASHFALL_SLEEP_SHELTER =
            Identifier.fromNamespaceAndPath("echoashfallprotocol", "secure_sleep_shelter");

    private RecoveryMissionCoreIntegration() {}

    public static void registerCommon() {
        if (registered) {
            return;
        }
        registered = true;
        EchoCoreServices.registerMissionContent(EchoRecovery.MODID, RecoveryMissionCoreIntegration::registerContent);
        EchoCoreServices.registerMissionHookCoverage(EchoRecovery.MODID, FIRST_RECOVERY, id("objective/grave_recovered"));
        EchoCoreServices.registerMissionHookCoverage(EchoRecovery.MODID, COMPASS, id("objective/compass_carried"));
        EchoRecovery.LOGGER.info("Recovery MissionCore content registered.");
    }

    public static void recordRecovered(ServerPlayer player) {
        EchoCoreServices.recordMissionObjective(player, MissionObjectiveType.CUSTOM, id("objective/grave_recovered"), 1);
    }

    private static void registerContent(IMissionRegistry registry) {
        registry.registerChapter(EchoRecovery.MODID, new MissionChapterDefinition(CHAPTER,
                "ECHO Recovery", "Death recovery, graves, field caches, and route support tools.", 42, 0x66D9EF));
        registry.registerMission(EchoRecovery.MODID, mission(FIRST_RECOVERY, 1, "First Grave Recovery",
                "Recover one protected grave or field cache.", "Open your grave and use Recover All or move items manually.",
                ItemStack.EMPTY, id("objective/grave_recovered")));
        registry.registerMission(EchoRecovery.MODID, mission(COMPASS, 2, "Compass Guidance",
                "Carry or craft a Recovery Compass.", "The compass shows nearest-cache status and distance when synced.",
                ItemStack.EMPTY, id("objective/compass_carried")));
        registry.registerMission(EchoRecovery.MODID, mission(REMOTE, 3, "Remote Recovery",
                "Optional remote recovery support.", "Disabled by default; enabled servers can recover by grave id.",
                ItemStack.EMPTY, id("objective/remote_recovered")));
        registry.registerMission(EchoRecovery.MODID, mission(TEAM, 4, "Shared Recovery",
                "Share or team-enable recovery access.", "Use /graves share <player> or enable team_access.",
                ItemStack.EMPTY, id("objective/team_recovery")));
    }

    private static MissionDefinition mission(Identifier id, int order, String title, String briefing,
            String guide, ItemStack icon, Identifier objective) {
        return MissionDefinition.builder(id, CHAPTER)
                .phase("survival_support", "Survival Support", 2, order)
                .text(title, briefing, guide)
                .category("Recovery Support", "Guided")
                .icon(icon)
                .kind(MissionKind.SIDE_OP)
                .metadata("terminal_route_phase", "1")
                .metadata("terminal_route_order", Integer.toString(order))
                .metadata("terminal_route_role", "OPTIONAL")
                .metadata("terminal_route_visible", "true")
                .metadata("terminal_route_anchor", routeAnchor(id).toString())
                .objective(ObjectiveDefinition.simple(objective, MissionObjectiveType.CUSTOM, title, briefing, icon, 1))
                .reward(RewardDefinition.text(id("reward/" + id.getPath().substring(id.getPath().lastIndexOf('/') + 1)),
                        "Recovery Guidance", guide))
                .build();
    }

    private static Identifier routeAnchor(Identifier id) {
        if (FIRST_RECOVERY.equals(id) || COMPASS.equals(id)) {
            return ASHFALL_OUTPOST;
        }
        return ASHFALL_SLEEP_SHELTER;
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoRecovery.MODID, path);
    }
}
