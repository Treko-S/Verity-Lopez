package com.lopez.client.renderer;

import com.lopez.entity.LopezEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public class LopezEntityModel extends HierarchicalModel<LopezEntity> {
    private final ModelPart root;
    private final ModelPart head;

    public LopezEntityModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition rootPart = mesh.getRoot();

        // Cabeza/Cuerpo esférico cúbico estilo Verity pero con anteojos y corona/gorrito
        PartDefinition head = rootPart.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-7.0F, -14.0F, -7.0F, 14.0F, 14.0F, 14.0F)
                        // Lentes de sol "Deal With It"
                        .texOffs(0, 28)
                        .addBox(-7.5F, -10.0F, -7.5F, 15.0F, 5.0F, 1.0F),
                PartPose.offset(0.0F, 16.0F, 0.0F)
        );

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(LopezEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        // Rotación de la cabeza mirando al jugador
        this.head.yRot = netHeadYaw * ((float) Math.PI / 180F);
        this.head.xRot = headPitch * ((float) Math.PI / 180F);

        // Flotación suave y oscilante
        this.head.y = 16.0F + (float) Math.sin(ageInTicks * 0.12F) * 2.5F;
    }
}
