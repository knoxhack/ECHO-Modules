package com.knoxhack.echostationfall.event;

import com.knoxhack.echo.adaptercore.EchoBackendClientBridge;
import com.knoxhack.echostationfall.EchoStationfall;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public class ModTooltipEvents {
    public void onItemTooltip(Object event) {
        ItemStack stack = EchoBackendClientBridge.tooltipItemStack(event);
        if (stack.isEmpty()) {
            return;
        }
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!EchoStationfall.MODID.equals(id.getNamespace())) {
            return;
        }
        String line = switch (id.getPath()) {
            case "station_access_card" -> "Boards Stationfall from orbital staging; sneak-use inside the station to return.";
            case "station_battery" -> "Restores section power; sneak-use on a node to overload it.";
            case "hull_cutter" -> "Forces damaged panels and partial power at a pressure and panic cost.";
            case "pressure_seal_kit" -> "Repairs hull breaches and restores suit pressure integrity.";
            case "emergency_oxygen_pack" -> "Vents emergency oxygen into the Orbital suit reserve.";
            case "signal_panic_dampener" -> "Suppresses hallucination events and reduces Signal Panic.";
            case "stationfall_blackbox" -> "Major Stationfall handoff item for Blackbox Protocol.";
            default -> "Stationfall component for power, pressure, panic, or crew archive recovery.";
        };
        EchoBackendClientBridge.addTooltipLine(event, Component.literal(line).withStyle(ChatFormatting.DARK_AQUA));
    }
}
