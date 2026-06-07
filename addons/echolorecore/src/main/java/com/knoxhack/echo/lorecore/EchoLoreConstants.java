package com.knoxhack.echo.lorecore;

import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.platformcore.EchoModuleId;

public final class EchoLoreConstants {
    public static final EchoModuleId MODULE_ID = EchoModuleId.of("echolorecore");
    public static final EchoFeatureId FEATURE_LORE_ENTRIES = EchoFeatureId.of("lore.entries");
    public static final EchoFeatureId FEATURE_LORE_AUDIO_LOGS = EchoFeatureId.of("lore.audio_logs");
    public static final EchoFeatureId FEATURE_LORE_BLACKBOX = EchoFeatureId.of("lore.blackbox_entries");
    public static final EchoFeatureId FEATURE_ENVIRONMENTAL_STORY = EchoFeatureId.of("lore.environmental_storytelling");

    private EchoLoreConstants() {
    }
}
