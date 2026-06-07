package com.knoxhack.echo.adaptercore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class EchoNativeAgent7LiveHookEvidenceBridge {
    public static final String DIRECT_EVIDENCE_PATH_PROPERTY = "echo.agent7.liveHookEvidencePath";
    private static final List<String> REQUIRED_HOOK_KEYS = List.of(
            "echoworldcore:player_tick.post",
            "echoweathercore:level_tick.post",
            "echoatmospherecore:level_tick.post",
            "echobiomecore:level_tick.post",
            "echostructurecore:level_tick.post",
            "echospawncore:finalize_spawn",
            "echodifficultycore:server_starting",
            "echostatuscore:server_starting"
    );
    private static final Map<String, EchoAgent7LiveHookEvidence> LIVE_HOOK_EVIDENCE = new LinkedHashMap<>();
    private static String lastDirectPersistencePath = "";
    private static boolean lastDirectPersistenceWritten;
    private static String lastDirectPersistenceFailureKind = "";
    private static String lastDirectPersistenceFailureMessage = "";

    private EchoNativeAgent7LiveHookEvidenceBridge() {
    }

    public static synchronized EchoAgent7LiveHookEvidence recordExactCallback(
            String moduleId,
            String event,
            long gameTick,
            String sourceReason
    ) {
        String safeModuleId = AdapterContractGuards.requireText(moduleId, "agent7 live hook module id");
        String safeEvent = AdapterContractGuards.requireText(event, "agent7 live hook event");
        String key = safeModuleId + ":" + safeEvent;
        if (!REQUIRED_HOOK_KEYS.contains(key)) {
            throw new IllegalArgumentException("unknown Agent 7 live hook key: " + key);
        }
        EchoAgent7LiveHookEvidence evidence = new EchoAgent7LiveHookEvidence(
                safeModuleId,
                safeEvent,
                key,
                Math.max(0L, gameTick),
                AdapterContractGuards.optionalText(sourceReason),
                true,
                true,
                "exact_neoforge_callback_observed"
        );
        LIVE_HOOK_EVIDENCE.put(key, evidence);
        persistSnapshotIfConfigured();
        return evidence;
    }

    public static synchronized Map<String, EchoAgent7LiveHookEvidence> exactCallbacks() {
        return Map.copyOf(LIVE_HOOK_EVIDENCE);
    }

    public static synchronized Map<String, Object> snapshot() {
        return buildSnapshot();
    }

    public static synchronized void resetForTest() {
        LIVE_HOOK_EVIDENCE.clear();
        lastDirectPersistencePath = "";
        lastDirectPersistenceWritten = false;
        lastDirectPersistenceFailureKind = "";
        lastDirectPersistenceFailureMessage = "";
    }

    private static Map<String, Object> buildSnapshot() {
        int verifiedCount = 0;
        List<Map<String, Object>> hooks = REQUIRED_HOOK_KEYS.stream()
                .map(key -> {
                    EchoAgent7LiveHookEvidence evidence = LIVE_HOOK_EVIDENCE.get(key);
                    boolean verified = evidence != null && evidence.liveGameplayHookVerified();
                    if (verified) {
                        return evidence.toMap();
                    }
                    String moduleId = key.substring(0, key.indexOf(':'));
                    String event = key.substring(key.indexOf(':') + 1);
                    Map<String, Object> hook = new LinkedHashMap<>();
                    hook.put("moduleId", moduleId);
                    hook.put("event", event);
                    hook.put("key", key);
                    hook.put("gameTick", 0L);
                    hook.put("sourceReason", "");
                    hook.put("minecraftRuntimeAccessed", false);
                    hook.put("liveGameplayHookVerified", false);
                    hook.put("evidenceMode", "exact_neoforge_callback_missing");
                    return hook;
                })
                .toList();
        for (Map<String, Object> hook : hooks) {
            if (Boolean.TRUE.equals(hook.get("liveGameplayHookVerified"))) {
                verifiedCount++;
            }
        }
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("schema", "echo.agent7.native_exact_live_hook_evidence.v1");
        snapshot.put("requiredHookCount", REQUIRED_HOOK_KEYS.size());
        snapshot.put("verifiedHookCount", verifiedCount);
        snapshot.put("allRequiredHooksVerified", verifiedCount == REQUIRED_HOOK_KEYS.size());
        snapshot.put("directPersistenceConfigured", !directEvidencePath().isBlank());
        snapshot.put("directPersistencePath", lastDirectPersistencePath);
        snapshot.put("directPersistenceWritten", lastDirectPersistenceWritten);
        snapshot.put("directPersistenceFailureKind", lastDirectPersistenceFailureKind);
        snapshot.put("directPersistenceFailureMessage", lastDirectPersistenceFailureMessage);
        snapshot.put("hooks", hooks);
        snapshot.put("summary", verifiedCount == REQUIRED_HOOK_KEYS.size()
                ? "All Agent 7 exact NeoForge live callbacks have recorded runtime evidence."
                : "Agent 7 exact NeoForge live callback evidence is incomplete.");
        return snapshot;
    }

    private static void persistSnapshotIfConfigured() {
        String pathText = directEvidencePath();
        if (pathText.isBlank()) {
            lastDirectPersistencePath = "";
            lastDirectPersistenceWritten = false;
            lastDirectPersistenceFailureKind = "";
            lastDirectPersistenceFailureMessage = "";
            return;
        }
        lastDirectPersistencePath = pathText;
        try {
            Path path = Path.of(pathText).toAbsolutePath().normalize();
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, writeJson(buildSnapshot()), StandardCharsets.UTF_8);
            lastDirectPersistenceWritten = true;
            lastDirectPersistenceFailureKind = "";
            lastDirectPersistenceFailureMessage = "";
        } catch (IOException | RuntimeException exception) {
            lastDirectPersistenceWritten = false;
            lastDirectPersistenceFailureKind = exception.getClass().getSimpleName();
            lastDirectPersistenceFailureMessage = String.valueOf(exception.getMessage());
        }
    }

    private static String directEvidencePath() {
        return System.getProperty(DIRECT_EVIDENCE_PATH_PROPERTY, "").trim();
    }

    private static String writeJson(Object value) {
        StringBuilder builder = new StringBuilder();
        writeValue(builder, value, 0);
        builder.append('\n');
        return builder.toString();
    }

    private static void writeValue(StringBuilder builder, Object value, int indent) {
        if (value == null) {
            builder.append("null");
        } else if (value instanceof String string) {
            builder.append('"').append(escape(string)).append('"');
        } else if (value instanceof Number || value instanceof Boolean) {
            builder.append(value);
        } else if (value instanceof Map<?, ?> map) {
            writeObject(builder, map, indent);
        } else if (value instanceof Iterable<?> iterable) {
            writeArray(builder, iterable, indent);
        } else {
            builder.append('"').append(escape(String.valueOf(value))).append('"');
        }
    }

    private static void writeObject(StringBuilder builder, Map<?, ?> map, int indent) {
        Map<String, Object> sorted = new TreeMap<>();
        map.forEach((key, value) -> sorted.put(String.valueOf(key), value));
        builder.append('{');
        if (!sorted.isEmpty()) {
            int index = 0;
            for (Map.Entry<String, Object> entry : sorted.entrySet()) {
                builder.append('\n').append("  ".repeat(indent + 1));
                builder.append('"').append(escape(entry.getKey())).append("\": ");
                writeValue(builder, entry.getValue(), indent + 1);
                if (++index < sorted.size()) {
                    builder.append(',');
                }
            }
            builder.append('\n').append("  ".repeat(indent));
        }
        builder.append('}');
    }

    private static void writeArray(StringBuilder builder, Iterable<?> iterable, int indent) {
        List<Object> items = new java.util.ArrayList<>();
        iterable.forEach(items::add);
        builder.append('[');
        if (!items.isEmpty()) {
            for (int index = 0; index < items.size(); index++) {
                builder.append('\n').append("  ".repeat(indent + 1));
                writeValue(builder, items.get(index), indent + 1);
                if (index + 1 < items.size()) {
                    builder.append(',');
                }
            }
            builder.append('\n').append("  ".repeat(indent));
        }
        builder.append(']');
    }

    private static String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    public record EchoAgent7LiveHookEvidence(
            String moduleId,
            String event,
            String key,
            long gameTick,
            String sourceReason,
            boolean minecraftRuntimeAccessed,
            boolean liveGameplayHookVerified,
            String evidenceMode
    ) {
        public Map<String, Object> toMap() {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("moduleId", moduleId);
            data.put("event", event);
            data.put("key", key);
            data.put("gameTick", gameTick);
            data.put("sourceReason", sourceReason);
            data.put("minecraftRuntimeAccessed", minecraftRuntimeAccessed);
            data.put("liveGameplayHookVerified", liveGameplayHookVerified);
            data.put("evidenceMode", evidenceMode);
            return data;
        }
    }
}
