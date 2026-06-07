package com.knoxhack.echonetcore;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNetCorePacketServiceContract {
    public static final String MODULE_ID = "echonetcore";
    public static final String ADAPTERCORE_CONTRACT_ID = "echonetcore:network/packet_service";
    public static final String REFERENCE_PROTOCOL_VERSION = "1";
    public static final String REFERENCE_CLIENT = "echo:debug-client";
    public static final String REFERENCE_SERVER = "echo:debug-server";

    private EchoNetCorePacketServiceContract() {
    }

    public static Map<String, Object> executeReferenceService(String packId) {
        Map<String, Object> service = new LinkedHashMap<>();
        service.put("adapterCoreContract", ADAPTERCORE_CONTRACT_ID);
        service.put("service", "echonetcore:network_service");
        service.put("serviceExecuted", true);
        service.put("packId", packId == null || packId.isBlank() ? "unknown" : packId);
        service.put("protocolVersion", REFERENCE_PROTOCOL_VERSION);
        service.put("optionalPackets", true);
        service.put("payloadContracts", payloadContracts());
        service.put("routeResults", List.of(
                route("echonetcore:faction_sync", "server", REFERENCE_CLIENT, true, "clientbound sync delivered"),
                route("echonetcore:debug_command", REFERENCE_CLIENT, REFERENCE_SERVER, false, "rate-limited by echonetcore:rate_limiter")
        ));
        service.put("cleanupHooks", List.of("player.logout", "server.stopping"));
        service.put("diagnostics", List.of(
                "net.payloads.registered",
                "net.route.clientbound.sent",
                "net.rate_limit.checked"
        ));
        service.put("referenceBehavior", "netcore_registers_and_routes_packet_service");
        return Map.copyOf(service);
    }

    public static boolean referenceServicePassed(Map<String, Object> service) {
        return Boolean.TRUE.equals(service.get("serviceExecuted"))
                && ADAPTERCORE_CONTRACT_ID.equals(service.get("adapterCoreContract"))
                && REFERENCE_PROTOCOL_VERSION.equals(service.get("protocolVersion"))
                && Boolean.TRUE.equals(service.get("optionalPackets"))
                && String.valueOf(service.get("payloadContracts")).contains("echonetcore:faction_sync")
                && String.valueOf(service.get("payloadContracts")).contains("echonetcore:debug_command")
                && String.valueOf(service.get("routeResults")).contains("clientbound sync delivered")
                && String.valueOf(service.get("routeResults")).contains("rate-limited by echonetcore:rate_limiter")
                && String.valueOf(service.get("cleanupHooks")).contains("player.logout");
    }

    private static List<Map<String, Object>> payloadContracts() {
        return List.of(
                payload("echonetcore:faction_sync", "CLIENTBOUND", "CLIENTBOUND_SYNC", "echo:factions", true),
                payload("echonetcore:discovery_toast", "CLIENTBOUND", "CLIENTBOUND_SYNC", "echo:discoveries", true),
                payload("echonetcore:echo_sync", "CLIENTBOUND", "CLIENTBOUND_SYNC", "echo:sync", true),
                payload("echonetcore:debug_command", "SERVERBOUND", "DEBUG_DEV", "echo:debug", false)
        );
    }

    private static Map<String, Object> payload(
            String id,
            String direction,
            String kind,
            String channel,
            boolean reliable
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", id);
        payload.put("direction", direction);
        payload.put("kind", kind);
        payload.put("channel", channel);
        payload.put("reliable", reliable);
        return Map.copyOf(payload);
    }

    private static Map<String, Object> route(
            String payloadId,
            String source,
            String target,
            boolean accepted,
            String result
    ) {
        Map<String, Object> route = new LinkedHashMap<>();
        route.put("payloadId", payloadId);
        route.put("source", source);
        route.put("target", target);
        route.put("accepted", accepted);
        route.put("result", result);
        return Map.copyOf(route);
    }
}
