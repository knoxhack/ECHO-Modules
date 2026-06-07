package com.knoxhack.echoscreencore.client.component;

import com.knoxhack.echoscreencore.api.EchoScreenRegistry;
import com.knoxhack.echoscreencore.api.component.EchoComponentFactory;
import com.knoxhack.echoscreencore.api.style.EchoStyle;
import com.knoxhack.echoscreencore.client.component.basic.ButtonComponent;
import com.knoxhack.echoscreencore.client.component.basic.CopyBlockComponent;
import com.knoxhack.echoscreencore.client.component.basic.EmptyStateComponent;
import com.knoxhack.echoscreencore.client.component.basic.IconComponent;
import com.knoxhack.echoscreencore.client.component.basic.ImageComponent;
import com.knoxhack.echoscreencore.client.component.basic.ItemIconComponent;
import com.knoxhack.echoscreencore.client.component.basic.ItemStackComponent;
import com.knoxhack.echoscreencore.client.component.basic.LabelComponent;
import com.knoxhack.echoscreencore.client.component.basic.PageComponent;
import com.knoxhack.echoscreencore.client.component.basic.ProgressBarComponent;
import com.knoxhack.echoscreencore.client.component.basic.SpacerComponent;
import com.knoxhack.echoscreencore.client.component.basic.StatBarComponent;
import com.knoxhack.echoscreencore.client.component.basic.StatCardComponent;
import com.knoxhack.echoscreencore.client.component.basic.StatusChipComponent;
import com.knoxhack.echoscreencore.client.component.basic.TextComponent;
import com.knoxhack.echoscreencore.client.component.basic.TitleComponent;
import com.knoxhack.echoscreencore.client.component.basic.TooltipComponent;
import com.knoxhack.echoscreencore.client.component.data.ListComponent;
import com.knoxhack.echoscreencore.client.component.data.ListRowComponent;
import com.knoxhack.echoscreencore.client.component.data.OptionComponent;
import com.knoxhack.echoscreencore.client.component.data.RepeatComponent;
import com.knoxhack.echoscreencore.client.component.data.SelectComponent;
import com.knoxhack.echoscreencore.client.component.data.InputTextComponent;
import com.knoxhack.echoscreencore.client.component.data.SearchBoxComponent;
import com.knoxhack.echoscreencore.client.component.data.ToggleComponent;
import com.knoxhack.echoscreencore.client.component.data.CheckboxComponent;
import com.knoxhack.echoscreencore.client.component.layout.CardComponent;
import com.knoxhack.echoscreencore.client.component.layout.ColumnComponent;
import com.knoxhack.echoscreencore.client.component.layout.ContainerComponent;
import com.knoxhack.echoscreencore.client.component.layout.DialogComponent;
import com.knoxhack.echoscreencore.client.component.layout.GridComponent;
import com.knoxhack.echoscreencore.client.component.layout.HeaderComponent;
import com.knoxhack.echoscreencore.client.component.layout.HeroCardComponent;
import com.knoxhack.echoscreencore.client.component.layout.PanelComponent;
import com.knoxhack.echoscreencore.client.component.layout.RowComponent;
import com.knoxhack.echoscreencore.client.component.layout.ScreenShellComponent;
import com.knoxhack.echoscreencore.client.component.layout.ScrollPanelComponent;
import com.knoxhack.echoscreencore.client.component.layout.SectionComponent;
import com.knoxhack.echoscreencore.client.component.layout.StackComponent;
import com.knoxhack.echoscreencore.client.component.layout.TabComponent;
import com.knoxhack.echoscreencore.client.component.layout.TabsComponent;
import com.knoxhack.echoscreencore.client.debug.EchoScreenDiagnostics;
import com.knoxhack.echoscreencore.client.parser.EchoNode;
import java.util.List;

public final class EchoComponentRegistry {
    private static boolean defaultsRegistered;

    private EchoComponentRegistry() {
    }

