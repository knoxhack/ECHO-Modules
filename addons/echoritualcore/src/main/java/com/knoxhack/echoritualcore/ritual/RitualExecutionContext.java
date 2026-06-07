package com.knoxhack.echoritualcore.ritual;

import com.knoxhack.echoritualcore.block.entity.BasicAltarBlockEntity;
import com.knoxhack.echoritualcore.block.entity.OfferingPedestalBlockEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public record RitualExecutionContext(
        ServerPlayer player,
        BlockPos altarPos,
        RitualStructureReport structure,
        List<OfferingPedestalBlockEntity> pedestals) {
    public RitualExecutionContext {
        altarPos = altarPos == null ? player.blockPosition() : altarPos.immutable();
        structure = structure == null ? RitualStructureValidator.validate(player.level(), altarPos) : structure;
        pedestals = pedestals == null ? List.of() : List.copyOf(pedestals);
    }

    public static RitualExecutionContext create(ServerPlayer player, BlockPos altarPos) {
        BlockPos safePos = altarPos == null ? player.blockPosition() : altarPos.immutable();
        return new RitualExecutionContext(
                player,
                safePos,
                RitualStructureValidator.validate(player.level(), safePos),
                RitualStructureValidator.pedestals(player.level(), safePos));
    }

    public RitualItemAccess items(ItemStack focus) {
        return new RitualItemAccess(player, focus, pedestals);
    }

    public boolean readyForIgnition(Identifier ritualId) {
        if (structure.validBasicArray()) {
            updateAltar(ritualId, null, BasicAltarBlockEntity.RESULT_READY,
                    "Array ready: " + structure.summary());
            return true;
        }
        updateAltar(ritualId, null, BasicAltarBlockEntity.RESULT_WARNING,
                "Array incomplete: " + String.join(", ", structure.missingAnchors()));
        return false;
    }

    public void updateAltar(Identifier ritualId, Identifier subjectId, int result, String message) {
        BlockEntity blockEntity = player.level().getBlockEntity(altarPos);
        if (blockEntity instanceof BasicAltarBlockEntity altar) {
            altar.updateStatus(ritualId, subjectId, result, message, structure);
        }
    }

    public void sendStructureDiagnostic() {
        player.sendSystemMessage(Component.translatable("block.echoritualcore.basic_altar.diagnostic",
                structure.runeCircles(),
                RitualStructureValidator.REQUIRED_RUNE_CIRCLES,
                structure.pedestalCount(),
                structure.stabilityScore()));
        if (!structure.validBasicArray()) {
            player.sendSystemMessage(Component.translatable("block.echoritualcore.basic_altar.missing_structure",
                    String.join(", ", structure.missingAnchors())));
        }
    }
}
