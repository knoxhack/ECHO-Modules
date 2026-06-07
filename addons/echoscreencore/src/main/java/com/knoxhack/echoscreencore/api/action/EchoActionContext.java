package com.knoxhack.echoscreencore.api.action;

import com.knoxhack.echoscreencore.api.EchoDataContext;
import java.util.Map;
import net.minecraft.resources.Identifier;

public record EchoActionContext(
    Identifier pageId,
    String componentId,
    EchoDataContext dataContext,
    EchoDataContext itemContext,
    String action,
    String argument,
    String actionValue,
    Map<String, String> params,
    String inputEvent,
    ScreenControls controls
) {
    public EchoActionContext {
        params = params == null ? Map.of() : Map.copyOf(params);
    }

    public EchoActionContext(Identifier pageId, EchoDataContext dataContext, String action, String argument, ScreenControls controls) {
        this(pageId, "", dataContext, null, action, argument, argument, Map.of(), "", controls);
    }

    public String param(String key) {
        return params.getOrDefault(key, "");
    }

    public boolean close() {
        return controls != null && controls.close();
    }

    public boolean back() {
        return controls != null && controls.back();
    }

    public boolean open(Identifier nextPage) {
        return controls != null && controls.open(nextPage, dataContext);
    }

    public boolean toggleDebug() {
        return controls != null && controls.toggleDebug();
    }

    public interface ScreenControls {
        boolean close();

        boolean back();

        boolean open(Identifier pageId, EchoDataContext context);

        boolean toggleDebug();
    }
}
