package com.knoxhack.echocommunitybridge;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class CommunityBridgeAdapterCoreContracts {
    public static final String SERVER_STATUS_CONTRACT_ID = "echocommunitybridge:networking/server_status";
    public static final String LAUNCHER_CHAT_CONTRACT_ID = "echocommunitybridge:networking/launcher_chat";
    public static final String DISCORD_SANITIZATION_CONTRACT_ID = "echocommunitybridge:diagnostics/discord_sanitization";
    public static final String PLAYER_IDENTITY_CONTRACT_ID = "echocommunitybridge:data/player_identity";
    public static final List<String> CONTRACT_IDS = List.of(
            SERVER_STATUS_CONTRACT_ID,
            LAUNCHER_CHAT_CONTRACT_ID,
            DISCORD_SANITIZATION_CONTRACT_ID,
            PLAYER_IDENTITY_CONTRACT_ID
    );

    private static final int MAX_PLAYER_NAME = 32;

    private CommunityBridgeAdapterCoreContracts() {
    }

    public static String sanitizePublicText(String value, int maxLength) {
        String safe = value == null ? "" : value;
        safe = safe.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");
        safe = safe.replace('\r', ' ').replace('\n', ' ').replace('\t', ' ');
        safe = safe.replace("@everyone", "@ everyone").replace("@here", "@ here");
        safe = safe.strip();
        return clamp(safe, Math.max(0, maxLength));
    }

    public static String sanitizeDiscordText(String value, int maxLength) {
        return sanitizePublicText(value, maxLength).replace("@", "@\u200B");
    }

    public static String sanitizePlayerName(String value) {
        String safe = value == null ? "" : value.strip();
        safe = safe.replaceAll("[^A-Za-z0-9_\\-.]", "");
        return clamp(safe, MAX_PLAYER_NAME);
    }

    public static Optional<String> launcherChatLine(String source, String nickname, String body) {
        String safeBody = sanitizePublicText(body, 500);
        if (safeBody.isBlank() || safeBody.startsWith("/")) {
            return Optional.empty();
        }
        String safeNickname = sanitizePublicText(nickname, 32);
        if (safeNickname.isBlank()) {
            safeNickname = sourceLabel(source);
        }
        return Optional.of("[" + sourceLabel(source) + "] <" + safeNickname + ">: " + safeBody);
    }

    public static String normalizePublicSource(String source) {
        String safe = source == null ? "" : source.strip().toLowerCase(Locale.ROOT);
        if ("launcher".equals(safe) || "android".equals(safe)) {
            return safe;
        }
        throw new IllegalArgumentException("Unsupported chat source.");
    }

    public static Map<String, Object> referenceProbe() {
        String publicText = sanitizePublicText("hello\r\n@everyone\u0000world", 20);
        String discordText = sanitizeDiscordText("ping @here\u0000", 40);
        String playerName = sanitizePlayerName(" Knox Hack! @admin ");
        Optional<String> launcher = launcherChatLine("launcher", "Launcher User", "hello @everyone\u0000");
        Optional<String> slashCommand = launcherChatLine("launcher", "Launcher User", "/op Knox");
        Optional<String> android = launcherChatLine("android", "Pixel Tester", "hello");
        boolean rejectedSource;
        try {
            normalizePublicSource("discord");
            rejectedSource = false;
        } catch (IllegalArgumentException exception) {
            rejectedSource = true;
        }
        return Map.of(
                "publicTextSanitized", !publicText.contains("\u0000")
                        && !publicText.contains("@everyone")
                        && publicText.length() <= 20,
                "discordTextSanitized", discordText.contains("@\u200B") && !discordText.contains("\u0000"),
                "playerNameSanitized", "KnoxHackadmin".equals(playerName),
                "launcherChatAccepted", launcher.isPresent()
                        && launcher.get().startsWith("[Launcher]")
                        && !launcher.get().contains("@everyone"),
                "launcherSlashCommandBlocked", slashCommand.isEmpty(),
                "androidChatLabeled", android.isPresent() && android.get().startsWith("[Android]"),
                "unsupportedPublicSourceRejected", rejectedSource
        );
    }

    public static boolean referenceProbePassed(Map<String, Object> probe) {
        for (Object value : probe.values()) {
            if (!Boolean.TRUE.equals(value)) {
                return false;
            }
        }
        return true;
    }

    private static String sourceLabel(String source) {
        return "android".equals(source) ? "Android" : "Launcher";
    }

    private static String clamp(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
