package com.knoxhack.echowiki.item;

import com.knoxhack.echowiki.content.GuideBookDefinition;
import com.knoxhack.echowiki.content.GuideBookRegistry;
import com.knoxhack.echowiki.content.GuideBookTarget;
import com.knoxhack.echowiki.registry.ModDataComponents;
import com.knoxhack.echowiki.registry.ModItems;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public final class GuideBookStacks {
    private GuideBookStacks() {
    }

    public static ItemStack stackFor(GuideBookDefinition guide) {
        if (guide == null) {
            return new ItemStack(ModItems.GUIDE_BOOK.get());
        }
        ItemStack stack = new ItemStack(ModItems.GUIDE_BOOK.get());
        stack.set(ModDataComponents.GUIDE_BOOK_TARGET.get(), new GuideBookTarget(guide.id()));
        stack.set(DataComponents.ITEM_NAME, Component.literal(guide.title()));
        return stack;
    }

    public static Optional<ItemStack> stackFor(Identifier guideId) {
        return GuideBookRegistry.guideBook(guideId).map(GuideBookStacks::stackFor);
    }

    public static List<ItemStack> visibleStacks() {
        return GuideBookRegistry.visibleGuideBooks().stream()
                .map(GuideBookStacks::stackFor)
                .toList();
    }

    public static Optional<GuideBookDefinition> definition(ItemStack stack) {
        Identifier id = guideId(stack);
        return id == null ? Optional.empty() : GuideBookRegistry.guideBook(id);
    }

    public static Optional<GuideBookDefinition> visibleDefinition(ItemStack stack) {
        Identifier id = guideId(stack);
        return id == null ? Optional.empty() : GuideBookRegistry.visibleGuideBook(id);
    }

    public static Identifier guideId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        GuideBookTarget target = stack.get(ModDataComponents.GUIDE_BOOK_TARGET.get());
        return target == null ? null : target.guideBookId();
    }
}
