package com.knoxhack.echoorbitalremnants.progression;

import com.knoxhack.echoorbitalremnants.Config;
import com.knoxhack.echoorbitalremnants.registry.ModBlocks;
import com.knoxhack.echoorbitalremnants.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public record LaunchReadiness(boolean ready, List<Component> missing) {
    public static LaunchReadiness evaluate(Player player) {
        return evaluateForLaunch(player);
    }

    public static LaunchReadiness evaluateForLaunch(Player player) {
        if (player == null) {
            return new LaunchReadiness(false, List.of(Component.literal("Player telemetry offline")));
        }
        if (!Config.REQUIRE_FULL_LAUNCH_READINESS.get() || bypassesReadiness(player)) {
            return new LaunchReadiness(true, List.of());
        }

        List<Component> missing = new ArrayList<>();
        requireInfrastructure(player, missing);
        requireEquipped(player, missing, EquipmentSlot.HEAD, ModItems.PRESSURIZED_HELMET.get(), "worn Pressurized Helmet");
        requireEquipped(player, missing, EquipmentSlot.CHEST, ModItems.PRESSURIZED_CHESTPLATE.get(), "worn Pressurized Chestplate");
        requireEquipped(player, missing, EquipmentSlot.LEGS, ModItems.PRESSURIZED_LEGGINGS.get(), "worn Pressurized Leggings");
        requireEquipped(player, missing, EquipmentSlot.FEET, ModItems.MAGNETIC_BOOTS.get(), "worn Magnetic Boots");
        require(player, missing, ModItems.OXYGEN_TANK.get(), "Oxygen Tank");

        return new LaunchReadiness(missing.isEmpty(), List.copyOf(missing));
    }

    public static LaunchReadiness evaluateForAssembly(Player player) {
        if (player == null) {
            return new LaunchReadiness(false, List.of(Component.literal("Player telemetry offline")));
        }
        if (!Config.REQUIRE_FULL_LAUNCH_READINESS.get() || bypassesReadiness(player)) {
            return new LaunchReadiness(true, List.of());
        }

        List<Component> missing = new ArrayList<>();
        requireInfrastructure(player, missing);
        require(player, missing, ModItems.ROCKET_NOSE_CONE.get(), "Rocket Nose Cone");
        require(player, missing, ModItems.FUEL_TANK.get(), "Fuel Tank");
        require(player, missing, ModItems.SALVAGED_ENGINE.get(), "Salvaged Engine");
        require(player, missing, ModItems.LANDING_GEAR.get(), "Landing Gear");
        require(player, missing, ModItems.ECHO_FLIGHT_CORE.get(), "ECHO Flight Core");
        require(player, missing, ModItems.NAVIGATION_COMPUTER.get(), "Navigation Computer");

        return new LaunchReadiness(missing.isEmpty(), List.copyOf(missing));
    }

    public Component summary() {
        if (ready) {
            return Component.literal("Launch status: READY");
        }
        return Component.literal("Launch status: HOLD (" + missing.size() + " missing)");
    }

    private static void require(Player player, List<Component> missing, ItemLike item, String name) {
        if (!has(player, item.asItem())) {
            missing.add(Component.literal("- " + name));
        }
    }

    private static void requireInfrastructure(Player player, List<Component> missing) {
        if (LaunchPadLocator.findNearbyPlatformCenter(player).isEmpty()) {
            missing.add(Component.literal("- complete nearby 5x5 Launch Platform grid"));
        }
        requireNearbyBlock(player, missing, ModBlocks.ROCKET_ASSEMBLY_FRAME.get(), "nearby Rocket Assembly Frame");
        requireNearbyBlock(player, missing, ModBlocks.FUEL_REFINERY.get(), "nearby Fuel Refinery");
        requireNearbyBlock(player, missing, ModBlocks.OXYGEN_COMPRESSOR.get(), "nearby Oxygen Compressor");
        requireNearbyBlock(player, missing, ModBlocks.NAVIGATION_CONSOLE.get(), "nearby Navigation Console");
    }

    private static void requireNearbyBlock(Player player, List<Component> missing, Block block, String name) {
        if (!hasNearbyBlock(player, block)) {
            missing.add(Component.literal("- " + name));
        }
    }

    private static void requireEquipped(Player player, List<Component> missing, EquipmentSlot slot, Item item, String name) {
        if (player.getItemBySlot(slot).getItem() != item && !has(player, item)) {
            missing.add(Component.literal("- " + name));
        }
    }

    private static boolean has(Player player, Item item) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty() && stack.getItem() == item) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasNearbyBlock(Player player, Block block) {
        BlockPos center = player.blockPosition();
        int radius = 12;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -4, -radius), center.offset(radius, 4, radius))) {
            if (player.level().getBlockState(pos).getBlock() == block) {
                return true;
            }
        }
        return false;
    }

    public static boolean bypassesReadiness(Player player) {
        if (player instanceof ServerPlayer serverPlayer && serverPlayer.gameMode.getGameModeForPlayer().isCreative()) {
            return true;
        }
        return player.hasInfiniteMaterials() || player.getAbilities().instabuild || player.isCreative();
    }

}
