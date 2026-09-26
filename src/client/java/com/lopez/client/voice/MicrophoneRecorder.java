package com.lopez.client.voice;

import com.lopez.LopezMod;
import com.lopez.config.LopezConfig;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public class MicrophoneRecorder {
    private static TargetDataLine line = null;
    private static ByteArrayOutputStream audioStream = null;
    private static Thread recordThread = null;
    private static volatile boolean recording = false;
    private static String activeDeviceName = "Desconocido";
    private static float activeSampleRate = 16000.0f;
    private static String lastError = null;

    public static List<String> getAvailableMicrophones() {
        List<String> list = new ArrayList<>();
        list.add("Automático (Mejor detectado)");
        Mixer.Info[] mixers = AudioSystem.getMixerInfo();
        for (Mixer.Info info : mixers) {
            String name = info.getName();
            if (name.startsWith("Port") || name.startsWith("Puerto")) continue;

            Mixer mixer = AudioSystem.getMixer(info);
            Line.Info[] targetLines = mixer.getTargetLineInfo();
            if (targetLines.length > 0) {
                String lower = name.toLowerCase();
                if (lower.contains("mic") || lower.contains("captura") || lower.contains("input") || lower.contains("headset") || lower.contains("audio") || lower.contains("controlador")) {
                    if (!list.contains(name)) {
                        list.add(name);
                    }
                }
            }
        }
        return list;
    }

    public static synchronized boolean start() {
        if (recording) return true;
        lastError = null;

        try {
            Mixer.Info[] mixers = AudioSystem.getMixerInfo();
            TargetDataLine targetLine = null;
            String chosenName = null;
            float chosenRate = 16000.0f;
            float[] sampleRates = {16000.0f, 44100.0f, 48000.0f};

            String preferred = LopezConfig.get().selectedMicrophone;
            boolean hasPreference = preferred != null && !preferred.equalsIgnoreCase("default") && !preferred.startsWith("Automático");

            // 1. Si el usuario seleccionó un micrófono específico en el menú
            if (hasPreference) {
                for (Mixer.Info info : mixers) {
                    if (info.getName().startsWith("Port") || info.getName().startsWith("Puerto")) continue;
                    if (info.getName().equalsIgnoreCase(preferred) || info.getName().toLowerCase().contains(preferred.toLowerCase())) {
                        Mixer mixer = AudioSystem.getMixer(info);
                        for (float rate : sampleRates) {
                            AudioFormat format = new AudioFormat(rate, 16, 1, true, false);
                            DataLine.Info dInfo = new DataLine.Info(TargetDataLine.class, format);
                            if (mixer.isLineSupported(dInfo)) {
                                try {
                                    targetLine = (TargetDataLine) mixer.getLine(dInfo);
                                    targetLine.open(format);
                                    chosenName = info.getName();
                                    chosenRate = rate;
                                    break;
                                } catch (Exception ignored) {
                                    targetLine = null;
                                }
                            }
                        }
                    }
                    if (targetLine != null && targetLine.isOpen()) break;
                }
            }

            // 2. Si no hay preferencia o falló, buscar automáticamente el mejor micrófono de captura
            if (targetLine == null || !targetLine.isOpen()) {
                for (Mixer.Info info : mixers) {
                    if (info.getName().startsWith("Port") || info.getName().startsWith("Puerto")) continue;
                    String nameLower = info.getName().toLowerCase();
                    if (nameLower.contains("mic") || nameLower.contains("captura") || nameLower.contains("input") || nameLower.contains("headset")) {
                        Mixer mixer = AudioSystem.getMixer(info);
                        for (float rate : sampleRates) {
                            AudioFormat format = new AudioFormat(rate, 16, 1, true, false);
                            DataLine.Info dInfo = new DataLine.Info(TargetDataLine.class, format);
                            if (mixer.isLineSupported(dInfo)) {
                                try {
                                    targetLine = (TargetDataLine) mixer.getLine(dInfo);
                                    targetLine.open(format);
                                    chosenName = info.getName();
                                    chosenRate = rate;
                                    break;
                                } catch (Exception ignored) {
                                    targetLine = null;
                                }
                            }
                        }
                        if (targetLine != null && targetLine.isOpen()) break;
                    }
                }
            }

            // 3. Fallback a cualquier mixer que admita TargetDataLine
            if (targetLine == null || !targetLine.isOpen()) {
                for (Mixer.Info info : mixers) {
                    if (info.getName().startsWith("Port") || info.getName().startsWith("Puerto")) continue;
                    Mixer mixer = AudioSystem.getMixer(info);
                    for (float rate : sampleRates) {
                        AudioFormat format = new AudioFormat(rate, 16, 1, true, false);
                        DataLine.Info dInfo = new DataLine.Info(TargetDataLine.class, format);
                        if (mixer.isLineSupported(dInfo)) {
                            try {
                                targetLine = (TargetDataLine) mixer.getLine(dInfo);
                                targetLine.open(format);
                                chosenName = info.getName();
                                chosenRate = rate;
                                break;
                            } catch (Exception ignored) {
                                targetLine = null;
                            }
                        }
                    }
                    if (targetLine != null && targetLine.isOpen()) break;
                }
            }

            if (targetLine == null || !targetLine.isOpen()) {
                lastError = "No se detectó ningún micrófono activo.";
                LopezMod.LOGGER.error(lastError);
                return false;
            }

            line = targetLine;
            activeDeviceName = chosenName;
            activeSampleRate = chosenRate;
            line.start();

            audioStream = new ByteArrayOutputStream();
            recording = true;

            final TargetDataLine currentLine = line;
            recordThread = new Thread(() -> {
                byte[] buffer = new byte[2048];
                while (recording && currentLine != null && currentLine.isOpen()) {
                    int bytesRead = currentLine.read(buffer, 0, buffer.length);
                    if (bytesRead > 0) {
                        audioStream.write(buffer, 0, bytesRead);
                    }
                }
            }, "Lopez-Mic-Recorder");
            recordThread.setDaemon(true);
            recordThread.start();

            LopezMod.LOGGER.info("Grabando audio con: " + activeDeviceName + " a " + activeSampleRate + " Hz");
            return true;
        } catch (Exception e) {
            lastError = "Error al abrir micrófono: " + e.getMessage();
            LopezMod.LOGGER.error(lastError);
            recording = false;
            return false;
        }
    }

    public static synchronized byte[] stop() {
        if (!recording) return null;
        recording = false;

        if (line != null) {
            try {
                line.stop();
                line.close();
            } catch (Exception ignored) {
            }
            line = null;
        }

        if (recordThread != null) {
            try {
                recordThread.join(500);
            } catch (InterruptedException ignored) {
            }
            recordThread = null;
        }

        if (audioStream == null) return null;
        byte[] pcmData = audioStream.toByteArray();
        audioStream = null;

        // Comprobar duración mínima (al menos 0.2 segundos)
        int minBytes = (int) (activeSampleRate * 2 * 0.20f);
        if (pcmData.length < minBytes) {
            return null;
        }

        // Comprobar que no sea puro silencio digital
        int maxAmp = 0;
        for (int i = 0; i < pcmData.length - 1; i += 2) {
            short sample = (short) ((pcmData[i] & 0xFF) | (pcmData[i + 1] << 8));
            int abs = Math.abs(sample);
            if (abs > maxAmp) maxAmp = abs;
        }
        if (maxAmp < 150) {
            lastError = "No se detectó sonido suficiente en el micrófono.";
            return null;
        }

        return createWavFile(pcmData, (int) activeSampleRate);
    }

    public static boolean isRecording() {
        return recording;
    }

    public static String getActiveDeviceName() {
        return activeDeviceName;
    }

    public static String getLastError() {
        return lastError;
    }

    private static byte[] createWavFile(byte[] pcmData, int sampleRate) {
        int totalAudioLen = pcmData.length;
        int totalDataLen = totalAudioLen + 36;
        int channels = 1;
        int byteRate = sampleRate * channels * 2;

        byte[] header = new byte[44];

        header[0] = 'R'; header[1] = 'I'; header[2] = 'F'; header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W'; header[9] = 'A'; header[10] = 'V'; header[11] = 'E';

        header[12] = 'f'; header[13] = 'm'; header[14] = 't'; header[15] = ' ';
        header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0;
        header[20] = 1; header[21] = 0;
        header[22] = (byte) channels; header[23] = 0;
        header[24] = (byte) (sampleRate & 0xff);
        header[25] = (byte) ((sampleRate >> 8) & 0xff);
        header[26] = (byte) ((sampleRate >> 16) & 0xff);
        header[27] = (byte) ((sampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (channels * 2); header[33] = 0;
        header[34] = 16; header[35] = 0;

        header[36] = 'd'; header[37] = 'a'; header[38] = 't'; header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

        byte[] wavBytes = new byte[44 + pcmData.length];
        System.arraycopy(header, 0, wavBytes, 0, 44);
        System.arraycopy(pcmData, 0, wavBytes, 44, pcmData.length);
        return wavBytes;
    }
}
