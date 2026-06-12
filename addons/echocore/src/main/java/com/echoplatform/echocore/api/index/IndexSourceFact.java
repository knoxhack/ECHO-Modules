package com.echoplatform.echocore.api.index;

import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public record IndexSourceFact(
        Identifier itemId,
        Identifier sourceId,
        IndexSourceKind kind,
        String title,
        List<String> notes,
        ItemStack icon,
        String sourceModId) {
    public IndexSourceFact {
        sourceId = sourceId == null ? itemId : sourceId;
        kind = kind == null ? IndexSourceKind.SOURCE_CARD : kind;
        title = title == null ? "" : title;
        notes = notes == null ? List.of() : List.copyOf(notes);
        icon = icon == null ? ItemStack.EMPTY : icon.copy();
        sourceModId = sourceModId == null ? "" : sourceModId;
    }

    public static IndexSourceFact of(
            Identifier itemId,
            Identifier sourceId,
            IndexSourceKind kind,
            String title,
            List<String> notes,
            Item icon,
            String sourceModId) {
        return new IndexSourceFact(itemId, sourceId, kind, title, notes,
                icon == null ? ItemStack.EMPTY : new ItemStack(icon), sourceModId);
    }
}
