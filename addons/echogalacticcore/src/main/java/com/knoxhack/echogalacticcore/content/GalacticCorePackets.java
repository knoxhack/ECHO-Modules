package com.knoxhack.echogalacticcore.content;

import dev.echo.nativeplatform.contracts.EchoNativeModuleLoadContext;
import dev.echo.nativeplatform.contracts.EchoNativeNetworkService;

public final class GalacticCorePackets {
    private GalacticCorePackets() {
    }

    public static void register(EchoNativeModuleLoadContext context, EchoNativeNetworkService network) {
        for (GalacticCoreContentDefinitions.Registration packet : GalacticCoreContentDefinitions.PACKETS) {
            GalacticCoreRegistrarSupport.record(
                    context,
                    network.registerPacket(GalacticCoreRegistrarSupport.mutation("network", "registerPacket", packet))
            );
        }
    }
}
