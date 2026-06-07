package com.knoxhack.echoscreencore.api.component;

import java.util.List;
import java.util.Map;
import java.util.Set;

@FunctionalInterface
public interface EchoComponentFactory {
    Object create(Context context);

    record Context(
        String tagName,
        String id,
        Set<String> classes,
        Map<String, String> attributes,
        String text,
        List<Object> children,
        Object sourceNode
    ) {
    }
}
