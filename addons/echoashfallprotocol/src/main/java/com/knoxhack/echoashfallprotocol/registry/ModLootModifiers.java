package com.knoxhack.echoashfallprotocol.registry;

import com.knoxhack.echo.adaptercore.EchoBackendLootModifierBridge;
import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echoashfallprotocol.loot.WikiManualLootModifier;

public final class ModLootModifiers {
    public static final Object LOOT_MODIFIER_SERIALIZERS =
            EchoBackendLootModifierBridge.createGlobalLootModifierSerializers(EchoAshfallProtocol.MODID);

    public static final Object WIKI_MANUAL =
            EchoBackendLootModifierBridge.registerSerializer(LOOT_MODIFIER_SERIALIZERS, "wiki_manual",
                    WikiManualLootModifier.CODEC);

    static {
        WikiManualLootModifier.configureBackend(() -> EchoBackendLootModifierBridge.codec(WIKI_MANUAL));
    }

    private ModLootModifiers() {
    }

    public static void register(Object eventBus) {
        EchoBackendLootModifierBridge.register(LOOT_MODIFIER_SERIALIZERS, eventBus);
    }
}
