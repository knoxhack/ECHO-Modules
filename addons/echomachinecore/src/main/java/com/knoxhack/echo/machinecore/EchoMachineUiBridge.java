package com.knoxhack.echo.machinecore;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public final class EchoMachineUiBridge {
    private EchoMachineUiBridge() {
    }

    public static String inventoryLine(EchoMachineRuntimeSnapshot snapshot) {
        EchoMachineRuntimeSnapshot.InventoryContract inventory = snapshot == null
                ? EchoMachineRuntimeSnapshot.InventoryContract.empty()
                : snapshot.inventory();
        return inventory.occupiedSlots() + "/" + inventory.totalSlots() + " slots";
    }

    public static String energyLine(EchoMachineRuntimeSnapshot snapshot) {
        EchoMachineRuntimeSnapshot.EnergyContract energy = snapshot == null
                ? EchoMachineRuntimeSnapshot.EnergyContract.empty()
                : snapshot.energy();
        if (energy.capacity() <= 0) {
            return "no energy buffer";
        }
        return energy.stored() + "/" + energy.capacity() + " " + energy.unit();
    }

    public static String fluidLine(EchoMachineRuntimeSnapshot snapshot) {
        EchoMachineRuntimeSnapshot.FluidContract fluids = snapshot == null
                ? EchoMachineRuntimeSnapshot.FluidContract.empty()
                : snapshot.fluids();
        if (!fluids.supported()) {
            return fluids.attributes().getOrDefault("model", "no fluid tank");
        }
        return fluids.input().fluidName() + " " + fluids.input().amount() + "/" + fluids.input().capacity()
                + " -> " + fluids.output().fluidName() + " " + fluids.output().amount() + "/"
                + fluids.output().capacity();
    }

    public static String processLine(EchoMachineRuntimeSnapshot snapshot) {
        EchoMachineRuntimeSnapshot.ProcessContract process = snapshot == null
                ? EchoMachineRuntimeSnapshot.ProcessContract.empty()
                : snapshot.process();
        if (process.maxProgressTicks() <= 0) {
            return process.status();
        }
        return process.status() + " " + process.progressTicks() + "/" + process.maxProgressTicks()
                + " (" + process.progressPercent() + "%)";
    }

    public static String sideLine(EchoMachineRuntimeSnapshot snapshot) {
        EchoMachineRuntimeSnapshot.SideConfigurationContract side = snapshot == null
                ? EchoMachineRuntimeSnapshot.SideConfigurationContract.empty()
                : snapshot.side();
        int routedSlots = side.upSlots().size() + side.downSlots().size() + side.sideSlots().size();
        return side.label() + " / " + routedSlots + " routed slot(s)";
    }

    public static String upgradeLine(EchoMachineRuntimeSnapshot snapshot) {
        EchoMachineRuntimeSnapshot.UpgradeContract upgrades = snapshot == null
                ? EchoMachineRuntimeSnapshot.UpgradeContract.empty()
                : snapshot.upgrades();
        return upgrades.installedCount() + "/" + upgrades.capacity() + " installed";
    }

    public static boolean hasAutomationSurface(EchoMachineRuntimeSnapshot snapshot) {
        if (snapshot == null || snapshot.inventory().totalSlots() <= 0) {
            return false;
        }
        EchoMachineRuntimeSnapshot.SideConfigurationContract side = snapshot.side();
        return side.upSlots().size() + side.downSlots().size() + side.sideSlots().size() > 0;
    }

    public static Optional<BlockPos> position(EchoMachineRuntimeSnapshot snapshot) {
        if (snapshot == null) {
            return Optional.empty();
        }
        Map<String, String> attributes = snapshot.attributes();
        String raw = attributes.getOrDefault("position", "");
        if (raw.isBlank()) {
            raw = attributes.getOrDefault("worldPos", "");
        }
        String[] parts = raw.split(",");
        if (parts.length != 3) {
            return Optional.empty();
        }
        try {
            return Optional.of(new BlockPos(
                    Integer.parseInt(parts[0].trim()),
                    Integer.parseInt(parts[1].trim()),
                    Integer.parseInt(parts[2].trim())));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    public static ResourceKey<Level> dimension(EchoMachineRuntimeSnapshot snapshot, ResourceKey<Level> fallback) {
        ResourceKey<Level> safeFallback = fallback == null ? Level.OVERWORLD : fallback;
        if (snapshot == null) {
            return safeFallback;
        }
        Identifier id = Identifier.tryParse(snapshot.attributes().getOrDefault("dimension", ""));
        return id == null ? safeFallback : ResourceKey.create(Registries.DIMENSION, id);
    }

    public static Identifier machineBlockIdentifier(EchoMachineRuntimeSnapshot snapshot) {
        if (snapshot == null) {
            return Identifier.withDefaultNamespace("air");
        }
        Identifier id = Identifier.tryParse(snapshot.machineBlockId());
        return id == null ? Identifier.withDefaultNamespace("air") : id;
    }

    public static Identifier recipeIdentifier(EchoMachineRecipeBinding binding, EchoMachineId fallback) {
        if (binding != null && binding.recipeId() != null && binding.recipeId().namespaced()) {
            Identifier id = Identifier.tryParse(binding.recipeId().value());
            if (id != null) {
                return id;
            }
        }
        String fallbackPath = fallback == null ? "unknown" : fallback.value();
        return Identifier.fromNamespaceAndPath(EchoMachineConstants.MOD_ID, "runtime/" + sanitizePath(fallbackPath));
    }

    public static String sanitizePath(String value) {
        String clean = value == null ? "unknown" : value.toLowerCase(Locale.ROOT).trim();
        clean = clean.replace(':', '/').replace('\\', '/').replaceAll("[^a-z0-9_./-]", "_");
        while (clean.contains("//")) {
            clean = clean.replace("//", "/");
        }
        return clean.isBlank() ? "unknown" : clean;
    }
}
