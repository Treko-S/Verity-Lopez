package com.lopez.entity;

import com.lopez.ai.LopezBrain;
import com.lopez.config.LopezConfig;
import com.lopez.network.LopezPackets;
import com.lopez.troll.TrollAction;
import com.lopez.troll.TrollManager;
import com.lopez.voice.LopezVoice;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class LopezEntity extends TamableAnimal {
    private static final EntityDataAccessor<Integer> DATA_VARIANT_ID = SynchedEntityData.defineId(LopezEntity.class, EntityDataSerializers.INT);

    private int prankCooldownTicks = 20 * 120; // 2 minutos
    private final String race = "LOPEZ";
    private static final Random RANDOM = new Random();

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_VARIANT_ID, 0);
    }

    public int getVariant() {
        return this.entityData.get(DATA_VARIANT_ID);
    }

    public void setVariant(int variant) {
        this.entityData.set(DATA_VARIANT_ID, variant);
    }

    public String getVariantName() {
        return switch (this.getVariant()) {
            case 1 -> "§6Ámbar Fuego Dorado";
            case 2 -> "§9Azul Cobalto & Zafiro";
            case 3 -> "§8Stealth Carbón & Grafito";
            default -> "§cRojo Clásico";
        };
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("LopezVariant", this.getVariant());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("LopezVariant")) {
            this.setVariant(tag.getInt("LopezVariant"));
        }
    }

    private static final String[] FREE_WILL_COMMENTS = {
            "Che compa, ¿cuánto falta para el asado?",
            "Mirá a ese bicho de allá, qué cara de gil que tiene.",
            "Menos mal que estoy yo para cuidarte, si no ya te hubieras muerto tres veces.",
            "Lindo día para ver el mundo arder con una dinamita.",
            "Tengo alta lija compa, conviden una empanada o algo.",
            "Si me llego a morir no se olviden de agarrar mi núcleo compa.",
            "¿Vieron eso? Juraría que Herobrine me acaba de mandar un mensaje.",
            "Seguí picando nomás capo, que el diamante no sale solo."
    };

    public LopezEntity(EntityType<? extends TamableAnimal> entityType, Level level) {
        super(entityType, level);
        if (this.getCustomName() == null) {
            this.setCustomName(Component.literal("§6§lLopez"));
        }
        this.setCustomNameVisible(true);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 80.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.ATTACK_DAMAGE, 10.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.3D, true));
        this.goalSelector.addGoal(3, new FollowOwnerGoal(this, 1.25D, 8.0F, 2.5F, false));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean success = super.doHurtTarget(target);
        if (success) {
            this.playSound(SoundEvents.PLAYER_ATTACK_CRIT, 1.0F, 1.0F);
            if (this.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(), 8, 0.2, 0.2, 0.2, 0.1);
            }
        }
        return success;
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        return null;
    }

    public String getRace() {
        return race;
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (!this.level().isClientSide) {
            // Libre Albedrío periódico al azar (actúa como un jugador autónomo)
            if (LopezConfig.get().enableAutonomousPranks && !this.isOrderedToSit()) {
                if (prankCooldownTicks > 0) {
                    prankCooldownTicks--;
                } else {
                    executeFreeWill();
                    int interval = Math.max(30, LopezConfig.get().autoPrankIntervalSeconds);
                    prankCooldownTicks = 20 * (interval + RANDOM.nextInt(60));
                }
            }
        }
    }

    private void executeFreeWill() {
        List<ServerPlayer> nearbyPlayers = this.level().getEntitiesOfClass(
                ServerPlayer.class,
                this.getBoundingBox().inflate(20.0D)
        );

        if (nearbyPlayers.isEmpty()) return;
        ServerPlayer target = nearbyPlayers.get(RANDOM.nextInt(nearbyPlayers.size()));

        int dice = RANDOM.nextInt(100);

        if (dice < 40) {
            // 40%: Troleo o broma aleatoria
            TrollAction prank = TrollManager.getRandomPrank();
            TrollManager.executePrank(target, prank);
            String message = "¡Qué hacés " + target.getName().getString() + "! Disfrutá de mi magia compa XD";
            target.server.getPlayerList().broadcastSystemMessage(Component.literal("§6[§e" + this.getName().getString() + "§6]§r §f" + message), false);
            if (LopezConfig.get().enableVoice) {
                LopezVoice.speak(message);
            }
        } else if (dice < 80) {
            // 40%: Comentario aleatorio con voz de Loquendo
            String comment = FREE_WILL_COMMENTS[RANDOM.nextInt(FREE_WILL_COMMENTS.length)];
            target.server.getPlayerList().broadcastSystemMessage(Component.literal("§6[§e" + this.getName().getString() + "§6]§r §f" + comment), false);
            if (LopezConfig.get().enableVoice) {
                LopezVoice.speak(comment);
            }
        } else {
            // 20%: Regalo o ayuda espontánea
            ItemStack food = new ItemStack(Items.COOKED_BEEF, 3);
            this.spawnAtLocation(food);
            String giftMsg = "Les dejo un asadito acá compas, no pasen hambre.";
            target.server.getPlayerList().broadcastSystemMessage(Component.literal("§6[§e" + this.getName().getString() + "§6]§r §a" + giftMsg), false);
            if (LopezConfig.get().enableVoice) {
                LopezVoice.speak(giftMsg);
            }
        }
    }

    public void duplicate(ServerPlayer owner) {
        if (owner == null) return;
        ServerLevel level = owner.serverLevel();
        LopezEntity clone = LopezEntities.LOPEZ.create(level);
        if (clone != null) {
            clone.moveTo(this.getX() + 1.2, this.getY(), this.getZ() + 1.2, this.getYRot(), 0);
            clone.tame(owner);
            int newVariant = this.random.nextInt(3) + 1; // 1 = warm, 2 = blue, 3 = dark
            clone.setVariant(newVariant);
            clone.setCustomName(Component.literal("§6§lLópez Clon (" + clone.getVariantName() + "§6)"));
            level.addFreshEntity(clone);
            level.sendParticles(ParticleTypes.EXPLOSION, clone.getX(), clone.getY() + 1, clone.getZ(), 8, 0.4, 0.4, 0.4, 0.05);
            owner.playNotifySound(SoundEvents.ZOMBIE_VILLAGER_CONVERTED, SoundSource.PLAYERS, 1.0F, 1.2F);
            String msg = "¡Mitosis completada! Ahora somos dos López para dominar el servidor compa.";
            owner.sendSystemMessage(Component.literal("§6[López] §a" + msg));
            if (LopezConfig.get().enableVoice) {
                LopezVoice.speak(msg);
            }
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND) {
            ItemStack held = player.getItemInHand(hand);

            // 1. Asignar dueño si aún no tiene
            if (!this.isTame() && !this.level().isClientSide) {
                this.tame(player);
                String welcomeMsg = "¡Haupei " + player.getName().getString() + "! Ahora soy tu asistente López de pura cepa. ¡Decime qué querés que haga compa!";
                player.sendSystemMessage(Component.literal("§6[§e" + this.getName().getString() + "§6]§r §a" + welcomeMsg));
                LopezVoice.speak(welcomeMsg);
                this.level().addParticle(ParticleTypes.HEART, this.getX(), this.getY() + 1.8, this.getZ(), 0, 0.2, 0);
                this.playSound(SoundEvents.PLAYER_LEVELUP, 1.0F, 1.0F);
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }

            // 2. Si el dueño se agacha (Shift + Click derecho), alterna entre Sentarse (Quedarse) y Seguir
            if (this.isOwnedBy(player) && player.isShiftKeyDown()) {
                if (!this.level().isClientSide) {
                    boolean sitting = !this.isOrderedToSit();
                    this.setOrderedToSit(sitting);
                    this.jumping = false;
                    this.navigation.stop();
                    this.setTarget(null);

                    String stateMsg = sitting ? "Listo kp, me quedo acá plantado esperando." : "¡De una compa, te sigo los pasos!";
                    player.sendSystemMessage(Component.literal("§6[§e" + this.getName().getString() + "§6]§r §e" + stateMsg));
                    LopezVoice.speak(stateMsg);
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }

            // 3. Renombrar con NameTag (mantiene raza LOPEZ)
            if (held.is(Items.NAME_TAG) && held.hasCustomHoverName()) {
                if (!this.level().isClientSide) {
                    String newName = held.getHoverName().getString();
                    this.setCustomName(Component.literal("§6§l" + newName));
                    held.shrink(1);
                    String renameMsg = "¡Epa! Ahora me llamo " + newName + ", pero en el fondo sigo siendo un LOPEZ de pura cepa kp.";
                    player.sendSystemMessage(Component.literal("§6[§e" + newName + "§6]§r §e" + renameMsg));
                    LopezVoice.speak(renameMsg);
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }

            // 4. Regalos especiales al asistente
            if (held.is(Items.DIAMOND) || held.is(Items.EMERALD)) {
                if (!this.level().isClientSide) {
                    held.shrink(1);
                    this.playSound(SoundEvents.PLAYER_LEVELUP, 1.0F, 1.2F);
                    this.level().addParticle(ParticleTypes.TOTEM_OF_UNDYING, this.getX(), this.getY() + 1.2, this.getZ(), 0, 0.4, 0);
                    String diaMsg = "¡UFFF gracias por la joya compa! Ahora sí que ando motivado al 100%.";
                    player.sendSystemMessage(Component.literal("§6[§e" + this.getName().getString() + "§6]§r §b" + diaMsg));
                    LopezVoice.speak(diaMsg);
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }

            if (held.is(Items.TNT)) {
                if (!this.level().isClientSide) {
                    held.shrink(1);
                    this.playSound(SoundEvents.TNT_PRIMED, 1.0F, 1.0F);
                    String tntMsg = "¿TNT para mí? Jajaja no tenés idea del peligro que acabás de desatar compa.";
                    player.sendSystemMessage(Component.literal("§6[§e" + this.getName().getString() + "§6]§r §c" + tntMsg));
                    LopezVoice.speak(tntMsg);
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }

            if (held.is(Items.TOTEM_OF_UNDYING)) {
                if (!this.level().isClientSide) {
                    held.shrink(1);
                    this.playSound(SoundEvents.TOTEM_USE, 0.8F, 1.0F);
                    String totemMsg = "¡Un tótem kp! Ahora soy prácticamente inmortal, que tiemblen los monstruos.";
                    player.sendSystemMessage(Component.literal("§6[§e" + this.getName().getString() + "§6]§r §e" + totemMsg));
                    LopezVoice.speak(totemMsg);
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }

            // 5. Alimentar y curar
            if (!held.isEmpty() && held.isEdible()) {
                if (!this.level().isClientSide) {
                    held.shrink(1);
                    this.heal(15.0F);
                    this.playSound(SoundEvents.PLAYER_BURP, 1.0F, 1.0F);
                    this.level().addParticle(ParticleTypes.HEART, this.getX(), this.getY() + 1.8, this.getZ(), 0, 0.2, 0);

                    String thanksMsg = "¡Uuuh gracias por el bajón " + player.getName().getString() + "! Ahora tengo más aguante para ayudarte.";
                    player.sendSystemMessage(Component.literal("§6[§e" + this.getName().getString() + "§6]§r §f" + thanksMsg));
                    LopezVoice.speak(thanksMsg);
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            } else if (held.isEmpty()) {
                // 6. Click derecho con mano vacía: Abre el panel interactivo de López para su dueño
                if (!this.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
                    if (this.isOwnedBy(player)) {
                        this.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 1.0F, 1.5F);
                        LopezPackets.sendOpenMenu(serverPlayer, this.getId());
                    } else {
                        this.playSound(SoundEvents.VILLAGER_YES, 1.0F, 1.0F);
                        LopezBrain.handlePlayerMessage(serverPlayer, "hola lopez");
                    }
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
        }
        return super.mobInteract(player, hand);
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean hitByPlayer) {
        super.dropCustomDeathLoot(source, looting, hitByPlayer);

        ItemStack core = new ItemStack(Items.ECHO_SHARD);
        String name = this.getName().getString();
        core.setHoverName(Component.literal("§6§lNúcleo de " + name));

        CompoundTag displayTag = core.getOrCreateTagElement("display");
        ListTag lore = new ListTag();
        lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal("§7Raza: §e" + race))));
        lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal("§7Dueño: §f" + (this.getOwner() != null ? this.getOwner().getName().getString() : "Desconocido")))));
        lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal("§dGuárdalo bien: Podrás revivir a este López en la mesa especial."))));
        displayTag.put("Lore", lore);

        this.spawnAtLocation(core);

        String deathMsg = "§c¡Oh no! " + name + " ha caído en batalla... ¡Recuperá su Núcleo para revivirlo!";
        if (this.getServer() != null) {
            this.getServer().getPlayerList().broadcastSystemMessage(Component.literal(deathMsg), false);
            if (LopezConfig.get().enableVoice) {
                LopezVoice.speak("He muerto compa... no pierdas mi núcleo kp...");
            }
        }
    }

    public static LopezEntity getLopezForPlayer(ServerPlayer player) {
        if (player == null) return null;
        ServerLevel level = player.serverLevel();
        List<LopezEntity> list = level.getEntitiesOfClass(
                LopezEntity.class,
                player.getBoundingBox().inflate(64.0D),
                e -> e.isAlive() && player.getUUID().equals(e.getOwnerUUID())
        );
        return list.isEmpty() ? null : list.get(0);
    }

    public static LivingEntity getLookingTarget(ServerPlayer player, double maxDist) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getViewVector(1.0F);
        Vec3 endPos = eyePos.add(lookVec.scale(maxDist));
        AABB aabb = player.getBoundingBox().expandTowards(lookVec.scale(maxDist)).inflate(2.0D);

        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                player.serverLevel(), player, eyePos, endPos, aabb,
                e -> e instanceof LivingEntity && !e.isSpectator() && e.isPickable() && !(e instanceof LopezEntity)
        );
        if (hit != null && hit.getEntity() instanceof LivingEntity living) {
            return living;
        }
        return null;
    }

    public static LivingEntity getNearestMonster(ServerPlayer player, double radius) {
        List<Monster> list = player.serverLevel().getEntitiesOfClass(
                Monster.class, player.getBoundingBox().inflate(radius),
                m -> m.isAlive() && !m.isSpectator()
        );
        if (list.isEmpty()) return null;
        list.sort(Comparator.comparingDouble(player::distanceToSqr));
        return list.get(0);
    }
}
