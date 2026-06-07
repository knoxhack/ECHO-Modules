package com.knoxhack.echocommunitybridge;

import dev.echo.nativeplatform.contracts.EchoNativeSurfaceModuleEntrypoint;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoCommunityBridgeNativeModule implements EchoNativeSurfaceModuleEntrypoint {
    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        Map<String, Object> referenceProbe = CommunityBridgeAdapterCoreContracts.referenceProbe();
        boolean probePassed = CommunityBridgeAdapterCoreContracts.referenceProbePassed(referenceProbe);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activated", probePassed);
        result.put("activationStage", "community_bridge_native_contract_active");
        result.put("adapterCoreUsed", true);
        result.put("nativeAdapterCodeExecuted", true);
        result.put("moduleId", EchoCommunityBridge.MODID);
        result.put("packId", context.getOrDefault("packId", "unknown"));
        result.put("registeredFeatureContracts", CommunityBridgeAdapterCoreContracts.CONTRACT_IDS);
        result.put("logicalRegistrationCount", CommunityBridgeAdapterCoreContracts.CONTRACT_IDS.size());
        result.put("adapterDomains", List.of("networking", "diagnostics", "data"));
        result.put("runtimeTargets", List.of("echo_native", "echo_runtime_standalone"));
        result.put("referenceProbe", referenceProbe);
        result.put("publicTextSanitized", referenceProbe.get("publicTextSanitized"));
        result.put("discordTextSanitized", referenceProbe.get("discordTextSanitized"));
        result.put("playerNameSanitized", referenceProbe.get("playerNameSanitized"));
        result.put("launcherChatAccepted", referenceProbe.get("launcherChatAccepted"));
        result.put("launcherSlashCommandBlocked", referenceProbe.get("launcherSlashCommandBlocked"));
        result.put("androidChatLabeled", referenceProbe.get("androidChatLabeled"));
        result.put("unsupportedPublicSourceRejected", referenceProbe.get("unsupportedPublicSourceRejected"));
        result.put("requiresServerStatusBridge", true);
        result.put("requiresLauncherChatBridge", true);
        result.put("requiresDiscordSanitizationBridge", true);
        result.put("registryInjected", false);
        result.put("registryMutated", false);
        result.put("serviceCodeExecuted", true);
        result.put("transformsPerformed", false);
        result.put("summary", "CommunityBridge native contract exercised server status, launcher chat, Discord sanitization, and player identity behavior for AdapterCore.");
        return Map.copyOf(result);
    }

    public static void main(String[] args) {
        Map<String, Object> activation = new EchoCommunityBridgeNativeModule()
                .describeNativeSurfaces(Map.of("packId", "agent2-community-bridge-smoke"));
        require(Boolean.TRUE.equals(activation.get("activated")),
                "CommunityBridge native adapter should activate");
        require(Boolean.TRUE.equals(activation.get("publicTextSanitized")),
                "CommunityBridge native adapter should sanitize public text");
        require(Boolean.TRUE.equals(activation.get("discordTextSanitized")),
                "CommunityBridge native adapter should sanitize Discord text");
        require(Boolean.TRUE.equals(activation.get("launcherSlashCommandBlocked")),
                "CommunityBridge native adapter should block slash-command relay");
        System.out.println("communitybridge native adapter smoke PASS contracts="
                + CommunityBridgeAdapterCoreContracts.CONTRACT_IDS.size());
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
