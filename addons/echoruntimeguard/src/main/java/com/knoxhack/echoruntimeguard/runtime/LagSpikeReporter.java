package com.knoxhack.echoruntimeguard.runtime;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class LagSpikeReporter {
    public static final LagSpikeReporter INSTANCE = new LagSpikeReporter();
    private static final int MAX_RECORDS = 64;
    private final Deque<LagSpike> spikes = new ArrayDeque<>();

    private LagSpikeReporter() {
    }

    public synchronized void record(double mspt, double tps, String reason) {
        spikes.addLast(new LagSpike(Instant.now(), mspt, tps, reason == null || reason.isBlank() ? "server tick" : reason));
        while (spikes.size() > MAX_RECORDS) {
            spikes.removeFirst();
        }
    }

    public synchronized List<LagSpike> recent() {
        return List.copyOf(spikes);
    }

    public synchronized List<String> warnings() {
        List<String> warnings = new ArrayList<>();
        for (LagSpike spike : spikes) {
            warnings.add("Lag spike " + Math.round(spike.mspt()) + "ms MSPT / "
                    + String.format(java.util.Locale.ROOT, "%.1f", spike.tps()) + " TPS: " + spike.reason());
        }
        return warnings;
    }

    public synchronized void reset() {
        spikes.clear();
    }

    public record LagSpike(Instant time, double mspt, double tps, String reason) {
    }
}
