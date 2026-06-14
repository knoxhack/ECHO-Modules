package com.knoxhack.echomissioncore.content;

import com.knoxhack.echo.adaptercore.EchoBackendWorldEventBridge;
import com.knoxhack.echomissioncore.EchoMissionCore;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;

public final class MissionCoreReloaders {
    private MissionCoreReloaders() {
    }

    public static List<NativeReloadListenerRegistration> reloadListeners() {
        return List.of(new NativeReloadListenerRegistration(
                Identifier.fromNamespaceAndPath(EchoMissionCore.MODID, "content"),
                new MissionCoreJsonReloadListener()));
    }

    public static void addServerReloadListeners(Object event) {
        for (NativeReloadListenerRegistration registration : reloadListeners()) {
            EchoBackendWorldEventBridge.addServerReloadListener(event, registration.id(), registration.listener());
        }
    }

    public record NativeReloadListenerRegistration(
            Identifier id,
            PreparableReloadListener listener) {
    }
}
