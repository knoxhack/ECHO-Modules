package com.knoxhack.echodatacore.integration;

import com.knoxhack.echocore.api.DataServiceDiagnostics;
import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.EchoDiagnosticBlocker;
import com.knoxhack.echocore.api.EchoDiagnosticService;
import com.knoxhack.echodatacore.DataCoreDataService;
import com.knoxhack.echodatacore.EchoDataCore;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public enum DataCoreDiagnostics implements EchoDiagnosticService {
    INSTANCE;

    public static void register() {
        EchoCoreServices.registerDiagnosticService(INSTANCE);
    }

    @Override
    public List<EchoDiagnosticBlocker> diagnostics(Player player) {
        DataServiceDiagnostics diagnostics = DataCoreDataService.INSTANCE.diagnostics();
        List<EchoDiagnosticBlocker> rows = new ArrayList<>();
        rows.add(info("backend", "DataCore backend online",
                "revision=" + diagnostics.revision()
                        + ", keys=" + diagnostics.registeredKeyCount()
                        + ", metadata=" + diagnostics.metadataKeyCount(),
                "No action needed."));
        if (DataCoreDataService.INSTANCE.duplicateKeyConflictCount() > 0) {
            rows.add(warning("duplicate_keys", "Duplicate data key contracts",
                    DataCoreDataService.INSTANCE.duplicateKeyConflictCount()
                            + " duplicate or mismatched Java key registrations were ignored.",
                    "Keep the first key id stable and move conflicting keys to a new namespaced id."));
        }
        if (DataCoreDataService.INSTANCE.metadataConflictCount() > 0) {
            rows.add(warning("metadata_conflicts", "Conflicting DataCore metadata",
                    DataCoreDataService.INSTANCE.metadataConflictCount()
                            + " datapack metadata entries disagreed with Java key scope or kind.",
                    "Update datapacks so metadata matches the owning Java key contract."));
        }
        if (DataCoreDataService.INSTANCE.lastStaleDatapackMetadataRemoved() > 0) {
            rows.add(info("stale_metadata_removed", "Stale metadata cleaned up",
                    DataCoreDataService.INSTANCE.lastStaleDatapackMetadataRemoved()
                            + " stale datapack metadata entries were removed on the last reload.",
                    "No action needed unless a datapack entry disappeared unexpectedly."));
        }
        int dirtyKeys = DataCoreDataService.INSTANCE.dirtyKeyCount();
        if (dirtyKeys > 256) {
            rows.add(warning("sync_pressure", "High DataCore sync pressure",
                    dirtyKeys + " dirty keys are queued for sync.",
                    "Increase sync batch size or reduce repeated writes if this persists."));
        } else {
            rows.add(info("sync_pressure", "DataCore sync pressure nominal",
                    "dirtyKeys=" + dirtyKeys + ", lastMetadataSync="
                            + DataCoreDataService.INSTANCE.lastMetadataSyncSize(),
                    "No action needed."));
        }
        return rows;
    }

    private static EchoDiagnosticBlocker info(String path, String title, String detail, String nextAction) {
        return blocker(path, EchoDiagnosticBlocker.Severity.INFO, title, detail, nextAction);
    }

    private static EchoDiagnosticBlocker warning(String path, String title, String detail, String nextAction) {
        return blocker(path, EchoDiagnosticBlocker.Severity.WARNING, title, detail, nextAction);
    }

    private static EchoDiagnosticBlocker blocker(
            String path,
            EchoDiagnosticBlocker.Severity severity,
            String title,
            String detail,
            String nextAction) {
        return new EchoDiagnosticBlocker(
                Identifier.fromNamespaceAndPath(EchoDataCore.MODID, "diagnostic/" + path),
                EchoDataCore.MODID,
                severity,
                title,
                detail,
                nextAction);
    }
}
