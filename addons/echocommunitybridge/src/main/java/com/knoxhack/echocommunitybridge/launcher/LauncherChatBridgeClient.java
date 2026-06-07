package com.knoxhack.echocommunitybridge.launcher;

import com.knoxhack.echocommunitybridge.CommunityBridgeAdapterCoreContracts;
import java.util.Optional;

public final class LauncherChatBridgeClient {
    private LauncherChatBridgeClient() {
    }

    public static Optional<String> launcherChatLine(String nickname, String body) {
        return launcherChatLine("launcher", nickname, body);
    }

    public static Optional<String> launcherChatLine(String source, String nickname, String body) {
        return CommunityBridgeAdapterCoreContracts.launcherChatLine(source, nickname, body);
    }
}
