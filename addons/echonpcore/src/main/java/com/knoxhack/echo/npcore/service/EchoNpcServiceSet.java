package com.knoxhack.echo.npcore.service;

import java.util.List;
import net.minecraft.resources.Identifier;

public record EchoNpcServiceSet(Identifier id, List<EchoNpcServiceDefinition> services) {
    public EchoNpcServiceSet {
        services = List.copyOf(services == null ? List.of() : services);
    }

    public EchoNpcServiceDefinition service(String serviceId) {
        return services.stream().filter(service -> service.id().equals(serviceId)).findFirst().orElse(null);
    }
}
