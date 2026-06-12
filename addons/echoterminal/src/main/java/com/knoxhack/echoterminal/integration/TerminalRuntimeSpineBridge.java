package com.knoxhack.echoterminal.integration;

import com.echoplatform.echocore.api.EchoRuntimeSpineBus;
import com.echoplatform.echocore.api.EchoRuntimeSpineEvent;
import com.knoxhack.echoterminal.EchoTerminal;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Publishes successful server-side Terminal mutations into the shared runtime spine.
 */
public final class TerminalRuntimeSpineBridge {
    public static final Identifier TERMINAL_REWARD_CLAIMED = id("terminal_reward_claimed");
    public static final Identifier TERMINAL_ARCHIVE_MARKED_READ = id("terminal_archive_marked_read");
    public static final Identifier TERMINAL_MISSION_ACTION = id("terminal_mission_action");
    public static final Identifier TERMINAL_MISSION_TRACKED = id("terminal_mission_tracked");
    public static final Identifier TERMINAL_MISSION_UNTRACKED = id("terminal_mission_untracked");
    public static final Identifier TERMINAL_CONFIG_APPLIED = id("terminal_config_applied");
    public static final Identifier TERMINAL_CONFIG_RESET = id("terminal_config_reset");

    private TerminalRuntimeSpineBridge() {
    }

    public static boolean publishAction(
            ServerPlayer player,
            Identifier eventId,
            Identifier targetId,
            Identifier tabId,
            Identifier actionId,
            Map<String, String> context) {
        if (player == null) {
            return false;
        }
        Map<String, String> safeContext = baseContext(tabId, actionId);
        if (context != null) {
            safeContext.putAll(context);
        }
        safeContext.putIfAbsent("objective_type", "custom");
        return EchoRuntimeSpineBus.publish(EchoRuntimeSpineEvent.of(
                EchoTerminal.MODID,
                eventId,
                player,
                targetId == null ? eventId : targetId,
                1,
                safeContext));
    }

    public static boolean publishConfigMutation(
            ServerPlayer player,
            Identifier eventId,
            String moduleId,
            String entryId,
            String operation) {
        return publishAction(
                player,
                eventId,
                configTarget(moduleId, entryId),
                id("settings"),
                eventId,
                Map.of(
                        "config_module", clean(moduleId),
                        "config_entry", clean(entryId),
                        "config_operation", clean(operation)));
    }

    private static Map<String, String> baseContext(Identifier tabId, Identifier actionId) {
        Map<String, String> context = new LinkedHashMap<>();
        context.put("ui_surface", "terminal");
        if (tabId != null) {
            context.put("terminal_tab", tabId.toString());
        }
        if (actionId != null) {
            context.put("terminal_action", actionId.toString());
        }
        return context;
    }

    private static Identifier configTarget(String moduleId, String entryId) {
        String namespace = sanitizeNamespace(moduleId);
        String path = sanitizePath(entryId);
        return Identifier.fromNamespaceAndPath(
                namespace.isBlank() ? EchoTerminal.MODID : namespace,
                path.isBlank() ? "config" : path);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoTerminal.MODID, path);
    }

    private static String clean(String value) {
        return value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
    }

    private static String sanitizeNamespace(String value) {
        return clean(value).replaceAll("[^a-z0-9_.-]", "_");
    }

    private static String sanitizePath(String value) {
        return clean(value).replaceAll("[^a-z0-9_./-]", "_");
    }
}
