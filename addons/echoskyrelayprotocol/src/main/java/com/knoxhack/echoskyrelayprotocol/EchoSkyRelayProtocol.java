package com.knoxhack.echoskyrelayprotocol;

import com.knoxhack.echoskyrelayprotocol.contract.SkyRelayRuntimeContracts;
import com.knoxhack.echoskyrelayprotocol.registry.SkyRelayBlocks;
import com.knoxhack.echoskyrelayprotocol.registry.SkyRelayItems;

/**
 * Minimal Sky Relay protocol entrypoint. The first implementation slice is
 * intentionally data-first so adapter work can bind to stable content IDs.
 */
public final class EchoSkyRelayProtocol {
    public static final String MODID = SkyRelayRuntimeContracts.MODULE_ID;
    public static final String PACK_ID = SkyRelayRuntimeContracts.PACK_ID;
    public static final String RESTORATION_MODE = SkyRelayRuntimeContracts.RESTORATION_MODE;

    public EchoSkyRelayProtocol() {
        commonSetup();
    }

    public EchoSkyRelayProtocol(Object modEventBus) {
        SkyRelayBlocks.register(modEventBus);
        SkyRelayItems.register(modEventBus);
        commonSetup();
    }

    public void commonSetup() {
        System.out.println("ECHO Sky Relay Protocol online: fragments="
                + SkyRelayRuntimeContracts.FRAGMENT_IDS.size()
                + " blocks=" + SkyRelayBlocks.ALL_BLOCKS.size()
                + " items=" + SkyRelayItems.CORE_ITEMS.size()
                + " blockItems=" + SkyRelayItems.BLOCK_ITEMS.size()
                + " phases=" + SkyRelayRuntimeContracts.PHASE_IDS.size());
    }
}
