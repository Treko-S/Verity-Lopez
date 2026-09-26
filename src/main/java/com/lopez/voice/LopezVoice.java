package com.lopez.voice;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lopez.LopezMod;
import com.lopez.config.LopezConfig;
import javazoom.jl.player.Player;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LopezVoice {
    private static final ExecutorService AUDIO_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Lopez-Voice-Thread");
        t.setDaemon(true);
        return t;
    });

    private static volatile Player currentPlayer = null;

    public static void speak(String rawText) {
        if (!LopezConfig.get().enableVoice || "none".equalsIgnoreCase(LopezConfig.get().ttsEngine)) {
            return;
        }

        // Limpiar comandos de acción o etiquetas para no leerlos en voz alta
        String cleanText = rawText.replaceAll("\\[ACTION:[^\\]]+\\]", "").trim();
        if (cleanText.isEmpty()) return;

        AUDIO_EXECUTOR.submit(() -> {
            try {
                stopCurrent();

                String voice = LopezConfig.get().voiceStyle != null ? LopezConfig.get().voiceStyle.toLowerCase() : "enrique";

                boolean played = false;

                // 1. Probar Loquendo GTA SA (Enrique / Jorge / Miguel / Conchita / Lupe) si no es "google"
                if (!voice.equals("google")) {
                    String speaker = "Enrique"; // Jorge Loquendo por defecto
                    if (voice.contains("jorge") || voice.contains("enrique")) speaker = "Enrique";
                    else if (voice.contains("miguel")) speaker = "Miguel";
                    else if (voice.contains("conchita")) speaker = "Conchita";
                    else if (voice.contains("lupe")) speaker = "Lupe";

                    try {
                        String postData = "msg=" + URLEncoder.encode(cleanText, StandardCharsets.UTF_8)
                                + "&lang=" + speaker + "&source=ttsmp3";

                        HttpURLConnection conn = (HttpURLConnection) new URL("https://ttsmp3.com/makemp3_new.php").openConnection();
                        conn.setRequestMethod("POST");
                        conn.setDoOutput(true);
                        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
                        conn.setConnectTimeout(4000);
                        conn.setReadTimeout(7000);

                        try (OutputStream os = conn.getOutputStream()) {
                            os.write(postData.getBytes(StandardCharsets.UTF_8));
                        }

                        if (conn.getResponseCode() == 200) {
                            try (InputStream in = conn.getInputStream()) {
                                String respStr = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                                JsonObject json = JsonParser.parseString(respStr).getAsJsonObject();
                                if (json.has("URL")) {
                                    String mp3Url = json.get("URL").getAsString();
                                    HttpURLConnection audioConn = (HttpURLConnection) new URL(mp3Url).openConnection();
                                    audioConn.setRequestProperty("User-Agent", "Mozilla/5.0");
                                    audioConn.setConnectTimeout(4000);
                                    audioConn.setReadTimeout(8000);

                                    if (audioConn.getResponseCode() == 200) {
                                        try (InputStream audioIn = new BufferedInputStream(audioConn.getInputStream())) {
                                            Player player = new Player(audioIn);
                                            currentPlayer = player;
                                            player.play();
                                            played = true;
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        LopezMod.LOGGER.warn("Loquendo server ocupado, usando fallback de Google: " + e.getMessage());
                    }
                }

                // 2. Fallback a Google Translate TTS (es-ES) si no se pudo con Loquendo o si eligió google
                if (!played) {
                    String encodedText = URLEncoder.encode(cleanText, StandardCharsets.UTF_8);
                    String ttsUrl = "https://translate.google.com/translate_tts?ie=UTF-8&tl=es-ES&client=tw-ob&q=" + encodedText;

                    HttpURLConnection connection = (HttpURLConnection) new URL(ttsUrl).openConnection();
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
                    connection.setConnectTimeout(4000);
                    connection.setReadTimeout(8000);

                    if (connection.getResponseCode() == 200) {
                        try (InputStream in = new BufferedInputStream(connection.getInputStream())) {
                            Player player = new Player(in);
                            currentPlayer = player;
                            player.play();
                        }
                    }
                }
            } catch (Exception e) {
                LopezMod.LOGGER.error("Error al reproducir voz de Lopez: " + e.getMessage());
            } finally {
                currentPlayer = null;
            }
        });
    }

    public static void stopCurrent() {
        if (currentPlayer != null) {
            try {
                currentPlayer.close();
            } catch (Exception ignored) {
            }
            currentPlayer = null;
        }
    }
}
