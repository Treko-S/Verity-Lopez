package com.lopez.client.renderer;

import com.lopez.entity.LopezEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;

public class LopezEntityModel extends PlayerModel<LopezEntity> {
    public LopezEntityModel(ModelPart root) {
        super(root, false);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = PlayerModel.createMesh(new CubeDeformation(0.0F), false);
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(LopezEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        this.crouching = entity.isOrderedToSit();
    }
}
