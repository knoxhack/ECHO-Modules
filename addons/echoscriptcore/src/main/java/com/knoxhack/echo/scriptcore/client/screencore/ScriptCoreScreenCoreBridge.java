package com.knoxhack.echo.scriptcore.client.screencore;

import com.knoxhack.echo.scriptcore.EchoScriptCore;
import com.knoxhack.echo.scriptcore.network.ScriptCoreUiActionPacket;
import com.knoxhack.echo.scriptcore.runtime.ScriptCoreUiExecutionService;
import com.knoxhack.echonetcore.client.EchoNetClientActions;
import com.knoxhack.echoscreencore.api.EchoScreenRegistry;
import com.knoxhack.echoscreencore.api.action.EchoActionContext;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.Identifier;

public final class ScriptCoreScreenCoreBridge {
    private static boolean registered;

    private ScriptCoreScreenCoreBridge() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        ScriptCoreScreenCoreClientState.markBridgeRegistered();
        EchoScreenRegistry.registerDataProvider("scriptcore", ScriptCoreScreenCoreClientState::resolve);
        EchoScreenRegistry.registerDataProvider(EchoScriptCore.id("scriptcore"), ScriptCoreScreenCoreClientState::resolve);
        EchoScreenRegistry.registerAction(ScriptCoreUiExecutionService.SCREENCORE_ACTION, ScriptCoreScreenCoreBridge::execute);
        EchoScreenRegistry.registerAction(ScriptCoreUiExecutionService.SCREENCORE_PREVIEW_ACTION, ScriptCoreScreenCoreBridge::preview);
        EchoScriptCore.LOGGER.info("ScriptCore ScreenCore UI action bridge registered.");
    }

    private static boolean execute(EchoActionContext context) {
        return send(context, ScriptCoreUiExecutionService.UiExecutionMode.EXECUTE);
    }

    private static boolean preview(EchoActionContext context) {
        return send(context, ScriptCoreUiExecutionService.UiExecutionMode.PREVIEW);
    }

    private static boolean send(EchoActionContext context, ScriptCoreUiExecutionService.UiExecutionMode mode) {
        if (context == null) {
            return false;
        }
        String rawDefinition = context.actionValue();
        if (rawDefinition == null || rawDefinition.isBlank()) {
            rawDefinition = context.param("definition");
        }
        Identifier definitionId = Identifier.tryParse(rawDefinition == null ? "" : rawDefinition.trim());
        if (definitionId == null) {
            EchoScriptCore.LOGGER.debug("Rejected ScreenCore scriptcore.execute action with invalid definition id: {}", rawDefinition);
            return false;
        }
        return EchoNetClientActions.trySendServerboundAction(new ScriptCoreUiActionPacket(
                mode,
                definitionId,
                context.param("slot"),
                context.pageId() == null ? "" : context.pageId().toString(),
                context.componentId(),
                rawDefinition,
                uiParams(context)));
    }

    private static Map<String, String> uiParams(EchoActionContext context) {
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        if (context == null || context.params() == null) {
            return Map.of();
        }
        for (Map.Entry<String, String> entry : context.params().entrySet()) {
            String key = entry.getKey();
            if (key == null || !key.startsWith("param-")) {
                continue;
            }
            if (params.size() >= ScriptCoreUiExecutionService.MAX_PARAMS_PER_TRIGGER) {
                break;
            }
            String paramId = key.substring("param-".length()).trim();
            if (!paramId.isBlank()) {
                params.put(paramId, entry.getValue() == null ? "" : entry.getValue());
            }
        }
        return Map.copyOf(params);
    }

    public static Map<String, String> uiParamsForTests(EchoActionContext context) {
        return uiParams(context);
    }
}
