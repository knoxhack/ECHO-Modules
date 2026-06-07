package com.knoxhack.echoscreencore.client.input;

import com.knoxhack.echoscreencore.client.component.EchoComponent;

public record EchoFocusNode(EchoComponent component, int order, boolean autofocus) {
}
