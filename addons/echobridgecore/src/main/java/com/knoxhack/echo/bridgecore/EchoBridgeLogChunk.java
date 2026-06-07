package com.knoxhack.echo.bridgecore;

import java.util.Map;
import java.util.Objects;

public record EchoBridgeLogChunk(
        EchoBridgeJobId jobId,
        EchoBridgeStreamCursor cursor,
        String streamName,
        String text,
        long createdAtEpochMillis,
        boolean errorStream,
        boolean redacted,
        Map<String, String> attributes
) {
    public EchoBridgeLogChunk {
        jobId = Objects.requireNonNull(jobId, "jobId");
        cursor = cursor == null ? EchoBridgeStreamCursor.beginning(jobId + ".stdout") : cursor;
        streamName = BridgeContractGuards.optionalText(streamName);
        text = BridgeContractGuards.optionalText(text);
        createdAtEpochMillis = BridgeContractGuards.nonNegativeLong(createdAtEpochMillis, "log chunk timestamp");
        attributes = BridgeContractGuards.immutableMap(attributes);
    }
}
