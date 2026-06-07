package com.knoxhack.echo.creatorcore.session;

import com.knoxhack.echo.creatorcore.api.CreatorProject;
import com.knoxhack.echo.creatorcore.config.CreatorCoreConfig;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class CreatorProjectManager {
    private List<CreatorProject> projects = List.of();

    public synchronized void refresh() {
        String draftRoot = CreatorCoreConfig.string(CreatorCoreConfig.DRAFT_ROOT, "config/echo/creatorcore/drafts");
        String exportRoot = CreatorCoreConfig.string(CreatorCoreConfig.EXPORT_ROOT, "config/echo/scripts");
        boolean writable = CreatorCoreConfig.bool(CreatorCoreConfig.ALLOW_DRAFT_WRITES, false);
        List<CreatorProject> next = new ArrayList<>();
        next.add(new CreatorProject("local_drafts", "Local Creator Drafts", "default",
                Path.of("").toAbsolutePath().resolve(draftRoot).normalize().toString(), writable));
        next.add(new CreatorProject("script_exports", "Script Export Root", "default",
                Path.of("").toAbsolutePath().resolve(exportRoot).normalize().toString(),
                CreatorCoreConfig.bool(CreatorCoreConfig.ALLOW_EXPORTS, false)));
        projects = List.copyOf(next);
    }

    public synchronized List<CreatorProject> projects() {
        if (projects.isEmpty()) {
            refresh();
        }
        return projects;
    }
}
