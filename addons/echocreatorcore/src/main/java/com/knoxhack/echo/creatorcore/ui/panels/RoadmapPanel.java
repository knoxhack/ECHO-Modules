package com.knoxhack.echo.creatorcore.ui.panels;

import com.knoxhack.echo.creatorcore.EchoCreatorCore;
import com.knoxhack.echo.creatorcore.ui.CreatorDashboardModel;
import com.knoxhack.echo.creatorcore.ui.CreatorPanel;
import java.util.List;
import net.minecraft.resources.Identifier;

public final class RoadmapPanel implements CreatorPanel {
    @Override
    public Identifier id() {
        return EchoCreatorCore.id("roadmap");
    }

    @Override
    public String title() {
        return "Roadmap";
    }

    @Override
    public String summary() {
        return "CreatorCore milestone track.";
    }

    @Override
    public List<String> lines(CreatorDashboardModel model) {
        return List.of(
                "1.0.0 Current",
                "  Dashboard shell, diagnostics, adapter registry, drafts, export foundation.",
                "",
                "0.2.0",
                "  ScriptCore full integration, Terminal entry, mission read-only preview, definition detail editor.",
                "",
                "0.3.0",
                "  Mission Studio form editor, Lore Archive Studio, Lens Scan Studio, HoloMap Marker Studio.",
                "",
                "0.4.0",
                "  Mission Graph Editor, condition/action visual builder, faction/world-state/weather editors.",
                "",
                "1.0.0",
                "  Full Creator Mode, AI-assisted content generation hooks, Command Center integration, ECHO Launcher publishing.");
    }
}
