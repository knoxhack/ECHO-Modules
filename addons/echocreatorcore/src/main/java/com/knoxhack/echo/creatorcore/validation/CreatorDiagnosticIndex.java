package com.knoxhack.echo.creatorcore.validation;

import com.knoxhack.echo.creatorcore.api.CreatorDiagnostic;
import java.util.ArrayList;
import java.util.List;

public final class CreatorDiagnosticIndex {
    private final List<CreatorDiagnostic> diagnostics = new ArrayList<>();

    public void add(CreatorDiagnostic diagnostic) {
        if (diagnostic != null) {
            diagnostics.add(diagnostic);
        }
    }

    public void addAll(List<CreatorDiagnostic> diagnostics) {
        if (diagnostics != null) {
            diagnostics.forEach(this::add);
        }
    }

    public List<CreatorDiagnostic> all() {
        return List.copyOf(diagnostics);
    }

    public long count(CreatorDiagnostic.Severity severity) {
        return diagnostics.stream().filter(diagnostic -> diagnostic.severity() == severity).count();
    }
}
