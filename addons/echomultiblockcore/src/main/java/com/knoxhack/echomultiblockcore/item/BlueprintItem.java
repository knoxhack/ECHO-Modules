package com.knoxhack.echomultiblockcore.item;

import com.knoxhack.echomultiblockcore.api.BlueprintData;
import com.knoxhack.echomultiblockcore.api.MultiblockDefinition;
import com.knoxhack.echomultiblockcore.api.MultiblockMaterialSummary;
import com.knoxhack.echomultiblockcore.block.entity.MultiblockControllerBlockEntity;
import com.knoxhack.echomultiblockcore.content.MultiblockContent;
import com.knoxhack.echomultiblockcore.registry.ModDataComponents;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class BlueprintItem extends Item {
    private final Identifier defaultDefinitionId;

    public BlueprintItem(Identifier defaultDefinitionId, Properties properties) {
        super(properties);
        this.defaultDefinitionId = defaultDefinitionId == null ? Identifier.fromNamespaceAndPath("echomultiblockcore", "unknown") : defaultDefinitionId;
    }

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        stack.set(ModDataComponents.BLUEPRINT_DATA.get(), new BlueprintData(defaultDefinitionId));
        return stack;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player == null) {
            return InteractionResult.CONSUME;
        }
        Identifier definitionId = definitionId(context.getItemInHand(), defaultDefinitionId);
        BlockEntity blockEntity = level.getBlockEntity(context.getClickedPos());
        if (blockEntity instanceof MultiblockControllerBlockEntity controller) {
            controller.inspectWithBlueprint(player, definitionId);
            return InteractionResult.SUCCESS_SERVER;
        }
        Optional<MultiblockDefinition> definition = MultiblockContent.definition(definitionId);
        if (definition.isPresent()) {
            MultiblockDefinition value = definition.get();
            player.sendSystemMessage(Component.translatable("message.echomultiblockcore.blueprint.summary",
                    value.displayName(), value.role().name(), value.width(), value.height(), value.depth()));
        } else {
            player.sendSystemMessage(Component.translatable("message.echomultiblockcore.blueprint.unknown", definitionId.toString())
                    .withStyle(ChatFormatting.RED));
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
            Consumer<Component> tooltip, TooltipFlag flag) {
        Identifier id = definitionId(stack, defaultDefinitionId);
        Optional<MultiblockDefinition> definition = MultiblockContent.definition(id);
        if (definition.isPresent()) {
            MultiblockDefinition value = definition.get();
            tooltip.accept(Component.translatable("tooltip.echomultiblockcore.blueprint.structure", value.displayName()).withStyle(ChatFormatting.AQUA));
            tooltip.accept(Component.translatable("tooltip.echomultiblockcore.blueprint.role", value.role().name()).withStyle(ChatFormatting.GRAY));
            tooltip.accept(Component.translatable("tooltip.echomultiblockcore.blueprint.size", value.width(), value.height(), value.depth()).withStyle(ChatFormatting.GRAY));
            MultiblockMaterialSummary materials = MultiblockMaterialSummary.from(value);
            tooltip.accept(Component.translatable("tooltip.echomultiblockcore.blueprint.materials",
                    materials.compactLine(4, false)).withStyle(ChatFormatting.DARK_AQUA));
            int extraMaterials = materials.extraPlaceableEntries(4);
            if (extraMaterials > 0) {
                tooltip.accept(Component.translatable("tooltip.echomultiblockcore.blueprint.materials_more", extraMaterials)
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
        } else {
            tooltip.accept(Component.translatable("tooltip.echomultiblockcore.blueprint.definition", id.toString()).withStyle(ChatFormatting.GRAY));
        }
        tooltip.accept(Component.translatable("tooltip.echomultiblockcore.blueprint.use").withStyle(ChatFormatting.DARK_AQUA));
    }

    public static Identifier definitionId(ItemStack stack, Identifier fallback) {
        if (stack != null && !stack.isEmpty()) {
            BlueprintData data = stack.get(ModDataComponents.BLUEPRINT_DATA.get());
            if (data != null) {
                return data.definitionId();
            }
        }
        return fallback == null ? BlueprintData.EMPTY.definitionId() : fallback;
    }
}
