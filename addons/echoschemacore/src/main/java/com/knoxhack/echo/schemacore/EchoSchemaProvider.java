package com.knoxhack.echo.schemacore;

import com.knoxhack.echo.platformcore.EchoModuleId;

import java.util.Set;

public interface EchoSchemaProvider {
    EchoModuleId moduleId();

    Set<EchoSchemaDescriptor> schemaDescriptors();
}
