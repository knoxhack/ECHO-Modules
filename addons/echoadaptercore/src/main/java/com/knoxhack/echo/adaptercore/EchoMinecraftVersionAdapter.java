package com.knoxhack.echo.adaptercore;

public record EchoMinecraftVersionAdapter(
        String minecraftVersion,
        String loaderName,
        String loaderVersion,
        String adapterVersion,
        boolean currentTarget
) {
    public EchoMinecraftVersionAdapter {
        minecraftVersion = AdapterContractGuards.requireText(minecraftVersion, "minecraft version");
        loaderName = AdapterContractGuards.requireText(loaderName, "loader name");
        loaderVersion = AdapterContractGuards.optionalText(loaderVersion);
        adapterVersion = AdapterContractGuards.optionalText(adapterVersion);
    }
}
