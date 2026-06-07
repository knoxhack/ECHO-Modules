package com.knoxhack.echogalacticcore.runtime;

import com.knoxhack.echogalacticcore.GalacticCoreIds;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class GalacticCoreLiveHostAdapters {
    private final GalacticCoreHostBindingContracts bindingContracts;

    public GalacticCoreLiveHostAdapters(GalacticCoreHostBindingContracts bindingContracts) {
        this.bindingContracts = Objects.requireNonNull(bindingContracts, "bindingContracts");
    }

    public List<LiveHostAdapterPlan> releaseLiveHostAdapterSmokePlans() {
        return bindingContracts.releaseHostBindingSmokeContracts().stream()
                .map(this::adapt)
                .toList();
    }

    public Map<String, Object> evidence() {
        return Map.of(
                "source", "galacticraft_legacy_live_host_adapters",
                "typedReceiptsOnly", true,
                "worldAdapter", "load destination, ticket chunk, teleport player, sync route progress",
                "entityAdapter", "load boss room, construct boss entity, attach encounter state, lock treasure room",
                "screenAdapter", "mount renderer, mount widgets, wire actions, sync screen state",
                "replaces", "WorldProvider transfer implementation, EntityBoss spawn wiring, Gui* menu opening"
        );
    }

    private LiveHostAdapterPlan adapt(GalacticCoreHostBindingContracts.HostBindingContract binding) {
        return switch (binding.evidence().get("hostSurface").toString()) {
            case "world" -> worldAdapter(binding);
            case "entity" -> entityAdapter(binding);
            case "screen" -> screenAdapter(binding);
            default -> throw new IllegalArgumentException("Unsupported host surface: " + binding.evidence().get("hostSurface"));
        };
    }

    private LiveHostAdapterPlan worldAdapter(GalacticCoreHostBindingContracts.HostBindingContract binding) {
        return new LiveHostAdapterPlan(
                GalacticCoreIds.id("live_host/world_dimension_transfer"),
                binding,
                "echo.native.worldgen",
                "worldgen",
                "placeStructure",
                GalacticCoreIds.id("live_host/world_dimension_transfer"),
                List.of("resolve_destination_level", "ticket_destination_chunk", "place_player_at_anchor", "sync_progression_attachment"),
                binding.saveDataTarget(),
                evidence(binding, "world", "ASDK live world adapter for dimension transfer")
        );
    }

    private LiveHostAdapterPlan entityAdapter(GalacticCoreHostBindingContracts.HostBindingContract binding) {
        return new LiveHostAdapterPlan(
                GalacticCoreIds.id("live_host/entity_boss_spawn"),
                binding,
                "echo.native.capabilities",
                "capabilities",
                "registerIntegration",
                GalacticCoreIds.id("live_host/entity_boss_spawn"),
                List.of("resolve_boss_room", "instantiate_boss_entity", "attach_boss_state", "lock_treasure_room"),
                binding.saveDataTarget(),
                evidence(binding, "entity", "ASDK live entity adapter for dungeon boss spawn")
        );
    }

    private LiveHostAdapterPlan screenAdapter(GalacticCoreHostBindingContracts.HostBindingContract binding) {
        String screenId = String.valueOf(binding.evidence().get("screenId"));
        return new LiveHostAdapterPlan(
                GalacticCoreIds.id("live_host/screen_" + screenId),
                binding,
                "echo.native.screens",
                "screens",
                "open",
                GalacticCoreIds.id("live_host/screen_" + screenId),
                List.of("resolve_screen_factory", "mount_renderer", "mount_widgets", "wire_actions", "sync_screen_state"),
                binding.saveDataTarget(),
                evidence(binding, "screen", "ASDK live screen adapter for " + screenId)
        );
    }

    private static Map<String, Object> evidence(
            GalacticCoreHostBindingContracts.HostBindingContract binding,
            String hostSurface,
            String replacement
    ) {
        return Map.ofEntries(
                Map.entry("source", "galacticraft_legacy_live_host_adapters"),
                Map.entry("typedReceiptsOnly", true),
                Map.entry("bindingSource", binding.evidence().get("source")),
                Map.entry("bindingId", binding.bindingId()),
                Map.entry("bindingTarget", binding.target()),
                Map.entry("bindingKind", binding.bindingKind()),
                Map.entry("bindingOwnerService", binding.serviceId()),
                Map.entry("hostSurface", hostSurface),
                Map.entry("requiredHostActions", binding.requiredHostActions()),
                Map.entry("adapterSteps", binding.requiredHostActions()),
                Map.entry("saveDataTarget", binding.saveDataTarget()),
                Map.entry("replacement", replacement)
        );
    }

    public record LiveHostAdapterPlan(
            String adapterId,
            GalacticCoreHostBindingContracts.HostBindingContract binding,
            String serviceId,
            String surface,
            String action,
            String target,
            List<String> executorSteps,
            String saveDataTarget,
            Map<String, Object> evidence
    ) {
        public LiveHostAdapterPlan {
            adapterId = requireText(adapterId, "adapterId");
            binding = Objects.requireNonNull(binding, "binding");
            serviceId = requireText(serviceId, "serviceId");
            surface = requireText(surface, "surface");
            action = requireText(action, "action");
            target = requireText(target, "target");
            executorSteps = List.copyOf(executorSteps == null ? List.of() : executorSteps);
            saveDataTarget = requireText(saveDataTarget, "saveDataTarget");
            evidence = Map.copyOf(evidence == null ? Map.of() : evidence);
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
