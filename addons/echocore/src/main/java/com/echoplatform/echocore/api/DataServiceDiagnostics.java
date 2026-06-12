package com.echoplatform.echocore.api;

import java.util.List;

public record DataServiceDiagnostics(
        boolean available,
        String provider,
        long revision,
        int registeredKeyCount,
        int syncedKeyCount,
        int metadataKeyCount,
        int dirtyOwnerCount,
        List<String> recentChanges,
        String status) {
    public DataServiceDiagnostics {
        provider = provider == null ? "" : provider;
        recentChanges = recentChanges == null ? List.of() : List.copyOf(recentChanges);
        status = status == null ? "" : status;
    }

    public DataServiceDiagnostics(boolean available, int registeredKeys, int playerStores, int worldStores, String status) {
        this(available, "", registeredKeys + playerStores + worldStores,
                registeredKeys, registeredKeys, registeredKeys, 0, List.of(), status);
    }

    public DataServiceDiagnostics(
            boolean available,
            String provider,
            long revision,
            int registeredKeyCount,
            int syncedKeyCount,
            int metadataKeyCount,
            int dirtyOwnerCount,
            List<String> recentChanges) {
        this(available, provider, revision, registeredKeyCount, syncedKeyCount, metadataKeyCount,
                dirtyOwnerCount, recentChanges, available ? "Data service online." : "Data service unavailable.");
    }

    public static DataServiceDiagnostics unavailable() {
        return new DataServiceDiagnostics(false, 0, 0, 0, "Data service unavailable.");
    }

    public String providerClass() {
        return provider;
    }
}
