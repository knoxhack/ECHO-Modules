package com.knoxhack.echolens.api;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public record LensReport(
        Component title,
        Component subtitle,
        ItemStack icon,
        LensTargetKind targetKind,
        Identifier targetId,
        String sourceModId,
        List<LensInfoSection> sections,
        List<LensAction> actions) {
    public LensReport {
        title = title == null ? Component.empty() : title;
        subtitle = subtitle == null ? Component.empty() : subtitle;
        icon = icon == null ? ItemStack.EMPTY : icon.copy();
        targetKind = targetKind == null ? LensTargetKind.MISS : targetKind;
        sourceModId = sourceModId == null ? "" : sourceModId;
        sections = List.copyOf(sections == null ? List.of() : sections.stream().filter(section -> section != null).toList());
        actions = List.copyOf(actions == null ? List.of() : actions.stream().filter(action -> action != null).toList());
    }

    public boolean isEmpty() {
        return targetKind == LensTargetKind.MISS || title.getString().isBlank();
    }

    public static LensReport empty() {
        return new LensReport(Component.empty(), Component.empty(), ItemStack.EMPTY, LensTargetKind.MISS, null, "",
                List.of(), List.of());
    }
}
