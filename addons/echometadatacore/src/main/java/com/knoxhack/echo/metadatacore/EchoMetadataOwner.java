package com.knoxhack.echo.metadatacore;

import java.util.List;

public record EchoMetadataOwner(
        String name,
        String role,
        String contact,
        List<String> areas
) {
    public EchoMetadataOwner {
        name = MetadataContractGuards.requireText(name, "owner name");
        role = MetadataContractGuards.optionalText(role);
        contact = MetadataContractGuards.optionalText(contact);
        areas = MetadataContractGuards.immutableList(areas);
    }
}
