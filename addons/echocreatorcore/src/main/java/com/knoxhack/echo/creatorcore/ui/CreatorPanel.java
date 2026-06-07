package com.knoxhack.echo.creatorcore.ui;

import java.util.List;
import net.minecraft.resources.Identifier;

public interface CreatorPanel {
    Identifier id();

    String title();

    default String summary() {
        return "";
    }

    List<String> lines(CreatorDashboardModel model);
}
