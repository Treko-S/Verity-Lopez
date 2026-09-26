package com.lopez.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lopez.LopezMod;
import com.lopez.config.LopezConfig;
import com.lopez.network.LopezPackets;
import com.lopez.troll.TrollManager;
import com.lopez.voice.LopezVoice;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LopezBrain {
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(4))
            .build();

    private static final Pattern ACTION_PATTERN = Pattern.compile("\\[ACTION:([a-zA-Z0-9_:]+)\\]");

    public static void handlePlayerMessage(ServerPlayer player, String message) {
        if (player == null || message == null || message.trim().isEmpty()) return;

        MinecraftServer server = player.getServer();
        if (server == null) return;

        String playerName = player.getName().getString();

        CompletableFuture.supplyAsync(() -> {
            LopezConfig config = LopezConfig.get();

            // Si está configurado en mock, no hay URL, o no hay apiKey para proveedores que la requieren
            if ("mock".equalsIgnoreCase(config.aiProvider) || config.apiUrl == null || config.apiUrl.isEmpty()
                    || (("groq".equalsIgnoreCase(config.aiProvider) || "openai".equalsIgnoreCase(config.aiProvider)) && (config.apiKey == null || config.apiKey.trim().isEmpty()))) {
                return MockTrollBrain.getResponse(playerName, message);
            }

            try {
                JsonObject requestBody = new JsonObject();
                requestBody.addProperty("model", config.model != null ? config.model : "qwen/qwen3.8-27b");

                JsonArray messages = new JsonArray();

                JsonObject sysMsg = new JsonObject();
                sysMsg.addProperty("role", "system");
                sysMsg.addProperty("content", config.systemPrompt);
                messages.add(sysMsg);

                JsonObject userMsg = new JsonObject();
                userMsg.addProperty("role", "user");
                userMsg.addProperty("content", "[Jugador " + playerName + "]: " + message);
                messages.add(userMsg);

                requestBody.add("messages", messages);
                requestBody.addProperty("temperature", 0.7);
                requestBody.addProperty("max_tokens", 80); // Respuestas cortas, concisas y rápidas

                HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(config.apiUrl))
                        .timeout(Duration.ofSeconds(8))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()));

                if (config.apiKey != null && !config.apiKey.isEmpty()) {
                    reqBuilder.header("Authorization", "Bearer " + config.apiKey);
                }

                HttpResponse<String> response = HTTP_CLIENT.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                    if (json.has("choices") && json.getAsJsonArray("choices").size() > 0) {
                        return json.getAsJsonArray("choices").get(0).getAsJsonObject()
                                .getAsJsonObject("message").get("content").getAsString();
                    }
                } else {
                    LopezMod.LOGGER.warn("API de IA retornó código HTTP " + response.statusCode() + ", usando cerebro local.");
                }
            } catch (Exception e) {
                LopezMod.LOGGER.info("Error al conectar a Groq IA (" + e.getMessage() + "), usando fallback local.");
            }

            return MockTrollBrain.getResponse(playerName, message);
        }).thenAccept(rawResponse -> {
            server.execute(() -> {
                processAndBroadcast(server, player, rawResponse);
            });
        });
    }

    private static void processAndBroadcast(MinecraftServer server, ServerPlayer player, String rawResponse) {
        if (rawResponse == null || rawResponse.trim().isEmpty()) return;

        // Limpiar posibles etiquetas de razonamiento interno
        rawResponse = rawResponse.replaceAll("(?s)<think>.*?</think>", "").trim();

        // Detectar y extraer acciones reales y troleos
        String actionTag = null;
        Matcher matcher = ACTION_PATTERN.matcher(rawResponse);
        if (matcher.find()) {
            actionTag = matcher.group(1);
        }

        // Limpiar el texto para mostrar en el chat y leer por voz
        String cleanMessage = matcher.replaceAll("").replaceAll("\\s+", " ").trim();
        if (cleanMessage.isEmpty()) return;

        String displayName = LopezConfig.get().entityDisplayName != null ? LopezConfig.get().entityDisplayName : "López";

        // Enviar al chat del juego con colores bonitos
        Component formattedChat = Component.literal("§6[§e" + displayName + "§6]§r §f" + cleanMessage);
        server.getPlayerList().broadcastSystemMessage(formattedChat, false);

        // Ejecutar la acción si fue indicada
        if (actionTag != null) {
            TrollManager.executeAction(player, actionTag);
        }

        // Reproducir la voz con Loquendo TTS
        if (LopezConfig.get().enableVoice) {
            LopezVoice.speak(cleanMessage);
            LopezPackets.broadcastVoice(server, cleanMessage, player.getX(), player.getY(), player.getZ());
        }
    }
}
