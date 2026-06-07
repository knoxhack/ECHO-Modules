package com.knoxhack.echo.creatorcore.ui.panels;

import com.knoxhack.echo.creatorcore.EchoCreatorCore;
import com.knoxhack.echo.creatorcore.api.CreatorDefinitionDetail;
import com.knoxhack.echo.creatorcore.ui.CreatorDashboardModel;
import com.knoxhack.echo.creatorcore.ui.CreatorPanel;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;

public final class DefinitionDetailPanel implements CreatorPanel {
    @Override
    public Identifier id() {
        return EchoCreatorCore.id("definition_detail");
    }

    @Override
    public String title() {
        return "Detail";
    }

    @Override
    public String summary() {
        return "Read-only definition detail and raw-data preview.";
    }

    @Override
    public List<String> lines(CreatorDashboardModel model) {
        List<String> lines = new ArrayList<>();
        if (model.definitionDetails().isEmpty()) {
            lines.add("No definition detail providers are available yet.");
            lines.add("Install or reload ScriptCore, MissionCore, Lens, or HoloMap to populate this view.");
            return lines;
        }
        for (CreatorDefinitionDetail detail : model.definitionDetails()) {
            lines.add(detail.id() + " | " + detail.type() + " | " + detail.title());
            lines.add("  adapter=" + detail.sourceAdapter() + " pack=" + detail.pack() + " status=" + detail.status());
            if (!detail.description().isBlank()) {
                lines.add("  " + detail.description());
            }
            detail.sourceFile().ifPresent(file -> lines.add("  file=" + file));
            if (!detail.tags().isEmpty()) {
                lines.add("  tags=" + String.join(", ", detail.tags()));
            }
            if (!detail.previewLines().isEmpty()) {
                detail.previewLines().stream().limit(4).forEach(line -> lines.add("  " + line));
            }
            lines.add("  raw fields=" + detail.rawJson().entrySet().size()
                    + " diagnostics=" + detail.diagnostics().size()
                    + " readOnly=" + detail.readOnly());
            lines.add("");
        }
        return lines;
    }
}

