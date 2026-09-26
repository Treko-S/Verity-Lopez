package com.lopez.client.voice;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lopez.LopezMod;
import com.lopez.config.LopezConfig;
import com.lopez.network.LopezPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class SpeechToTextClient {
    private static final String GROQ_WHISPER_URL = "https://api.groq.com/openai/v1/audio/transcriptions";

    public static void transcribeAndSend(byte[] wavData) {
        if (wavData == null || wavData.length == 0) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.sendSystemMessage(Component.literal("§7[🎙️ López escuchando lo que dijiste...]"));
        }

        CompletableFuture.runAsync(() -> {
            try {
                LopezConfig config = LopezConfig.get();
                String apiKey = config.apiKey;
                if (apiKey == null || apiKey.trim().isEmpty()) {
                    if (mc.player != null) {
                        mc.player.sendSystemMessage(Component.literal("§c[López] Para usar el micrófono necesitas tener tu API key de Groq en config/lopez.json"));
                    }
                    return;
                }

                String boundary = "---------------------------" + System.currentTimeMillis();
                String lineEnd = "\r\n";
                String twoHyphens = "--";

                ByteArrayOutputStream body = new ByteArrayOutputStream();

                // 1. Campo: model
                body.write((twoHyphens + boundary + lineEnd).getBytes(StandardCharsets.UTF_8));
                body.write(("Content-Disposition: form-data; name=\"model\"" + lineEnd + lineEnd).getBytes(StandardCharsets.UTF_8));
                body.write(("whisper-large-v3-turbo" + lineEnd).getBytes(StandardCharsets.UTF_8));

                // 2. Campo: response_format
                body.write((twoHyphens + boundary + lineEnd).getBytes(StandardCharsets.UTF_8));
                body.write(("Content-Disposition: form-data; name=\"response_format\"" + lineEnd + lineEnd).getBytes(StandardCharsets.UTF_8));
                body.write(("json" + lineEnd).getBytes(StandardCharsets.UTF_8));

                // 3. Campo: language
                body.write((twoHyphens + boundary + lineEnd).getBytes(StandardCharsets.UTF_8));
                body.write(("Content-Disposition: form-data; name=\"language\"" + lineEnd + lineEnd).getBytes(StandardCharsets.UTF_8));
                body.write(("es" + lineEnd).getBytes(StandardCharsets.UTF_8));

                // 4. Campo: file
                body.write((twoHyphens + boundary + lineEnd).getBytes(StandardCharsets.UTF_8));
                body.write(("Content-Disposition: form-data; name=\"file\"; filename=\"speech.wav\"" + lineEnd).getBytes(StandardCharsets.UTF_8));
                body.write(("Content-Type: audio/wav" + lineEnd + lineEnd).getBytes(StandardCharsets.UTF_8));
                body.write(wavData);
                body.write(lineEnd.getBytes(StandardCharsets.UTF_8));

                // Fin de multipart
                body.write((twoHyphens + boundary + twoHyphens + lineEnd).getBytes(StandardCharsets.UTF_8));

                byte[] bodyBytes = body.toByteArray();

                HttpURLConnection conn = (HttpURLConnection) new URL(GROQ_WHISPER_URL).openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + apiKey.trim());
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
                conn.setFixedLengthStreamingMode(bodyBytes.length);
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(12000);
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(bodyBytes);
                    os.flush();
                }

                int code = conn.getResponseCode();
                if (code == 200) {
                    try (InputStream is = conn.getInputStream()) {
                        String respJson = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                        JsonObject json = JsonParser.parseString(respJson).getAsJsonObject();
                        if (json.has("text")) {
                            String transcribedText = json.get("text").getAsString().trim();
                            
                            // Filtrar artefactos comunes de Whisper en audio silencioso
                            String lower = transcribedText.toLowerCase();
                            boolean isNoise = transcribedText.length() < 2 
                                    || lower.equals("gracias") 
                                    || lower.equals("gracias.")
                                    || lower.equals("y")
                                    || lower.equals("subtítulos realizados por la comunidad de amara.org");

                            if (!transcribedText.isEmpty() && !isNoise) {
                                mc.execute(() -> {
                                    if (mc.player != null) {
                                        mc.player.sendSystemMessage(Component.literal("§6[🎙️ Tú]: §f\"" + transcribedText + "\""));
                                    }
                                    FriendlyByteBuf buf = PacketByteBufs.create();
                                    buf.writeUtf(transcribedText);
                                    ClientPlayNetworking.send(LopezPackets.PLAYER_SPEECH_PACKET_ID, buf);
                                });
                            } else {
                                mc.execute(() -> {
                                    if (mc.player != null) {
                                        mc.player.sendSystemMessage(Component.literal("§7[López no logró entender claramente el audio, intenta hablar un poco más fuerte]"));
                                    }
                                });
                            }
                        }
                    }
                } else {
                    String errDetail = "";
                    try (InputStream es = conn.getErrorStream()) {
                        if (es != null) {
                            errDetail = new String(es.readAllBytes(), StandardCharsets.UTF_8);
                        }
                    } catch (Exception ignored) {}
                    LopezMod.LOGGER.warn("Whisper STT retornó HTTP " + code + ": " + errDetail);
                    mc.execute(() -> {
                        if (mc.player != null) {
                            mc.player.sendSystemMessage(Component.literal("§c[López] Error al procesar audio (HTTP " + code + ")"));
                        }
                    });
                }
            } catch (Exception e) {
                LopezMod.LOGGER.error("Error al transcribir voz de micrófono: " + e.getMessage());
                mc.execute(() -> {
                    if (mc.player != null) {
                        mc.player.sendSystemMessage(Component.literal("§c[López] Excepción de micrófono: " + e.getMessage()));
                    }
                });
            }
        });
    }
}
