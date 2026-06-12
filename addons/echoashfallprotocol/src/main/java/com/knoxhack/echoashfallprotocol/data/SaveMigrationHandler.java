package com.knoxhack.echoashfallprotocol.data;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.EchoFactionDataService;
import com.echoplatform.echocore.api.mission.MissionDefinition;
import com.echoplatform.echocore.api.mission.MissionStatus;
import com.echoplatform.echocore.api.mission.ObjectiveDefinition;
import com.echoplatform.echocore.api.mission.RewardDefinition;
import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echoashfallprotocol.echo.AshfallMissionRoute;
import com.knoxhack.echoashfallprotocol.echo.QuestData;
import com.knoxhack.echoashfallprotocol.integration.AshfallMissionCoreIntegration;
import com.knoxhack.echoashfallprotocol.registry.ModAttachments;
import com.knoxhack.echoashfallprotocol.research.ResearchData;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

/**
 * Handles save migration for ECHO exploration progress.
 * 
 * Migration rules:
 * - Existing saves: Neutral reputation (0) for all factions
 * - Existing saves: 0 research points, no perks unlocked
 * - Existing recipes: Remain unlocked (no gating retroactive)
 * - New Tier 2+ recipes: Require schematic fragments
 */
public class SaveMigrationHandler {

    // Migration version - increment when data format changes
    public static final int CURRENT_MIGRATION_VERSION = 4;
    private static final String MISSIONCORE_ALIAS_MIGRATION_SOURCE = "ashfall_deprecated_aliases_v4";
    private static final String CONTACTED_KEY = "contacted";
    private static final String REPUTATION_KEY = "reputation";
    private static final String COMPLETED_KEY = "completed_contracts";
    private static final String COOLDOWN_KEY = "cooldown_until";
    private static final String MEMORY_KEY = "npc_memory";
    private static final String CONTACT_COUNT_KEY = "contact_count";
    private static final String LAST_INTERACTION_KEY = "last_interaction_tick";
    private static final String LAST_ROLE_KEY = "last_role_id";
    private static final Map<String, String> FACTION_ALIASES = Map.ofEntries(
            Map.entry("echoashfallprotocol:survivor_network", "echoashfallprotocol:radwarden_compact"),
            Map.entry("echoashfallprotocol:ashland_rangers", "echoashfallprotocol:radwarden_compact"),
            Map.entry("echoashfallprotocol:thawbound_collective", "echoashfallprotocol:radwarden_compact"),
            Map.entry("echoashfallprotocol:remnant_collective", "echoashfallprotocol:radwarden_compact"),
            Map.entry("echoashfallprotocol:dustline_freeholds", "echoashfallprotocol:crashbreak_salvage"),
            Map.entry("echoashfallprotocol:metro_archivists", "echoashfallprotocol:crashbreak_salvage"),
            Map.entry("echoashfallprotocol:rustworks_union", "echoashfallprotocol:crashbreak_salvage"),
            Map.entry("echoashfallprotocol:salvager_guild", "echoashfallprotocol:crashbreak_salvage"),
            Map.entry("echocore:survivors", "echoashfallprotocol:crashbreak_salvage"),
            Map.entry("echoashfallprotocol:scarbound_conclave", "echoashfallprotocol:sporebound_sanctum"),
            Map.entry("echoashfallprotocol:mutant_front", "echoashfallprotocol:sporebound_sanctum"),
            Map.entry("echoorbitalremnants:orbital_remnants", "echoashfallprotocol:radwarden_compact"),
            Map.entry("echoorbitalremnants:void_salvagers", "echoashfallprotocol:crashbreak_salvage"),
            Map.entry("echoorbitalremnants:nexus_choir", "echoashfallprotocol:sporebound_sanctum"),
            Map.entry("echoarmory:remnant_collective", "echoashfallprotocol:radwarden_compact"),
            Map.entry("echoarmory:salvager_guild", "echoashfallprotocol:crashbreak_salvage"),
            Map.entry("echoarmory:construct_foundry", "echoashfallprotocol:crashbreak_salvage"));

    public static void onPlayerLogin(Object event) {
        if (!(eventValue(event, "getEntity") instanceof ServerPlayer player)) {
            return;
        }
        ensureCurrent(player, "login");
    }

    public static boolean ensureCurrent(ServerPlayer player, String reason) {
        if (player == null || player.level().isClientSide()) {
            return false;
        }
        MigrationData migration = player.getData(ModAttachments.MIGRATION_DATA);
        if (migration.getVersion() >= CURRENT_MIGRATION_VERSION) {
            return false;
        }
        performMigration(player, migration.getVersion(), reason);
        migration.setVersion(CURRENT_MIGRATION_VERSION);
        return true;
    }

