package com.lopez.client.renderer;

import com.lopez.LopezMod;
import com.lopez.entity.LopezEntity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class LopezEntityRenderer extends MobRenderer<LopezEntity, LopezEntityModel> {
    public static final ModelLayerLocation LOPEZ_LAYER = new ModelLayerLocation(
            new ResourceLocation(LopezMod.MOD_ID, "lopez"), "main"
    );
    private static final ResourceLocation TEXTURE = new ResourceLocation(LopezMod.MOD_ID, "textures/entity/lopez.png");

    public LopezEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new LopezEntityModel(context.bakeLayer(LOPEZ_LAYER)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(LopezEntity entity) {
        return TEXTURE;
    }
}
