package com.knoxhack.echo.adaptercore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeEventBridge {
    private final String moduleId;
    private final List<Map<String, Object>> hooks = new ArrayList<>();

    public EchoNativeEventBridge(String moduleId) {
        this.moduleId = AdapterContractGuards.requireText(moduleId, "module id");
    }

    public EchoNativeEventBridge hook(String event, String handler, String summary) {
        Map<String, Object> hook = new LinkedHashMap<>();
        hook.put("event", AdapterContractGuards.requireText(event, "event"));
        hook.put("handler", AdapterContractGuards.requireText(handler, "handler"));
        hook.put("summary", AdapterContractGuards.optionalText(summary));
        hook.put("planned", true);
        hook.put("executionMode", "native_event_host_subscription");
        hooks.add(hook);
        return this;
    }

    public Map<String, Object> describe() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("moduleId", moduleId);
        data.put("hookCount", hooks.size());
        data.put("hooks", List.copyOf(hooks));
        data.put("bridge", "adaptercore.native_event");
        data.put("executionMode", "native_event_host_subscription");
        data.put("summary", "Native event bridge declares module event hooks for Native Loader event-host subscription.");
        return data;
    }
}
