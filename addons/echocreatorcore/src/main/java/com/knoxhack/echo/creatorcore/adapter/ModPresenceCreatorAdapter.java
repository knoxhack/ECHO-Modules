package com.knoxhack.echo.creatorcore.adapter;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.knoxhack.echo.creatorcore.EchoCreatorCore;
import com.knoxhack.echo.creatorcore.api.CreatorAdapter;
import com.knoxhack.echo.creatorcore.api.CreatorDiagnostic;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.Identifier;
import com.echoplatform.echocore.api.EchoRuntimeModules;

abstract class ModPresenceCreatorAdapter implements CreatorAdapter {
    private final Identifier id;
    private final String modId;
    private final String displayName;
    private final String apiClassName;
    private final Set<String> capabilities;
    private final String missingStatus;
    private final String detectedStatus;
    private final boolean availableWhenDetected;

    ModPresenceCreatorAdapter(
            String path,
            String modId,
            String displayName,
            String apiClassName,
            Set<String> capabilities,
            String missingStatus,
            String detectedStatus,
            boolean availableWhenDetected) {
        this.id = EchoCreatorCore.id(path);
        this.modId = modId;
        this.displayName = displayName;
        this.apiClassName = apiClassName;
        this.capabilities = Set.copyOf(capabilities);
        this.missingStatus = missingStatus;
        this.detectedStatus = detectedStatus;
        this.availableWhenDetected = availableWhenDetected;
    }

    @Override
    public Identifier id() {
        return id;
    }

    @Override
    public String displayName() {
        return displayName;
    }

    @Override
    public boolean isAvailable() {
        return detectedMod() && (availableWhenDetected || apiWired());
    }

    @Override
    public String status() {
        if (!detectedMod()) {
            return missingStatus;
        }
        if (apiClassName != null && !apiWired()) {
            return displayName + " detected, but no public CreatorCore adapter API is wired yet.";
        }
        return detectedStatus;
    }

    @Override
    public Set<String> capabilities() {
        return isAvailable() ? capabilities : Set.of();
    }

    @Override
    public List<CreatorDiagnostic> diagnostics() {
        if (!detectedMod()) {
            return List.of(CreatorDiagnostic.info("creatorcore.optional_missing." + modId,
                    displayName + " is not installed. Related creator panels remain in stub mode.", displayName));
        }
        if (apiClassName != null && !apiWired()) {
            return List.of(CreatorDiagnostic.warning("creatorcore.api_unwired." + modId,
                    displayName + " is installed, but CreatorCore 1.0.0 could not find a stable public API hook.",
                    displayName, "Keep using the dashboard shell; wire this adapter when the target addon exposes authoring APIs."));
        }
        return List.of();
    }

    @Override
    public JsonObject debugInfo() {
        JsonObject object = CreatorAdapter.super.debugInfo();
        object.addProperty("targetMod", modId);
        object.addProperty("detectedMod", detectedMod());
        object.addProperty("apiClass", apiClassName == null ? "" : apiClassName);
        object.addProperty("apiWired", apiWired());
        JsonArray warnings = new JsonArray();
        diagnostics().forEach(diagnostic -> warnings.add(diagnostic.message()));
        object.add("warnings", warnings);
        return object;
    }

    protected boolean detectedMod() {
        return modId == null || EchoRuntimeModules.isLoaded(modId);
    }

    protected boolean apiWired() {
        if (apiClassName == null || apiClassName.isBlank()) {
            return detectedMod();
        }
        try {
            Class.forName(apiClassName, false, Thread.currentThread().getContextClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError exception) {
            return false;
        }
    }
}
