package com.lopez.client;

import com.lopez.client.gui.LopezMenuScreen;
import com.lopez.client.renderer.LopezEntityModel;
import com.lopez.client.renderer.LopezEntityRenderer;
import com.lopez.client.voice.VoiceKeyHandler;
import com.lopez.entity.LopezEntities;
import com.lopez.network.LopezPackets;
import com.lopez.voice.LopezVoice;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

@Environment(EnvType.CLIENT)
public class LopezModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // 1. Registrar capa del modelo
        EntityModelLayerRegistry.registerModelLayer(
                LopezEntityRenderer.LOPEZ_LAYER,
                LopezEntityModel::createBodyLayer
        );

        // 2. Registrar el renderizador de la entidad Lopez
        EntityRendererRegistry.register(
                LopezEntities.LOPEZ,
                LopezEntityRenderer::new
        );

        // 3. Registrar receptor de paquetes de voz para clientes en multijugador
        ClientPlayNetworking.registerGlobalReceiver(LopezPackets.VOICE_PACKET_ID, (client, handler, buf, responseSender) -> {
            String textToSpeak = buf.readUtf();
            buf.readDouble(); // x
            buf.readDouble(); // y
            buf.readDouble(); // z

            client.execute(() -> {
                LopezVoice.speak(textToSpeak);
            });
        });

        // 4. Registrar apertura del menú interactivo de López
        ClientPlayNetworking.registerGlobalReceiver(LopezPackets.OPEN_MENU_PACKET_ID, (client, handler, buf, responseSender) -> {
            int entityId = buf.readInt();
            client.execute(() -> {
                client.setScreen(new LopezMenuScreen(entityId));
            });
        });

        // 5. Registrar sistema de pulsar para hablar con el micrófono (Push-to-Talk)
        VoiceKeyHandler.register();
    }
}
