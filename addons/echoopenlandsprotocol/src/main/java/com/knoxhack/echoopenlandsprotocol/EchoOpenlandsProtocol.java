package com.knoxhack.echoopenlandsprotocol;

import com.knoxhack.echoopenlandsprotocol.contract.OpenlandsRuntimeContracts;
import com.knoxhack.echoopenlandsprotocol.runtime.OpenlandsFirstHourRuntime;

/**
 * Minimal Openlands protocol entrypoint. Gameplay content is intentionally
 * data-first; runtime adapters should consume the Echo data contracts in
 * src/main/resources/data/echoopenlandsprotocol/openlands.
 */
public final class EchoOpenlandsProtocol {
    public static final String MODID = OpenlandsRuntimeContracts.MODULE_ID;
    public static final String PACK_ID = OpenlandsRuntimeContracts.PACK_ID;
    public static final String STANDARD_MODE = OpenlandsRuntimeContracts.STANDARD_MODE;

    public EchoOpenlandsProtocol() {
        commonSetup();
    }

    public void commonSetup() {
        System.out.println("ECHO Openlands Protocol online: relaxed sandbox contract active. contracts="
                + OpenlandsRuntimeContracts.CONTRACT_RESOURCES.size()
                + " firstHourHooks=" + OpenlandsFirstHourRuntime.adapterBindingManifest().get("callableHooks")
                + " runtimes=" + OpenlandsRuntimeContracts.RUNTIME_TARGETS);
    }
}
