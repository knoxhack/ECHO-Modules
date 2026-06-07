package com.knoxhack.echoashfallprotocol.item;

import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResult;
import com.knoxhack.echoashfallprotocol.event.AshfallAdapterCoreExplorationRuntime;
import com.knoxhack.echoashfallprotocol.world.POIScannerService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Signal Scanner - ECHO exploration tool for POI route discovery.
 */
public class SignalScannerItem extends Item {
    
    public SignalScannerItem(Properties properties) {
        super(properties);
    }
    
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        
        if (!player.isShiftKeyDown()) {
            POIScannerService.ScanHit hit = POIScannerService.scan(serverPlayer);
            NativeResult scanResult = AshfallAdapterCoreExplorationRuntime.portableScannerUsed(
                    serverPlayer,
                    hit,
                    "portable_signal_scanner",
                    false,
                    hand,
                    1,
                    false,
                    0);
            return scanResult.terminalFailure() ? InteractionResult.FAIL : InteractionResult.SUCCESS;
        } else {
            POIScannerService.ScanHit hit = POIScannerService.scan(serverPlayer);
            NativeResult scanResult = AshfallAdapterCoreExplorationRuntime.portableScannerUsed(
                    serverPlayer,
                    hit,
                    "portable_signal_scanner_deep_scan",
                    true,
                    hand,
                    3,
                    true,
                    2);
            return scanResult.terminalFailure() ? InteractionResult.FAIL : InteractionResult.SUCCESS;
        }
    }
    
    /**
     * Signal Scanner cannot be enchanted.
     */
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }
    
    /**
     * Signal Scanner can be repaired with scrap circuits.
     */
    public boolean isRepairable(ItemStack stack) {
        return true;
    }
}
