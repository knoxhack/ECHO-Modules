package com.knoxhack.echoashfallprotocol.client.renderer;

import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echoashfallprotocol.guardian.BiomeGuardianProfile;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/**
 * Compact guardian boss model used by biome bosses.
 * It keeps the HumanoidModel part contract while avoiding the oversized Warden silhouette.
 */
public class GuardianBossModel<S extends HumanoidRenderState> extends HumanoidModel<S> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "guardian_boss"), "main");

    private final ModelPart antennae;
    private final ModelPart commandShoulders;
    private final ModelPart stalkerSpines;
    private final ModelPart juggernautPlating;
    private final ModelPart hiveSacs;
    private final ModelPart wreckagePlates;
    private final ModelPart reactorCore;
    private final ModelPart cryoTanks;
    private final ModelPart nexusSpines;

    public GuardianBossModel(ModelPart root) {
        super(root);
        ModelPart head = root.getChild("head");
        ModelPart body = root.getChild("body");
        this.antennae = head.getChild("antennae");
        this.commandShoulders = body.getChild("command_shoulders");
        this.stalkerSpines = body.getChild("stalker_spines");
        this.juggernautPlating = body.getChild("juggernaut_plating");
        this.hiveSacs = body.getChild("hive_sacs");
        this.wreckagePlates = body.getChild("wreckage_plates");
        this.reactorCore = body.getChild("reactor_core");
        this.cryoTanks = body.getChild("cryo_tanks");
        this.nexusSpines = body.getChild("nexus_spines");
    }

    @Override
    public void setupAnim(S state) {
        super.setupAnim(state);
        BiomeGuardianProfile.VisualVariant variant = BiomeGuardianProfile.VisualVariant.NONE;
        if (state instanceof BiomeBossRenderer.State bossState) {
            variant = bossState.variant;
        }
        applyVariant(variant);
    }

    private void applyVariant(BiomeGuardianProfile.VisualVariant variant) {
        hideAttachments();
        switch (variant) {
            case SENTINEL -> antennae.visible = true;
            case WARLORD -> commandShoulders.visible = true;
            case STALKER -> stalkerSpines.visible = true;
            case JUGGERNAUT -> juggernautPlating.visible = true;
            case MATRIARCH -> hiveSacs.visible = true;
            case COLOSSUS -> wreckagePlates.visible = true;
            case BEHEMOTH -> reactorCore.visible = true;
            case OVERSEER -> cryoTanks.visible = true;
            case NEXUS -> nexusSpines.visible = true;
            case NONE -> {
            }
        }
    }

    private void hideAttachments() {
        antennae.visible = false;
        commandShoulders.visible = false;
        stalkerSpines.visible = false;
        juggernautPlating.visible = false;
        hiveSacs.visible = false;
        wreckagePlates.visible = false;
        reactorCore.visible = false;
        cryoTanks.visible = false;
        nexusSpines.visible = false;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition root = meshDefinition.getRoot();

        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        head.addOrReplaceChild("hat",
                CubeListBuilder.create()
                        .texOffs(32, 0)
                        .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.45F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        head.addOrReplaceChild("antennae",
                CubeListBuilder.create()
                        .texOffs(96, 0)
                        .addBox(-4.5F, -14.0F, -0.5F, 1.0F, 6.0F, 1.0F)
                        .addBox(3.5F, -14.0F, -0.5F, 1.0F, 6.0F, 1.0F)
                        .texOffs(100, 0)
                        .addBox(-5.0F, -15.0F, -1.0F, 2.0F, 1.0F, 2.0F)
                        .addBox(3.0F, -15.0F, -1.0F, 2.0F, 1.0F, 2.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(16, 16)
                        .addBox(-4.5F, 0.0F, -2.5F, 9.0F, 13.0F, 5.0F)
                        .texOffs(64, 16)
                        .addBox(-5.5F, 2.0F, -3.0F, 11.0F, 5.0F, 6.0F, new CubeDeformation(0.15F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        body.addOrReplaceChild("command_shoulders",
                CubeListBuilder.create()
                        .texOffs(64, 0)
                        .addBox(-7.0F, 0.5F, -3.25F, 3.0F, 4.0F, 6.5F, new CubeDeformation(0.2F))
                        .mirror()
                        .addBox(4.0F, 0.5F, -3.25F, 3.0F, 4.0F, 6.5F, new CubeDeformation(0.2F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        body.addOrReplaceChild("stalker_spines",
                CubeListBuilder.create()
                        .texOffs(112, 0)
                        .addBox(-0.5F, -1.0F, 2.7F, 1.0F, 3.0F, 3.0F)
                        .addBox(-0.5F, 3.0F, 2.7F, 1.0F, 3.0F, 3.0F)
                        .addBox(-0.5F, 7.0F, 2.7F, 1.0F, 3.0F, 3.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        body.addOrReplaceChild("juggernaut_plating",
                CubeListBuilder.create()
                        .texOffs(64, 36)
                        .addBox(-6.0F, 1.0F, -3.4F, 12.0F, 8.0F, 1.0F, new CubeDeformation(0.2F))
                        .texOffs(96, 16)
                        .addBox(-6.5F, 7.0F, -3.0F, 3.0F, 5.0F, 6.0F, new CubeDeformation(0.1F))
                        .mirror()
                        .addBox(3.5F, 7.0F, -3.0F, 3.0F, 5.0F, 6.0F, new CubeDeformation(0.1F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        body.addOrReplaceChild("hive_sacs",
                CubeListBuilder.create()
                        .texOffs(96, 32)
                        .addBox(-7.0F, 3.0F, 1.5F, 3.0F, 5.0F, 4.0F, new CubeDeformation(0.35F))
                        .mirror()
                        .addBox(4.0F, 5.0F, 1.5F, 3.0F, 4.0F, 4.0F, new CubeDeformation(0.25F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        body.addOrReplaceChild("wreckage_plates",
                CubeListBuilder.create()
                        .texOffs(96, 48)
                        .addBox(-6.5F, -0.5F, -3.5F, 5.0F, 5.0F, 1.0F, new CubeDeformation(0.1F))
                        .addBox(1.5F, 3.5F, -3.6F, 5.0F, 5.0F, 1.0F, new CubeDeformation(0.1F))
                        .texOffs(112, 48)
                        .addBox(-2.0F, 8.5F, -3.7F, 4.0F, 4.0F, 1.0F, new CubeDeformation(0.1F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        body.addOrReplaceChild("reactor_core",
                CubeListBuilder.create()
                        .texOffs(112, 16)
                        .addBox(-2.5F, 3.0F, -3.8F, 5.0F, 5.0F, 1.0F, new CubeDeformation(0.15F))
                        .texOffs(112, 22)
                        .addBox(-1.5F, 4.0F, -4.2F, 3.0F, 3.0F, 1.0F, new CubeDeformation(0.1F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        body.addOrReplaceChild("cryo_tanks",
                CubeListBuilder.create()
                        .texOffs(96, 0)
                        .addBox(-4.5F, 1.5F, 2.6F, 2.0F, 9.0F, 2.0F, new CubeDeformation(0.15F))
                        .mirror()
                        .addBox(2.5F, 1.5F, 2.6F, 2.0F, 9.0F, 2.0F, new CubeDeformation(0.15F))
                        .texOffs(112, 28)
                        .addBox(-5.5F, 4.0F, 3.5F, 11.0F, 1.0F, 3.0F)
                        .addBox(-5.5F, 8.0F, 3.5F, 11.0F, 1.0F, 3.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        body.addOrReplaceChild("nexus_spines",
                CubeListBuilder.create()
                        .texOffs(112, 32)
                        .addBox(-0.5F, -2.5F, 2.5F, 1.0F, 4.0F, 5.0F)
                        .addBox(-3.5F, 2.0F, 2.7F, 1.0F, 4.0F, 4.0F)
                        .addBox(2.5F, 2.0F, 2.7F, 1.0F, 4.0F, 4.0F)
                        .addBox(-0.5F, 7.0F, 2.8F, 1.0F, 4.0F, 4.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create()
                        .texOffs(40, 16)
                        .addBox(-3.0F, -2.0F, -2.0F, 4.0F, 13.0F, 4.0F)
                        .texOffs(64, 28)
                        .addBox(-3.5F, 7.5F, -2.5F, 5.0F, 4.0F, 5.0F, new CubeDeformation(0.1F)),
                PartPose.offset(-5.5F, 2.0F, 0.0F));

        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create()
                        .texOffs(40, 16)
                        .mirror()
                        .addBox(-1.0F, -2.0F, -2.0F, 4.0F, 13.0F, 4.0F)
                        .texOffs(64, 28)
                        .mirror()
                        .addBox(-1.5F, 7.5F, -2.5F, 5.0F, 4.0F, 5.0F, new CubeDeformation(0.1F)),
                PartPose.offset(5.5F, 2.0F, 0.0F));

        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create()
                        .texOffs(0, 16)
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offset(-2.2F, 12.0F, 0.0F));

        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create()
                        .texOffs(0, 16)
                        .mirror()
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offset(2.2F, 12.0F, 0.0F));

        return LayerDefinition.create(meshDefinition, 128, 64);
    }
}
