package com.tkisor.nekojs.command;

import com.mojang.brigadier.CommandDispatcher;
import com.tkisor.nekojs.NekoJS;
import com.tkisor.nekojs.core.error.NekoErrorTracker;
import com.tkisor.nekojs.core.error.ScriptError;
import com.tkisor.nekojs.network.OpenWorkspacePacket;
import com.tkisor.nekojs.network.ShowErrorListPacket;
import com.tkisor.nekojs.network.ShowErrorScreenPacket;
import com.tkisor.nekojs.network.dto.ErrorSummaryDTO;
import com.tkisor.nekojs.script.ScriptType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public final class NekoJSCommands {

    private NekoJSCommands() {}

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
                Commands.literal("nekojs")
                        .requires(source -> Commands.LEVEL_GAMEMASTERS.check(source.permissions()))

                        .then(Commands.literal("reload")
                                .executes(context -> {
                                    CommandSourceStack source = context.getSource();
                                    source.sendSystemMessage(Component.literal("§e[NekoJS] 正在重载脚本..."));

                                    try {
                                        NekoErrorTracker.clearAll();

                                        NekoJS.SCRIPT_MANAGER.reloadScripts(ScriptType.COMMON);
                                        NekoJS.SCRIPT_MANAGER.reloadScripts(ScriptType.SERVER);

                                        if (NekoErrorTracker.hasErrors()) {
                                            source.sendFailure(Component.literal("§c[NekoJS] ✖ 重载完毕，但存在 " + NekoErrorTracker.getAllErrors().size() + " 个错误："));
                                            // 复用输出逻辑
                                            printErrorsToSource(source);
                                        } else {
                                            source.sendSuccess(() -> Component.literal("§a[NekoJS] ✔ 脚本完美重载！"), true);
                                        }
                                    } catch (Exception e) {
                                        source.sendFailure(Component.literal("§c[NekoJS] ✖ 重载发生致命崩溃！请查看后台日志。"));
                                    }
                                    return 1;
                                })
                        )

                        .then(Commands.literal("error")
                                .executes(context -> {
                                    CommandSourceStack source = context.getSource();

                                    if (NekoErrorTracker.hasErrors()) {
                                        source.sendFailure(Component.literal("§c[NekoJS] ⚠ 当前环境存在 " + NekoErrorTracker.getAllErrors().size() + " 个运行错误："));
                                        // 复用输出逻辑
                                        printErrorsToSource(source);
                                    } else {
                                        source.sendSuccess(() -> Component.literal("§a[NekoJS] ✔ 当前运行环境非常健康，没有任何脚本错误！"), false);
                                    }
                                    return 1;
                                })
                        )
                        .then(Commands.literal("view_all_errors")
                                .executes(context -> {
                                    CommandSourceStack source = context.getSource();
                                    if (NekoErrorTracker.hasErrors()) {
                                        ServerPlayer player = source.getPlayerOrException();

                                        // 将 Tracker 里的数据转换为 DTO
                                        List<ErrorSummaryDTO> dtoList = NekoErrorTracker.getAllErrors().stream()
                                                .map(err -> new ErrorSummaryDTO(
                                                        err.getErrorId().toString(),
                                                        err.getDisplayPath(),
                                                        err.getLineNumber(),
                                                        err.getOccurrenceCount(),
                                                        err.getErrorMessage(),
                                                        err.getFullDetailText()
                                                )).toList();

                                        // 发送列表包
                                        PacketDistributor.sendToPlayer(player, new ShowErrorListPacket(dtoList));
                                    } else {
                                        source.sendSuccess(() -> Component.literal("§a[NekoJS] ✔ 当前没有脚本错误！"), false);
                                    }
                                    return 1;
                                })
                        )
                        .then(Commands.literal("editor")
                                .executes(context -> {
                                    CommandSourceStack source = context.getSource();
                                    ServerPlayer player = source.getPlayerOrException();

                                    // 发送打开工作区的包给客户端
                                    PacketDistributor.sendToPlayer(player, new OpenWorkspacePacket());
                                    return 1;
                                })
                        )
        );
    }

    private static void printErrorsToSource(CommandSourceStack source) {
        source.sendSystemMessage(Component.literal("§a[点击此处打开错误大盘 UI]")
                .withStyle(style -> style
                        .withClickEvent(new ClickEvent.RunCommand("/nekojs view_all_errors"))
                        .withHoverEvent(new HoverEvent.ShowText(Component.literal("在全屏列表中查看所有错误")))
                ));
    }
}