package com.knoxhack.echoterminal.discovery;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.EchoDiscoveryCategory;
import com.knoxhack.echocore.api.EchoDiscoveryEntry;
import com.knoxhack.echocore.api.EchoDiscoveryProvider;
import com.knoxhack.echocore.api.EchoDiscoveryState;
import com.knoxhack.echocore.api.EchoFactionDefinition;
import com.knoxhack.echocore.api.EchoRouteRecord;
import com.knoxhack.echoterminal.api.TerminalContact;
import com.knoxhack.echoterminal.api.TerminalContactRegistry;
import com.knoxhack.echoterminal.api.TerminalVisualAssets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public final class TerminalDiscoveryProvider implements EchoDiscoveryProvider {
    @Override
    public List<EchoDiscoveryEntry> entries(Player player) {
        List<EchoDiscoveryEntry> entries = new ArrayList<>();
        int sort = 10_000;
        for (EchoRouteRecord record : EchoCoreServices.routeRecords(player)) {
            Identifier entryId = routeEntryId(record.id());
            EchoDiscoveryCategory category = routeCategory(record);
            entries.add(new EchoDiscoveryEntry(
                    entryId,
                    Identifier.fromNamespaceAndPath(record.id().getNamespace(), cleanChapter(record.chapterId())),
                    category,
                    record.title(),
                    category == EchoDiscoveryCategory.GUARDIAN ? "Buried Hostile Signature" : "Unmapped Field Signal",
                    "A chapter route has a sealed signal here. Find it in the field to reveal the full record.",
                    record.summary().isBlank() ? record.dimensionHint() : record.summary(),
                    TerminalVisualAssets.missionCategoryIcon(record.category()),
                    TerminalVisualAssets.missionCategoryArt(record.category()),
                    record.complete() ? 0xFF92F7A6 : 0xFF9FD1FF,
                    record.id(),
                    sort++));
        }

        sort = 20_000;
        for (EchoFactionDefinition definition : EchoCoreServices.factionDefinitions()) {
            entries.add(new EchoDiscoveryEntry(
                    factionEntryId(definition.id()),
                    Identifier.fromNamespaceAndPath(definition.id().getNamespace(), cleanChapter(definition.modId())),
                    EchoDiscoveryCategory.FACTION,
                    definition.displayName(),
                    "Unknown Faction Signal",
                    definition.landmarkFaction()
                            ? "A standing signal belongs to a local power. Make contact to reveal the faction."
                            : "A moving faction channel is present. Contact a representative to add it to the grid.",
                    definition.summary().isBlank() ? definition.serviceSummary() : definition.summary(),
                    TerminalVisualAssets.missionCategoryIcon("faction"),
                    TerminalVisualAssets.missionCategoryArt("faction"),
                    definition.accentColor() == 0 ? 0xFFB889F5 : definition.accentColor(),
                    null,
                    sort++));
        }
        sort = 30_000;
        for (TerminalContact contact : TerminalContactRegistry.contacts(player)) {
            entries.add(new EchoDiscoveryEntry(
                    contactEntryId(contact.id()),
                    Identifier.fromNamespaceAndPath(contact.id().getNamespace(), cleanChapter(contact.id().getNamespace())),
                    EchoDiscoveryCategory.FACTION,
                    contact.displayName(),
                    contact.role(),
                    contact.summary().isBlank() ? "NPC contact discovered in the field." : contact.summary(),
                    contact.factionId() == null ? "" : contact.factionId().toString(),
                    TerminalVisualAssets.missionCategoryIcon("faction"),
                    TerminalVisualAssets.missionCategoryArt("faction"),
                    0xFF66E8FF,
                    contact.id(),
                    sort++));
        }
        return entries;
    }

    @Override
    public EchoDiscoveryState state(Player player, EchoDiscoveryEntry entry) {
        if (player == null || entry == null) {
            return EchoDiscoveryState.LOCKED;
        }
        Identifier id = entry.id();
        String path = id.getPath();
        if (path.startsWith("route/")) {
            String routePath = path.substring("route/".length());
            for (EchoRouteRecord record : EchoCoreServices.routeRecords(player)) {
                if (record.id().getNamespace().equals(id.getNamespace()) && record.id().getPath().equals(routePath)) {
                    if (record.complete()) {
                        return EchoDiscoveryState.CHECKED;
                    }
                    return EchoCoreServices.hasDiscoveredFeature(player, id)
                            ? EchoDiscoveryState.DISCOVERED
                            : EchoDiscoveryState.LOCKED;
                }
            }
        }
        if (path.startsWith("faction/")) {
            Identifier factionId = Identifier.fromNamespaceAndPath(id.getNamespace(), path.substring("faction/".length()));
            return EchoCoreServices.factionProfile(player, factionId)
                    .filter(profile -> profile.contacted() || profile.contactCount() > 0)
                    .map(profile -> profile.completedContracts() > 0
                            ? EchoDiscoveryState.CHECKED
                            : EchoDiscoveryState.DISCOVERED)
                    .orElse(EchoDiscoveryState.LOCKED);
        }
        if (path.startsWith("contact/")) {
            Identifier contactId = Identifier.fromNamespaceAndPath(id.getNamespace(), path.substring("contact/".length()));
            return TerminalContactRegistry.contacts(player).stream()
                    .anyMatch(contact -> contact.id().equals(contactId))
                    ? EchoDiscoveryState.DISCOVERED
                    : EchoDiscoveryState.LOCKED;
        }
        return EchoDiscoveryState.LOCKED;
    }

    public static Identifier routeEntryId(Identifier recordId) {
        return EchoCoreServices.routeDiscoveryId(recordId);
    }

    public static Identifier factionEntryId(Identifier factionId) {
        return Identifier.fromNamespaceAndPath(factionId.getNamespace(), "faction/" + factionId.getPath());
    }

    public static Identifier contactEntryId(Identifier contactId) {
        return Identifier.fromNamespaceAndPath(contactId.getNamespace(), "contact/" + contactId.getPath());
    }

    private static EchoDiscoveryCategory routeCategory(EchoRouteRecord record) {
        String key = (record.category() + " " + record.title() + " " + record.summary()).toLowerCase(Locale.ROOT);
        if (key.contains("guardian") || key.contains("warden") || key.contains("boss")) {
            return EchoDiscoveryCategory.GUARDIAN;
        }
        if (key.contains("event") || key.contains("storm") || key.contains("quarantine")) {
            return EchoDiscoveryCategory.EVENT;
        }
        if (key.contains("faction")) {
            return EchoDiscoveryCategory.FACTION;
        }
        if (key.contains("biome")) {
            return EchoDiscoveryCategory.BIOME;
        }
        return EchoDiscoveryCategory.STRUCTURE;
    }

    private static String cleanChapter(String value) {
        String cleaned = value == null ? "" : value.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_/.-]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
        return cleaned.isBlank() ? "terminal" : cleaned;
    }
}
