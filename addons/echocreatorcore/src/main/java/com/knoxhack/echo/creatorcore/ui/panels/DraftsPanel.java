package com.knoxhack.echo.creatorcore.ui.panels;

import com.knoxhack.echo.creatorcore.EchoCreatorCore;
import com.knoxhack.echo.creatorcore.ui.CreatorDashboardModel;
import com.knoxhack.echo.creatorcore.ui.CreatorPanel;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;

public final class DraftsPanel implements CreatorPanel {
    @Override
    public Identifier id() {
        return EchoCreatorCore.id("drafts");
    }

    @Override
    public String title() {
        return "Drafts";
    }

    @Override
    public String summary() {
        return "Draft templates and local draft store status.";
    }

    @Override
    public List<String> lines(CreatorDashboardModel model) {
        List<String> lines = new ArrayList<>();
        lines.add("Draft writes: " + (model.writeLocked() ? "LOCKED" : "ALLOWED"));
        lines.add("Templates: mission, archive_entry, lens_scan, holomap_marker, weather_event, faction, world_state, tutorial_hint");
        lines.add("");
        lines.add("Create: /echo creatorcore drafts create <type> <pack> <id>");
        lines.add("Validate: /echo creatorcore drafts validate <id>");
        lines.add("");
        if (model.drafts().isEmpty()) {
            lines.add("No drafts yet.");
            return lines;
        }
        model.drafts().stream().limit(80).forEach(draft -> lines.add(draft.id()
                + " | " + draft.type()
                + " | pack=" + draft.pack()
                + " | status=" + draft.status()
                + " | " + draft.title()));
        return lines;
    }
}
