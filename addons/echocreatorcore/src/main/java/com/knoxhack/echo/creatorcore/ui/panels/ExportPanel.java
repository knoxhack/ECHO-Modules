package com.knoxhack.echo.creatorcore.ui.panels;

import com.knoxhack.echo.creatorcore.EchoCreatorCore;
import com.knoxhack.echo.creatorcore.api.CreatorCoreApi;
import com.knoxhack.echo.creatorcore.config.CreatorCoreConfig;
import com.knoxhack.echo.creatorcore.ui.CreatorDashboardModel;
import com.knoxhack.echo.creatorcore.ui.CreatorPanel;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;

public final class ExportPanel implements CreatorPanel {
    @Override
    public Identifier id() {
        return EchoCreatorCore.id("export");
    }

    @Override
    public String title() {
        return "Export";
    }

    @Override
    public String summary() {
        return "Script-compatible JSON export status.";
    }

    @Override
    public List<String> lines(CreatorDashboardModel model) {
        List<String> lines = new ArrayList<>();
        lines.add("Export mode: " + (model.exportsLocked() ? "LOCKED" : "ALLOWED"));
        lines.add("Export root: " + CreatorCoreConfig.string(CreatorCoreConfig.EXPORT_ROOT, "config/echo/scripts"));
        lines.add("Pending drafts: " + model.drafts().stream().filter(draft -> draft.status() != com.knoxhack.echo.creatorcore.api.CreatorDraft.DraftStatus.EXPORTED).count());
        lines.add("");
        lines.add("Export: /echo creatorcore drafts export <id>");
        lines.add("CreatorCore 1.0.0 writes conservative draft JSON directly when exports are unlocked.");
        lines.add("");
        var last = CreatorCoreApi.get().exports().lastResult();
        lines.add("Last export: " + (last.success() ? "success" : "not successful"));
        lines.add("  " + last.message());
        if (!last.targetPath().isBlank()) {
            lines.add("  target: " + last.targetPath());
        }
        return lines;
    }
}
