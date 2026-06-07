package com.knoxhack.echomissioncore.content;

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

    public record NativeReloadListenerRegistration(
            Identifier id,
            PreparableReloadListener listener) {
    }
}
