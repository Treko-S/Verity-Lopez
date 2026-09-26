package com.lopez.network;

import com.lopez.LopezMod;
import com.lopez.ai.LopezBrain;
import com.lopez.config.LopezConfig;
import com.lopez.entity.LopezEntity;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public class LopezPackets {
    public static final ResourceLocation VOICE_PACKET_ID = new ResourceLocation(LopezMod.MOD_ID, "voice_packet");
    public static final ResourceLocation PLAYER_SPEECH_PACKET_ID = new ResourceLocation(LopezMod.MOD_ID, "player_speech");
    public static final ResourceLocation OPEN_MENU_PACKET_ID = new ResourceLocation(LopezMod.MOD_ID, "open_menu");
    public static final ResourceLocation MENU_ACTION_PACKET_ID = new ResourceLocation(LopezMod.MOD_ID, "menu_action");

    public static void registerServer() {
        // Receptor de voz transcrita
        ServerPlayNetworking.registerGlobalReceiver(PLAYER_SPEECH_PACKET_ID, (server, player, handler, buf, responseSender) -> {
            String speechText = buf.readUtf();
            server.execute(() -> {
                LopezBrain.handlePlayerMessage(player, speechText);
            });
        });

        // Receptor de acciones del menú GUI de López
        ServerPlayNetworking.registerGlobalReceiver(MENU_ACTION_PACKET_ID, (server, player, handler, buf, responseSender) -> {
            String action = buf.readUtf();
            int entityId = buf.readInt();
            server.execute(() -> {
                Entity e = player.serverLevel().getEntity(entityId);
                if (e instanceof LopezEntity lopez && lopez.isOwnedBy(player)) {
                    if ("duplicate".equals(action)) {
                        lopez.duplicate(player);
                    } else if ("toggle_freewill".equals(action)) {
                        LopezConfig.get().enableAutonomousPranks = !LopezConfig.get().enableAutonomousPranks;
                        LopezConfig.save();
                        String status = LopezConfig.get().enableAutonomousPranks ? "§aActivado" : "§cDesactivado";
                        player.sendSystemMessage(Component.literal("§6[López] §eLibre albedrío: " + status));
                    } else if ("toggle_sit".equals(action)) {
                        boolean sit = !lopez.isOrderedToSit();
                        lopez.setOrderedToSit(sit);
                        String msg = sit ? "§eMe quedo vigilando acá." : "§a¡Te sigo los pasos compa!";
                        player.sendSystemMessage(Component.literal("§6[López] " + msg));
                    } else if ("cycle_troll".equals(action)) {
                        int lvl = (LopezConfig.get().trollLevel % 10) + 1;
                        LopezConfig.get().trollLevel = lvl;
                        LopezConfig.save();
                        player.sendSystemMessage(Component.literal("§6[López] §eNivel de maldad: §c" + lvl + "/10"));
                    } else if ("cycle_skin".equals(action)) {
                        int next = (lopez.getVariant() + 1) % 4;
                        lopez.setVariant(next);
                        player.sendSystemMessage(Component.literal("§6[López] §eTraje cambiado a: " + lopez.getVariantName()));
                    }
                }
            });
        });
    }

    public static void sendOpenMenu(ServerPlayer player, int entityId) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeInt(entityId);
        ServerPlayNetworking.send(player, OPEN_MENU_PACKET_ID, buf);
    }

    public static void broadcastVoice(MinecraftServer server, String text, double x, double y, double z) {
        if (server != null && server.isDedicatedServer()) {
            FriendlyByteBuf buf = PacketByteBufs.create();
            buf.writeUtf(text);
            buf.writeDouble(x);
            buf.writeDouble(y);
            buf.writeDouble(z);

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                ServerPlayNetworking.send(player, VOICE_PACKET_ID, buf);
            }
        }
    }
}
