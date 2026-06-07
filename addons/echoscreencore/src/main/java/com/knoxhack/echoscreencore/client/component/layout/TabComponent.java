package com.knoxhack.echoscreencore.client.component.layout;

import com.knoxhack.echoscreencore.api.component.EchoComponentFactory;

public final class TabComponent extends ContainerComponent {
    public TabComponent(EchoComponentFactory.Context context) {
        super(context);
    }

    public String tabId() {
        String id = node().attribute("id", "");
        return id.isBlank() ? node().attribute("title", "") : id;
    }

    public String tabTitle() {
        String title = node().attribute("title", "");
        return title.isBlank() ? tabId() : title;
    }
}
