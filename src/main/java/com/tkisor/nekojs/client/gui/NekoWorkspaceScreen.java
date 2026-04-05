package com.tkisor.nekojs.client.gui;

import com.tkisor.nekojs.client.gui.components.NekoTabbedEditor;
import com.tkisor.nekojs.core.fs.NekoJSPaths;
import com.tkisor.nekojs.network.FetchAllScriptsRequestPacket;
import com.tkisor.nekojs.network.NekoJSNetwork;
import com.tkisor.nekojs.network.SaveScriptPacket;
import com.tkisor.nekojs.network.UploadAllScriptsPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NekoWorkspaceScreen extends Screen {

    private NekoTabbedEditor tabbedEditor;
    private FileListWidget fileListWidget;
    private EditBox searchBox;

    private List<String> allLocalFiles = new ArrayList<>();

    private int leftW;
    private int rightX;
    private String toastMessage = "";
    private long toastTime = 0;

    public NekoWorkspaceScreen() {
        super(Component.literal("NekoJS Workspace"));
    }

    @Override
    protected void init() {
        super.init();
        scanLocalFiles();

        this.leftW = Math.max(160, (int) (this.width * 0.25)); // 左侧占 25%
        this.rightX = leftW + 1; // 分界线

        int topY = 24; // 顶部工具栏高度
        int contentH = this.height - topY;

        // 1. 初始化左侧搜索框
        this.searchBox = new EditBox(this.font, 4, topY + 4, leftW - 8, 14, Component.literal("搜索..."));
        this.searchBox.setHint(Component.literal("§8过滤文件..."));
        this.searchBox.setResponder(s -> refreshFileList());
        this.addRenderableWidget(this.searchBox);

        // 2. 初始化左侧文件列表
        this.fileListWidget = new FileListWidget(this.minecraft, leftW, contentH - 22, topY + 22, 14);
        refreshFileList();
        this.addRenderableWidget(this.fileListWidget);

        // 3. 初始化右侧标签页编辑器
        if (this.tabbedEditor == null) {
            this.tabbedEditor = new NekoTabbedEditor(this.font, rightX, topY, this.width - rightX, contentH,
                    this::doSaveTab,
                    () -> {}, // 没有标签页时不做什么特殊处理
                    () -> {}); // 标签页切换时不做什么特殊处理
        } else {
            this.tabbedEditor.setBounds(rightX, topY, this.width - rightX, contentH);
        }
    }

    private void scanLocalFiles() {
        // 使用之前的通用扫描方法扫描本地 nekojs 目录
        Map<String, String> files = NekoJSNetwork.collectAllValidScripts(NekoJSPaths.ROOT);
        this.allLocalFiles = new ArrayList<>(files.keySet());
        this.allLocalFiles.sort(String::compareTo); // 按字母排序
    }

    private void refreshFileList() {
        if (fileListWidget == null) return;
        fileListWidget.clearEntries();
        String filter = searchBox.getValue().toLowerCase();

        for (String path : allLocalFiles) {
            if (filter.isEmpty() || path.toLowerCase().contains(filter)) {
                fileListWidget.addEntry(new FileEntry(path));
            }
        }
    }

    private void openFileInEditor(String path) {
        try {
            Path p = NekoJSPaths.ROOT.resolve(path);
            String text = Files.exists(p) ? Files.readString(p) : "";
            tabbedEditor.openTab(path, text);
        } catch (Exception e) {
            showToast("§c✖ 无法读取文件: " + e.getMessage());
        }
    }

    private void doSaveTab(NekoTabbedEditor.Tab tab) {
        if (tab == null || tab.editor == null) return;
        try {
            Path path = NekoJSPaths.ROOT.resolve(tab.path);
            Files.writeString(path, tab.editor.getValue());
            showToast("§a✔ 已保存 " + tab.path);
            tab.editor.markSaved();
        } catch (Exception e) { showToast("§c✖ 保存失败: " + e.getMessage()); }
    }

    public void showToast(String msg) {
        this.toastMessage = msg;
        this.toastTime = System.currentTimeMillis() + 2000;
    }

    // 🌟 处理网络回调，这个方法需要在 NekoJSNetwork 的 handleSyncFeedbackOnClient 中被调用
    public void onSyncFeedback(boolean success, String message) {
        String prefix = success ? "§a✔ " : "§c✖ ";
        this.showToast(prefix + message);

        if (success && tabbedEditor != null && tabbedEditor.getActiveTab() != null) {
            tabbedEditor.getActiveTab().editor.markSaved();
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // VSCode 深色主题背景色
        graphics.fillGradient(0, 0, this.width, this.height, 0xFF1E1E1E, 0xFF1E1E1E);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // 1. 绘制顶部工具栏底色
        graphics.fill(0, 0, this.width, 24, 0xFF333333);

        // 2. 绘制标题
        graphics.text(this.font, "§cNEKO§fJS §8Workspace", 10, 8, -1);

        // 3. 绘制顶部菜单操作按钮
        renderTopMenuBar(graphics, mouseX, mouseY);

        // 4. 绘制左右分割线
        graphics.fill(leftW, 24, rightX, this.height, 0xFF252526);

        // 5. 绘制右侧编辑器（如果未打开任何标签，绘制一个占位提示）
        if (tabbedEditor != null && !tabbedEditor.isEmpty()) {
            tabbedEditor.renderUnderlay(graphics, mouseX, mouseY);
        } else {
            graphics.fill(rightX, 24, this.width, this.height, 0xFF1E1E1E);
            graphics.centeredText(this.font, "§7从左侧选择文件以开始编辑", rightX + (this.width - rightX) / 2, this.height / 2, -1);
        }

        super.extractRenderState(graphics, tabbedEditor != null && tabbedEditor.isHoveringDropdown(mouseX, mouseY) ? -999 : mouseX, tabbedEditor != null && tabbedEditor.isHoveringDropdown(mouseX, mouseY) ? -999 : mouseY, partialTick);

        // 6. 渲染全局 Toast 提示
        if (System.currentTimeMillis() < toastTime) {
            float yAnim = Mth.clamp((2000f - (toastTime - System.currentTimeMillis())) / 150f, 0, 1);
            int tw = this.font.width(toastMessage) + 20;
            int ty = this.height - 30 - (int)(15 * yAnim);
            graphics.fill(this.width/2-tw/2, ty, this.width/2+tw/2, ty+16, 0xCC000000);
            graphics.outline(this.width/2-tw/2, ty, tw, 16, 0xFF44FF44);
            graphics.centeredText(this.font, toastMessage, this.width / 2, ty + 4, -1);
        }
    }

    private void renderTopMenuBar(GuiGraphicsExtractor g, int mx, int my) {
        int curX = this.width - 10;

        curX = renderTopButton(g, "§c✖", curX, mx, my, this::onClose);
        curX -= 15; // 分隔间距

        if (tabbedEditor != null && !tabbedEditor.isEmpty() && tabbedEditor.getActiveTab() != null) {
            boolean isDirty = tabbedEditor.getActiveTab().editor.isDirty();
            curX = renderTopButton(g, isDirty ? "§e[保存当前*]" : "§a[保存当前]", curX, mx, my, () -> doSaveTab(tabbedEditor.getActiveTab()));
            curX = renderTopButton(g, "§e[↑ 推送当前]", curX, mx, my, () -> {
                ClientPacketDistributor.sendToServer(new SaveScriptPacket(tabbedEditor.getActiveTab().path, tabbedEditor.getActiveTab().editor.getValue()));
                showToast("§e正在推送当前文件...");
            });
        }

        curX -= 10;
        curX = renderTopButton(g, "§b[↓↓ 拉取所有]", curX, mx, my, () -> {
            ClientPacketDistributor.sendToServer(new FetchAllScriptsRequestPacket());
            showToast("§b请求覆盖拉取服务端所有代码...");
        });
        curX = renderTopButton(g, "§e[↑↑ 推送所有]", curX, mx, my, () -> {
            Map<String, String> localFiles = NekoJSNetwork.collectAllValidScripts(NekoJSPaths.ROOT);
            ClientPacketDistributor.sendToServer(new UploadAllScriptsPacket(localFiles));
            showToast("§e正在打包推送所有本地代码...");
        });

        curX -= 10;
        curX = renderTopButton(g, "§7[打开本地目录]", curX, mx, my, () -> {
            Util.getPlatform().openFile(NekoJSPaths.ROOT.toFile());
        });
        curX = renderTopButton(g, "§7[刷新列表]", curX, mx, my, () -> {
            scanLocalFiles();
            refreshFileList();
            showToast("§a本地文件列表已刷新");
        });
    }

    private int renderTopButton(GuiGraphicsExtractor g, String text, int x, int mx, int my, Runnable action) {
        int w = this.font.width(text);
        int targetX = x - w - 5;
        boolean hov = mx >= targetX && mx <= targetX + w && my >= 6 && my <= 16;
        g.text(this.font, text, targetX, 8, -1);
        if (hov) g.fill(targetX, 8 + this.font.lineHeight, targetX + w, 8 + this.font.lineHeight + 1, 0xFFFFFFFF);
        return targetX;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        // 1. 处理顶部菜单栏点击
        if (event.y() >= 0 && event.y() <= 24) {
            // 我们通过简单的坐标范围判断点击。为了严谨，这里复用了计算逻辑
            int curX = this.width - 10;
            int cw1 = this.font.width("§c✖"); curX = curX - cw1 - 5;
            if (event.x() >= curX && event.x() <= curX + cw1) { this.onClose(); return true; }
            curX -= 15;

            if (tabbedEditor != null && !tabbedEditor.isEmpty() && tabbedEditor.getActiveTab() != null) {
                boolean isDirty = tabbedEditor.getActiveTab().editor.isDirty();
                int cw2 = this.font.width(isDirty ? "§e[保存当前*]" : "§a[保存当前]"); curX = curX - cw2 - 5;
                if (event.x() >= curX && event.x() <= curX + cw2) { doSaveTab(tabbedEditor.getActiveTab()); return true; }

                int cw3 = this.font.width("§e[↑ 推送当前]"); curX = curX - cw3 - 5;
                if (event.x() >= curX && event.x() <= curX + cw3) {
                    ClientPacketDistributor.sendToServer(new SaveScriptPacket(tabbedEditor.getActiveTab().path, tabbedEditor.getActiveTab().editor.getValue()));
                    showToast("§e正在推送当前文件...");
                    return true;
                }
            }

            curX -= 10;
            int cw4 = this.font.width("§b[↓↓ 拉取所有]"); curX = curX - cw4 - 5;
            if (event.x() >= curX && event.x() <= curX + cw4) {
                ClientPacketDistributor.sendToServer(new FetchAllScriptsRequestPacket());
                showToast("§b请求覆盖拉取服务端所有代码...");
                return true;
            }
            int cw5 = this.font.width("§e[↑↑ 推送所有]"); curX = curX - cw5 - 5;
            if (event.x() >= curX && event.x() <= curX + cw5) {
                Map<String, String> localFiles = NekoJSNetwork.collectAllValidScripts(NekoJSPaths.ROOT);
                ClientPacketDistributor.sendToServer(new UploadAllScriptsPacket(localFiles));
                showToast("§e正在打包推送所有本地代码...");
                return true;
            }

            curX -= 10;
            int cw6 = this.font.width("§7[打开本地目录]"); curX = curX - cw6 - 5;
            if (event.x() >= curX && event.x() <= curX + cw6) { Util.getPlatform().openFile(NekoJSPaths.ROOT.toFile()); return true; }

            int cw7 = this.font.width("§7[刷新列表]"); curX = curX - cw7 - 5;
            if (event.x() >= curX && event.x() <= curX + cw7) { scanLocalFiles(); refreshFileList(); showToast("§a本地文件列表已刷新"); return true; }
        }

        // 2. 委托给右侧编辑器
        if (tabbedEditor != null) {
            if (tabbedEditor.mouseClicked(event.x(), event.y(), event.button())) return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (tabbedEditor != null && tabbedEditor.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (tabbedEditor != null && tabbedEditor.keyPressed(event)) return true;
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (tabbedEditor != null && tabbedEditor.charTyped((char) event.codepoint())) return true;
        return super.charTyped(event);
    }

    // ==========================================
    // 左侧文件列表的内部类实现
    // ==========================================
    private class FileListWidget extends ObjectSelectionList<FileEntry> {
        public FileListWidget(Minecraft mc, int w, int h, int y, int ih) { super(mc, w, h, y, ih); }
        @Override public int getRowWidth() { return this.width - 12; }
        @Override protected int scrollBarX() { return this.width - 6; }
        @Override protected void extractListBackground(GuiGraphicsExtractor g) {}
        @Override protected void extractListSeparators(GuiGraphicsExtractor g) {}
        @Override public int addEntry(FileEntry entry) { return super.addEntry(entry); }
    }

    private class FileEntry extends ObjectSelectionList.Entry<FileEntry> {
        private final String path;
        private final String displayName;

        public FileEntry(String path) {
            this.path = path;
            // 获取文件名，不要长长的路径
            int lastSlash = path.lastIndexOf('/');
            this.displayName = lastSlash == -1 ? path : path.substring(lastSlash + 1);
        }

        @Override
        public void extractContent(GuiGraphicsExtractor g, int mx, int my, boolean isHovered, float pt) {
            boolean isActive = (tabbedEditor != null && tabbedEditor.getActiveTab() != null && tabbedEditor.getActiveTab().path.equals(path));

            // 背景高亮
            if (isActive) {
                g.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), 0xFF094771);
            } else if (isHovered) {
                g.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), 0x33FFFFFF);
            }

            // 文件名与相对目录
            int color = isActive ? 0xFFFFFFFF : 0xFFCCCCCC;
            g.text(NekoWorkspaceScreen.this.font, this.displayName, this.getX() + 4, this.getY() + 3, color);

            // 在右侧以非常暗的颜色显示所属目录，方便区分同名文件
            int slashIdx = path.lastIndexOf('/');
            if (slashIdx != -1) {
                String dirName = path.substring(0, slashIdx);
                int dirW = NekoWorkspaceScreen.this.font.width(dirName);
                g.text(NekoWorkspaceScreen.this.font, dirName, this.getX() + this.getWidth() - dirW - 4, this.getY() + 3, 0xFF555555);
            }
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (event.button() == 0) {
                NekoWorkspaceScreen.this.openFileInEditor(this.path);
                return true;
            }
            return false;
        }

        @Override public Component getNarration() { return Component.literal(this.path); }
    }
}