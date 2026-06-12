package com.knoxhack.echorelictech.integration.datacore;

import com.echoplatform.echocore.api.DataScope;
import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.IDataKey;
import com.knoxhack.echorelictech.EchoRelicTech;
import com.knoxhack.echorelictech.api.RelicTechApi;
import com.knoxhack.echorelictech.api.event.RelicTechEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public final class RelicTechDataCoreIntegration {
    private static final IDataKey<Long> ANALYZED = counter("analyzed");
    private static final IDataKey<Long> USES = counter("uses");
    private static final IDataKey<Long> CONTAINED = counter("contained");
    private static final IDataKey<Long> FAILURES = counter("failures");
    private static final IDataKey<String> LAST_RELIC = string("last_relic");
    private static final IDataKey<String> SUMMARY = string("summary");
    private static boolean registered;

    private RelicTechDataCoreIntegration() {}

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        EchoCoreServices.registerDataKey(ANALYZED);
        EchoCoreServices.registerDataKey(USES);
        EchoCoreServices.registerDataKey(CONTAINED);
        EchoCoreServices.registerDataKey(FAILURES);
        EchoCoreServices.registerDataKey(LAST_RELIC);
        EchoCoreServices.registerDataKey(SUMMARY);
        RelicTechEvents.onAnalyze(event -> {
            increment(event.player(), ANALYZED);
            refreshSummary(event.player());
        });
        RelicTechEvents.onUse(event -> {
            increment(event.player(), USES);
            EchoCoreServices.playerData(event.player()).set(LAST_RELIC, event.relicId().toString());
            refreshSummary(event.player());
        });
        RelicTechEvents.onContain(event -> {
            increment(event.player(), CONTAINED);
            refreshSummary(event.player());
        });
        RelicTechEvents.onFailure(event -> {
            increment(event.player(), FAILURES);
            refreshSummary(event.player());
        });
        EchoRelicTech.LOGGER.info("RelicTech DataCore profile keys registered.");
    }

    private static void refreshSummary(ServerPlayer player) {
        if (player != null) {
            EchoCoreServices.playerData(player).set(SUMMARY, RelicTechApi.getTerminalRelicSummary(player));
        }
    }

    private static void increment(ServerPlayer player, IDataKey<Long> key) {
        if (player == null || key == null) {
            return;
        }
        Long current = EchoCoreServices.playerData(player).get(key);
        EchoCoreServices.playerData(player).set(key, Math.max(0L, current == null ? 1L : current + 1L));
    }

    private static IDataKey<Long> counter(String path) {
        return IDataKey.counter(id("counter/" + path), DataScope.PLAYER, 0L, true);
    }

    private static IDataKey<String> string(String path) {
        return IDataKey.string(id("profile/" + path), DataScope.PLAYER, "", true);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoRelicTech.MODID, path);
    }
}
