package com.knoxhack.echoscreencore.api;

import java.util.List;

@FunctionalInterface
public interface EchoDataProvider {
    Object resolve(EchoDataContext context, List<String> path);
}
