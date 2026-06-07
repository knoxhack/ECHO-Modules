package com.knoxhack.echo.packcore;

import java.util.List;

public record EchoJarInventory(
        List<EchoInstalledModule> installedModules,
        int jarCount
) {
    public EchoJarInventory {
        installedModules = PackContractGuards.immutableList(installedModules);
        jarCount = Math.max(0, jarCount);
    }
}
