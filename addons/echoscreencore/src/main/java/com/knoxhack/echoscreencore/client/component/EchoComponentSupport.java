package com.knoxhack.echoscreencore.client.component;

import com.knoxhack.echoscreencore.api.component.EchoComponentFactory;
import com.knoxhack.echoscreencore.client.parser.EchoNode;
import java.util.ArrayList;
import java.util.List;

public final class EchoComponentSupport {
    private EchoComponentSupport() {
    }

    public static EchoNode node(EchoComponentFactory.Context context) {
        if (context.sourceNode() instanceof EchoNode node) {
            return node;
        }
        return new EchoNode(context.tagName(), context.attributes(), context.text(), List.of(), "");
    }

    public static List<EchoComponent> children(EchoComponentFactory.Context context) {
        ArrayList<EchoComponent> components = new ArrayList<>();
        for (Object child : context.children()) {
            if (child instanceof EchoComponent component) {
                components.add(component);
            }
        }
        return List.copyOf(components);
    }
}
