package com.knoxhack.echoscreencore.client.layout;

import com.knoxhack.echoscreencore.client.component.EchoComponent;
import com.knoxhack.echoscreencore.client.parser.EchoNode;

public final class EchoResponsiveResolver {
    public EchoResponsiveRules resolve(EchoComponent component, EchoResponsiveContext context) {
        EchoNode node = component == null ? null : component.node();
        int width = context == null ? 9999 : context.viewportWidth();
        boolean hidden = below(node, "hide-below", width);
        boolean stacked = below(node, "stack-below", width);
        boolean compact = below(node, "compact-below", width);
        boolean dense = below(node, "dense-below", width);
        boolean collapsed = below(node, "collapse-below", width);
        boolean sidebarCollapsed = below(node, "sidebar-collapse-below", width);
        boolean detailCollapsed = below(node, "detail-collapse-below", width);
        String tag = node == null ? "" : node.tagName();
        if ("app-sidebar".equals(tag) && sidebarCollapsed) {
            compact = true;
            collapsed = true;
        }
        if (("detail-panel".equals(tag) || "inspector-panel".equals(tag)) && detailCollapsed) {
            stacked = true;
        }
        return new EchoResponsiveRules(hidden, stacked, compact, dense, collapsed, sidebarCollapsed,
            detailCollapsed, context == null ? EchoBreakpoint.XL : context.activeBreakpoint());
    }

    private static boolean below(EchoNode node, String attribute, int viewportWidth) {
        if (node == null || !node.hasAttribute(attribute)) {
            return false;
        }
        int threshold = EchoBreakpoint.threshold(node.attribute(attribute, ""), -1);
        return threshold >= 0 && viewportWidth < threshold;
    }
}
