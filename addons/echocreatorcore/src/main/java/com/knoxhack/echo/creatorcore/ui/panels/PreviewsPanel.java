package com.knoxhack.echo.creatorcore.ui.panels;

import com.knoxhack.echo.creatorcore.EchoCreatorCore;
import com.knoxhack.echo.creatorcore.api.CreatorPreviewSummary;
import com.knoxhack.echo.creatorcore.ui.CreatorDashboardModel;
import com.knoxhack.echo.creatorcore.ui.CreatorPanel;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;

public final class PreviewsPanel implements CreatorPanel {
    @Override
    public Identifier id() {
        return EchoCreatorCore.id("previews");
    }

    @Override
    public String title() {
        return "Previews";
    }

    @Override
    public String summary() {
        return "Read-only Mission, Lens, and HoloMap preview signals.";
    }

    @Override
    public List<String> lines(CreatorDashboardModel model) {
        List<String> lines = new ArrayList<>();
        if (model.previews().isEmpty()) {
            lines.add("No preview-capable adapters are currently exposing runtime data.");
            lines.add("MissionCore, Lens, and HoloMap previews stay read-only in CreatorCore 0.2.0.");
            return lines;
        }
        for (CreatorPreviewSummary preview : model.previews().stream().limit(100).toList()) {
            lines.add(preview.id() + " | " + preview.type() + " | " + preview.title()
                    + " | " + preview.sourceAdapter() + " | " + preview.scope());
            preview.lines().stream().limit(4).forEach(line -> lines.add("  " + line));
        }
        return lines;
    }
}

