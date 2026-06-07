package com.knoxhack.echogalacticcore.content;

import com.knoxhack.echogalacticcore.GalacticCoreIds;
import com.knoxhack.echogalacticcore.asdk.GalacticCoreNativeMutations;
import dev.echo.nativeplatform.contracts.EchoNativeCapabilityService;
import dev.echo.nativeplatform.contracts.EchoNativeModuleLoadContext;
import dev.echo.nativeplatform.contracts.EchoNativeSaveDataService;

import java.util.List;
import java.util.Map;

public final class GalacticCoreMachines {
    private static final List<String> MACHINES = List.of(
            "oxygen_collector",
            "oxygen_sealer",
            "fuel_loader",
            "rocket_workbench"
    );

    private GalacticCoreMachines() {
    }

    public static void register(
            EchoNativeModuleLoadContext context,
            EchoNativeCapabilityService capabilities,
            EchoNativeSaveDataService saveData
    ) {
        for (String machine : MACHINES) {
            GalacticCoreNativeMutations.record(
                    context,
                    capabilities.mutate(GalacticCoreNativeMutations.common(
                            "capabilities",
                            "mutate",
                            GalacticCoreIds.id("machine/" + machine),
                            Map.of(
                                    "kind", "machine_runtime_contract",
                                    "legacySource", "micdoodle8.mods.galacticraft.core.tile",
                                    "bridge", "echoadaptercore.machine_runtime",
                                    "stateModel", "energy, inventory, progress, redstone, oxygen/fuel where applicable"
                            )
                    ))
            );
            GalacticCoreNativeMutations.record(
                    context,
                    saveData.write(GalacticCoreNativeMutations.server(
                            "save_data",
                            "write",
                            GalacticCoreIds.id("machine_state/" + machine),
                            Map.of(
                                    "kind", "machine_save_contract",
                                    "deterministic", true,
                                    "legacySource", "TileEntity NBT state"
                            )
                    ))
            );
        }
    }
}
