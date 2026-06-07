package com.knoxhack.echoterminal.api;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public final class TerminalContactRegistry {
    private static final Map<Identifier, TerminalContactProvider> PROVIDERS = new ConcurrentHashMap<>();
    private static volatile List<TerminalContactProvider> sortedProviders = List.of();

    private TerminalContactRegistry() {
    }

    public static void register(TerminalContactProvider provider) {
        if (provider == null || provider.providerId() == null) {
            return;
        }
        PROVIDERS.putIfAbsent(provider.providerId(), provider);
        sortedProviders = sorted();
    }

    public static Optional<TerminalContactProvider> provider(Identifier id) {
        return Optional.ofNullable(PROVIDERS.get(id));
    }

    public static List<TerminalContactProvider> providers() {
        return sortedProviders;
    }

    public static List<TerminalContact> contacts(Player player) {
        List<TerminalContact> contacts = new ArrayList<>();
        for (TerminalContactProvider provider : sortedProviders) {
            try {
                List<TerminalContact> provided = provider.contacts(player);
                if (provided != null) {
                    contacts.addAll(provided.stream().filter(contact -> contact != null).toList());
                }
            } catch (RuntimeException ignored) {
                // Contact providers are optional addon surfaces; one bad provider should not break Terminal.
            }
        }
        contacts.sort(Comparator.comparing(TerminalContact::displayName));
        return List.copyOf(contacts);
    }

    private static List<TerminalContactProvider> sorted() {
        return PROVIDERS.values().stream()
                .sorted(Comparator.comparing(provider -> provider.providerId().toString()))
                .toList();
    }
}