    private static void performMigration(ServerPlayer player, int fromVersion, String reason) {
        EchoAshfallProtocol.LOGGER.info("Performing save migration for player {} from version {} via {}",
            player.getName().getString(), fromVersion, reason == null || reason.isBlank() ? "unknown" : reason);

        // Initialize research data for existing players
        ResearchData research = ResearchData.get(player);
        if (research.getPoints() == 0 && research.getUnlockedPerks().isEmpty()) {
            EchoAshfallProtocol.LOGGER.debug("Initialized research data for {}", 
                player.getName().getString());
        }

        if (fromVersion < 3) {
            migrateFactionAliases(player);
        }
        if (fromVersion < 4) {
            migrateDeprecatedMissionAliases(player);
        }

        // Send notification to player about new content
        player.sendSystemMessage(Component.translatable(
            "message.EchoAshfallProtocol.migration_complete",
            "v1.3.1 three-faction alignment"
        ));

        EchoAshfallProtocol.LOGGER.info("Save migration completed for player {}", 
            player.getName().getString());
    }

    private static void migrateFactionAliases(ServerPlayer player) {
        CompoundTag root = EchoFactionDataService.exportRoot(player);
        boolean changed = false;
        for (Map.Entry<String, String> alias : FACTION_ALIASES.entrySet()) {
            if (!root.contains(alias.getKey())) {
                continue;
            }
            CompoundTag source = root.getCompoundOrEmpty(alias.getKey()).copy();
            CompoundTag target = root.getCompoundOrEmpty(alias.getValue()).copy();
            mergeFactionTag(target, source);
            root.put(alias.getValue(), target);
            root.remove(alias.getKey());
            changed = true;
        }
        if (changed) {
            EchoFactionDataService.importRoot(player, root);
            EchoCoreServices.syncFactionDataToClient(player);
        }
    }

    private static void migrateDeprecatedMissionAliases(ServerPlayer player) {
        QuestData quest = QuestData.get(player);
        if (quest.repairMissionState(player)) {
            QuestData.saveAndSync(player, quest);
        }
        migrateMissionCoreDeprecatedAliases(player, quest);
    }

    private static boolean migrateMissionCoreDeprecatedAliases(ServerPlayer player, QuestData quest) {
        if (!EchoCoreServices.missionCoreAvailable()) {
            return false;
        }
        Object data = missionCorePlayerData(player);
        if (data == null || missionCoreHasMigrated(data, MISSIONCORE_ALIAS_MIGRATION_SOURCE)) {
            return false;
        }

        boolean changed = false;
        for (String replacementMissionId : AshfallMissionRoute.replacementMissionIds()) {
            Identifier replacementId = AshfallMissionCoreIntegration.missionId(replacementMissionId);
            if (missionCoreStateTerminal(data, replacementId)) {
                continue;
            }
            boolean questCompleted = quest != null && quest.isMissionCompleted(replacementMissionId);
            boolean aliasStatesCompleted = missionCoreAliasStatesComplete(data,
                    AshfallMissionRoute.deprecatedAliasesFor(replacementMissionId));
            if (questCompleted || aliasStatesCompleted) {
                changed |= markMissionCoreMissionClaimed(player, data, replacementId);
            }
        }

        if (changed) {
            missionCoreMarkMigrated(data, MISSIONCORE_ALIAS_MIGRATION_SOURCE);
            missionCoreSaveAndSync(player, data);
        }
        return changed;
    }

    private static boolean missionCoreAliasStatesComplete(Object data, Iterable<String> aliases) {
        boolean sawAlias = false;
        for (String alias : aliases) {
            sawAlias = true;
            if (!missionCoreStateTerminal(data, AshfallMissionCoreIntegration.missionId(alias))) {
                return false;
            }
        }
        return sawAlias;
    }

    private static boolean markMissionCoreMissionClaimed(ServerPlayer player, Object data, Identifier missionId) {
        MissionDefinition definition = EchoCoreServices.missionService()
                .missionDefinition(missionId)
                .orElse(null);
        if (definition == null) {
            return false;
        }
        try {
            Object state = data.getClass().getMethod("state", Identifier.class).invoke(data, missionId);
            for (ObjectiveDefinition objective : definition.objectives()) {
                state.getClass().getMethod("setObjectiveProgress", Identifier.class, int.class)
                        .invoke(state, objective.id(), objective.required());
                state.getClass().getMethod("revealObjective", Identifier.class).invoke(state, objective.id());
            }
            for (RewardDefinition reward : definition.rewards()) {
                state.getClass().getMethod("claimReward", Identifier.class).invoke(state, reward.id());
            }
            state.getClass().getMethod("status", MissionStatus.class).invoke(state, MissionStatus.CLAIMED);
            state.getClass().getMethod("incrementRepeatCompletions").invoke(state);
            state.getClass().getMethod("lastCompletedGameTime", long.class).invoke(state, player.level().getGameTime());
            return true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            EchoAshfallProtocol.LOGGER.warn("Could not migrate MissionCore deprecated alias state for {}.", missionId, exception);
            return false;
        }
    }

