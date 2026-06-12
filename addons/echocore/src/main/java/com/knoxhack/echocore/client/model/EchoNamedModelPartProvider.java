package com.knoxhack.echocore.client.model;

import java.util.Map;
import net.minecraft.client.model.geom.ModelPart;

public interface EchoNamedModelPartProvider {
    Map<String, ModelPart> echoNamedModelParts();
}
