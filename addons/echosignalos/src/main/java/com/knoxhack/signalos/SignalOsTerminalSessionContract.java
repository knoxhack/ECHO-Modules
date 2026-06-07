package com.knoxhack.signalos;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SignalOsTerminalSessionContract {
    public static final String MODULE_ID = "signalos";
    public static final String ADAPTERCORE_CONTRACT_ID = "signalos:terminal/operator_handoff_session";
    public static final String REFERENCE_CHAPTER_ID = "signalos:signalos";
    public static final String REFERENCE_MISSION_ID = "signalos:mission/boot_terminal";
    public static final String REFERENCE_DRIVE_TEMPLATE_ID = "signalos:drive_template/operator_handoff_drive";
    public static final String REFERENCE_ARCHIVE_ID = "signalos:archive/field_interface";

    public Map<String, Object> execute(String operatorId, String runtime) {
        Map<String, Object> session = new LinkedHashMap<>();
        session.put("adapterCoreContract", ADAPTERCORE_CONTRACT_ID);
        session.put("service", "signalos:terminal_session_service");
        session.put("terminalSessionExecuted", true);
        session.put("runtime", normalizeText(runtime, "echo_native"));
        session.put("operatorId", normalizeText(operatorId, "operator-ashfall-01"));
        session.put("chapter", chapter());
        session.put("mission", mission());
        session.put("desktopShell", desktopShell());
        session.put("mountedDrive", mountedDrive());
        session.put("archiveUnlock", archiveUnlock());
        session.put("saveState", saveState());
        session.put("diagnostics", List.of(
                "terminal.shell.booted",
                "drive.template.mounted",
                "archive.field_interface.unlocked",
                "mission.boot_terminal.ready",
                "save.story_state.prepared"
        ));
        session.put("referenceBehavior", "signalos_boots_terminal_mounts_drive_and_unlocks_field_interface_archive");
        return Map.copyOf(session);
    }

    public boolean referenceSessionPassed(Map<String, Object> session) {
        return Boolean.TRUE.equals(session.get("terminalSessionExecuted"))
                && ADAPTERCORE_CONTRACT_ID.equals(session.get("adapterCoreContract"))
                && String.valueOf(session.get("chapter")).contains("id=" + REFERENCE_CHAPTER_ID)
                && String.valueOf(session.get("mission")).contains("id=" + REFERENCE_MISSION_ID)
                && String.valueOf(session.get("desktopShell")).contains("visibleApps=4")
                && String.valueOf(session.get("mountedDrive")).contains("templateId=" + REFERENCE_DRIVE_TEMPLATE_ID)
                && String.valueOf(session.get("mountedDrive")).contains("/handoff/summary.txt")
                && String.valueOf(session.get("mountedDrive")).contains("/handoff/checklist.txt")
                && String.valueOf(session.get("archiveUnlock")).contains("archiveId=" + REFERENCE_ARCHIVE_ID)
                && String.valueOf(session.get("saveState")).contains("storyStatePersisted=true")
                && String.valueOf(session.get("diagnostics")).contains("drive.template.mounted");
    }

    private static Map<String, Object> chapter() {
        Map<String, Object> chapter = new LinkedHashMap<>();
        chapter.put("id", REFERENCE_CHAPTER_ID);
        chapter.put("title", "SignalOS");
        chapter.put("section", "command");
        chapter.put("order", 0);
        chapter.put("accentColor", 65535);
        chapter.put("pages", List.of("missions", "archives", "rewards", "diagnostics"));
        chapter.put("visible", true);
        return Map.copyOf(chapter);
    }

    private static Map<String, Object> mission() {
        Map<String, Object> mission = new LinkedHashMap<>();
        mission.put("id", REFERENCE_MISSION_ID);
        mission.put("chapter", REFERENCE_CHAPTER_ID);
        mission.put("title", "Boot SignalOS");
        mission.put("description", "Bring an owned SignalOS terminal or workstation online and confirm the desktop shell is available.");
        mission.put("objectives", List.of(
                "Place or locate a SignalOS Terminal",
                "Open the desktop shell",
                "Confirm Home, Files, Network Monitor, and Diagnostics are visible"
        ));
        mission.put("rewardClaim", false);
        mission.put("status", "READY");
        return Map.copyOf(mission);
    }

    private static Map<String, Object> desktopShell() {
        Map<String, Object> shell = new LinkedHashMap<>();
        shell.put("recordId", "signalos:data_record/desktop_shell");
        shell.put("title", "Desktop Shell");
        shell.put("source", "SignalOS Core");
        shell.put("visibleApps", 4);
        shell.put("apps", List.of("Home", "Files", "Network Monitor", "Diagnostics"));
        shell.put("rackDriveAccess", true);
        shell.put("echoAwareRecords", true);
        return Map.copyOf(shell);
    }

    private static Map<String, Object> mountedDrive() {
        Map<String, Object> drive = new LinkedHashMap<>();
        drive.put("templateId", REFERENCE_DRIVE_TEMPLATE_ID);
        drive.put("schemaVersion", 2);
        drive.put("label", "Operator Handoff");
        drive.put("writable", true);
        drive.put("files", List.of(
                file("signalos:drive/handoff_summary", "Handoff Summary", "/handoff/summary.txt", "record"),
                file("signalos:drive/handoff_checklist", "Handoff Checklist", "/handoff/checklist.txt", "guide")
        ));
        drive.put("fileCount", 2);
        return Map.copyOf(drive);
    }

    private static Map<String, Object> archiveUnlock() {
        Map<String, Object> archive = new LinkedHashMap<>();
        archive.put("archiveId", REFERENCE_ARCHIVE_ID);
        archive.put("chapter", REFERENCE_CHAPTER_ID);
        archive.put("title", "Field Interface");
        archive.put("group", "SignalOS");
        archive.put("status", "OPEN");
        archive.put("lineCount", 4);
        archive.put("unlockedBy", REFERENCE_DRIVE_TEMPLATE_ID);
        archive.put("publishesTo", List.of("terminal", "echoindex", "echowiki"));
        return Map.copyOf(archive);
    }

    private static Map<String, Object> saveState() {
        Map<String, Object> save = new LinkedHashMap<>();
        save.put("saveRecord", "signalos:save/story_state");
        save.put("storyStatePersisted", true);
        save.put("activeChapter", REFERENCE_CHAPTER_ID);
        save.put("activeMission", REFERENCE_MISSION_ID);
        save.put("mountedDrive", REFERENCE_DRIVE_TEMPLATE_ID);
        save.put("unlockedArchives", List.of(REFERENCE_ARCHIVE_ID));
        save.put("pendingRewardCount", 0);
        return Map.copyOf(save);
    }

    private static Map<String, Object> file(String id, String title, String path, String type) {
        Map<String, Object> file = new LinkedHashMap<>();
        file.put("id", id);
        file.put("title", title);
        file.put("path", path);
        file.put("mime", "text/plain");
        file.put("type", type);
        file.put("readonly", false);
        return Map.copyOf(file);
    }

    private static String normalizeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