    private static Object missionCorePlayerData(ServerPlayer player) {
        try {
            Class<?> dataClass = Class.forName("com.knoxhack.echomissioncore.storage.MissionPlayerData");
            return dataClass.getMethod("get", net.minecraft.world.entity.player.Player.class).invoke(null, player);
        } catch (ReflectiveOperationException | LinkageError exception) {
            EchoAshfallProtocol.LOGGER.debug("MissionCore player data unavailable during Ashfall alias migration.", exception);
            return null;
        }
    }

    private static boolean missionCoreStateTerminal(Object data, Identifier missionId) {
        try {
            Object state = data.getClass().getMethod("stateIfPresent", Identifier.class).invoke(data, missionId);
            if (state == null) {
                return false;
            }
            Object status = state.getClass().getMethod("status").invoke(state);
            return status == MissionStatus.COMPLETED
                    || status == MissionStatus.CLAIMABLE
                    || status == MissionStatus.CLAIMED;
        } catch (ReflectiveOperationException | LinkageError exception) {
            EchoAshfallProtocol.LOGGER.debug("Could not inspect MissionCore alias state for {}.", missionId, exception);
            return false;
        }
    }

    private static boolean missionCoreHasMigrated(Object data, String source) {
        try {
            Object result = data.getClass().getMethod("hasMigrated", String.class).invoke(data, source);
            return Boolean.TRUE.equals(result);
        } catch (ReflectiveOperationException | LinkageError exception) {
            return false;
        }
    }

    private static void missionCoreMarkMigrated(Object data, String source) {
        try {
            data.getClass().getMethod("markMigrated", String.class).invoke(data, source);
        } catch (ReflectiveOperationException | LinkageError exception) {
            EchoAshfallProtocol.LOGGER.debug("Could not mark MissionCore alias migration source.", exception);
        }
    }

    private static void missionCoreSaveAndSync(ServerPlayer player, Object data) {
        try {
            Class<?> dataClass = Class.forName("com.knoxhack.echomissioncore.storage.MissionPlayerData");
            dataClass.getMethod("saveAndSync", ServerPlayer.class, dataClass).invoke(null, player, data);
        } catch (ReflectiveOperationException | LinkageError exception) {
            EchoAshfallProtocol.LOGGER.warn("Could not save MissionCore alias migration state.", exception);
        }
    }

    private static void mergeFactionTag(CompoundTag target, CompoundTag source) {
        target.putBoolean(CONTACTED_KEY,
                target.getBooleanOr(CONTACTED_KEY, false) || source.getBooleanOr(CONTACTED_KEY, false));
        target.putInt(REPUTATION_KEY,
                Math.max(target.getIntOr(REPUTATION_KEY, 0), source.getIntOr(REPUTATION_KEY, 0)));
        target.putString(COMPLETED_KEY,
                mergeTokens(target.getStringOr(COMPLETED_KEY, ""), source.getStringOr(COMPLETED_KEY, "")));
        target.putLong(COOLDOWN_KEY,
                Math.max(target.getLongOr(COOLDOWN_KEY, 0L), source.getLongOr(COOLDOWN_KEY, 0L)));
        target.putString(MEMORY_KEY,
                mergeMemory(target.getStringOr(MEMORY_KEY, ""), source.getStringOr(MEMORY_KEY, "")));
        target.putInt(CONTACT_COUNT_KEY,
                target.getIntOr(CONTACT_COUNT_KEY, 0) + source.getIntOr(CONTACT_COUNT_KEY, 0));
        if (source.getLongOr(LAST_INTERACTION_KEY, 0L) >= target.getLongOr(LAST_INTERACTION_KEY, 0L)) {
            target.putLong(LAST_INTERACTION_KEY, source.getLongOr(LAST_INTERACTION_KEY, 0L));
            target.putString(LAST_ROLE_KEY, source.getStringOr(LAST_ROLE_KEY, ""));
        }
    }

    private static String mergeTokens(String left, String right) {
        Set<String> tokens = new LinkedHashSet<>();
        addTokens(tokens, left);
        addTokens(tokens, right);
        return String.join("|", tokens);
    }

    private static void addTokens(Set<String> tokens, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        for (String token : value.split("\\|")) {
            if (!token.isBlank()) {
                tokens.add(token);
            }
        }
    }

    private static String mergeMemory(String left, String right) {
        if (right == null || right.isBlank()) {
            return left == null ? "" : left;
        }
        if (left == null || left.isBlank()) {
            return right;
        }
        return left.contains(right) ? left : left + " | " + right;
    }

    private static Object eventValue(Object event, String methodName) {
        if (event == null) {
            return null;
        }
        try {
            return event.getClass().getMethod(methodName).invoke(event);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return null;
        }
    }
}
