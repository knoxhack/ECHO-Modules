package com.knoxhack.echo.adaptercore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeLifecycleBridge {
    private final String moduleId;
    private final List<Map<String, Object>> phases = new ArrayList<>();

    public EchoNativeLifecycleBridge(String moduleId) {
        this.moduleId = AdapterContractGuards.requireText(moduleId, "module id");
    }

    public EchoNativeLifecycleBridge phase(String id, String summary) {
        Map<String, Object> phase = new LinkedHashMap<>();
        phase.put("id", AdapterContractGuards.requireText(id, "lifecycle phase id"));
        phase.put("summary", AdapterContractGuards.optionalText(summary));
        phase.put("planned", true);
        phase.put("executionMode", "native_lifecycle_host_record");
        phases.add(phase);
        return this;
    }

    public Map<String, Object> describe() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("moduleId", moduleId);
        data.put("phaseCount", phases.size());
        data.put("phases", List.copyOf(phases));
        data.put("bridge", "adaptercore.native_lifecycle");
        data.put("executionMode", "native_lifecycle_host_record");
        data.put("summary", "Native lifecycle bridge declares deterministic phases for Native Loader lifecycle-host recording.");
        return data;
    }
}
