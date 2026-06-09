package com.knoxhack.echocore.api.diagnostic;

import java.util.ArrayList;
import java.util.List;

public final class EchoDiagnosticService {
    private final List<EchoDiagnosticBlocker> blockers = new ArrayList<>();

    public void report(EchoDiagnosticBlocker blocker) {
        blockers.add(blocker);
    }

    public List<EchoDiagnosticBlocker> blockers() {
        return List.copyOf(blockers);
    }

    public boolean healthy() {
        return blockers.isEmpty();
    }
}
