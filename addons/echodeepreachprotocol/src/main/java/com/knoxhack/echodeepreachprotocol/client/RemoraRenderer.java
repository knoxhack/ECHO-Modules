package com.knoxhack.echodeepreachprotocol.client;

import com.knoxhack.echodeepreachprotocol.EchoDeepReachProtocol;
import com.knoxhack.echodeepreachprotocol.client.model.RemoraModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.AbstractBoatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.BoatRenderState;
import net.minecraft.resources.Identifier;

/**
 * Client renderer for the Remora submersible using a custom submarine model.
 */
public final class RemoraRenderer extends AbstractBoatRenderer {
    public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            EchoDeepReachProtocol.MODID, "textures/entity/remora.png");

    private final EntityModel<BoatRenderState> model;

    public RemoraRenderer(EntityRendererProvider.Context context) {
        super(context, TEXTURE);
        this.model = new RemoraModel(context.bakeLayer(RemoraModel.LAYER_LOCATION));
    }

    @Override
    protected EntityModel<BoatRenderState> model() {
        return model;
    }
}
