package com.knoxhack.echoterminal.mission;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echoterminal.EchoTerminal;
import com.knoxhack.echoterminal.api.mission.TerminalMissionAction;
import com.knoxhack.echoterminal.api.mission.TerminalMissionChapter;
import com.knoxhack.echoterminal.api.mission.TerminalMissionDefinition;
import com.knoxhack.echoterminal.api.mission.TerminalMissionProvider;
import com.knoxhack.echoterminal.api.mission.TerminalMissionRequirement;
import com.knoxhack.echoterminal.api.mission.TerminalMissionReward;
import com.knoxhack.echoterminal.api.mission.TerminalMissionRole;
import com.knoxhack.echoterminal.api.mission.TerminalMissionSnapshot;
import com.knoxhack.echoterminal.api.mission.TerminalMissionStatus;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class VanillaJourneyProvider implements TerminalMissionProvider {
    public static final VanillaJourneyProvider INSTANCE = new VanillaJourneyProvider();
    public static final Identifier CHAPTER_ID =
            Identifier.fromNamespaceAndPath(EchoTerminal.MODID, "vanilla_journey");
    public static final Identifier TAB_ID =
            Identifier.fromNamespaceAndPath(EchoTerminal.MODID, "vanilla_journey");
    private static final Identifier REFRESH_ID =
            Identifier.fromNamespaceAndPath(EchoTerminal.MODID, "vanilla_journey_refresh");
    private static final String ACTION_REFRESH = "refresh";
    private static final String ACTION_CLAIM = "claim_reward";
    private static final List<VanillaJourneyDefinitions.VanillaMission> MISSIONS =
            VanillaJourneyDefinitions.missions();

    private VanillaJourneyProvider() {
    }

    @Override
    public TerminalMissionChapter chapter() {
        return new TerminalMissionChapter(
                CHAPTER_ID,
                "Baseline",
                "Recovered Minecraft advancement route with claimable ECHO utility caches.",
                50,
                0xFF92F7A6,
                true);
    }

    @Override
    public List<TerminalMissionDefinition> missions(Player player) {
        VanillaJourneyData data = VanillaJourneyData.get(player);
        return MISSIONS.stream()
                .map(mission -> definition(mission, data))
                .toList();
    }

    @Override
    public TerminalMissionSnapshot snapshot(Player player, Identifier missionId) {
        VanillaJourneyDefinitions.VanillaMission mission = mission(missionId);
        if (mission == null) {
            return new TerminalMissionSnapshot(missionId, TerminalMissionStatus.LOCKED, 0.0F,
                    "LOCKED", "Baseline record not found in the current ECHO index.",
                    "No active Baseline record is available for this signal.", List.of());
        }
        VanillaJourneyData data = VanillaJourneyData.get(player);
        boolean completed = data.isCompleted(mission.id());
        boolean claimed = data.isClaimed(mission.id());
        if (!mission.claimable()) {
            return new TerminalMissionSnapshot(mission.id(), completed ? TerminalMissionStatus.COMPLETED : TerminalMissionStatus.UNLOCKED,
                    completed ? 1.0F : 0.0F, completed ? "OPEN" : "GUIDE", "",
                    "Guide header. Complete the linked advancement route to fill this chapter.",
                    List.of(refreshAction()));
        }
        TerminalMissionStatus status = claimed
                ? TerminalMissionStatus.CLAIMED
                : completed ? TerminalMissionStatus.CLAIMABLE : TerminalMissionStatus.UNLOCKED;
        return new TerminalMissionSnapshot(
                mission.id(),
                status,
                completed ? 1.0F : 0.0F,
                claimed ? "CLAIMED" : completed ? "CLAIMABLE" : "ADVANCEMENT",
                completed ? "" : "Complete the linked vanilla advancement.",
                claimed ? "Cache claimed. Keep following the vanilla route."
                        : completed ? "Advancement complete. Cache ready to claim."
                        : "ECHO validates advancement progress before cache claims.",
                actions(completed, claimed));
    }

    @Override
    public boolean handleAction(ServerPlayer player, Identifier missionId, String actionId) {
        if (player == null) {
            return false;
        }
        if (ACTION_REFRESH.equals(actionId)) {
            refresh(player);
            return true;
        }
        if (!ACTION_CLAIM.equals(actionId)) {
            return false;
        }
        VanillaJourneyDefinitions.VanillaMission mission = mission(missionId);
        if (mission == null || !mission.claimable()) {
            return false;
        }
        VanillaJourneyData data = VanillaJourneyData.get(player);
        refresh(player, data);
        if (!data.isCompleted(mission.id()) || data.isClaimed(mission.id())) {
            VanillaJourneyData.saveAndSync(player, data);
            return false;
        }
        if (!EchoCoreServices.storeTerminalRewards(player, mission.id().toString(), mission.rewardStacks())) {
            player.sendSystemMessage(Component.literal(
                    "[ECHO-7] No owned terminal cache found. Place or open your ECHO Terminal, then claim again."), true);
            VanillaJourneyData.saveAndSync(player, data);
            return false;
        }
        data.markClaimed(mission.id());
        VanillaJourneyData.saveAndSync(player, data);
        return true;
    }

    @Override
    public TerminalMissionRole role(Player player, TerminalMissionDefinition definition, TerminalMissionSnapshot snapshot) {
        VanillaJourneyDefinitions.VanillaMission mission = mission(definition.id());
        return mission == null ? TerminalMissionRole.MAIN : mission.role();
    }

    public void refresh(ServerPlayer player) {
        VanillaJourneyData data = VanillaJourneyData.get(player);
        refresh(player, data);
        VanillaJourneyData.saveAndSync(player, data);
    }

    public boolean refreshIfChanged(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        VanillaJourneyData data = VanillaJourneyData.get(player);
        boolean changed = refresh(player, data);
        if (changed) {
            VanillaJourneyData.saveAndSync(player, data);
        }
        return changed;
    }

    public boolean tracksAdvancement(Identifier advancementId) {
        return mission(advancementId) != null;
    }

    private boolean refresh(ServerPlayer player, VanillaJourneyData data) {
        List<Identifier> completed = new ArrayList<>();
        for (VanillaJourneyDefinitions.VanillaMission mission : MISSIONS) {
            if (hasAdvancement(player, mission.id())) {
                completed.add(mission.id());
            }
        }
        return data.setCompleted(completed);
    }

    private static TerminalMissionDefinition definition(
            VanillaJourneyDefinitions.VanillaMission mission, VanillaJourneyData data) {
        boolean completed = data.isCompleted(mission.id());
        List<TerminalMissionRequirement> requirements = mission.claimable()
                ? List.of(TerminalMissionRequirement.custom(
                        "Vanilla advancement",
                        completed ? "Complete" : "Complete advancement: " + mission.title(),
                        new ItemStack(mission.icon()),
                        completed ? 1 : 0,
                        1,
                        completed))
                : List.of();
        return new TerminalMissionDefinition(
                mission.id(),
                CHAPTER_ID,
                mission.phaseId(),
                mission.phaseTitle(),
                mission.phaseOrder(),
                mission.missionOrder(),
                mission.title(),
                mission.briefing(),
                mission.guide(),
                mission.phaseTitle(),
                mission.tier().label(),
                new ItemStack(mission.icon()),
                List.of(),
                requirements,
                mission.rewardStacks().stream().map(TerminalMissionReward::of).toList());
    }

    private static List<TerminalMissionAction> actions(boolean completed, boolean claimed) {
        TerminalMissionAction claim = claimed
                ? TerminalMissionAction.disabled(ACTION_CLAIM, "CLAIM CACHE", "Reward cache already claimed.")
                : completed
                        ? TerminalMissionAction.enabled(ACTION_CLAIM, "CLAIM CACHE")
                        : TerminalMissionAction.disabled(ACTION_CLAIM, "CLAIM CACHE", "Complete the vanilla advancement first.");
        return completed && !claimed
                ? List.of(claim, refreshAction())
                : List.of(refreshAction(), claim);
    }

    private static TerminalMissionAction refreshAction() {
        return TerminalMissionAction.enabled(ACTION_REFRESH, "SYNC ADVANCEMENTS");
    }

    private static boolean hasAdvancement(ServerPlayer player, Identifier id) {
        if (player.level().getServer() == null) {
            return false;
        }
        AdvancementHolder holder = player.level().getServer().getAdvancements().get(id);
        return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
    }

    private static VanillaJourneyDefinitions.VanillaMission mission(Identifier id) {
        return VanillaJourneyDefinitions.mission(id);
    }
}
