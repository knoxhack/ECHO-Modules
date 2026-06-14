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
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class HadalWraithModel extends EntityModel<LivingEntityRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(EchoDeepReachProtocol.MODID, "hadal_wraith"), "main");

    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart cloakLeft;
    private final ModelPart cloakRight;

    public HadalWraithModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.cloakLeft = root.getChild("cloak_left");
        this.cloakRight = root.getChild("cloak_right");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -6.0F, -3.0F, 6.0F, 6.0F, 6.0F),
                PartPose.offset(0.0F, 6.0F, 0.0F));
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 18).addBox(-2.5F, 0.0F, -1.5F, 5.0F, 11.0F, 3.0F),
                PartPose.offset(0.0F, 6.0F, 0.0F));
        root.addOrReplaceChild("cloak_left",
                CubeListBuilder.create().texOffs(24, 0).addBox(0.0F, 0.0F, -1.0F, 4.0F, 14.0F, 2.0F),
                PartPose.offset(2.0F, 7.0F, 0.5F));
        root.addOrReplaceChild("cloak_right",
                CubeListBuilder.create().texOffs(24, 16).addBox(-4.0F, 0.0F, -1.0F, 4.0F, 14.0F, 2.0F),
                PartPose.offset(-2.0F, 7.0F, 0.5F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks * 0.12F;
        cloakLeft.zRot = Mth.sin(t) * 0.08F;
        cloakRight.zRot = -Mth.sin(t) * 0.08F;
        body.y = 6.0F + Mth.sin(t * 0.25F) * 0.2F;
        head.yRot = state.yRot * ((float) Math.PI / 180.0F) * 0.5F;
    }
}
