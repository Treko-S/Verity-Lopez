package com.lopez.client.renderer;

import com.lopez.LopezMod;
import com.lopez.entity.LopezEntity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

public class LopezEntityRenderer extends HumanoidMobRenderer<LopezEntity, LopezEntityModel> {
    public static final ModelLayerLocation LOPEZ_LAYER = new ModelLayerLocation(
            new ResourceLocation(LopezMod.MOD_ID, "lopez"), "main"
    );

    private static final ResourceLocation TEXTURE_DEFAULT = new ResourceLocation(LopezMod.MOD_ID, "textures/entity/lopez.png");
    private static final ResourceLocation TEXTURE_WARM = new ResourceLocation(LopezMod.MOD_ID, "textures/entity/lopez_warm.png");
    private static final ResourceLocation TEXTURE_BLUE = new ResourceLocation(LopezMod.MOD_ID, "textures/entity/lopez_blue.png");
    private static final ResourceLocation TEXTURE_DARK = new ResourceLocation(LopezMod.MOD_ID, "textures/entity/lopez_dark.png");

    public LopezEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new LopezEntityModel(context.bakeLayer(LOPEZ_LAYER)), 0.5F);
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(LopezEntity entity) {
        int variant = entity.getVariant();
        return switch (variant) {
            case 1 -> TEXTURE_WARM;
            case 2 -> TEXTURE_BLUE;
            case 3 -> TEXTURE_DARK;
            default -> TEXTURE_DEFAULT;
        };
    }
}
