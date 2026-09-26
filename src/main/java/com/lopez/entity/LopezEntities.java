package com.lopez.entity;

import com.lopez.LopezMod;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;

public class LopezEntities {
    public static final EntityType<LopezEntity> LOPEZ = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            new ResourceLocation(LopezMod.MOD_ID, "lopez"),
            FabricEntityTypeBuilder.create(MobCategory.CREATURE, LopezEntity::new)
                    .dimensions(EntityDimensions.fixed(0.6F, 1.8F))
                    .trackRangeBlocks(64)
                    .build()
    );

    public static final Item LOPEZ_SPAWN_EGG = Registry.register(
            BuiltInRegistries.ITEM,
            new ResourceLocation(LopezMod.MOD_ID, "lopez_spawn_egg"),
            new SpawnEggItem(LOPEZ, 0xE5C158, 0x1B263B, new Item.Properties())
    );

    public static void register() {
        FabricDefaultAttributeRegistry.register(LOPEZ, LopezEntity.createAttributes());

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS).register(content -> {
            content.accept(LOPEZ_SPAWN_EGG);
        });

        LopezMod.LOGGER.info("Entidad e Item de Lopez registrados exitosamente.");
    }
}
