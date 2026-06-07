package com.knoxhack.echo.adaptercore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class EchoNativeStoryRuntimeBridge {
    private final String moduleId;
    private final List<Map<String, Object>> executedHandlers = new ArrayList<>();
    private final Set<String> unlockedArchives = new LinkedHashSet<>();
    private final Set<String> unlockedChapters = new LinkedHashSet<>();
    private final Set<String> presenceLinks = new LinkedHashSet<>();
    private final List<String> loreUpdates = new ArrayList<>();
    private final Map<String, Boolean> flags = new LinkedHashMap<>();
    private final Map<String, Integer> gameplayStats = new LinkedHashMap<>();
    private boolean terminalOpen;
    private boolean dataDriveRead;
    private boolean missionStarted;
    private String activeMissionId = "";

    public EchoNativeStoryRuntimeBridge(String moduleId) {
        this.moduleId = AdapterContractGuards.requireText(moduleId, "module id");
    }

    public EchoNativeStoryRuntimeBridge openTerminal(String surfaceId, String signalId) {
        terminalOpen = true;
        execute("terminal.open", surfaceId);
        lore("terminal:" + surfaceId);
        lore("index:signalos");
        if (!AdapterContractGuards.optionalText(signalId).isBlank()) {
            execute("signal.render", signalId);
        }
        return this;
    }

    public EchoNativeStoryRuntimeBridge readDataDrive(String driveId, String archiveId, String flagId) {
        dataDriveRead = true;
        execute("data_drive.read", driveId);
        if (!AdapterContractGuards.optionalText(archiveId).isBlank()) {
            unlockedArchives.add(archiveId);
            execute("archive.unlock", archiveId);
        }
        if (!AdapterContractGuards.optionalText(flagId).isBlank()) {
            flags.put(flagId, true);
            execute("story.flag.save", flagId);
        }
        lore("wiki:signalos/data_drives");
        lore("lore:" + driveId);
        return this;
    }

    public EchoNativeStoryRuntimeBridge startMission(String signalId, String missionId) {
        missionStarted = true;
        activeMissionId = AdapterContractGuards.requireText(missionId, "mission id");
        execute("signal.receive", signalId);
        execute("story.mission.start", activeMissionId);
        lore("mission:" + activeMissionId);
        return this;
    }

    public EchoNativeStoryRuntimeBridge applyRelicEffect(
            String effectId,
            String gameplayStat,
            int delta,
            String archiveId
    ) {
        if (!AdapterContractGuards.optionalText(archiveId).isBlank()) {
            unlockedArchives.add(archiveId);
        }
        mutate(gameplayStat, delta);
        execute("relic.effect.apply", effectId);
        lore("relic:" + effectId);
        return this;
    }

    public EchoNativeStoryRuntimeBridge castSpell(String spellId, String gameplayStat, int delta) {
        mutate(gameplayStat, delta);
        execute("spell.cast", spellId);
        lore("spell:" + spellId);
        return this;
    }

    public EchoNativeStoryRuntimeBridge activateRitual(
            String ritualId,
            String gameplayStat,
            int delta,
            String flagId
    ) {
        mutate(gameplayStat, delta);
        if (!AdapterContractGuards.optionalText(flagId).isBlank()) {
            flags.put(flagId, true);
        }
        execute("ritual.activate", ritualId);
        lore("ritual:" + ritualId);
        return this;
    }

    public EchoNativeStoryRuntimeBridge applyCurse(String curseId, String gameplayStat, int delta) {
        mutate(gameplayStat, delta);
        execute("curse.apply", curseId);
        lore("curse:" + curseId);
        return this;
    }

    public EchoNativeStoryRuntimeBridge triggerRift(String riftId, String chapterId, String flagId) {
        if (!AdapterContractGuards.optionalText(flagId).isBlank()) {
            flags.put(flagId, true);
        }
        execute("rift.trigger", riftId);
        execute("chapter.route", chapterId);
        lore("rift:" + riftId);
        return this;
    }

    public EchoNativeStoryRuntimeBridge unlockChapter(String chapterId, String requiredFlagId) {
        if (!AdapterContractGuards.optionalText(requiredFlagId).isBlank()) {
            flags.putIfAbsent(requiredFlagId, true);
        }
        unlockedChapters.add(chapterId);
        execute("chapter.unlock", chapterId);
        lore("chapter:" + chapterId);
        return this;
    }

    public EchoNativeStoryRuntimeBridge linkPresence(String presenceId, String state) {
        presenceLinks.add(presenceId + "=" + AdapterContractGuards.optionalText(state));
        execute("presence.link", presenceId);
        lore("presence:" + presenceId);
        return this;
    }

    public Map<String, Object> report() {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("moduleId", moduleId);
        report.put("bridge", "adaptercore.native_story_runtime");
        report.put("serviceCodeExecuted", !executedHandlers.isEmpty());
        report.put("handlerExecuted", !executedHandlers.isEmpty());
        report.put("handlerExecutionCount", executedHandlers.size());
        report.put("terminalOpen", terminalOpen);
        report.put("dataDriveRead", dataDriveRead);
        report.put("missionStarted", missionStarted);
        report.put("activeMissionId", activeMissionId);
        report.put("unlockedArchiveIds", List.copyOf(unlockedArchives));
        report.put("unlockedChapterIds", List.copyOf(unlockedChapters));
        report.put("flags", Map.copyOf(flags));
        report.put("gameplayStats", Map.copyOf(gameplayStats));
        report.put("presenceLinks", List.copyOf(presenceLinks));
        report.put("loreUpdates", List.copyOf(loreUpdates));
        report.put("executedHandlers", List.copyOf(executedHandlers));
        report.put("summary", "Native Story Runtime executed portable terminal, data-drive, mission, effect, chapter, presence, save, and lore behaviors without Minecraft registry mutation.");
        return report;
    }

    private void mutate(String gameplayStat, int delta) {
        gameplayStats.merge(AdapterContractGuards.requireText(gameplayStat, "gameplay stat"), delta, Integer::sum);
    }

    private void lore(String update) {
        String normalized = AdapterContractGuards.requireText(update, "lore update");
        if (!loreUpdates.contains(normalized)) {
            loreUpdates.add(normalized);
        }
    }

    private void execute(String behavior, String contentId) {
        Map<String, Object> handler = new LinkedHashMap<>();
        handler.put("behavior", AdapterContractGuards.requireText(behavior, "story behavior"));
        handler.put("contentId", AdapterContractGuards.requireText(contentId, "story content id"));
        handler.put("handlerExecuted", true);
        handler.put("nativeLoaderBackend", true);
        handler.put("minecraftRegistryMutated", false);
        executedHandlers.add(handler);
    }
}
