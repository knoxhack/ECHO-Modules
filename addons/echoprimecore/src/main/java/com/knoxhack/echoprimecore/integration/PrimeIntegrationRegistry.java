package com.knoxhack.echoprimecore.integration;

import com.echoplatform.echocore.api.EchoRuntimeModules;
import com.echoplatform.echocore.api.prime.PrimeAuditRegistry;
import com.echoplatform.echocore.api.prime.PrimeHoloMapRegistry;
import com.echoplatform.echocore.api.prime.PrimeIndexRegistry;
import com.echoplatform.echocore.api.prime.PrimeIntegrationContext;
import com.echoplatform.echocore.api.prime.PrimeLensRegistry;
import com.echoplatform.echocore.api.prime.PrimeLootRegistry;
import com.echoplatform.echocore.api.prime.PrimeMissionRegistry;
import com.echoplatform.echocore.api.prime.PrimeProgressionRegistry;
import com.echoplatform.echocore.api.prime.PrimeRouteRegistry;
import com.echoplatform.echocore.api.prime.PrimeTerminalRegistry;
import com.echoplatform.echocore.api.prime.PrimeWorldRegistry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;

public final class PrimeIntegrationRegistry implements PrimeIntegrationContext,
        PrimeRouteRegistry,
        PrimeMissionRegistry,
        PrimeProgressionRegistry,
        PrimeIndexRegistry,
        PrimeLensRegistry,
        PrimeHoloMapRegistry,
        PrimeTerminalRegistry,
        PrimeLootRegistry,
        PrimeWorldRegistry,
        PrimeAuditRegistry {
    private final Map<Identifier, PrimeRoute> routes = new LinkedHashMap<>();
    private final Map<Identifier, PrimeMissionChain> missionChains = new LinkedHashMap<>();
    private final Map<Identifier, PrimeProgressionFlag> flags = new LinkedHashMap<>();
    private final Map<Identifier, PrimeIndexCategory> categories = new LinkedHashMap<>();
    private final Map<Identifier, PrimeRecipeHint> recipeHints = new LinkedHashMap<>();
    private final Map<Identifier, PrimeScanType> scanTypes = new LinkedHashMap<>();
    private final Map<Identifier, PrimeScanData> scanData = new LinkedHashMap<>();
    private final Map<Identifier, PrimeMapLayer> layers = new LinkedHashMap<>();
    private final Map<Identifier, PrimeMarkerType> markerTypes = new LinkedHashMap<>();
    private final Map<Identifier, PrimeTerminalCard> cards = new LinkedHashMap<>();
    private final Map<Identifier, PrimeLootPool> pools = new LinkedHashMap<>();
    private final Map<Identifier, PrimeLootInjection> injections = new LinkedHashMap<>();
    private final Map<Identifier, PrimeStructure> structures = new LinkedHashMap<>();
    private final Map<Identifier, PrimeWorldSignal> worldSignals = new LinkedHashMap<>();
    private final Map<Identifier, PrimeAuditDiagnostic> diagnostics = new LinkedHashMap<>();

    @Override
    public boolean moduleLoaded(String modId) {
        return EchoRuntimeModules.isLoaded(modId);
    }

    @Override
    public PrimeRouteRegistry routeRegistry() {
        return this;
    }

    @Override
    public PrimeMissionRegistry missionRegistry() {
        return this;
    }

    @Override
    public PrimeProgressionRegistry progressionRegistry() {
        return this;
    }

    @Override
    public PrimeIndexRegistry indexRegistry() {
        return this;
    }

    @Override
    public PrimeLensRegistry lensRegistry() {
        return this;
    }

    @Override
    public PrimeHoloMapRegistry holoMapRegistry() {
        return this;
    }

    @Override
    public PrimeTerminalRegistry terminalRegistry() {
        return this;
    }

    @Override
    public PrimeLootRegistry lootRegistry() {
        return this;
    }

    @Override
    public PrimeWorldRegistry worldRegistry() {
        return this;
    }

    @Override
    public PrimeAuditRegistry auditRegistry() {
        return this;
    }

    @Override
    public boolean registerRoute(PrimeRoute route) {
        return route != null && routes.putIfAbsent(route.id(), route) == null;
    }

    @Override
    public List<PrimeRoute> routes() {
        return ordered(routes.values(), Comparator.comparingInt(PrimeRoute::order).thenComparing(route -> route.id().toString()));
    }

    @Override
    public boolean registerMissionChain(PrimeMissionChain chain) {
        return chain != null && missionChains.putIfAbsent(chain.id(), chain) == null;
    }

    @Override
    public List<PrimeMissionChain> missionChains() {
        return ordered(missionChains.values(), Comparator.comparingInt(PrimeMissionChain::order).thenComparing(chain -> chain.id().toString()));
    }

    @Override
    public boolean registerFlag(PrimeProgressionFlag flag) {
        return flag != null && flags.putIfAbsent(flag.id(), flag) == null;
    }

    @Override
    public List<PrimeProgressionFlag> flags() {
        return ordered(flags.values(), Comparator.comparingInt(PrimeProgressionFlag::order).thenComparing(flag -> flag.id().toString()));
    }

    @Override
    public boolean registerCategory(PrimeIndexCategory category) {
        return category != null && categories.putIfAbsent(category.id(), category) == null;
    }

    @Override
    public boolean registerRecipeHint(PrimeRecipeHint hint) {
        return hint != null && recipeHints.putIfAbsent(hint.id(), hint) == null;
    }

    @Override
    public List<PrimeIndexCategory> categories() {
        return ordered(categories.values(), Comparator.comparingInt(PrimeIndexCategory::order).thenComparing(category -> category.id().toString()));
    }

    @Override
    public List<PrimeRecipeHint> recipeHints() {
        return ordered(recipeHints.values(), Comparator.comparingInt(PrimeRecipeHint::order).thenComparing(hint -> hint.id().toString()));
    }

    @Override
    public boolean registerScanType(PrimeScanType scanType) {
        return scanType != null && scanTypes.putIfAbsent(scanType.id(), scanType) == null;
    }

    @Override
    public boolean registerScanData(PrimeScanData value) {
        return value != null && scanData.putIfAbsent(value.id(), value) == null;
    }

    @Override
    public List<PrimeScanType> scanTypes() {
        return ordered(scanTypes.values(), Comparator.comparingInt(PrimeScanType::order).thenComparing(type -> type.id().toString()));
    }

    @Override
    public List<PrimeScanData> scanData() {
        return ordered(scanData.values(), Comparator.comparing(data -> data.id().toString()));
    }

    @Override
    public boolean registerLayer(PrimeMapLayer layer) {
        return layer != null && layers.putIfAbsent(layer.id(), layer) == null;
    }

    @Override
    public boolean registerMarkerType(PrimeMarkerType markerType) {
        return markerType != null && markerTypes.putIfAbsent(markerType.id(), markerType) == null;
    }

    @Override
    public List<PrimeMapLayer> layers() {
        return ordered(layers.values(), Comparator.comparingInt(PrimeMapLayer::order).thenComparing(layer -> layer.id().toString()));
    }

    @Override
    public List<PrimeMarkerType> markerTypes() {
        return ordered(markerTypes.values(), Comparator.comparingInt(PrimeMarkerType::order).thenComparing(marker -> marker.id().toString()));
    }

    @Override
    public boolean registerCard(PrimeTerminalCard card) {
        return card != null && cards.putIfAbsent(card.id(), card) == null;
    }

    @Override
    public List<PrimeTerminalCard> cards() {
        return ordered(cards.values(), Comparator.comparingInt(PrimeTerminalCard::order).thenComparing(card -> card.id().toString()));
    }

    @Override
    public boolean registerPool(PrimeLootPool pool) {
        return pool != null && pools.putIfAbsent(pool.id(), pool) == null;
    }

    @Override
    public boolean registerInjection(PrimeLootInjection injection) {
        return injection != null && injections.putIfAbsent(injection.id(), injection) == null;
    }

    @Override
    public List<PrimeLootPool> pools() {
        return ordered(pools.values(), Comparator.comparingInt(PrimeLootPool::order).thenComparing(pool -> pool.id().toString()));
    }

    @Override
    public List<PrimeLootInjection> injections() {
        return ordered(injections.values(), Comparator.comparing(injection -> injection.id().toString()));
    }

    @Override
    public boolean registerStructure(PrimeStructure structure) {
        return structure != null && structures.putIfAbsent(structure.id(), structure) == null;
    }

    @Override
    public boolean registerWorldSignal(PrimeWorldSignal signal) {
        return signal != null && worldSignals.putIfAbsent(signal.id(), signal) == null;
    }

    @Override
    public List<PrimeStructure> structures() {
        return ordered(structures.values(), Comparator.comparingInt(PrimeStructure::order).thenComparing(structure -> structure.id().toString()));
    }

    @Override
    public List<PrimeWorldSignal> worldSignals() {
        return ordered(worldSignals.values(), Comparator.comparingInt(PrimeWorldSignal::order).thenComparing(signal -> signal.id().toString()));
    }

    @Override
    public boolean registerDiagnostic(PrimeAuditDiagnostic diagnostic) {
        return diagnostic != null && diagnostics.putIfAbsent(diagnostic.id(), diagnostic) == null;
    }

    @Override
    public List<PrimeAuditDiagnostic> diagnostics() {
        return ordered(diagnostics.values(), Comparator.comparing(diagnostic -> diagnostic.id().toString()));
    }

    private static <T> List<T> ordered(Iterable<T> values, Comparator<T> comparator) {
        List<T> list = new ArrayList<>();
        values.forEach(list::add);
        list.sort(comparator);
        return List.copyOf(list);
    }
}
