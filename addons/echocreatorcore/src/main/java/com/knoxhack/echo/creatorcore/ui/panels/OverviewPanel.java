package com.knoxhack.echo.creatorcore.ui.panels;

import com.knoxhack.echo.creatorcore.EchoCreatorCore;
import com.knoxhack.echo.creatorcore.ui.CreatorDashboardModel;
import com.knoxhack.echo.creatorcore.ui.CreatorPanel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.resources.Identifier;

public final class OverviewPanel implements CreatorPanel {
    @Override
    public Identifier id() {
        return EchoCreatorCore.id("overview");
    }

    @Override
    public String title() {
        return "Overview";
    }

    @Override
    public String summary() {
        return "Creator mode status, project counts, diagnostics, and quick actions.";
    }

    @Override
    public List<String> lines(CreatorDashboardModel model) {
        List<String> lines = new ArrayList<>();
        lines.add("CreatorCore by ECHO Labs");
        lines.add("Visual authoring/dashboard layer for ScriptCore and ECHO runtime systems.");
        lines.add("");
        lines.add("Adapters: " + model.doctorReport().adaptersAvailable() + "/" + model.doctorReport().adaptersTotal() + " available");
        lines.add("Definitions: " + model.definitions().size() + " across installed adapters");
        lines.add("Drafts: " + model.drafts().size() + " (" + (model.writeLocked() ? "write locked" : "write allowed") + ")");
        lines.add("Diagnostics: " + model.doctorReport().errors() + " errors, "
                + model.doctorReport().warnings() + " warnings, " + model.doctorReport().info() + " info");
        lines.add("");
        Map<String, Long> byType = model.definitions().stream()
                .collect(Collectors.groupingBy(definition -> definition.type(), Collectors.counting()));
        lines.add("Definition counts by type:");
        if (byType.isEmpty()) {
            lines.add("  No external definitions are exposed yet.");
        } else {
            byType.forEach((type, count) -> lines.add("  " + type + ": " + count));
        }
        lines.add("");
        lines.add("Quick actions: /echo creatorcore doctor, /echo creatorcore drafts list, /echo creatorcore adapters");
        return lines;
    }
}
