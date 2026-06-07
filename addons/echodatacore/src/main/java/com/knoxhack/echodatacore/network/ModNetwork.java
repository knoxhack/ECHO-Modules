package com.knoxhack.echodatacore.network;

import com.knoxhack.echodatacore.DataCoreDataService;
import com.knoxhack.echonetcore.api.EchoNetPayloads;
import com.knoxhack.echonetcore.api.EchoPayloadRegistrar;

public final class ModNetwork {
    private ModNetwork() {
    }

    public static EchoPayloadRegistrar registerPayloads() {
        return registerPayloads(EchoNetPayloads.optional());
    }

    public static EchoPayloadRegistrar registerPayloads(EchoPayloadRegistrar registrar) {
        EchoNetPayloads.clientboundSync(registrar, DataCoreSyncPacket.TYPE, DataCoreSyncPacket.CODEC,
                (packet, player, context) -> DataCoreDataService.INSTANCE.applyClientSync(packet));
        EchoNetPayloads.clientboundSync(registrar, DataCoreMetadataSyncPacket.TYPE, DataCoreMetadataSyncPacket.CODEC,
                (packet, player, context) -> DataCoreDataService.INSTANCE.applyClientMetadataSync(packet));
        return registrar;
    }
}
