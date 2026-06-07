package com.knoxhack.echoindex.service;

import com.knoxhack.echocore.api.index.IIndexRecipeProvider;
import com.knoxhack.echocore.api.index.IndexRecipeCategory;
import com.knoxhack.echocore.api.index.IndexRecipeSlot;
import com.knoxhack.echocore.api.index.IndexRecipeView;
import com.knoxhack.echocore.api.index.IndexSourceFact;
import com.knoxhack.echocore.api.index.IndexSourceKind;
import com.knoxhack.echoindex.EchoIndex;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public enum IndexSourceRecipeProvider implements IIndexRecipeProvider {
    INSTANCE;

    public static final Identifier CATEGORY = EchoIndex.id("recipe/sources");
    private static final Identifier PROVIDER_ID = EchoIndex.id("provider/sources");
    private volatile List<SourceFact> resourceSources = List.of();
    private volatile List<SourceFact> providerSources = List.of();

    @Override
    public Identifier id() {
        return PROVIDER_ID;
    }

    @Override
    public List<IndexRecipeCategory> recipeCategories(Player player) {
        return List.of(new IndexRecipeCategory(
                CATEGORY,
                "Sources",
                new ItemStack(Items.SPYGLASS),
                0xFFFFD166,
                90));
    }

    @Override
    public List<IndexRecipeView> recipes(Player player) {
        List<IndexRecipeView> views = new ArrayList<>();
        for (SourceFact source : sourceFacts()) {
            BuiltInRegistries.ITEM.getOptional(source.itemId()).ifPresent(item -> {
                ItemStack output = safeStack(item);
                if (output.isEmpty()) {
                    return;
                }
                ItemStack icon = iconStack(source, output);
                List<String> notes = source.notes().isEmpty()
                        ? List.of("Source type: " + source.kind().label())
                        : source.notes();
                List<IndexRecipeSlot> slots = new ArrayList<>();
                slots.add(IndexRecipeSlot.machine(icon));
                slots.add(IndexRecipeSlot.output(output));
                views.add(new IndexRecipeView(
                        viewId(source),
                        CATEGORY,
                        source.title(),
                        icon,
                        slots,
                        notes,
                        0,
                        false,
                        source.sourceModId()));
            });
        }
        views.sort(Comparator.comparing(view -> view.id().toString()));
        return views;
    }

    public void replaceSources(Collection<SourceFact> newSources) {
        resourceSources = normalize(newSources);
    }

    public void replaceProviderSources(Collection<IndexSourceFact> newSources) {
        providerSources = newSources == null ? List.of() : normalize(newSources.stream()
                .map(SourceFact::from)
                .toList());
    }

    public List<SourceFact> sourceFacts() {
        List<SourceFact> merged = new ArrayList<>(resourceSources.size() + providerSources.size());
        merged.addAll(resourceSources);
        merged.addAll(providerSources);
        return normalize(merged);
    }

    private static List<SourceFact> normalize(Collection<SourceFact> sources) {
        if (sources == null) {
            return List.of();
        }
        List<SourceFact> sorted = sources.stream()
                .filter(source -> source != null && source.itemId() != null)
                .sorted(Comparator.comparing((SourceFact source) -> source.itemId().toString())
                        .thenComparing(source -> source.kind().name())
                        .thenComparing(source -> source.sourceId().toString())
                        .thenComparing(SourceFact::title))
                .toList();
        java.util.Map<String, SourceFact> deduped = new java.util.LinkedHashMap<>();
        for (SourceFact source : sorted) {
            deduped.putIfAbsent(source.itemId() + "|" + source.kind() + "|" + source.sourceId() + "|" + source.title(), source);
        }
        return List.copyOf(deduped.values());
    }

    public int sourceFactCount() {
        return sourceFacts().size();
    }

    public int sourceRecipeCount(Player player) {
        return recipes(player).size();
    }

    private static Identifier viewId(SourceFact source) {
        return EchoIndex.id("source/" + sanitize(source.itemId().getNamespace()) + "/"
                + sanitize(source.itemId().getPath()) + "/"
                + sanitize(source.kind().name().toLowerCase(Locale.ROOT)) + "/"
                + sanitize(source.sourceId().getNamespace()) + "/"
                + sanitize(source.sourceId().getPath()) + "/"
                + sanitize(source.title().toLowerCase(Locale.ROOT)));
    }

    private static String sanitize(String value) {
        StringBuilder builder = new StringBuilder();
        String safe = value == null ? "" : value.toLowerCase(Locale.ROOT);
        for (int i = 0; i < safe.length(); i++) {
            char c = safe.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == '/') {
                builder.append(c);
            } else {
                builder.append('_');
            }
        }
        return builder.isEmpty() ? "unknown" : builder.toString();
    }

    private static ItemStack iconStack(SourceFact source, ItemStack output) {
        if (!source.icon().isEmpty()) {
            return source.icon();
        }
        Identifier iconItemId = source.iconItemId();
        if (iconItemId != null) {
            ItemStack resolved = BuiltInRegistries.ITEM.getOptional(iconItemId)
                    .map(IndexSourceRecipeProvider::safeStack)
                    .orElse(ItemStack.EMPTY);
            if (!resolved.isEmpty()) {
                return resolved;
            }
        }
        return output;
    }

    private static ItemStack safeStack(Item item) {
        if (item == null) {
            return ItemStack.EMPTY;
        }
        try {
            return new ItemStack(item);
        } catch (RuntimeException exception) {
            return ItemStack.EMPTY;
        }
    }

    private static Identifier registryItemId(Item item) {
        if (item == null) {
            return null;
        }
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        return id != null && BuiltInRegistries.ITEM.getOptional(id).isPresent() ? id : null;
    }

    public record SourceFact(
            Identifier itemId,
            Identifier sourceId,
            IndexSourceKind kind,
            String title,
            List<String> notes,
            ItemStack icon,
            Identifier iconItemId,
            String sourceModId) {
        public SourceFact {
            sourceId = sourceId == null ? itemId : sourceId;
            kind = kind == null ? IndexSourceKind.SOURCE_CARD : kind;
            title = title == null || title.isBlank() ? kind.label() : title.strip();
            notes = notes == null ? List.of() : notes.stream()
                    .filter(note -> note != null && !note.isBlank())
                    .map(String::strip)
                    .toList();
            icon = icon == null ? ItemStack.EMPTY : icon.copy();
            iconItemId = iconItemId == null && !icon.isEmpty() ? registryItemId(icon.getItem()) : iconItemId;
            sourceModId = sourceModId == null || sourceModId.isBlank()
                    ? itemId == null ? EchoIndex.MODID : itemId.getNamespace()
                    : sourceModId.strip();
        }

        public static SourceFact of(Identifier itemId, Identifier sourceId, String title, List<String> notes,
                Item icon, String sourceModId) {
            return of(itemId, sourceId, IndexSourceKind.SOURCE_CARD, title, notes, icon, sourceModId);
        }

        public static SourceFact of(Identifier itemId, Identifier sourceId, IndexSourceKind kind, String title,
                List<String> notes, Item icon, String sourceModId) {
            return new SourceFact(itemId, sourceId, kind, title, notes, safeStack(icon), registryItemId(icon), sourceModId);
        }

        public static SourceFact from(IndexSourceFact fact) {
            if (fact == null) {
                return null;
            }
            return new SourceFact(fact.itemId(), fact.sourceId(), fact.kind(), fact.title(), fact.notes(),
                    fact.icon(), null, fact.sourceModId());
        }
    }
}
