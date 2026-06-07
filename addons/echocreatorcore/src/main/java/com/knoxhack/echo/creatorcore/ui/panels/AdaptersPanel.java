package com.knoxhack.echo.creatorcore.ui.panels;

import com.knoxhack.echo.creatorcore.EchoCreatorCore;
import com.knoxhack.echo.creatorcore.ui.CreatorDashboardModel;
import com.knoxhack.echo.creatorcore.ui.CreatorPanel;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;

public final class AdaptersPanel implements CreatorPanel {
    @Override
    public Identifier id() {
        return EchoCreatorCore.id("adapters");
    }

    @Override
    public String title() {
        return "Adapters";
    }

    @Override
    public String summary() {
        return "Optional addon bridge status and capabilities.";
    }

    @Override
    public List<String> lines(CreatorDashboardModel model) {
        List<String> lines = new ArrayList<>();
        model.adapters().forEach(adapter -> {
            lines.add(adapter.displayName() + " [" + (adapter.isAvailable() ? "available" : "stub") + "]");
            lines.add("  id: " + adapter.id());
            lines.add("  capabilities: " + (adapter.capabilities().isEmpty() ? "none" : adapter.capabilities()));
            lines.add("  status: " + adapter.status());
            lines.add("");
        });
        return lines;
    }
}
