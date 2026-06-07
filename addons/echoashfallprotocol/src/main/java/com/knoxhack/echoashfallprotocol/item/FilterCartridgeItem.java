package com.knoxhack.echoashfallprotocol.item;

import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResult;
import com.knoxhack.echoashfallprotocol.event.AshfallAdapterCoreHazardRuntime;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Filter Cartridge - consumed by the Gas Mask air system.
 * Tiered cartridges refill the shared gas mask hazard-zone capacity.
 */
public class FilterCartridgeItem extends Item {

    public enum Tier {
        BASIC(1, "Basic", 0xAAAA00),
        ADVANCED(2, "Advanced", 0x5555FF),
        ELITE(3, "Elite", 0xFF55FF);

        private final int level;
        private final String displayName;
        private final int color;

        Tier(int level, String displayName, int color) {
            this.level = level;
            this.displayName = displayName;
            this.color = color;
        }

        public int getLevel() {
            return level;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getColor() {
            return color;
        }
    }

    private final Tier tier;

    public FilterCartridgeItem(Properties properties, Tier tier) {
        super(properties);
        this.tier = tier;
    }

    public Tier getFilterTier() {
        return tier;
    }

    /**
     * Legacy tooltip value retained for compatibility with old item text.
     * Runtime filter drain is now configured globally and only active in toxic zones.
     */
    public float getDegradationRate() {
        return switch (tier) {
            case BASIC -> 1.0F;
            case ADVANCED -> 0.5F;
            case ELITE -> 0.2F;
        };
    }

    /**
     * Filter life amount provided by this cartridge.
     * BASIC: +300, ADVANCED: +600, ELITE: +1000
     */
    public int getFilterAmount() {
        return switch (tier) {
            case BASIC -> 300;
            case ADVANCED -> 600;
            case ELITE -> 1000;
        };
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            NativeResult result = AshfallAdapterCoreHazardRuntime.filterCartridgeUsed(
                    serverPlayer,
                    stack,
                    hand,
                    tier.getDisplayName(),
                    tier.getLevel(),
                    getFilterAmount());
            if (result.terminalFailure()) {
                return InteractionResult.FAIL;
            }
            return result.mutated() ? InteractionResult.CONSUME : InteractionResult.FAIL;
        }

        return InteractionResult.SUCCESS;
    }
}
