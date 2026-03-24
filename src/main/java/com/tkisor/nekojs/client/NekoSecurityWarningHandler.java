package com.tkisor.nekojs.client;

import com.tkisor.nekojs.NekoJS;
import com.tkisor.nekojs.core.fs.NekoJSPaths;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = "nekojs", value = Dist.CLIENT)
public class NekoSecurityWarningHandler {

    private static boolean titleWarningShown = false;
    private static boolean chatWarningShown = false;

    private static boolean checkedHost = false;
    private static boolean needsWarningThisSession = false;

    private static void checkHostOnce() {
        if (!checkedHost) {
            checkedHost = true;
            if (NekoJSPaths.disableStrictSandbox) {
                if (!NekoHostIdentifier.isHostAcknowledged()) {
                    needsWarningThisSession = true;
                    NekoHostIdentifier.saveHostCode();
                }
            }
        }
    }

    @SubscribeEvent
    public static void onMainMenuInit(ScreenEvent.Init.Post event) {
        checkHostOnce();

        if (needsWarningThisSession && !titleWarningShown && event.getScreen() instanceof TitleScreen) {
            SystemToast.addOrUpdate(
                    Minecraft.getInstance().getToastManager(),
                    SystemToast.SystemToastId.PACK_LOAD_FAILURE,
                    Component.literal("NekoJS 危险警告").withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                    Component.literal("严格类访问沙盒已被禁用，请注意风险！")
            );
            NekoJS.LOGGER.warn("[NekoJS Security] 严格安全沙盒已禁用，已向玩家发送主界面警告。");
            titleWarningShown = true;
        }
    }

    @SubscribeEvent
    public static void onClientJoin(ClientPlayerNetworkEvent.LoggingIn event) {
        checkHostOnce();

        if (needsWarningThisSession && !chatWarningShown) {
            HoverEvent hoverEvent = new HoverEvent.ShowText(getDetailedWarningText());

            MutableComponent details = Component.literal("\n[鼠标悬停查看解禁的底层权限列表]")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.UNDERLINE)
                    .withStyle(style -> style.withHoverEvent(hoverEvent));

            MutableComponent warningMsg = Component.literal("\n[NekoJS 引擎安全提示]\n")
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                    .append(Component.literal("当前的 NekoJS 环境已 ").withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal("关闭严格的 Java 类访问沙盒").withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
                    .append(Component.literal("。\n这通常是因为整合包作者需要使用底层 API 进行高级控制，但这也意味着脚本获得了极高的系统级权限。请确保您信任该整合包的来源。").withStyle(ChatFormatting.YELLOW))
                    .append(details)
                    .append(Component.literal("\n"));

            event.getPlayer().sendSystemMessage(warningMsg);
            chatWarningShown = true;
        }
    }

    private static Component getDetailedWarningText() {
        return Component.literal("已解禁的高危类库群组：\n\n")
                .append(Component.literal("■ 多线程与系统进程 (java.lang.Thread / Process)\n").withStyle(ChatFormatting.RED))
                .append(Component.literal("允许脚本在后台创建无管制的常驻线程，甚至执行系统级控制台命令（如启动外部程序）。\n\n").withStyle(ChatFormatting.WHITE))

                .append(Component.literal("■ 本地文件系统 IO (java.io / java.nio)\n").withStyle(ChatFormatting.RED))
                .append(Component.literal("允许脚本越过沙盒，直接读写、修改甚至删除您电脑上的任意本地文件。\n\n").withStyle(ChatFormatting.WHITE))

                .append(Component.literal("■ 反射与代码注入 (java.lang.reflect / MethodHandles)\n").withStyle(ChatFormatting.RED))
                .append(Component.literal("允许脚本通过反射强行越权访问和修改私有数据，可绕过所有安全限制。\n\n").withStyle(ChatFormatting.WHITE))

                .append(Component.literal("■ 游戏底层内核 (net.neoforged.fml / org.objectweb.asm)\n").withStyle(ChatFormatting.RED))
                .append(Component.literal("允许脚本直接操控 Mod 加载器内核或修改字节码，操作不当会导致严重的内存泄漏或崩溃。").withStyle(ChatFormatting.WHITE));
    }
}