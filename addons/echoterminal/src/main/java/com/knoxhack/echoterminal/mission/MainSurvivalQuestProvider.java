package com.knoxhack.echoterminal.mission;

import com.knoxhack.echoterminal.EchoTerminal;
import com.knoxhack.echoterminal.api.mission.TerminalMissionChapter;
import com.knoxhack.echoterminal.api.mission.TerminalMissionDefinition;
import com.knoxhack.echoterminal.api.mission.TerminalMissionIntelUnlock;
import com.knoxhack.echoterminal.api.mission.TerminalMissionPresentation;
import com.knoxhack.echoterminal.api.mission.TerminalMissionProvider;
import com.knoxhack.echoterminal.api.mission.TerminalMissionRegistry;
import com.knoxhack.echoterminal.api.mission.TerminalMissionRole;
import com.knoxhack.echoterminal.api.mission.TerminalMissionRoutePlacement;
import com.knoxhack.echoterminal.api.mission.TerminalMissionSnapshot;
import com.knoxhack.echoterminal.api.mission.TerminalMissionStatus;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;

public final class MainSurvivalQuestProvider implements TerminalMissionProvider {
    public static final MainSurvivalQuestProvider INSTANCE = new MainSurvivalQuestProvider();
    public static final Identifier CHAPTER_ID =
            Identifier.fromNamespaceAndPath(EchoTerminal.MODID, "main_survival_route");
    public static final Identifier TAB_ID =
            Identifier.fromNamespaceAndPath(EchoTerminal.MODID, "main_survival_route");
    private static final int ACCENT = 0xFF92F7A6;
    private static final int CACHE_REFRESH_TICKS = 40;
    private static final int MAX_ROUTE_RECORDS = 250;
    private static final Identifier OVERFLOW_ID =
            Identifier.fromNamespaceAndPath(EchoTerminal.MODID, "main_survival_route_overflow");
    private static final UUID NULL_PLAYER_ID = new UUID(0L, 0L);
    private final Map<RouteCacheKey, RouteSnapshot> routeCache = new HashMap<>();

    private MainSurvivalQuestProvider() {
    }

    @Override
    public TerminalMissionChapter chapter() {
        return new TerminalMissionChapter(
                CHAPTER_ID,
                "Survival Route",
                "One authored field route for installed ECHO chapter progress and remaining registered addon mission signals.",
                45,
                ACCENT,
                true);
    }

    @Override
    public List<TerminalMissionDefinition> missions(Player player) {
        return routeSnapshot(player).definitions();
    }

    @Override
    public TerminalMissionSnapshot snapshot(Player player, Identifier missionId) {
        if (OVERFLOW_ID.equals(missionId)) {
            RouteSnapshot snapshot = routeSnapshot(player);
            return overflowSnapshot(snapshot.overflowCount());
        }
        RouteSnapshot routeSnapshot = routeSnapshot(player);
        SourceRecord record = routeSnapshot.sourceById().get(missionId);
        if (record == null) {
            return new TerminalMissionSnapshot(missionId, TerminalMissionStatus.LOCKED, 0.0F,
                    "MISSING", "Survival route signal not found.",
                    "Reopen the terminal after installed ECHO chapters finish registering.", List.of());
        }
        Optional<Identifier> missingGate = missingRoutePrerequisite(player, record, routeSnapshot);
        if (missingGate.isPresent()) {
            String gate = readableId(missingGate.get().getPath());
            return new TerminalMissionSnapshot(
                    missionId,
                    TerminalMissionStatus.LOCKED,
                    0.0F,
                    "LOCKED",
                    "Complete " + gate + " before this route opens.",
                    "Complete " + gate + " first.",
                    List.of());
        }
        TerminalMissionSnapshot child = record.snapshot();
        return new TerminalMissionSnapshot(
                missionId,
                child.status(),
                child.progress(),
                child.statusLabel(),
                child.unlockReason(),
                guideHint(record, child),
                child.actions());
    }

