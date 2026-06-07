package com.knoxhack.echoterminal.network;

import com.knoxhack.echocore.api.config.EchoConfigModuleSnapshot;
import java.util.List;
import java.util.Optional;

public final class TerminalConfigClientState {
    private static volatile List<EchoConfigModuleSnapshot> commonModules = List.of();
    private static volatile String status = "";

    private TerminalConfigClientState() {
    }

    public static void apply(TerminalConfigSyncPacket packet) {
        commonModules = packet == null ? List.of() : packet.modules();
        status = packet == null ? "" : packet.status();
    }

    public static List<EchoConfigModuleSnapshot> commonModules() {
        return commonModules;
    }

    public static Optional<EchoConfigModuleSnapshot> commonModule(String moduleId) {
        if (moduleId == null || moduleId.isBlank()) {
            return Optional.empty();
        }
        String id = moduleId.strip().toLowerCase(java.util.Locale.ROOT);
        return commonModules.stream()
                .filter(module -> module.moduleId().equals(id))
                .findFirst();
    }

    public static String status() {
        return status;
    }
}
