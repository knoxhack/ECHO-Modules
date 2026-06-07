package com.knoxhack.echo.packcore;

import com.knoxhack.echo.platformcore.EchoPackId;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public record EchoPackProfileIndex(Map<EchoPackId, EchoPackProfileParseResult> profiles) {
    public EchoPackProfileIndex {
        profiles = PackContractGuards.immutableMap(profiles);
    }

    public static EchoPackProfileIndex of(Collection<EchoPackProfileParseResult> results) {
        Map<EchoPackId, EchoPackProfileParseResult> indexed = results.stream()
                .sorted(Comparator.comparing(result -> result.requestedPackId().value()))
                .collect(Collectors.toMap(
                        EchoPackProfileParseResult::requestedPackId,
                        result -> result,
                        (first, ignored) -> first,
                        java.util.LinkedHashMap::new
                ));
        return new EchoPackProfileIndex(indexed);
    }

    public Optional<EchoPackProfileParseResult> find(EchoPackId packId) {
        return Optional.ofNullable(profiles.get(packId));
    }
}