    public Optional<TerminalMissionChapter> sourceChapter(Player player, Identifier missionId) {
        if (missionId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(routeSnapshot(player).sourceById().get(missionId))
                .map(SourceRecord::chapter);
    }

    @Override
    public TerminalMissionPresentation presentation(
            Player player,
            TerminalMissionDefinition definition,
            TerminalMissionSnapshot snapshot) {
        if (definition != null && OVERFLOW_ID.equals(definition.id())) {
            return new TerminalMissionPresentation(
                    "More Signals Available",
                    "Additional installed mission signals are hidden to keep the Survival Route responsive.",
                    "Open the owning chapter tabs for the remaining detailed records.",
                    "Performance guard",
                    "reference",
                    List.of("Survival Route", "Overflow", "Performance Guard"),
                    null);
        }
        SourceRecord record = routeSnapshot(player).sourceById().get(definition == null ? null : definition.id());
        if (record == null) {
            return TerminalMissionPresentation.fallback(definition, snapshot);
        }
        TerminalMissionSnapshot childSnapshot = usableSnapshot(snapshot, record);
        TerminalMissionPresentation child = safePresentation(record.provider(), player, record.definition(), childSnapshot);
        List<String> tags = new ArrayList<>(child.tags());
        tags.add("From " + record.chapter().title());
        return new TerminalMissionPresentation(
                child.shortTitle(),
                child.objectiveSummary(),
                guideHint(record, childSnapshot),
                record.chapter().title(),
                child.statusTone(),
                tags,
                child.relatedIntelKey());
    }

    @Override
    public TerminalMissionRole role(Player player, TerminalMissionDefinition definition, TerminalMissionSnapshot snapshot) {
        if (definition != null && OVERFLOW_ID.equals(definition.id())) {
            return TerminalMissionRole.REFERENCE;
        }
        SourceRecord record = routeSnapshot(player).sourceById().get(definition == null ? null : definition.id());
        return record == null
                ? TerminalMissionRole.REFERENCE
                : record.role();
    }

    @Override
    public List<Identifier> routePrerequisites(
            Player player,
            TerminalMissionDefinition definition,
            TerminalMissionSnapshot snapshot,
            TerminalMissionRole role) {
        SourceRecord record = routeSnapshot(player).sourceById().get(definition == null ? null : definition.id());
        return record == null ? List.of() : record.routePrerequisites();
    }

    @Override
    public Optional<Identifier> routeAnchor(
            Player player,
            TerminalMissionDefinition definition,
            TerminalMissionSnapshot snapshot,
            TerminalMissionRole role) {
        SourceRecord record = routeSnapshot(player).sourceById().get(definition == null ? null : definition.id());
        return record == null ? Optional.empty() : record.routeAnchor();
    }

    @Override
    public List<TerminalMissionIntelUnlock> intelUnlocks(
            Player player,
            TerminalMissionDefinition definition,
            TerminalMissionSnapshot snapshot,
            TerminalMissionRole role) {
        SourceRecord record = routeSnapshot(player).sourceById().get(definition == null ? null : definition.id());
        return record == null ? List.of() : record.intelUnlocks();
    }

    @Override
    public boolean handleAction(ServerPlayer player, Identifier missionId, String actionId) {
        if (missionId == null || actionId == null || actionId.isBlank()) {
            return false;
        }
        RouteSnapshot snapshot = routeSnapshot(player);
        SourceRecord record = snapshot.sourceById().get(missionId);
        if (record == null) {
            return false;
        }
        if (missingRoutePrerequisite(player, record, snapshot).isPresent()) {
            return false;
        }
        boolean handled = record.provider().handleAction(player, record.definition().id(), actionId);
        if (handled) {
            routeCache.clear();
        }
        return handled;
    }

    public static int maxRouteRecordsForTests() {
        return MAX_ROUTE_RECORDS;
    }

    public void invalidateRouteCache() {
        routeCache.clear();
    }

    public void clearCacheForTests() {
        invalidateRouteCache();
    }

    private static TerminalMissionDefinition definition(SourceRecord record) {
        TerminalMissionDefinition child = record.definition();
        RoutePhase phase = record.phase();
        return new TerminalMissionDefinition(
                child.id(),
                CHAPTER_ID,
                phase.id(),
                phase.title(),
                phase.order(),
                record.routeOrder(),
                child.title(),
                child.briefing(),
                child.fieldGuide(),
                record.chapter().title(),
                child.difficulty(),
                child.icon(),
                child.prerequisites(),
                child.requirements(),
                child.rewards());
    }

    private RouteSnapshot routeSnapshot(Player player) {
        RouteCacheKey key = cacheKey(player);
        RouteSnapshot cached = routeCache.get(key);
        if (cached != null) {
            return cached;
        }
        RouteSnapshot snapshot = buildRouteSnapshot(player);
        routeCache.clear();
        routeCache.put(key, snapshot);
        return snapshot;
    }

    private RouteCacheKey cacheKey(Player player) {
        UUID playerId = player == null ? NULL_PLAYER_ID : player.getUUID();
        long gameTime = player == null || player.level() == null ? 0L : player.level().getGameTime();
        long refreshBucket = Math.max(0L, gameTime / CACHE_REFRESH_TICKS);
        return new RouteCacheKey(playerId, refreshBucket, providerFingerprint());
    }

    private static String providerFingerprint() {
        StringBuilder builder = new StringBuilder();
        for (TerminalMissionProvider provider : TerminalMissionRegistry.providers()) {
            if (provider != null && provider != INSTANCE) {
                TerminalMissionChapter chapter = safeChapter(provider);
                builder.append(provider.getClass().getName())
                        .append('@')
                        .append(System.identityHashCode(provider))
                        .append('#')
                        .append(chapter == null ? "" : chapter.id())
                        .append(';');
            }
        }
        return builder.toString();
    }

    private static RouteSnapshot buildRouteSnapshot(Player player) {
        List<SourceCandidate> candidates = candidates(player);
        List<SourceRecord> records = records(candidates);
        int overflowCount = Math.max(0, records.size() - MAX_ROUTE_RECORDS);
        if (overflowCount > 0) {
            records = List.copyOf(records.subList(0, MAX_ROUTE_RECORDS));
        }
        Map<Identifier, SourceRecord> sourceById = new HashMap<>();
        Map<Identifier, TerminalMissionStatus> statusById = new HashMap<>();
        for (SourceCandidate candidate : candidates) {
            statusById.put(candidate.definition().id(), candidate.snapshot().status());
        }
        List<TerminalMissionDefinition> definitions = new ArrayList<>();
        for (SourceRecord record : records) {
            sourceById.put(record.definition().id(), record);
            definitions.add(definition(record));
        }
        if (overflowCount > 0) {
            definitions.add(overflowDefinition(overflowCount));
        }
        return new RouteSnapshot(List.copyOf(records), Map.copyOf(sourceById), Map.copyOf(statusById),
                List.copyOf(definitions), overflowCount);
    }

    private static List<SourceRecord> records(List<SourceCandidate> candidates) {
        List<SourceCandidate> selected = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (SourceCandidate candidate : candidates) {
            RoutePhase phase = explicitPhase(candidate).orElseGet(() -> authoredPhase(candidate));
            if (phase != null && seen.add(candidate.key())) {
                selected.add(candidate.withPhase(phase));
            }
        }
        for (SourceCandidate candidate : candidates) {
            if (!seen.contains(candidate.key())
                    && (visibleInOtherSignals(candidate.role()) || candidate.routeAnchor().isPresent())
                    && seen.add(candidate.key())) {
                selected.add(candidate.withPhase(RoutePhase.PHASE_15));
            }
        }
        Map<RoutePhase, Integer> phaseCounts = new EnumMap<>(RoutePhase.class);
        List<SourceRecord> records = new ArrayList<>();
        for (SourceCandidate candidate : selected) {
            int order = candidate.placement()
                    .map(TerminalMissionRoutePlacement::missionOrder)
                    .orElseGet(() -> phaseCounts.merge(candidate.phase(), 1, Integer::sum));
            records.add(new SourceRecord(candidate.provider(), candidate.chapter(), candidate.definition(),
                    candidate.snapshot(), candidate.presentation(), candidate.role(), candidate.phase(),
                    candidate.routePrerequisites(), candidate.routeAnchor(), candidate.intelUnlocks(), order));
        }
        return records.stream()
                .sorted(Comparator
                        .comparingInt((SourceRecord record) -> record.phase().order())
                        .thenComparingInt(SourceRecord::routeOrder)
                        .thenComparing(record -> record.definition().id().toString()))
                .toList();
    }

    private static TerminalMissionDefinition overflowDefinition(int overflowCount) {
        return new TerminalMissionDefinition(
                OVERFLOW_ID,
                CHAPTER_ID,
                RoutePhase.PHASE_15.id(),
                RoutePhase.PHASE_15.title(),
                RoutePhase.PHASE_15.order(),
                MAX_ROUTE_RECORDS + 1,
                "More Signals Available",
                overflowCount + " additional mission records are available in installed chapter tabs.",
                "The aggregate Survival Route is showing priority records to keep the terminal responsive.",
                "Performance Guard",
                "Reference",
                ItemStack.EMPTY,
                List.of(),
                List.of(),
                List.of());
    }

    private static TerminalMissionSnapshot overflowSnapshot(int overflowCount) {
        return new TerminalMissionSnapshot(
                OVERFLOW_ID,
                TerminalMissionStatus.VIEW_ONLY,
                0.0F,
                "BOUNDED",
                overflowCount + " additional installed mission records are hidden from this aggregate view.",
                "Open installed chapter tabs for full mission lists.",
                List.of());
    }

    private static List<SourceCandidate> candidates(Player player) {
        List<SourceCandidate> candidates = new ArrayList<>();
        for (TerminalMissionProvider provider : TerminalMissionRegistry.providers()) {
            if (provider == null || provider == INSTANCE) {
                continue;
            }
            TerminalMissionChapter chapter = safeChapter(provider);
            if (chapter == null || CHAPTER_ID.equals(chapter.id())
                    || VanillaJourneyProvider.CHAPTER_ID.equals(chapter.id())) {
                continue;
            }
            for (TerminalMissionDefinition definition : safeMissions(provider, player)) {
                if (definition == null) {
                    continue;
                }
                TerminalMissionSnapshot snapshot = safeSnapshot(provider, player, definition);
                TerminalMissionRole role = safeRole(provider, player, definition, snapshot);
                Optional<TerminalMissionRoutePlacement> placement =
                        safeRoutePlacement(provider, player, definition, snapshot, role);
                if (placement.isPresent()) {
                    if (!placement.get().includeInSurvivalRoute()) {
                        continue;
                    }
                    role = placement.get().role();
                }
                List<Identifier> routePrerequisites =
                        safeRoutePrerequisites(provider, player, definition, snapshot, role);
                Optional<Identifier> routeAnchor =
                        safeRouteAnchor(provider, player, definition, snapshot, role);
                List<TerminalMissionIntelUnlock> intelUnlocks =
                        safeIntelUnlocks(provider, player, definition, snapshot, role);
                TerminalMissionPresentation presentation = safePresentation(provider, player, definition, snapshot);
                candidates.add(new SourceCandidate(provider, chapter, definition, snapshot, presentation, role,
                        placement, routePrerequisites, routeAnchor, intelUnlocks, null));
            }
        }
        return candidates;
    }

    private static TerminalMissionSnapshot lightweightSnapshot(TerminalMissionDefinition definition) {
        return new TerminalMissionSnapshot(definition.id(), TerminalMissionStatus.LOCKED, 0.0F,
                "ROUTE", "Open this route to evaluate live progress.", "Open the owning chapter for actions.",
                List.of());
    }

    private static RoutePhase authoredPhase(SourceCandidate candidate) {
        TerminalMissionChapter chapter = candidate.chapter();
        TerminalMissionDefinition definition = candidate.definition();
        String chapterId = chapter.id().toString();
        String namespace = definition.id().getNamespace();
        String path = definition.id().getPath().toLowerCase(Locale.ROOT);
        String phase = definition.phaseTitle().toLowerCase(Locale.ROOT);
        String title = definition.title().toLowerCase(Locale.ROOT);

        if (chapterId.contains("echoashfallprotocol:ashfall_protocol")
                || "echoashfallprotocol".equals(namespace)) {
            String signal = path + " " + phase + " " + title;
            if (containsAny(signal, "aftermath", "seal", "mastery")) {
                return RoutePhase.PHASE_15;
            }
            if (containsAny(signal, "nexus", "ending", "guardian", "echo-0", "echo_zero", "core", "decision")) {
                return RoutePhase.PHASE_14;
            }
            if (containsAny(signal, "cryo", "cryogenic")) {
                return RoutePhase.PHASE_13;
            }
            if (containsAny(signal, "boss", "warlord", "stalker", "juggernaut", "matriarch", "colossus", "behemoth")) {
                return RoutePhase.PHASE_12;
            }
            if (containsAny(signal, "grid", "relay", "station", "industrial", "infrastructure")
                    || definition.phaseOrder() == 6) {
                return RoutePhase.PHASE_11;
            }
            if (containsAny(signal, "deep", "vault", "alloy", "extraction")) {
                return RoutePhase.PHASE_10;
            }
            if (containsAny(signal, "biohazard", "radiation", "med", "medicine", "lab", "mutagen")) {
                return RoutePhase.PHASE_09;
            }
            if (containsAny(signal, "faction", "drone")) {
                return RoutePhase.PHASE_08;
            }
            if (containsAny(signal, "scan", "scanner", "poi", "expedition", "recon")) {
                return RoutePhase.PHASE_07;
            }
            if (containsAny(signal, "filter", "mask", "hazard")) {
                return RoutePhase.PHASE_06;
            }
            return switch (definition.phaseOrder()) {
                case 0 -> RoutePhase.PHASE_00;
                case 1 -> RoutePhase.PHASE_01;
                case 2 -> RoutePhase.PHASE_04;
                case 3 -> RoutePhase.PHASE_07;
                default -> RoutePhase.PHASE_10;
            };
        }
        if (chapterId.contains("echoorbitalremnants") || namespace.equals("echoorbitalremnants")) {
            String signal = path + " " + phase + " " + title;
            if (containsAny(signal, "seal", "survey", "faction", "mastery")) {
                return RoutePhase.PHASE_15;
            }
            if (containsAny(signal, "echo_zero", "echo-0", "final", "core", "guardian")) {
                return RoutePhase.PHASE_14;
            }
            return containsAny(signal, "deep_space", "deep space", "radiation", "cryo", "lab", "vault")
                    ? RoutePhase.PHASE_13
                    : RoutePhase.PHASE_10;
        }
        if (chapterId.contains("echostationfall") || chapterId.endsWith(":stationfall")
                || namespace.equals("echostationfall")) {
            String signal = path + " " + phase + " " + title;
            if (containsAny(signal, "boss", "blackbox", "black box", "ai_core", "ai core", "guardian")) {
                return RoutePhase.PHASE_14;
            }
            if (containsAny(signal, "deep", "radiation", "cryo", "lab", "vault", "reactor")) {
                return RoutePhase.PHASE_13;
            }
            return definition.phaseOrder() <= 0 ? RoutePhase.PHASE_10 : RoutePhase.PHASE_11;
        }
        if (chapterId.contains("echoindustrialnexus") || namespace.equals("echoindustrialnexus")) {
            String signal = path + " " + phase + " " + title;
            if (containsAny(signal, "survived", "radiation", "cryo", "lab", "vault", "boss")) {
                return RoutePhase.PHASE_12;
            }
            return containsAny(signal, "filter", "metal", "grind", "reclaim_power", "power")
                            ? RoutePhase.PHASE_05
                            : RoutePhase.PHASE_10;
        }
        if (chapterId.contains("echonexusprotocol") || namespace.equals("echonexusprotocol")
                || chapterId.contains("echoblackboxprotocol") || namespace.equals("echoblackboxprotocol")) {
            String signal = path + " " + phase + " " + title;
            if (containsAny(signal, "aftermath", "seal", "survey", "faction", "mastery")) {
                return RoutePhase.PHASE_15;
            }
            return containsAny(signal, "ending", "guardian", "echo_zero", "echo-0", "core", "decision")
                            ? RoutePhase.PHASE_14
                            : RoutePhase.PHASE_13;
        }
        return null;
    }

    private static Optional<RoutePhase> explicitPhase(SourceCandidate candidate) {
        return candidate.placement().map(placement -> RoutePhase.byOrder(placement.phaseOrder()));
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static String readableId(String path) {
        if (path == null || path.isBlank()) {
            return "route milestone";
        }
        StringBuilder label = new StringBuilder();
        for (String word : path.replace('/', '_').split("_")) {
            if (word.isBlank()) {
                continue;
            }
            if (label.length() > 0) {
                label.append(' ');
            }
            label.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                label.append(word.substring(1));
            }
        }
        return label.length() == 0 ? "route milestone" : label.toString();
    }

    private static boolean visibleInOtherSignals(TerminalMissionRole role) {
        return role == TerminalMissionRole.MAIN || role == TerminalMissionRole.REFERENCE;
    }

    private static String guideHint(SourceRecord record, TerminalMissionSnapshot snapshot) {
        String childHint = snapshot == null ? "" : snapshot.actionHint();
        return childHint == null || childHint.isBlank()
                ? "Open " + record.chapter().title() + " for this route record."
                : childHint;
    }

    private static TerminalMissionSnapshot usableSnapshot(TerminalMissionSnapshot snapshot, SourceRecord record) {
        return snapshot != null && record.definition().id().equals(snapshot.missionId()) ? snapshot : record.snapshot();
    }

    private static TerminalMissionChapter safeChapter(TerminalMissionProvider provider) {
        try {
            return provider.chapter();
        } catch (RuntimeException | LinkageError exception) {
            EchoTerminal.LOGGER.debug("Survival route skipped a provider with failing chapter metadata.", exception);
            return null;
        }
    }

    private static List<TerminalMissionDefinition> safeMissions(TerminalMissionProvider provider, Player player) {
        List<TerminalMissionDefinition> liveMissions = safeMissions(provider, player, true);
        if (!liveMissions.isEmpty() || player == null) {
            return liveMissions;
        }
        List<TerminalMissionDefinition> definitionOnlyMissions = safeMissions(provider, null, false);
        return definitionOnlyMissions.isEmpty() ? liveMissions : definitionOnlyMissions;
    }

    private static List<TerminalMissionDefinition> safeMissions(
            TerminalMissionProvider provider,
            Player player,
            boolean logFailures) {
        try {
            List<TerminalMissionDefinition> missions = provider.missions(player);
            return missions == null ? List.of() : missions;
        } catch (RuntimeException | LinkageError exception) {
            if (logFailures) {
                EchoTerminal.LOGGER.debug("Survival route skipped a provider with failing mission records.", exception);
            }
            return List.of();
        }
    }

    private static TerminalMissionSnapshot safeSnapshot(
            TerminalMissionProvider provider,
            Player player,
            TerminalMissionDefinition definition) {
        try {
            TerminalMissionSnapshot snapshot = provider.snapshot(player, definition.id());
            return snapshot == null
                    ? new TerminalMissionSnapshot(definition.id(), TerminalMissionStatus.LOCKED, 0.0F,
                            "LOCKED", "Mission provider returned no snapshot.", "Open the owning chapter.", List.of())
                    : snapshot;
        } catch (RuntimeException | LinkageError exception) {
            EchoTerminal.LOGGER.debug("Survival route locked a mission with failing snapshot metadata.", exception);
            return new TerminalMissionSnapshot(definition.id(), TerminalMissionStatus.LOCKED, 0.0F,
                    "LOCKED", "Mission provider snapshot failed.", "Open the owning chapter after the chapter reloads.",
                    List.of());
        }
    }

    private static TerminalMissionPresentation safePresentation(
            TerminalMissionProvider provider,
            Player player,
            TerminalMissionDefinition definition,
            TerminalMissionSnapshot snapshot) {
        try {
            TerminalMissionPresentation presentation = provider.presentation(player, definition, snapshot);
            return presentation == null ? TerminalMissionPresentation.fallback(definition, snapshot) : presentation;
        } catch (RuntimeException | LinkageError exception) {
            EchoTerminal.LOGGER.debug("Survival route used fallback presentation for a mission.", exception);
            return TerminalMissionPresentation.fallback(definition, snapshot);
        }
    }

    private static TerminalMissionRole safeRole(
            TerminalMissionProvider provider,
            Player player,
            TerminalMissionDefinition definition,
            TerminalMissionSnapshot snapshot) {
        try {
            TerminalMissionRole role = provider.role(player, definition, snapshot);
            return role == null ? TerminalMissionRole.fallback(definition, snapshot) : role;
        } catch (RuntimeException | LinkageError exception) {
            EchoTerminal.LOGGER.debug("Survival route used fallback role for a mission.", exception);
            return TerminalMissionRole.fallback(definition, snapshot);
        }
    }

    private static Optional<TerminalMissionRoutePlacement> safeRoutePlacement(
            TerminalMissionProvider provider,
            Player player,
            TerminalMissionDefinition definition,
            TerminalMissionSnapshot snapshot,
            TerminalMissionRole role) {
        try {
            Optional<TerminalMissionRoutePlacement> placement =
                    provider.routePlacement(player, definition, snapshot, role);
            return placement == null ? Optional.empty() : placement;
        } catch (RuntimeException | LinkageError exception) {
            EchoTerminal.LOGGER.debug("Survival route used fallback route placement for a mission.", exception);
            return Optional.empty();
        }
    }

    private static List<Identifier> safeRoutePrerequisites(
            TerminalMissionProvider provider,
            Player player,
            TerminalMissionDefinition definition,
            TerminalMissionSnapshot snapshot,
            TerminalMissionRole role) {
        try {
            List<Identifier> prerequisites = provider.routePrerequisites(player, definition, snapshot, role);
            return prerequisites == null ? List.of() : prerequisites.stream()
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .toList();
        } catch (RuntimeException | LinkageError exception) {
            EchoTerminal.LOGGER.debug("Survival route used fallback route prerequisites for a mission.", exception);
            return List.of();
        }
    }

    private static Optional<Identifier> safeRouteAnchor(
            TerminalMissionProvider provider,
            Player player,
            TerminalMissionDefinition definition,
            TerminalMissionSnapshot snapshot,
            TerminalMissionRole role) {
        try {
            Optional<Identifier> anchor = provider.routeAnchor(player, definition, snapshot, role);
            return anchor == null ? Optional.empty() : anchor.filter(java.util.Objects::nonNull);
        } catch (RuntimeException | LinkageError exception) {
            EchoTerminal.LOGGER.debug("Survival route used fallback route anchor for a mission.", exception);
            return Optional.empty();
        }
    }

    private static List<TerminalMissionIntelUnlock> safeIntelUnlocks(
            TerminalMissionProvider provider,
            Player player,
            TerminalMissionDefinition definition,
            TerminalMissionSnapshot snapshot,
            TerminalMissionRole role) {
        try {
            List<TerminalMissionIntelUnlock> unlocks = provider.intelUnlocks(player, definition, snapshot, role);
            return unlocks == null ? List.of() : unlocks.stream()
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .toList();
        } catch (RuntimeException | LinkageError exception) {
            EchoTerminal.LOGGER.debug("Survival route used fallback intel unlocks for a mission.", exception);
            return List.of();
        }
    }

    private static Optional<Identifier> missingRoutePrerequisite(Player player, SourceRecord record, RouteSnapshot snapshot) {
        if (record.routePrerequisites().isEmpty()) {
            return Optional.empty();
        }
        for (Identifier prerequisite : record.routePrerequisites()) {
            TerminalMissionStatus status = snapshot.statusById().get(prerequisite);
            if (status != null && !routeGateComplete(status)) {
                return Optional.of(prerequisite);
            }
            if (status == null && !routePrerequisiteComplete(player, prerequisite)) {
                return Optional.of(prerequisite);
            }
        }
        return Optional.empty();
    }

    private static boolean routePrerequisiteComplete(Player player, Identifier prerequisite) {
        for (TerminalMissionProvider provider : TerminalMissionRegistry.providers()) {
            if (provider == null || provider == INSTANCE) {
                continue;
            }
            for (TerminalMissionDefinition definition : safeMissions(provider, player)) {
                if (!definition.id().equals(prerequisite)) {
                    continue;
                }
                return routeGateComplete(safeSnapshot(provider, player, definition).status());
            }
        }
        return true;
    }

    private static boolean routeGateComplete(TerminalMissionStatus status) {
        return status == TerminalMissionStatus.COMPLETED
                || status == TerminalMissionStatus.CLAIMED
                || status == TerminalMissionStatus.CLAIMABLE;
    }

    private enum RoutePhase {
        PHASE_00("phase_00", "Podfall", 0),
        PHASE_01("phase_01", "First Night", 1),
        PHASE_02("phase_02", "Water Security", 2),
        PHASE_03("phase_03", "Field Kit", 3),
        PHASE_04("phase_04", "Powered Workshop", 4),
        PHASE_05("phase_05", "Machine Tools", 5),
        PHASE_06("phase_06", "Hazard Filters", 6),
        PHASE_07("phase_07", "Recon", 7),
        PHASE_08("phase_08", "Faction/Drone", 8),
        PHASE_09("phase_09", "Biohazard Medicine", 9),
        PHASE_10("phase_10", "Deep Extraction", 10),
        PHASE_11("phase_11", "Grid Restoration", 11),
        PHASE_12("phase_12", "Wasteland Bosses", 12),
        PHASE_13("phase_13", "Cryogenic Route", 13),
        PHASE_14("phase_14", "Nexus Decision", 14),
        PHASE_15("phase_15", "Aftermath", 15);

        private final String id;
        private final String title;
        private final int order;

        RoutePhase(String id, String title, int order) {
            this.id = id;
            this.title = title;
            this.order = order;
        }

        String id() {
            return id;
        }

        String title() {
            return title;
        }

        int order() {
            return order;
        }

        static RoutePhase byOrder(int order) {
            for (RoutePhase phase : values()) {
                if (phase.order == order) {
                    return phase;
                }
            }
            return values()[Math.max(0, Math.min(values().length - 1, order))];
        }
    }

    private record SourceCandidate(
            TerminalMissionProvider provider,
            TerminalMissionChapter chapter,
            TerminalMissionDefinition definition,
            TerminalMissionSnapshot snapshot,
            TerminalMissionPresentation presentation,
            TerminalMissionRole role,
            Optional<TerminalMissionRoutePlacement> placement,
            List<Identifier> routePrerequisites,
            Optional<Identifier> routeAnchor,
            List<TerminalMissionIntelUnlock> intelUnlocks,
            RoutePhase phase) {
        String key() {
            return chapter.id() + "|" + definition.id();
        }

        SourceCandidate withPhase(RoutePhase phase) {
            return new SourceCandidate(provider, chapter, definition, snapshot, presentation, role, placement,
                    routePrerequisites, routeAnchor, intelUnlocks, phase);
        }
    }

    private record SourceRecord(
            TerminalMissionProvider provider,
            TerminalMissionChapter chapter,
            TerminalMissionDefinition definition,
            TerminalMissionSnapshot snapshot,
            TerminalMissionPresentation presentation,
            TerminalMissionRole role,
            RoutePhase phase,
            List<Identifier> routePrerequisites,
            Optional<Identifier> routeAnchor,
            List<TerminalMissionIntelUnlock> intelUnlocks,
            int routeOrder) {
        SourceRecord {
            routeAnchor = routeAnchor == null ? Optional.empty() : routeAnchor;
            intelUnlocks = List.copyOf(intelUnlocks == null ? List.of() : intelUnlocks);
        }
    }

    private record RouteCacheKey(UUID playerId, long refreshBucket, String providerFingerprint) {
    }

    private record RouteSnapshot(
            List<SourceRecord> records,
            Map<Identifier, SourceRecord> sourceById,
            Map<Identifier, TerminalMissionStatus> statusById,
            List<TerminalMissionDefinition> definitions,
            int overflowCount) {
    }
}
