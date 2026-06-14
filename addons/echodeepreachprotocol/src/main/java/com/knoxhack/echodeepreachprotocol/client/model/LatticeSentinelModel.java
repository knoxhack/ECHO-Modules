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

public class LatticeSentinelModel extends EntityModel<LivingEntityRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(EchoDeepReachProtocol.MODID, "lattice_sentinel"), "main");

    private final ModelPart core;
    private final ModelPart shard1;
    private final ModelPart shard2;
    private final ModelPart shard3;
    private final ModelPart shard4;

    public LatticeSentinelModel(ModelPart root) {
        super(root);
        this.core = root.getChild("core");
        this.shard1 = root.getChild("shard1");
        this.shard2 = root.getChild("shard2");
        this.shard3 = root.getChild("shard3");
        this.shard4 = root.getChild("shard4");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("core",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 6.0F),
                PartPose.offset(0.0F, 15.0F, 0.0F));
        root.addOrReplaceChild("shard1",
                CubeListBuilder.create().texOffs(24, 0).addBox(-2.0F, -6.0F, -1.0F, 4.0F, 12.0F, 2.0F),
                PartPose.offset(6.0F, 15.0F, 0.0F));
        root.addOrReplaceChild("shard2",
                CubeListBuilder.create().texOffs(24, 0).addBox(-2.0F, -6.0F, -1.0F, 4.0F, 12.0F, 2.0F),
                PartPose.offset(-6.0F, 15.0F, 0.0F));
        root.addOrReplaceChild("shard3",
                CubeListBuilder.create().texOffs(36, 0).addBox(-1.0F, -2.0F, -6.0F, 2.0F, 4.0F, 12.0F),
                PartPose.offset(0.0F, 15.0F, 6.0F));
        root.addOrReplaceChild("shard4",
                CubeListBuilder.create().texOffs(36, 0).addBox(-1.0F, -2.0F, -6.0F, 2.0F, 4.0F, 12.0F),
                PartPose.offset(0.0F, 15.0F, -6.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks * 0.12F;
        shard1.zRot = Mth.sin(t) * 0.15F;
        shard2.zRot = -Mth.sin(t) * 0.15F;
        shard3.xRot = Mth.sin(t + Mth.PI) * 0.15F;
        shard4.xRot = -Mth.sin(t + Mth.PI) * 0.15F;
        core.y = 15.0F + Mth.sin(t * 0.5F) * 0.5F;
    }
}
