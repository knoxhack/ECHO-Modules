package com.knoxhack.echowiki.api;

import com.knoxhack.echowiki.content.GuideBookRegistry;
import com.knoxhack.echowiki.item.GuideBookStacks;
import com.knoxhack.echowiki.registry.ModItems;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * Small public surface for optional ECHO addon integrations.
 */
public final class EchoWikiApi {
    private EchoWikiApi() {
    }

    /**
     * Returns a targeted guide-book stack only when the guide is visible in the current addon set.
     */
    public static ItemStack guideBookStack(Identifier guideBookId) {
        return GuideBookRegistry.visibleGuideBook(guideBookId)
                .map(GuideBookStacks::stackFor)
                .orElse(ItemStack.EMPTY);
    }

    /**
     * Returns an untagged guide-book stack that opens the loaded guide-book library.
     */
    public static ItemStack guideBookLibraryStack() {
        return new ItemStack(ModItems.GUIDE_BOOK.get());
    }

    public static boolean isGuideBookVisible(Identifier guideBookId) {
        return GuideBookRegistry.visibleGuideBook(guideBookId).isPresent();
    }
}
