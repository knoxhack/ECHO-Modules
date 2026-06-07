package com.knoxhack.echo.bridgecore;

public record EchoBridgeStreamCursor(
        String streamId,
        long offset,
        long sequence,
        boolean endReached
) {
    public EchoBridgeStreamCursor {
        streamId = BridgeContractGuards.requireText(streamId, "bridge stream id");
        offset = BridgeContractGuards.nonNegativeLong(offset, "stream offset");
        sequence = BridgeContractGuards.nonNegativeLong(sequence, "stream sequence");
    }

    public static EchoBridgeStreamCursor beginning(String streamId) {
        return new EchoBridgeStreamCursor(streamId, 0L, 0L, false);
    }
}
