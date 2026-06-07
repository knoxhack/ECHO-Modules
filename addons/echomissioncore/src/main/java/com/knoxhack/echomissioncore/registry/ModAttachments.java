package com.knoxhack.echomissioncore.registry;

import com.knoxhack.echomissioncore.EchoMissionCore;
import com.knoxhack.echomissioncore.storage.MissionPlayerData;
import java.util.function.Supplier;
import net.minecraft.resources.Identifier;

public final class ModAttachments {
    public static final Supplier<NativeAttachmentType<MissionPlayerData>> MISSION_PLAYER_DATA =
            () -> new NativeAttachmentType<>(
                    Identifier.fromNamespaceAndPath(EchoMissionCore.MODID, "mission_player_data"),
                    MissionPlayerData::new,
                    true,
                    true);

    private ModAttachments() {
    }

    public static void register() {
    }
}
