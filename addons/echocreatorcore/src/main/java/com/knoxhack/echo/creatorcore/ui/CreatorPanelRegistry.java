package com.knoxhack.echo.creatorcore.ui;

import com.knoxhack.echo.creatorcore.ui.panels.AdaptersPanel;
import com.knoxhack.echo.creatorcore.ui.panels.CodexStudioPanel;
import com.knoxhack.echo.creatorcore.ui.panels.DefinitionDetailPanel;
import com.knoxhack.echo.creatorcore.ui.panels.DefinitionsPanel;
import com.knoxhack.echo.creatorcore.ui.panels.DraftsPanel;
import com.knoxhack.echo.creatorcore.ui.panels.ExportPanel;
import com.knoxhack.echo.creatorcore.ui.panels.MissionStudioPanel;
import com.knoxhack.echo.creatorcore.ui.panels.OverviewPanel;
import com.knoxhack.echo.creatorcore.ui.panels.PreviewsPanel;
import com.knoxhack.echo.creatorcore.ui.panels.RoadmapPanel;
import com.knoxhack.echo.creatorcore.ui.panels.ValidationPanel;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;

public final class CreatorPanelRegistry {
    private final Map<Identifier, CreatorPanel> panels = new LinkedHashMap<>();

    public synchronized void register(CreatorPanel panel) {
        if (panel != null) {
            panels.put(panel.id(), panel);
        }
    }

    public synchronized void registerDefaults() {
        if (!panels.isEmpty()) {
            return;
        }
        register(new OverviewPanel());
        register(new DefinitionsPanel());
        register(new DefinitionDetailPanel());
        register(new ValidationPanel());
        register(new DraftsPanel());
        register(new MissionStudioPanel());
        register(new PreviewsPanel());
        register(new CodexStudioPanel());
        register(new AdaptersPanel());
        register(new ExportPanel());
        register(new RoadmapPanel());
    }

    public synchronized List<CreatorPanel> panels() {
        return List.copyOf(panels.values());
    }

    public synchronized Optional<CreatorPanel> get(Identifier id) {
        return Optional.ofNullable(panels.get(id));
    }
}
