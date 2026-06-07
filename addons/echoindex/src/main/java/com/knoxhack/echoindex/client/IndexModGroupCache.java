package com.knoxhack.echoindex.client;

import com.knoxhack.echoindex.service.IndexService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

final class IndexModGroupCache {
    private static long cachedRevision = Long.MIN_VALUE;
    private static boolean cachedClientContext;
    private static List<IndexModGroup> cachedGroups = List.of();

    private IndexModGroupCache() {
    }

    static List<IndexModGroup> groups(Player player) {
        long revision = IndexService.INSTANCE.itemCatalogRevision();
        boolean clientContext = player != null && player.level() != null && player.level().getServer() == null;
        if (revision == cachedRevision && clientContext == cachedClientContext && !cachedGroups.isEmpty()) {
            return cachedGroups;
        }
        Map<String, ListBuilder> byMod = new LinkedHashMap<>();
        for (ItemStack stack : IndexService.INSTANCE.itemCatalog(player)) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            Identifier id = IndexService.itemId(stack.getItem());
            byMod.computeIfAbsent(id.getNamespace(), ListBuilder::new).add(stack);
        }
        cachedGroups = byMod.values().stream()
                .map(ListBuilder::toGroup)
                .toList();
        cachedRevision = revision;
        cachedClientContext = clientContext;
        return cachedGroups;
    }

    private static final class ListBuilder {
        private final String modId;
        private final java.util.ArrayList<ItemStack> items = new java.util.ArrayList<>();

        private ListBuilder(String modId) {
            this.modId = modId;
        }

        private void add(ItemStack stack) {
            items.add(stack.copy());
        }

        private IndexModGroup toGroup() {
            ItemStack itemIcon = items.stream()
                    .filter(stack -> stack.getItem() instanceof BlockItem)
                    .findFirst()
                    .orElseGet(() -> items.isEmpty() ? ItemStack.EMPTY : items.getFirst())
                    .copy();
            IndexAddonPresentation.Style style = IndexAddonPresentation.style(modId);
            ItemStack icon = style.icon().isEmpty() ? itemIcon : style.icon();
            return new IndexModGroup(modId, style.displayName(), icon, items.size(), items, items,
                    false, false, false, style.version(), style.accent());
        }
    }
}
