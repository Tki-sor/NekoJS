package com.tkisor.nekojs.client.gui;

import com.tkisor.nekojs.client.gui.components.NekoMenuBar;
import com.tkisor.nekojs.client.gui.components.NekoContextMenu.MenuItem;
import com.tkisor.nekojs.client.gui.components.NekoTabbedEditor;
import com.tkisor.nekojs.client.gui.components.NekoToast;
import com.tkisor.nekojs.core.fs.NekoJSPaths;
import com.tkisor.nekojs.network.*;
import net.minecraft.util.Util;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class NekoWorkspaceActions {

    // 一键生成所有界面通用的顶栏菜单
    public static NekoMenuBar createSharedMenuBar(Supplier<NekoTabbedEditor> editorSupplier, NekoToast toast, Runnable onRefresh, Runnable onClose) {
        return new NekoMenuBar(List.of(
                new NekoMenuBar.MenuCategory("文件", List.of(
                        new MenuItem("💾 保存当前修改 (Ctrl+S)", () -> saveTab(editorSupplier.get() != null ? editorSupplier.get().getActiveTab() : null, toast)),
                        new MenuItem("📂 在外部资源管理器中打开", NekoWorkspaceActions::openLocalDir),
                        new MenuItem("❌ 退出面板", onClose)
                )),
                new NekoMenuBar.MenuCategory("视图", List.of(
                        new MenuItem("🔄 刷新本地文件缓存", () -> {
                            if (onRefresh != null) onRefresh.run();
                            toast.show("§a✔ 刷新成功");
                        })
                )),
                new NekoMenuBar.MenuCategory("云端同步 (Sync)", List.of(
                        new MenuItem("↑ 将当前脚本推送到服务端", () -> syncUploadCurrent(editorSupplier.get(), toast)),
                        new MenuItem("↓ 从服务端拉取覆盖当前脚本", () -> syncDownloadCurrent(editorSupplier.get(), toast)),
                        new MenuItem("⇈ 强制推送所有本地脚本 (危险)", () -> syncUploadAll(toast)),
                        new MenuItem("⇊ 强制拉取服务端所有脚本 (危险)", () -> syncDownloadAll(toast))
                ))
        ));
    }

    public static void saveTab(NekoTabbedEditor.Tab tab, NekoToast toast) {
        if (tab == null || tab.editor == null) {
            toast.show("§c❌ 请先打开一个文件"); return;
        }
        try {
            Path path = NekoJSPaths.ROOT.resolve(tab.path);
            Files.writeString(path, tab.editor.getValue());
            toast.show("§a✔ 已保存 " + tab.path);
            tab.editor.markSaved();
        } catch (Exception e) {
            toast.show("§c✖ 保存失败: " + e.getMessage());
        }
    }

    public static void syncUploadCurrent(NekoTabbedEditor tabbedEditor, NekoToast toast) {
        if (tabbedEditor == null || tabbedEditor.getActiveTab() == null) {
            toast.show("§c❌ 错误：请先打开一个文件！"); return;
        }
        ClientPacketDistributor.sendToServer(new SaveScriptPacket(tabbedEditor.getActiveTab().path, tabbedEditor.getActiveTab().editor.getValue()));
        toast.show("§e[↑] 正在推送到服务端...");
    }

    public static void syncDownloadCurrent(NekoTabbedEditor tabbedEditor, NekoToast toast) {
        if (tabbedEditor == null || tabbedEditor.getActiveTab() == null) {
            toast.show("§c❌ 错误：请先打开一个文件！"); return;
        }
        ClientPacketDistributor.sendToServer(new FetchScriptRequestPacket(tabbedEditor.getActiveTab().path));
        toast.show("§b[↓] 正在请求拉取代码...");
    }

    public static void syncUploadAll(NekoToast toast) {
        toast.show("§e[↑↑] 正在扫描本地文件并推送...");
        Map<String, String> localFiles = NekoJSNetwork.collectAllValidScripts(NekoJSPaths.ROOT);
        if (localFiles.isEmpty()) {
            toast.show("§c✖ 本地 nekojs 目录为空或不存在 js 文件！"); return;
        }
        ClientPacketDistributor.sendToServer(new UploadAllScriptsPacket(localFiles));
    }

    public static void syncDownloadAll(NekoToast toast) {
        toast.show("§b[↓↓] 正在请求拉取所有服务端代码...");
        ClientPacketDistributor.sendToServer(new FetchAllScriptsRequestPacket());
    }

    public static void openLocalDir() {
        Util.getPlatform().openFile(NekoJSPaths.ROOT.toFile());
    }
}