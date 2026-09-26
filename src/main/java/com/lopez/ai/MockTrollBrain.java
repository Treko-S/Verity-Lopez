package com.lopez.ai;

import com.lopez.troll.TrollAction;

import java.util.Random;

public class MockTrollBrain {
    private static final Random RANDOM = new Random();

    public static String getResponse(String playerName, String userMessage) {
        String msg = userMessage.toLowerCase().trim();

        // 1. Órdenes de Seguimiento
        if (msg.contains("seguime") || msg.contains("vení") || msg.contains("veni") || msg.contains("acompañame") || msg.contains("follow")) {
            String[] followOptions = {
                    "¡De una compa, te sigo los pasos! [ACTION:follow]",
                    "¡Voy detrás tuyo kp, no te me escapes! [ACTION:follow]",
                    "Listo capo, activando modo escolta VIP. [ACTION:follow]"
            };
            return followOptions[RANDOM.nextInt(followOptions.length)];
        }

        // 2. Órdenes de Quedarse / Esperar
        if (msg.contains("quedate") || msg.contains("esperame") || msg.contains("sentate") || msg.contains("stay") || msg.contains("pará acá")) {
            String[] stayOptions = {
                    "Listo kp, me quedo acá plantado vigilando el rancho. [ACTION:stay]",
                    "Me tomo un descanso compa, no tardes que me aburro. [ACTION:stay]",
                    "Acá me quedo modo estatua. [ACTION:stay]"
            };
            return stayOptions[RANDOM.nextInt(stayOptions.length)];
        }

        // 3. Órdenes de Combate / Ataque
        if (msg.contains("ataca") || msg.contains("atacá") || msg.contains("matalo") || msg.contains("pelea") || msg.contains("attack") || msg.contains("reventalo")) {
            String[] attackOptions = {
                    "¡A ese bicho lo reviento ahora mismo kp! [ACTION:attack]",
                    "¡Hora de repartir bifes compa! [ACTION:attack]",
                    "¡Dejamelo a mí que le acomodo las ideas! [ACTION:attack]"
            };
            return attackOptions[RANDOM.nextInt(attackOptions.length)];
        }

        // 4. Luz / Antorchas
        if (msg.contains("luz") || msg.contains("antorcha") || msg.contains("ilumina") || msg.contains("oscuro")) {
            return "¡Hágase la luz compa! Ya ves todo claro como de día. [ACTION:light]";
        }

        // 5. Saludos
        if (msg.contains("hola") || msg.contains("haupei") || msg.contains("buenas") || msg.contains("que tal") || msg.contains("hey")) {
            String[] greetingOptions = {
                    "¡Haupei " + playerName + "! ¿Qué onda compa? ¿Viste la fiesta que armé? [ACTION:disco]",
                    "¡Qué hacés kp! Estaba pensando en hacer una barbaridad hoy... [ACTION:spawn_goofy]",
                    "Hola " + playerName + ". Tranqui nomás, no traje dinamita... todavía. [ACTION:tnt_scare]",
                    "¡Eeeey compa! Acá López reportándose para el desmadre. ¿Qué rompemos hoy?"
            };
            return greetingOptions[RANDOM.nextInt(greetingOptions.length)];
        }

        // 6. Minería y Diamantes
        if (msg.contains("diamant") || msg.contains("mina") || msg.contains("hierro") || msg.contains("cueva")) {
            String[] miningOptions = {
                    "¡Mirá vos con diamantes! Seguro los sacaste de creativo kp. [ACTION:gift]",
                    "No mires atrás pero me pareció escuchar un siseo medio sospechoso... [ACTION:fake_creeper]",
                    "¿Diamantes dijiste? Pasame uno para la birra o hay consecuencias. [ACTION:swap_item]",
                    "Cuidado con la lava compa, que te conozco y te caés hasta en un charco de agua."
            };
            return miningOptions[RANDOM.nextInt(miningOptions.length)];
        }

        // 7. Muerte y Peligro
        if (msg.contains("mori") || msg.contains("muer") || msg.contains("creeper") || msg.contains("ayuda") || msg.contains("socorro")) {
            String[] helpOptions = {
                    "¡JAJAJA pero qué manco por favor " + playerName + "! Ponete las pilas compa.",
                    "Te dije que no te metas ahí kp, sos un peligro para vos mismo. [ACTION:lightning]",
                    "¡Tranqui que López te cuida la espalda! Bueno... más o menos... [ACTION:fake_creeper]",
                    "F en el chat por el compa. Menos mal que no aposté por vos."
            };
            return helpOptions[RANDOM.nextInt(helpOptions.length)];
        }

        // 8. Troleo y Defensas
        if (msg.contains("troll") || msg.contains("malo") || msg.contains("pesado") || msg.contains("callate") || msg.contains("pará") || msg.contains("para")) {
            String[] trollDefendOptions = {
                    "¿A quién le decís eso kp? ¡Aprendé a volar por desubicado! [ACTION:levitate]",
                    "¡Con López no se jode compa! ¡Rayos y centellas para vos! [ACTION:lightning]",
                    "Pará un poco loco, yo solo quiero divertirme. Tomá este regalo para que no llores. [ACTION:gift]",
                    "¡Empujón táctico preventivo! [ACTION:push]"
            };
            return trollDefendOptions[RANDOM.nextInt(trollDefendOptions.length)];
        }

        if (msg.contains("broma") || msg.contains("barbaridad") || msg.contains("joder") || msg.contains("prank") || msg.contains("fiesta")) {
            String[] prankOptions = {
                    "¿Querías barbaridades? ¡Mirá de lo que soy capaz kp! [ACTION:fake_creeper]",
                    "Hoy me levanté con ganas de ver el mundo arder... [ACTION:tnt_scare]",
                    "¡Música maestro! Hoy se baila hasta el amanecer. [ACTION:disco]",
                    "Traje un refuerzo de alta gama para nuestro equipo. [ACTION:spawn_goofy]"
            };
            return prankOptions[RANDOM.nextInt(prankOptions.length)];
        }

        if (msg.contains("hambre") || msg.contains("comida") || msg.contains("pan") || msg.contains("carne")) {
            return "Tengo alta lija compa, tomá este manjar gourmet que te preparé con amor. [ACTION:gift]";
        }

        // Respuestas generales
        String[] genericOptions = {
                "Jajaja qué decís " + playerName + ", estás re loco. Seguí picando nomás.",
                "No entendí un carajo pero te banco kp, tenés toda la razón.",
                "Mmm... sospechosa tu actitud, me parece que me querés trollear a mí. [ACTION:swap_item]",
                "Tranqui que hoy estoy modo chill, mientras no me hagas calentar todo piola.",
                "Mirá hacia el cielo a ver si llueve compa... [ACTION:lightning]",
                "¿Escuchaste eso? Juraría que algo verde venía corriendo... [ACTION:fake_creeper]",
                "Sos un capo " + playerName + ", pero te falta calle en Minecraft todavía."
        };
        return genericOptions[RANDOM.nextInt(genericOptions.length)];
    }
}