    public static void registerDefaults() {
        if (defaultsRegistered) {
            return;
        }
        defaultsRegistered = true;
        register("page", PageComponent::new);
        register("screen-shell", ScreenShellComponent::new);
        register("header", HeaderComponent::new);
        register("sidebar", PanelComponent::new);
        register("section", SectionComponent::new);
        register("panel", PanelComponent::new);
        register("card", CardComponent::new);
        register("hero-card", HeroCardComponent::new);
        register("button", ButtonComponent::new);
        register("copy-block", CopyBlockComponent::new);
        register("text", TextComponent::new);
        register("title", TitleComponent::new);
        register("label", LabelComponent::new);
        register("icon", IconComponent::new);
        register("image", ImageComponent::new);
        register("item-icon", ItemIconComponent::new);
        register("item-stack", ItemStackComponent::new);
        register("row", RowComponent::new);
        register("column", ColumnComponent::new);
        register("stack", StackComponent::new);
        register("grid", GridComponent::new);
        register("scroll", ScrollPanelComponent::new);
        register("tabs", TabsComponent::new);
        register("tab", TabComponent::new);
        register("select", SelectComponent::new);
        register("dropdown", SelectComponent::new);
        register("dropdown-menu", SelectComponent::new);
        register("dropdown-item", OptionComponent::new);
        register("option", OptionComponent::new);
        register("divider", OptionComponent::new);
        register("menu-section", OptionComponent::new);
        register("tooltip", TooltipComponent::new);
        register("modal", DialogComponent::new);
        register("dialog", DialogComponent::new);
        register("dialog-title", TitleComponent::new);
        register("dialog-body", ContainerComponent::new);
        register("dialog-actions", RowComponent::new);
        register("list", ListComponent::new);
        register("list-row", ListRowComponent::new);
        register("repeat", RepeatComponent::new);
        register("input", InputTextComponent::new);
        register("search-box", SearchBoxComponent::new);
        register("toggle", ToggleComponent::new);
        register("checkbox", CheckboxComponent::new);
        register("status-chip", StatusChipComponent::new);
        register("progress-bar", ProgressBarComponent::new);
        register("stat-card", StatCardComponent::new);
        register("stat-bar", StatBarComponent::new);
        register("empty-state", EmptyStateComponent::new);
        register("spacer", SpacerComponent::new);
        register("split", ContainerComponent::new);
        register("app-shell", ScreenShellComponent::new);
        register("app-header", HeaderComponent::new);
        register("app-sidebar", PanelComponent::new);
        register("app-content", ContainerComponent::new);
        register("app-footer", ContainerComponent::new);
        register("nav-list", ListComponent::new);
        register("nav-item", ListRowComponent::new);
        register("command-card", CardComponent::new);
        register("detail-panel", PanelComponent::new);
        register("inspector-panel", PanelComponent::new);
        register("split-view", GridComponent::new);
    }

    public static EchoComponent create(EchoNode node, List<EchoComponent> children, EchoStyle style, EchoScreenDiagnostics diagnostics) {
        registerDefaults();
        EchoComponentFactory factory = EchoScreenRegistry.componentFactory(node.tagName()).orElse(null);
        if (factory == null) {
            if (diagnostics != null) {
                diagnostics.warnOnce("unknown_component_tag", node.tagName());
            }
            factory = ContainerComponent::new;
        }
        Object created = factory.create(new EchoComponentFactory.Context(
            node.tagName(),
            node.id(),
            node.classes(),
            node.attributes(),
            node.text(),
            List.copyOf(children),
            node
        ));
        EchoComponent component = created instanceof EchoComponent resolved
            ? resolved
            : new ContainerComponent(new EchoComponentFactory.Context(node.tagName(), node.id(), node.classes(), node.attributes(), node.text(), List.copyOf(children), node));
        component.setStyle(style);
        return component;
    }

    private static void register(String tag, EchoComponentFactory factory) {
        EchoScreenRegistry.registerComponent(tag, factory);
    }
}
