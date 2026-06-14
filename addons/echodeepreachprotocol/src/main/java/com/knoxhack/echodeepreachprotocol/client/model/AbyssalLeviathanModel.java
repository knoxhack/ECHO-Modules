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

public class AbyssalLeviathanModel extends EntityModel<LivingEntityRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(EchoDeepReachProtocol.MODID, "abyssal_leviathan"), "main");

    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart tail;
    private final ModelPart finTop;

    public AbyssalLeviathanModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.tail = root.getChild("tail");
        this.finTop = root.getChild("fin_top");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -4.0F, -10.0F, 10.0F, 8.0F, 10.0F),
                PartPose.offset(0.0F, 18.0F, -8.0F));
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 18).addBox(-6.0F, -5.0F, -8.0F, 12.0F, 10.0F, 16.0F),
                PartPose.offset(0.0F, 18.0F, 6.0F));
        root.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(32, 18).addBox(-3.0F, -3.0F, 0.0F, 6.0F, 6.0F, 12.0F),
                PartPose.offset(0.0F, 18.0F, 14.0F));
        root.addOrReplaceChild("fin_top",
                CubeListBuilder.create().texOffs(0, 44).addBox(-1.0F, -8.0F, -4.0F, 2.0F, 8.0F, 10.0F),
                PartPose.offset(0.0F, 12.0F, 4.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks * 0.08F;
        tail.yRot = Mth.sin(t) * 0.25F;
        head.yRot = Mth.sin(t + Mth.PI) * 0.1F;
        finTop.xRot = Mth.sin(t * 0.5F) * 0.1F;
    }
}
