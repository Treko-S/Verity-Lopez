package com.lopez;

import com.lopez.ai.LopezBrain;
import com.lopez.command.LopezCommand;
import com.lopez.config.LopezConfig;
import com.lopez.entity.LopezEntities;
import com.lopez.entity.LopezEntity;
import com.lopez.network.LopezPackets;
import com.lopez.voice.LopezVoice;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class LopezMod implements ModInitializer {
    public static final String MOD_ID = "lopez";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Inicializando Lopez - El Compa AI & Troll para Minecraft 1.20.1...");

        // 1. Cargar configuración
        LopezConfig.load();

        // 2. Registrar entidades e items
        LopezEntities.register();

        // 3. Registrar canales de red
        LopezPackets.registerServer();

        // 4. Registrar comandos (/lopez ...)
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            LopezCommand.register(dispatcher);
        });

        // 5. Auto-spawn de López al aparecer el creador / jugador en el mundo
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            if (!LopezConfig.get().autoSpawnOnJoin) return;

            server.execute(() -> {
                boolean exists = false;
                for (ServerLevel level : server.getAllLevels()) {
                    List<LopezEntity> list = level.getEntitiesOfClass(LopezEntity.class, player.getBoundingBox().inflate(256.0D), e -> {
                        return e.isAlive() && player.getUUID().equals(e.getOwnerUUID());
                    });
                    if (!list.isEmpty()) {
                        exists = true;
                        break;
                    }
                }

                if (!exists) {
                    ServerLevel world = player.serverLevel();
                    LopezEntity lopez = LopezEntities.LOPEZ.create(world);
                    if (lopez != null) {
                        lopez.moveTo(player.getX() + 1.2, player.getY(), player.getZ() + 1.2, player.getYRot(), 0);
                        lopez.tame(player);
                        world.addFreshEntity(lopez);

                        String welcome = "¡Haupei " + player.getName().getString() + "! Ya llegué compa. Soy tu asistente López de pura cepa. Decime qué hacemos o a quién reventamos hoy.";
                        player.sendSystemMessage(Component.literal("§6[§eLopez§6]§r §a" + welcome));
                        if (LopezConfig.get().enableVoice) {
                            LopezVoice.speak(welcome);
                        }
                    }
                }
            });
        });

        // 6. Escuchar chat para que López responda de forma natural y ejecute órdenes
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            String content = message.decoratedContent().getString().trim();
            String lower = content.toLowerCase();

            boolean mentioned = lower.contains("lopez") || lower.contains("lópez") || lower.contains("kp");

            // Si el jugador tiene a su López cerca (a menos de 24 bloques), lo escucha hablar
            boolean nearLopez = false;
            LopezEntity myLopez = LopezEntity.getLopezForPlayer(sender);
            if (myLopez != null && myLopez.distanceToSqr(sender) < 576.0D) {
                nearLopez = true;
            }

            if (mentioned || nearLopez || LopezConfig.get().respondToAnyChat) {
                LopezBrain.handlePlayerMessage(sender, content);
            }
        });

        // 7. Limpieza al apagar el servidor
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            LopezVoice.stopCurrent();
        });

        LOGGER.info("Lopez Mod iniciado correctamente. ¡Listo para las barbaridades!");
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
