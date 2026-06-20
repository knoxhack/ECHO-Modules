package com.knoxhack.echoindex.client;

import com.echoplatform.echocore.api.EchoRuntimeModules;
import com.knoxhack.echoindex.EchoIndexClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

final class IndexTooltipUtil {
    private IndexTooltipUtil() {
    }

    static List<Component> itemTooltip(ItemStack stack, Component... extraLines) {
        Minecraft minecraft = Minecraft.getInstance();
        Item.TooltipContext context = minecraft.level == null ? Item.TooltipContext.EMPTY : Item.TooltipContext.of(minecraft.level);
        List<Component> tooltip = new ArrayList<>(stack.getTooltipLines(context, minecraft.player, TooltipFlag.NORMAL));
        if (extraLines != null) {
            for (Component line : extraLines) {
                if (line != null) {
                    tooltip.add(line);
                }
            }
        }
        appendModName(tooltip, stack);
        return tooltip;
    }

    static void showItemTooltip(GuiGraphicsExtractor graphics, Font font, ItemStack stack, int x, int y) {
        showItemTooltip(graphics, font, stack, itemTooltip(stack), x, y);
    }

    static void showItemTooltip(GuiGraphicsExtractor graphics, Font font, ItemStack stack, List<Component> tooltip,
            int x, int y) {
        if (graphics == null || font == null || stack == null || stack.isEmpty()) {
            return;
        }
        List<FormattedCharSequence> lines = new ArrayList<>();
        for (Component line : tooltip) {
            if (line != null) {
                lines.add(line.getVisualOrderText());
            }
        }
        boolean nativeClient = EchoIndexClient.nativeLoaderClientActiveForScreens();
        Optional<TooltipComponent> image = nativeClient ? Optional.empty() : stack.getTooltipImage();
        Optional<TooltipComponent> customImage = image;
        if (!nativeClient && stack.getItem() instanceof BlockItem) {
            customImage = Optional.of(new IndexBlockPreviewTooltipData(stack, image, previewSize(graphics)));
        }
        setTooltipForNextFrame(graphics, font, lines, customImage, x, y, stack.get(DataComponents.TOOLTIP_STYLE));
    }

    private static void setTooltipForNextFrame(GuiGraphicsExtractor graphics, Font font,
            List<FormattedCharSequence> lines, Optional<TooltipComponent> image, int x, int y, Identifier style) {
        try {
            graphics.setTooltipForNextFrame(font, lines, image, IndexTooltipPositioner.INSTANCE, x, y, false, style);
        } catch (RuntimeException exception) {
            try {
                graphics.setTooltipForNextFrame(font, lines, Optional.empty(), IndexTooltipPositioner.INSTANCE, x, y,
                        false, null);
            } catch (RuntimeException ignored) {
            }
        }
    }

    private static int previewSize(GuiGraphicsExtractor graphics) {
        int shortSide = Math.min(graphics.guiWidth(), graphics.guiHeight());
        if (shortSide <= 260) {
            return 32;
        }
        if (shortSide <= 420) {
            return 48;
        }
        return 64;
    }

    static void appendModName(List<Component> tooltip, ItemStack stack) {
        String modName = modName(stack);
        if (!modName.isBlank() && tooltip.stream().noneMatch(line -> modName.equals(line.getString()))) {
            tooltip.add(Component.literal(modName).withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC));
        }
    }

    private static String modName(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemId == null) {
            return "";
        }
        String namespace = itemId.getNamespace();
        String displayName = EchoRuntimeModules.metadata(namespace, namespace).displayName();
        return displayName == null || displayName.isBlank() ? namespace : displayName;
    }
}
