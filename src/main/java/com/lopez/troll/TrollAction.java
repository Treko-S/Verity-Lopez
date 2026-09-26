package com.lopez.troll;

public enum TrollAction {
    // Órdenes y Acciones Reales
    FOLLOW("follow", "Seguir al dueño"),
    STAY("stay", "Quedarse quieto y esperar"),
    ATTACK("attack", "Atacar al enemigo objetivo"),
    LIGHT("light", "Iluminar el área o dar visión nocturna"),
    REAL_TNT("real_tnt", "Colocar dinamita TNT real activada a punto de explotar"),
    GIVE("give", "Entregar cualquier ítem del juego al jugador"),
    SPAWN("spawn", "Spawnear cualquier mob real en el mundo"),
    KILL_MOBS("kill_mobs", "Fulminar a todos los monstruos hostiles cercanos"),
    HEAL("heal", "Curar y saciar al jugador"),
    TELEPORT("teleport", "Teletransportar a López junto al jugador"),

    // Barbaridades y Troleos cómicos
    FAKE_CREEPER("fake_creeper", "Siseo de Creeper falso detrás del jugador"),
    LIGHTNING("lightning", "Caída de rayo cómico cerca"),
    SWAP_ITEM("swap_item", "Intercambio de ítem de la mano"),
    LEVITATE("levitate", "Vuelo temporal con levitación"),
    DISCO("disco", "Modo disco con partículas de notas"),
    TNT_SCARE("tnt_scare", "Sonido de TNT activada falso"),
    SPAWN_GOOFY("spawn_goofy", "Aparición de un mob gracioso"),
    PUSH("push", "Empujoncito sorpresa"),
    GIFT("gift", "Entrega de regalo absurdo");

    private final String id;
    private final String description;

    TrollAction(String id, String description) {
        this.id = id;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public static TrollAction fromString(String text) {
        if (text == null) return null;
        String clean = text.trim().toLowerCase();
        for (TrollAction action : values()) {
            if (action.id.equalsIgnoreCase(clean) || action.name().equalsIgnoreCase(clean)) {
                return action;
            }
        }
        return null;
    }
}
