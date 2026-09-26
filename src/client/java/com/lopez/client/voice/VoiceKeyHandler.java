package com.lopez.client.voice;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

public class VoiceKeyHandler {
    public static KeyMapping TALK_KEY;
    private static boolean warnedError = false;

    public static void register() {
        TALK_KEY = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.lopez.talk",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_V, // Tecla V por defecto
                "category.lopez"
        ));

        // Detección de pulsación y liberación de la tecla
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            if (TALK_KEY.isDown()) {
                if (!MicrophoneRecorder.isRecording()) {
                    boolean started = MicrophoneRecorder.start();
                    if (started) {
                        client.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.5F, 1.4F);
                        warnedError = false;
                    } else if (!warnedError) {
                        warnedError = true;
                        String err = MicrophoneRecorder.getLastError();
                        client.player.sendSystemMessage(Component.literal("§c[López] " + (err != null ? err : "No se pudo acceder al micrófono.")));
                    }
                }
            } else {
                warnedError = false;
                if (MicrophoneRecorder.isRecording()) {
                    client.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.5F, 1.0F);
                    byte[] wav = MicrophoneRecorder.stop();
                    if (wav != null) {
                        SpeechToTextClient.transcribeAndSend(wav);
                    } else {
                        String err = MicrophoneRecorder.getLastError();
                        if (err != null) {
                            client.player.sendSystemMessage(Component.literal("§7[López] " + err));
                        }
                    }
                }
            }
        });

        // Indicador visual en pantalla cuando el micrófono está grabando
        HudRenderCallback.EVENT.register((guiGraphics, tickDelta) -> {
            if (MicrophoneRecorder.isRecording()) {
                Minecraft mc = Minecraft.getInstance();
                int screenWidth = mc.getWindow().getGuiScaledWidth();
                String devName = MicrophoneRecorder.getActiveDeviceName();
                String text = "🎙 [GRABANDO CON: " + devName + "] Suelta V para hablar";
                int textWidth = mc.font.width(text);
                int x = (screenWidth - textWidth) / 2;
                int y = 20;

                guiGraphics.fill(x - 8, y - 4, x + textWidth + 8, y + 14, 0x99000000);
                guiGraphics.drawString(mc.font, text, x, y, 0xFFFF3333, false);
            }
        });
    }
}
