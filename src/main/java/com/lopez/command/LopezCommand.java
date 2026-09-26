package com.lopez.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.lopez.ai.LopezBrain;
import com.lopez.config.LopezConfig;
import com.lopez.entity.LopezEntities;
import com.lopez.entity.LopezEntity;
import com.lopez.troll.TrollAction;
import com.lopez.troll.TrollManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class LopezCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("lopez")
                .then(Commands.literal("spawn")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            LopezEntity lopez = LopezEntities.LOPEZ.create(player.serverLevel());
                            if (lopez != null) {
                                lopez.moveTo(player.getX() + 1, player.getY(), player.getZ() + 1, player.getYRot(), 0);
                                lopez.tame(player);
                                player.serverLevel().addFreshEntity(lopez);
                                ctx.getSource().sendSuccess(() -> Component.literal("§a¡López ha sido invocado y vinculado como tu asistente!"), false);
                            }
                            return 1;
                        }))
                .then(Commands.literal("talk")
                        .then(Commands.argument("mensaje", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    String mensaje = StringArgumentType.getString(ctx, "mensaje");
                                    LopezBrain.handlePlayerMessage(player, mensaje);
                                    return 1;
                                })))
                .then(Commands.literal("prank")
                        .then(Commands.argument("jugador", EntityArgument.player())
                                .executes(ctx -> {
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "jugador");
                                    TrollAction action = TrollManager.getRandomPrank();
                                    TrollManager.executePrank(target, action);
                                    ctx.getSource().sendSuccess(() -> Component.literal("§eTroleo '" + action.getId() + "' ejecutado en " + target.getName().getString()), true);
                                    return 1;
                                })
                                .then(Commands.argument("accion", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            for (TrollAction a : TrollAction.values()) {
                                                builder.suggest(a.getId());
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> {
                                            ServerPlayer target = EntityArgument.getPlayer(ctx, "jugador");
                                            String accionStr = StringArgumentType.getString(ctx, "accion");
                                            TrollAction action = TrollAction.fromString(accionStr);
                                            if (action == null) {
                                                ctx.getSource().sendFailure(Component.literal("§cAcción de troleo no válida."));
                                                return 0;
                                            }
                                            TrollManager.executePrank(target, action);
                                            ctx.getSource().sendSuccess(() -> Component.literal("§eTroleo '" + action.getId() + "' ejecutado en " + target.getName().getString()), true);
                                            return 1;
                                        }))))
                .then(Commands.literal("reload")
                        .executes(ctx -> {
                            LopezConfig.load();
                            ctx.getSource().sendSuccess(() -> Component.literal("§aConfiguración de López recargada desde disk."), true);
                            return 1;
                        }))
                .then(Commands.literal("troll_level")
                        .then(Commands.argument("nivel", IntegerArgumentType.integer(1, 10))
                                .executes(ctx -> {
                                    int nivel = IntegerArgumentType.getInteger(ctx, "nivel");
                                    LopezConfig.get().trollLevel = nivel;
                                    LopezConfig.save();
                                    ctx.getSource().sendSuccess(() -> Component.literal("§eNivel de troleo establecido en " + nivel + "/10"), true);
                                    return 1;
                                })))
                .then(Commands.literal("voice")
                        .then(Commands.argument("estilo", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    builder.suggest("enrique");
                                    builder.suggest("jorge");
                                    builder.suggest("miguel");
                                    builder.suggest("conchita");
                                    builder.suggest("lupe");
                                    builder.suggest("google");
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    String estilo = StringArgumentType.getString(ctx, "estilo");
                                    LopezConfig.get().voiceStyle = estilo.toLowerCase();
                                    LopezConfig.save();
                                    ctx.getSource().sendSuccess(() -> Component.literal("§aEstilo de voz cambiado a: §e" + estilo), true);
                                    return 1;
                                })))
        );
    }
}
