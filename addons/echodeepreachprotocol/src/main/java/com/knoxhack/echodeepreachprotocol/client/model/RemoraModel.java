package com.knoxhack.echodeepreachprotocol.client.model;

import com.knoxhack.echodeepreachprotocol.EchoDeepReachProtocol;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.BoatRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Custom submarine model for the Remora submersible.
 */
public class RemoraModel extends EntityModel<BoatRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(EchoDeepReachProtocol.MODID, "remora"), "main");

    private final ModelPart hull;
    private final ModelPart conning;
    private final ModelPart propeller;

    public RemoraModel(ModelPart root) {
        super(root);
        this.hull = root.getChild("hull");
        this.conning = root.getChild("conning");
        this.propeller = root.getChild("propeller");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("hull",
                CubeListBuilder.create().texOffs(0, 0).addBox(-7.0F, -3.0F, -18.0F, 14.0F, 7.0F, 36.0F),
                PartPose.offset(0.0F, 21.0F, 0.0F));
        root.addOrReplaceChild("conning",
                CubeListBuilder.create().texOffs(0, 43).addBox(-3.0F, -5.0F, -4.0F, 6.0F, 5.0F, 8.0F),
                PartPose.offset(0.0F, 18.0F, 4.0F));
        root.addOrReplaceChild("propeller",
                CubeListBuilder.create().texOffs(28, 43).addBox(-4.0F, -3.0F, -1.0F, 8.0F, 6.0F, 2.0F),
                PartPose.offset(0.0F, 21.0F, 18.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(BoatRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks * 0.3F;
        propeller.yRot = t;
        hull.yRot = Mth.sin(t * 0.1F) * 0.02F;
        conning.yRot = Mth.sin(t * 0.1F) * 0.02F;
    }
}
