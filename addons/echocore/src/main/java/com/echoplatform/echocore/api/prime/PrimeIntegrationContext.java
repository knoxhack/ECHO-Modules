package com.echoplatform.echocore.api.prime;

public interface PrimeIntegrationContext {
    boolean moduleLoaded(String modId);

    PrimeRouteRegistry routeRegistry();

    PrimeMissionRegistry missionRegistry();

    PrimeProgressionRegistry progressionRegistry();

    PrimeIndexRegistry indexRegistry();

    PrimeLensRegistry lensRegistry();

    PrimeHoloMapRegistry holoMapRegistry();

    PrimeTerminalRegistry terminalRegistry();

    PrimeLootRegistry lootRegistry();

    PrimeWorldRegistry worldRegistry();

    PrimeAuditRegistry auditRegistry();
}
