package com.lopez.client.gui;

import com.lopez.client.voice.MicrophoneRecorder;
import com.lopez.config.LopezConfig;
import com.lopez.network.LopezPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

import java.util.List;

public class LopezMenuScreen extends Screen {
    private final int lopezEntityId;
    private int micIndex = 0;
    private static final String[] VOICES = {"enrique", "miguel", "conchita", "lupe", "google"};
    private int voiceIndex = 0;

    public LopezMenuScreen(int lopezEntityId) {
        super(Component.literal("Panel de Control de López"));
        this.lopezEntityId = lopezEntityId;

        // Buscar indice de voz actual
        String currentVoice = LopezConfig.get().voiceStyle;
        for (int i = 0; i < VOICES.length; i++) {
            if (VOICES[i].equalsIgnoreCase(currentVoice)) {
                voiceIndex = i;
                break;
            }
        }
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = 35;
        int gap = 24;

        // 1. Botón: Duplicar a López
        this.addRenderableWidget(Button.builder(Component.literal("👥 Duplicar a este López"), b -> {
            sendAction("duplicate");
            this.onClose();
        }).bounds(centerX - 120, startY, 240, 20).build());

        // 2. Botón: Seleccionar Micrófono
        List<String> mics = MicrophoneRecorder.getAvailableMicrophones();
        String currentMic = LopezConfig.get().selectedMicrophone;
        for (int i = 0; i < mics.size(); i++) {
            if (mics.get(i).equalsIgnoreCase(currentMic)) {
                micIndex = i;
                break;
            }
        }
        String micLabel = "🎙 Mic: " + truncate(mics.get(micIndex), 22);
        Button micButton = Button.builder(Component.literal(micLabel), b -> {
            micIndex = (micIndex + 1) % mics.size();
            String chosen = mics.get(micIndex);
            LopezConfig.get().selectedMicrophone = chosen;
            LopezConfig.save();
            b.setMessage(Component.literal("🎙 Mic: " + truncate(chosen, 22)));
        }).bounds(centerX - 120, startY + gap, 240, 20).build();
        this.addRenderableWidget(micButton);

        // 3. Botón: Cambiar Estilo de Voz (Loquendo GTA SA / otros)
        String voiceLabel = "🗣 Voz: " + formatVoiceName(VOICES[voiceIndex]);
        Button voiceButton = Button.builder(Component.literal(voiceLabel), b -> {
            voiceIndex = (voiceIndex + 1) % VOICES.length;
            String newVoice = VOICES[voiceIndex];
            LopezConfig.get().voiceStyle = newVoice;
            LopezConfig.save();
            b.setMessage(Component.literal("🗣 Voz: " + formatVoiceName(newVoice)));
        }).bounds(centerX - 120, startY + gap * 2, 240, 20).build();
        this.addRenderableWidget(voiceButton);

        // 4. Botón: Libre Albedrío
        boolean freeWill = LopezConfig.get().enableAutonomousPranks;
        String freeWillLabel = "🎲 Libre Albedrío: " + (freeWill ? "§aActivado" : "§cDesactivado");
        Button freeWillButton = Button.builder(Component.literal(freeWillLabel), b -> {
            sendAction("toggle_freewill");
            boolean now = !LopezConfig.get().enableAutonomousPranks;
            b.setMessage(Component.literal("🎲 Libre Albedrío: " + (now ? "§aActivado" : "§cDesactivado")));
        }).bounds(centerX - 120, startY + gap * 3, 240, 20).build();
        this.addRenderableWidget(freeWillButton);

        // 5. Botón: Modo Seguir / Quedarse
        this.addRenderableWidget(Button.builder(Component.literal("⚔ Alternar Seguir / Quedarse"), b -> {
            sendAction("toggle_sit");
            this.onClose();
        }).bounds(centerX - 120, startY + gap * 4, 240, 20).build());

        // 6. Botón: Nivel de Maldad
        int lvl = LopezConfig.get().trollLevel;
        Button trollButton = Button.builder(Component.literal("💣 Nivel de Maldad: §c" + lvl + "/10"), b -> {
            sendAction("cycle_troll");
            int n = (LopezConfig.get().trollLevel % 10) + 1;
            b.setMessage(Component.literal("💣 Nivel de Maldad: §c" + n + "/10"));
        }).bounds(centerX - 120, startY + gap * 5, 240, 20).build();
        this.addRenderableWidget(trollButton);

        // 7. Botón: Cambiar Traje / Skin (Rojo, Ámbar, Azul, Stealth)
        this.addRenderableWidget(Button.builder(Component.literal("🎭 Cambiar Traje (Skin de Jugador)"), b -> {
            sendAction("cycle_skin");
        }).bounds(centerX - 120, startY + gap * 6, 240, 20).build());

        // 8. Botón: Cerrar
        this.addRenderableWidget(Button.builder(Component.literal("Cerrar"), b -> {
            this.onClose();
        }).bounds(centerX - 60, startY + gap * 7 + 4, 120, 20).build());
    }

    private void sendAction(String action) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeUtf(action);
        buf.writeInt(lopezEntityId);
        ClientPlayNetworking.send(LopezPackets.MENU_ACTION_PACKET_ID, buf);
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() > max ? text.substring(0, max - 3) + "..." : text;
    }

    private String formatVoiceName(String v) {
        if ("enrique".equalsIgnoreCase(v)) return "Jorge (Loquendo GTA SA)";
        if ("miguel".equalsIgnoreCase(v)) return "Miguel (Latino)";
        if ("conchita".equalsIgnoreCase(v)) return "Conchita (España)";
        if ("lupe".equalsIgnoreCase(v)) return "Lupe (Latina)";
        return "Google (Robot)";
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        this.renderBackground(guiGraphics);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 16, 0xFFFFAA00);
        super.render(guiGraphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
