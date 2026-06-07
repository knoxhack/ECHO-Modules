package com.knoxhack.echo.creatorcore.ui.panels;

import com.knoxhack.echo.creatorcore.EchoCreatorCore;
import com.knoxhack.echo.creatorcore.api.CreatorDiagnostic;
import com.knoxhack.echo.creatorcore.ui.CreatorDashboardModel;
import com.knoxhack.echo.creatorcore.ui.CreatorPanel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.resources.Identifier;

public final class ValidationPanel implements CreatorPanel {
    @Override
    public Identifier id() {
        return EchoCreatorCore.id("validation");
    }

    @Override
    public String title() {
        return "Validation";
    }

    @Override
    public String summary() {
        return "Doctor report and diagnostics from adapters, drafts, and CreatorCore.";
    }

    @Override
    public List<String> lines(CreatorDashboardModel model) {
        List<String> lines = new ArrayList<>();
        lines.add("Run: /echo creatorcore doctor or /echo creatorcore report");
        lines.add("");
        if (model.diagnostics().isEmpty()) {
            lines.add("No diagnostics.");
            return lines;
        }
        model.diagnostics().stream()
                .sorted(Comparator.comparing(CreatorDiagnostic::severity).reversed()
                        .thenComparing(CreatorDiagnostic::code))
                .limit(120)
                .forEach(diagnostic -> {
                    lines.add(diagnostic.severity() + " " + diagnostic.code());
                    lines.add("  " + diagnostic.message());
                    diagnostic.suggestion().ifPresent(suggestion -> lines.add("  Suggestion: " + suggestion));
                });
        return lines;
    }
}
