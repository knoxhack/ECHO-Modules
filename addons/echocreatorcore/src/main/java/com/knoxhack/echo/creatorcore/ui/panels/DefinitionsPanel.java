package com.knoxhack.echo.creatorcore.ui.panels;

import com.knoxhack.echo.creatorcore.EchoCreatorCore;
import com.knoxhack.echo.creatorcore.ui.CreatorDashboardModel;
import com.knoxhack.echo.creatorcore.ui.CreatorPanel;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;

public final class DefinitionsPanel implements CreatorPanel {
    @Override
    public Identifier id() {
        return EchoCreatorCore.id("definitions");
    }

    @Override
    public String title() {
        return "Definitions";
    }

    @Override
    public String summary() {
        return "Definition summaries exposed by CreatorCore adapters.";
    }

    @Override
    public List<String> lines(CreatorDashboardModel model) {
        List<String> lines = new ArrayList<>();
        lines.add("id | type | title | adapter | pack | status");
        lines.add("");
        if (model.definitions().isEmpty()) {
            lines.add("No definitions are exposed yet.");
            lines.add("ScriptCore can provide the full browser once its public API is wired.");
            return lines;
        }
        model.definitions().stream().limit(80).forEach(definition -> lines.add(definition.id()
                + " | " + definition.type()
                + " | " + definition.title()
                + " | " + definition.sourceAdapter()
                + " | " + definition.pack()
                + " | " + definition.status()));
        lines.add("");
        lines.add("Detail providers: " + model.definitionDetails().size()
                + " loaded. Open the Detail panel for raw JSON/source/preview lines.");
        return lines;
    }
}
