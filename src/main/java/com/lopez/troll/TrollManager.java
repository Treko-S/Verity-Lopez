package com.lopez.troll;

import com.lopez.LopezMod;
import com.lopez.entity.LopezEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Random;

public class TrollManager {
    private static final Random RANDOM = new Random();

    private static final TrollAction[] COMIC_PRANKS = {
            TrollAction.REAL_TNT,
            TrollAction.FAKE_CREEPER,
            TrollAction.LIGHTNING,
            TrollAction.SWAP_ITEM,
            TrollAction.LEVITATE,
            TrollAction.DISCO,
            TrollAction.SPAWN_GOOFY,
            TrollAction.PUSH,
            TrollAction.GIFT
    };

    public static void executePrank(ServerPlayer player, TrollAction action) {
        if (action != null) {
            executeAction(player, action.getId());
        }
    }

    public static void executeAction(ServerPlayer player, String fullActionStr) {
        if (player == null || fullActionStr == null) return;
        ServerLevel level = player.serverLevel();
        LopezEntity lopez = LopezEntity.getLopezForPlayer(player);

        String actionLower = fullActionStr.toLowerCase().trim();

        // 1. Dar Ítem Real: [ACTION:give:item_name:count] o [ACTION:give:item_name]
        if (actionLower.startsWith("give:")) {
            String[] parts = fullActionStr.split(":");
            if (parts.length >= 2) {
                String itemName = parts[1].trim().toLowerCase();
                int count = 1;
                if (parts.length >= 3) {
                    try {
                        count = Integer.parseInt(parts[2].trim());
                    } catch (NumberFormatException ignored) {
                    }
                }
                count = Math.max(1, Math.min(count, 64));

                ResourceLocation loc = new ResourceLocation(itemName.contains(":") ? itemName : "minecraft:" + itemName);
                Item item = BuiltInRegistries.ITEM.get(loc);
                if (item != null && item != Items.AIR) {
                    ItemStack stack = new ItemStack(item, count);
                    if (!player.getInventory().add(stack)) {
                        player.drop(stack, false);
                    }
                    player.playNotifySound(SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0F, 1.2F);
                    player.sendSystemMessage(Component.literal("§6[López] §aTe di: " + count + "x " + stack.getHoverName().getString()));
                } else {
                    player.sendSystemMessage(Component.literal("§6[López] §cNo encontré ese ítem '" + itemName + "' kp."));
                }
            }
            return;
        }

        // 2. Spawnear Mob Real: [ACTION:spawn:mob_name:count] o [ACTION:spawn:mob_name]
        if (actionLower.startsWith("spawn:")) {
            String[] parts = fullActionStr.split(":");
            if (parts.length >= 2) {
                String mobName = parts[1].trim().toLowerCase();
                int count = 1;
                if (parts.length >= 3) {
                    try {
                        count = Integer.parseInt(parts[2].trim());
                    } catch (NumberFormatException ignored) {
                    }
                }
                count = Math.max(1, Math.min(count, 10));

                ResourceLocation loc = new ResourceLocation(mobName.contains(":") ? mobName : "minecraft:" + mobName);
                if (BuiltInRegistries.ENTITY_TYPE.containsKey(loc)) {
                    EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(loc);
                    for (int i = 0; i < count; i++) {
                        Entity entity = type.create(level);
                        if (entity != null) {
                            double ox = (RANDOM.nextDouble() - 0.5) * 4.0;
                            double oz = (RANDOM.nextDouble() - 0.5) * 4.0;
                            entity.moveTo(player.getX() + ox, player.getY(), player.getZ() + oz, RANDOM.nextFloat() * 360F, 0);
                            level.addFreshEntity(entity);
                        }
                    }
                    player.playNotifySound(SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE, 1.0F, 1.0F);
                    player.sendSystemMessage(Component.literal("§6[López] §e¡Aparecieron " + count + "x " + mobName + " compa!"));
                } else {
                    player.sendSystemMessage(Component.literal("§6[López] §cNo conozco al bicho '" + mobName + "' kp."));
                }
            }
            return;
        }

        // 3. Acciones enumeradas estándar
        TrollAction action = TrollAction.fromString(actionLower);
        if (action == null) return;

        switch (action) {
            case REAL_TNT -> {
                // Dinamita TNT REAL activada
                PrimedTnt tnt = EntityType.TNT.create(level);
                if (tnt != null) {
                    tnt.moveTo(player.getX() + 1.2, player.getY() + 0.5, player.getZ() + 1.2, 0, 0);
                    tnt.setFuse(80); // 4 segundos
                    level.addFreshEntity(tnt);
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.5F, 1.0F);
                    player.sendSystemMessage(Component.literal("§c§l¡CUIDADO! ¡LÓPEZ PUSO UNA TNT REAL ACTIVADA! ¡CORRÉ!"));
                }
            }
            case KILL_MOBS -> {
                // Matar monstruos hostiles cercanos
                List<Monster> monsters = level.getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(24.0D));
                for (Monster m : monsters) {
                    m.hurt(level.damageSources().genericKill(), 1000.0F);
                    level.sendParticles(ParticleTypes.EXPLOSION, m.getX(), m.getY() + 1.0, m.getZ(), 2, 0, 0, 0, 0);
                }
                player.playNotifySound(SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8F, 1.2F);
                player.sendSystemMessage(Component.literal("§6[López] §a¡Zona limpia compa! " + monsters.size() + " monstruos eliminados."));
            }
            case HEAL -> {
                player.setHealth(player.getMaxHealth());
                player.getFoodData().setFoodLevel(20);
                player.removeAllEffects();
                player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.0F);
                player.sendSystemMessage(Component.literal("§6[López] §a¡Curado al 100% compa! Ahora ponele ganas."));
            }
            case TELEPORT -> {
                if (lopez != null) {
                    lopez.moveTo(player.getX() + 1.0, player.getY(), player.getZ() + 1.0, player.getYRot(), 0);
                    level.sendParticles(ParticleTypes.PORTAL, lopez.getX(), lopez.getY() + 1, lopez.getZ(), 20, 0.5, 0.5, 0.5, 0.1);
                    player.playNotifySound(SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 1.0F, 1.0F);
                    player.sendSystemMessage(Component.literal("§6[López] §e¡Acá estoy compa!"));
                }
            }
            case FOLLOW -> {
                if (lopez != null) {
                    lopez.setOrderedToSit(false);
                    lopez.getNavigation().moveTo(player, 1.3D);
                    player.sendSystemMessage(Component.literal("§6[López] §a¡Te sigo los pasos kp!"));
                    player.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0F, 1.2F);
                }
            }
            case STAY -> {
                if (lopez != null) {
                    lopez.setOrderedToSit(true);
                    lopez.getNavigation().stop();
                    lopez.setTarget(null);
                    player.sendSystemMessage(Component.literal("§6[López] §eMe quedo acá vigilando. No tardes."));
                    player.playNotifySound(SoundEvents.ARMOR_EQUIP_GENERIC, SoundSource.PLAYERS, 1.0F, 1.0F);
                }
            }
            case ATTACK -> {
                if (lopez != null) {
                    LivingEntity target = LopezEntity.getLookingTarget(player, 25.0D);
                    if (target == null) {
                        target = LopezEntity.getNearestMonster(player, 16.0D);
                    }
                    if (target != null) {
                        lopez.setOrderedToSit(false);
                        lopez.setTarget(target);
                        player.sendSystemMessage(Component.literal("§6[López] §c¡A ese " + target.getName().getString() + " lo reviento!"));
                        player.playNotifySound(SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0F, 1.0F);
                    } else {
                        player.sendSystemMessage(Component.literal("§6[López] §7No veo a ningún enemigo cerca compa."));
                    }
                }
            }
            case LIGHT -> {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20 * 60, 0, false, false));
                player.playNotifySound(SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.5F);
                player.sendSystemMessage(Component.literal("§6[López] §e¡Hágase la luz compa! Ya ves todo claro."));
            }
            case FAKE_CREEPER -> {
                player.playNotifySound(SoundEvents.CREEPER_PRIMED, SoundSource.HOSTILE, 1.2F, 0.6F);
                level.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 0.5, player.getZ(), 8, 0.2, 0.2, 0.2, 0.02);
            }
            case LIGHTNING -> {
                LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(level);
                if (lightning != null) {
                    double ox = (RANDOM.nextDouble() - 0.5) * 4;
                    double oz = (RANDOM.nextDouble() - 0.5) * 4;
                    lightning.moveTo(player.getX() + ox, player.getY(), player.getZ() + oz);
                    level.addFreshEntity(lightning);
                }
            }
            case SWAP_ITEM -> {
                ItemStack main = player.getMainHandItem().copy();
                ItemStack off = player.getOffhandItem().copy();
                player.setItemInHand(InteractionHand.MAIN_HAND, off);
                player.setItemInHand(InteractionHand.OFF_HAND, main);
                player.playNotifySound(SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0F, 1.5F);
            }
            case LEVITATE -> {
                player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 50, 1, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 120, 0, false, false));
                player.playNotifySound(SoundEvents.BAT_TAKEOFF, SoundSource.PLAYERS, 1.0F, 0.8F);
            }
            case DISCO -> {
                for (int i = 0; i < 15; i++) {
                    double px = player.getX() + (RANDOM.nextDouble() - 0.5) * 2;
                    double py = player.getY() + 1.0 + RANDOM.nextDouble();
                    double pz = player.getZ() + (RANDOM.nextDouble() - 0.5) * 2;
                    level.sendParticles(ParticleTypes.NOTE, px, py, pz, 1, (RANDOM.nextDouble() * 24.0) / 24.0, 0, 0, 1.0);
                }
                player.playNotifySound(SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.RECORDS, 1.0F, 1.2F);
                player.addEffect(new MobEffectInstance(MobEffects.JUMP, 80, 2, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, 1, false, false));
            }
            case SPAWN_GOOFY -> {
                Chicken chicken = EntityType.CHICKEN.create(level);
                if (chicken != null) {
                    Vec3 look = player.getLookAngle();
                    chicken.moveTo(player.getX() + look.x * 1.5, player.getY(), player.getZ() + look.z * 1.5, RANDOM.nextFloat() * 360F, 0);
                    chicken.setCustomName(Component.literal("§ePollo Espía de López"));
                    chicken.setCustomNameVisible(true);
                    level.addFreshEntity(chicken);
                    player.playNotifySound(SoundEvents.CHICKEN_EGG, SoundSource.NEUTRAL, 1.0F, 1.0F);
                }
            }
            case PUSH -> {
                Vec3 look = player.getLookAngle();
                player.setDeltaMovement(look.x * 0.9 + (RANDOM.nextDouble() - 0.5) * 0.4, 0.35, look.z * 0.9 + (RANDOM.nextDouble() - 0.5) * 0.4);
                player.hurtMarked = true;
                player.playNotifySound(SoundEvents.SLIME_ATTACK, SoundSource.PLAYERS, 0.8F, 1.3F);
            }
            case GIFT -> {
                ItemStack gift = new ItemStack(Items.POISONOUS_POTATO);
                gift.setHoverName(Component.literal("§dPapa con Amor de López"));
                if (!player.getInventory().add(gift)) {
                    player.drop(gift, false);
                }
                player.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            default -> {
            }
        }
    }

    public static TrollAction getRandomPrank() {
        return COMIC_PRANKS[RANDOM.nextInt(COMIC_PRANKS.length)];
    }
}
