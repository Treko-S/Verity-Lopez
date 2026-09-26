# 🕶️ López - El Compa IA & Troll (Minecraft 1.20.1 Fabric)

> **Inspirado en el concepto de *Verity Mod*, pero rediseñado desde cero: en vez de terror o un monstruo que te asusta, ¡es un compañero leal, ultra inteligente, sarcástico y jodón con voz Loquendo (GTA San Andreas) que hace barbaridades y te asiste con IA real en el juego!**

---

## ✨ Características Principales

### 🎙️ 1. Micrófono Push-to-Talk en Vivo (Tecla `V`)
* **Detección inteligente de hardware**: Escanea y filtra automáticamente los micrófonos de Windows (Realtek, USB, auriculares).
* **Transcripción ultra rápida**: Integra la API de **Groq Whisper** (`whisper-large-v3-turbo`) para convertir tu voz en texto en menos de medio segundo.
* **HUD en pantalla**: Muestra el dispositivo activo y el estado de grabación en tiempo real.

### 🔊 2. Voz Loquendo GTA SA (Jorge)
* Motor de Text-to-Speech integrado mediante **JLayer MP3**, sin necesidad de tarjetas de crédito ni suscripciones de pago.
* Estilo de voz icónico de los creepypastas de GTA San Andreas (*"Jorge"* / *"Enrique"*), además de voces alternativas (Diego, Conchita, Google TTS).

### ⚙️ 3. Menú Interactivo por Clic Derecho
Haz clic derecho sobre López con la mano vacía para abrir su panel de configuración:
* **👥 Duplicar a López**: Clona la entidad conservando la raza `LOPEZ` y el nombre `Lopez`.
* **🎤 Selector de Micrófonos**: Alterna entre todos los dispositivos de captura detectados en tu PC.
* **🔊 Selector de Voces Loquendo**: Elige entre Jorge, Diego, Conchita o Google.
* **🧠 Libre Albedrío**: Activa o desactiva su autonomía para hablar o hacer bromas por su cuenta.
* **🏃 Modo de Acompañamiento**: Alterna entre *Seguir al Dueño* o *Quedarse Quieto*.
* **😈 Nivel de Maldad (1 al 10)**: Ajusta la frecuencia e intensidad de los troleos.

### 💥 4. Acciones Reales y Barbaridades en el Mundo
López puede ejecutar comandos e interactuar físicamente con el servidor:
* `[ACTION:real_tnt]`: Invoca una dinamita encendida con mecha activa.
* `[ACTION:give:item:cantidad]`: Te entrega cualquier ítem solicitado (ej. `give:diamond:64`, `give:netherite_sword:1`).
* `[ACTION:spawn:mob:cantidad]`: Spawnea cualquier mob real (creepper, warden, golem, etc.).
* `[ACTION:kill_mobs]`: Fulmina a los monstruos hostiles cercanos si te encuentras en peligro.
* `[ACTION:heal]`: Cura tus corazones y llena tu barra de hambre.
* `[ACTION:lightning]`: Rayo cómico real.
* `[ACTION:levitate]`: Hace volar al jugador por los aires.
* `[ACTION:fake_creeper]`: Siseo falso de creeper a la nuca.

### 💎 5. Interacciones con Ítems
* **Diamante o Esmeralda**: Se emociona, lanza partículas doradas y agradece con entusiasmo.
* **TNT**: Se ríe de forma malvada advirtiendo el peligro inminente.
* **Tótem de Inmortalidad**: Festeja que ahora es inmortal.
* **Comida**: Come, se cura y eructa.
* **Etiqueta (NameTag)**: Permite renombrarlo manteniendo su raza `LOPEZ`.

---

## 🏗️ Arquitectura Técnica y Colaboración con IA

Para detalles técnicos sobre la estructura interna, paquetes, protocolo de red y sugerencias de optimización para **Gemini Pro**, consulta:
📄 **[ARCHITECTURE.md](ARCHITECTURE.md)**

---

## 🎮 Comandos en el Juego

| Comando | Descripción |
| :--- | :--- |
| `/lopez spawn` | Invoca a López a tu lado inmediatamente. |
| `/lopez talk <mensaje>` | Le habla a López por chat de texto si no deseas usar micrófono. |
| `/lopez prank <jugador> [accion]` | Ejecuta un troleo inmediato en un amigo. |
| `/lopez troll_level <1-10>` | Ajusta el nivel de maldad autónoma. |
| `/lopez reload` | Recarga la configuración de `config/lopez.json`. |

---

## 📦 Instalación

1. Instala **Minecraft Java 1.20.1** con **Fabric Loader** y **Fabric API**.
2. Copia el archivo compilado `build/libs/lopez-1.0.0.jar` a tu carpeta `.minecraft/mods` o perfil de CurseForge.
3. Al ingresar al mundo por primera vez, López aparecerá automáticamente junto al creador del mundo.
4. Tu API key de Groq se configura de manera segura en `.minecraft/config/lopez.json`:

```json
{
  "aiProvider": "groq",
  "apiUrl": "https://api.groq.com/openai/v1/chat/completions",
  "apiKey": "TU_API_KEY_DE_GROQ",
  "model": "qwen/qwen3.8-27b",
  "enableVoice": true,
  "ttsEngine": "loquendo",
  "voiceStyle": "enrique"
}
```

---

## 🛠️ Compilación desde el Código Fuente

```bash
# En Windows (PowerShell / CMD)
.\gradlew.bat build

# En Linux / macOS
./gradlew build
```
El archivo JAR generado se encontrará en `build/libs/lopez-1.0.0.jar`.
