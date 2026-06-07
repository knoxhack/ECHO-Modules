package com.knoxhack.echoashfallprotocol.event;

import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeMutationLedgerEntry;

public interface MinecraftRuntimeMutationLedgerSink {
    void record(NativeMutationLedgerEntry entry);
}
