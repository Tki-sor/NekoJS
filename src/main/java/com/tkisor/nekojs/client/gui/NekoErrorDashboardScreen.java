package com.tkisor.nekojs.client.gui;

import com.tkisor.nekojs.client.gui.components.NekoTabbedEditor;
import com.tkisor.nekojs.core.fs.NekoJSPaths;
import com.tkisor.nekojs.network.*;
import com.tkisor.nekojs.network.dto.ErrorSummaryDTO;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class NekoErrorDashboardScreen extends Screen {
    private final List<ErrorSummaryDTO> errors;

    private ErrorListWidget listWidget;
    private EditBox searchBox;
    private StackTraceList stackList;
    private ContextMenu activeContextMenu = null;

    private ErrorSummaryDTO selectedError = null;
    private FilterType currentFilter = FilterType.ALL;

    private boolean isMaximized = false;
    private boolean isEditing = false;

    private NekoTabbedEditor tabbedEditor = null;

    private int layoutRightX, layoutRightW, layoutContentY, layoutContentH;
    private String toastMessage = "";
    private long toastTime = 0;
    private long openTime;

    private final int[] filterCounts = new int[FilterType.VALUES.length];
    private final String[] filterLabels = new String[FilterType.VALUES.length];

    private enum FilterType {
        ALL("全部", 0xFFFFFFFF), RUNTIME("运行", 0xFFE5534B),
        SYNTAX("语法", 0xFFF2A134), OTHER("其他", 0xFF748394);
        final String label; final int color;
        FilterType(String label, int color) { this.label = label; this.color = color; }

        public static final FilterType[] VALUES = values();
    }

    private record MenuCategory(String name, List<MenuItem> items) {}
    private final List<MenuCategory> menuCategories = List.of(
            new MenuCategory("文件", List.of(
                    new MenuItem("[S] 保存当前修改 (Ctrl+S)", this::actionSaveActiveTab),
                    new MenuItem("[D] 在外部资源管理器中打开", this::actionLocate),
                    new MenuItem("[X] 退出面板", this::onClose)
            )),
            new MenuCategory("云端同步 (Sync)", List.of(
                    new MenuItem("[↑] 将当前脚本推送到服务端", this::actionSyncUploadCurrent),
                    new MenuItem("[↓] 从服务端拉取覆盖当前脚本", this::actionSyncDownloadCurrent),
                    new MenuItem("[↑↑] 强制推送所有本地脚本 (危险)", this::actionSyncUploadAll),
                    new MenuItem("[↓↓] 强制拉取服务端所有脚本 (危险)", this::actionSyncDownloadAll)
            ))
    );

    public NekoErrorDashboardScreen(List<ErrorSummaryDTO> errors) {
        super(Component.literal("NekoJS Dashboard"));
        this.errors = errors;
        if (!errors.isEmpty()) this.selectedError = errors.get(0);

        for (ErrorSummaryDTO dto : errors) {
            filterCounts[FilterType.ALL.ordinal()]++;
            filterCounts[judgeErrorType(dto).ordinal()]++;
        }
        for (FilterType type : FilterType.VALUES) {
            filterLabels[type.ordinal()] = type.label + " §8" + filterCounts[type.ordinal()];
        }
    }

    @Override
    protected void init() {
        super.init();
        this.openTime = System.currentTimeMillis();
        this.buildDashboardLayout();
    }

    private void buildDashboardLayout() {
        String oldSearch = this.searchBox != null ? this.searchBox.getValue() : "";
        double oldScroll = this.listWidget != null ? this.listWidget.scrollAmount() : 0;
        this.clearWidgets();

        int margin = 15;
        int leftW = isMaximized ? 0 : Math.max(160, (int)(this.width * 0.35));
        this.layoutRightX = isMaximized ? margin : leftW + margin * 2;
        this.layoutRightW = this.width - layoutRightX - margin;
        this.layoutContentY = 55;
        this.layoutContentH = this.height - layoutContentY - margin;

        if (!isMaximized) {
            this.searchBox = new EditBox(this.font, margin, 18, leftW, 12, Component.literal("搜索..."));
            this.searchBox.setHint(Component.literal("§8过滤路径..."));
            this.searchBox.setValue(oldSearch);
            this.searchBox.setResponder(s -> this.refreshList());
            this.addRenderableWidget(this.searchBox);

            this.listWidget = new ErrorListWidget(this.minecraft, leftW, layoutContentH, layoutContentY, 36);
            this.listWidget.setX(margin);
            this.refreshList();
            this.listWidget.setScrollAmount(oldScroll); // 恢复重绘前的滚动条位置
            this.addRenderableWidget(this.listWidget);
        }

        if (!isEditing) {
            this.stackList = new StackTraceList(this.minecraft, layoutRightW - 2, layoutContentH - 2, layoutContentY + 2, 12);
            this.stackList.setX(layoutRightX + 1);
            this.refreshStackView();
            this.addRenderableWidget(this.stackList);
        } else {
            if (tabbedEditor == null) {
                tabbedEditor = new NekoTabbedEditor(this.font, layoutRightX, layoutContentY, layoutRightW, layoutContentH,
                        this::doSaveTab,
                        () -> { this.isEditing = false; buildDashboardLayout(); },
                        this::buildDashboardLayout);
            } else {
                tabbedEditor.setBounds(layoutRightX, layoutContentY, layoutRightW, layoutContentH);
            }

            if (tabbedEditor.getActiveTab() != null && tabbedEditor.getActiveTab().editor != null) {
                this.addRenderableWidget(tabbedEditor.getActiveTab().editor.getWidget());
                this.setFocused(tabbedEditor.getActiveTab().editor.getWidget());
            }
        }
    }

    private String getInitialTextForPath(String targetPath) {
        try {
            Path p = NekoJSPaths.ROOT.resolve(targetPath);
            if (Files.exists(p)) return Files.readString(p);
        } catch (Exception e) { return "// 读取失败: " + e.getMessage(); }
        return "";
    }

    public void openTab(String path) {
        this.isEditing = true;
        if (tabbedEditor == null) buildDashboardLayout();
        String text = getInitialTextForPath(path);
        tabbedEditor.openTab(path, text);
    }

    private void refreshList() {
        if (listWidget == null) return;
        String filter = searchBox.getValue().toLowerCase();
        this.listWidget.clearEntries();
        for (ErrorSummaryDTO dto : errors) {
            if (currentFilter != FilterType.ALL && judgeErrorType(dto) != currentFilter) continue;
            if (filter.isEmpty() || dto.path().toLowerCase().contains(filter) || dto.message().toLowerCase().contains(filter)) {
                this.listWidget.add(new ErrorEntry(dto));
            }
        }
        // 🌟 修复：每次重新填充列表内容时，将滚动条强制归零。
        // （不用担心重绘窗口时会丢失进度，因为 buildDashboardLayout 会在调用此方法后立即重新赋值 oldScroll）
        this.listWidget.setScrollAmount(0);
    }

    private void refreshStackView() {
        if (this.stackList == null) return;
        this.stackList.clearEntries();
        if (selectedError != null) {
            int maxLineW = Math.max(100, this.stackList.getRowWidth() - 10);

            selectedError.fullDetails().replace("\t", "    ").lines().forEach(rawLine -> {
                if (rawLine.trim().isEmpty()) {
                    this.stackList.addEntry(new TextLineEntry("", 0xFFCCCCCC));
                    return;
                }

                int color = rawLine.contains("Error") ? 0xFFE5534B : (rawLine.trim().startsWith("at") ? 0xFF888888 : 0xFFCCCCCC);

                List<net.minecraft.network.chat.FormattedText> wrappedLines =
                        this.font.getSplitter().splitLines(rawLine, maxLineW, net.minecraft.network.chat.Style.EMPTY);

                for (net.minecraft.network.chat.FormattedText wrapped : wrappedLines) {
                    this.stackList.addEntry(new TextLineEntry(wrapped.getString(), color));
                }
            });
        }
    }

    private void selectError(ErrorSummaryDTO dto) {
        this.selectedError = dto;
        if (!this.isEditing) { this.refreshStackView(); }
    }

    private void actionLocate() {
        if (selectedError == null) return;
        Util.getPlatform().openFile(NekoJSPaths.ROOT.resolve(selectedError.path()).toFile());
        showToast("§a✔ 尝试打开文件");
    }

    private void actionLog() {
        Util.getPlatform().openFile(NekoJSPaths.GAME_DIR.resolve("logs/latest.log").toFile());
        showToast("§a✔ 打开 latest.log");
    }

    private void actionCopy() {
        if (selectedError == null) return;
        Minecraft.getInstance().keyboardHandler.setClipboard(selectedError.fullDetails());
        showToast("§a✔ 已存入剪贴板");
    }

    private void actionSaveActiveTab() {
        if (tabbedEditor != null && tabbedEditor.getActiveTab() != null) {
            doSaveTab(tabbedEditor.getActiveTab());
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

    public void loadServerScript(String content) {
        if (isEditing && tabbedEditor != null && tabbedEditor.getActiveTab() != null) {
            tabbedEditor.getActiveTab().editor.getWidget().setValue(content);
            tabbedEditor.getActiveTab().editor.markSaved();
            this.showToast("§a✔ 已成功从服务端拉取代码");
        }
    }

    private void actionSyncUploadCurrent() {
        if (!this.isEditing || tabbedEditor == null || tabbedEditor.getActiveTab() == null) {
            this.showToast("§c❌ 错误：请先进入 [编辑] 模式！");
            return;
        }
        ClientPacketDistributor.sendToServer(new SaveScriptPacket(tabbedEditor.getActiveTab().path, tabbedEditor.getActiveTab().editor.getValue()));
        showToast("§e[↑] 正在推送到服务端...");
    }

    private void actionSyncDownloadCurrent() {
        if (!this.isEditing || tabbedEditor == null || tabbedEditor.getActiveTab() == null) {
            this.showToast("§c❌ 错误：请先进入 [编辑] 模式！");
            return;
        }
        ClientPacketDistributor.sendToServer(new FetchScriptRequestPacket(tabbedEditor.getActiveTab().path));
        showToast("§b[↓] 正在请求拉取代码...");
    }

    private void actionSyncUploadAll() {
        showToast("§e[↑↑] 正在扫描本地文件并推送...");
        Map<String, String> localFiles = NekoJSNetwork.collectAllValidScripts(NekoJSPaths.ROOT);

        if (localFiles.isEmpty()) {
            showToast("§c✖ 本地 nekojs 目录为空或不存在 js 文件！");
            return;
        }
        ClientPacketDistributor.sendToServer(new UploadAllScriptsPacket(localFiles));
    }

    private void actionSyncDownloadAll() {
        showToast("§b[↓↓] 正在请求拉取所有服务端代码...");
        ClientPacketDistributor.sendToServer(new FetchAllScriptsRequestPacket());
    }

    public void showToast(String msg) {
        this.toastMessage = msg;
        this.toastTime = System.currentTimeMillis() + 2000;
    }

    public void onSyncFeedback(boolean success, String message) {
        String prefix = success ? "§a✔ " : "§c✖ ";
        this.showToast(prefix + message);

        if (success && this.isEditing && tabbedEditor != null && tabbedEditor.getActiveTab() != null) {
            tabbedEditor.getActiveTab().editor.markSaved();
        }
    }

    private FilterType judgeErrorType(ErrorSummaryDTO dto) {
        String msg = dto.message().toLowerCase();
        if (msg.contains("syntax") || msg.contains("unexpected")) return FilterType.SYNTAX;
        if (msg.contains("null") || msg.contains("undefined") || msg.contains("error")) return FilterType.RUNTIME;
        return FilterType.OTHER;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fillGradient(0, 0, this.width, this.height, 0xFF0A0A0B, 0xFF121214);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        float fade = Mth.clamp((System.currentTimeMillis() - openTime) / 300f, 0, 1);

        graphics.fill(0, 0, this.width, 14, 0xFF18181A);
        graphics.fill(0, 14, this.width, 50, ARGB.color((int)(fade * 180), 12, 12, 12));
        graphics.fill(0, 50, this.width, 51, 0x30FFFFFF);

        graphics.text(this.font, "§cNEKO§fJS", 15, 21, -1);

        int menuX = 5;
        for (MenuCategory cat : menuCategories) {
            int w = this.font.width(cat.name());
            boolean hov = mouseX >= menuX && mouseX <= menuX + w + 8 && mouseY >= 0 && mouseY <= 14;
            if (hov && this.activeContextMenu == null) {
                graphics.fill(menuX, 0, menuX + w + 8, 14, 0xFF3E3E42);
            }
            graphics.text(this.font, cat.name(), menuX + 4, 3, 0xFFCCCCCC);
            menuX += w + 8;
        }

        if (!isMaximized) {
            int tabX = 15, tabY = 38;
            for (FilterType type : FilterType.VALUES) {
                String lbl = filterLabels[type.ordinal()];
                int lw = this.font.width(lbl);
                boolean isCur = currentFilter == type;
                if (isCur) {
                    graphics.fill(tabX - 4, tabY - 2, tabX + lw + 4, tabY + 12, 0x20FFFFFF);
                    graphics.fill(tabX - 4, tabY + 11, tabX + lw + 4, tabY + 12, type.color);
                }
                graphics.text(this.font, lbl, tabX, tabY, isCur ? -1 : 0xFF666666);
                tabX += lw + 12;
            }
        }

        if (isEditing && tabbedEditor != null) {
            tabbedEditor.renderUnderlay(graphics, mouseX, mouseY);
        } else {
            graphics.fill(layoutRightX, layoutContentY, layoutRightX + layoutRightW, layoutContentY + layoutContentH, 0xFF1E1E1E);
        }

        boolean blockEditorHover = (activeContextMenu != null) || (tabbedEditor != null && tabbedEditor.isHoveringDropdown(mouseX, mouseY));
        super.extractRenderState(graphics, blockEditorHover ? -999 : mouseX, blockEditorHover ? -999 : mouseY, partialTick);

        renderHeaderButtons(graphics, mouseX, mouseY);

        boolean isDirty = isEditing && tabbedEditor != null && tabbedEditor.getActiveTab() != null && tabbedEditor.getActiveTab().editor.isDirty();
        int decorColor = isEditing ? (isDirty ? 0xFFFFDD00 : 0xFF44FF44) : (selectedError != null ? judgeErrorType(selectedError).color : 0xFF333333);
        graphics.fill(layoutRightX, layoutContentY, layoutRightX + 2, layoutContentY + layoutContentH, decorColor);
        graphics.outline(layoutRightX, layoutContentY, layoutRightW, layoutContentH, 0xFF333333);

        if (System.currentTimeMillis() < toastTime) {
            float yAnim = Mth.clamp((2000f - (toastTime - System.currentTimeMillis())) / 150f, 0, 1);
            int tw = this.font.width(toastMessage) + 20;
            int ty = this.height - 45 - (int)(15 * yAnim);
            graphics.fill(this.width/2-tw/2, ty, this.width/2+tw/2, ty+16, 0xCC000000);
            graphics.outline(this.width/2-tw/2, ty, tw, 16, 0xFF44FF44);
            graphics.centeredText(this.font, toastMessage, this.width / 2, ty + 4, -1);
        }

        if (this.activeContextMenu != null) this.activeContextMenu.render(graphics, mouseX, mouseY);
    }

    private void renderHeaderButtons(GuiGraphicsExtractor g, int mx, int my) {
        if (selectedError == null) return;
        boolean isDirty = isEditing && tabbedEditor != null && tabbedEditor.getActiveTab() != null && tabbedEditor.getActiveTab().editor.isDirty();

        if (!isEditing) {
            g.text(this.font, "§8目标: §e" + selectedError.path(), layoutRightX + 5, 38, -1);
        }

        int curX = layoutRightX + layoutRightW - 10;
        int closeX = this.width - 25;
        g.text(this.font, (mx >= closeX && mx <= closeX + 10 && my >= 16 && my <= 26) ? "§c✖" : "§7✖", closeX, 16, -1);
        int maxX = closeX - 25;
        g.text(this.font, (mx >= maxX && mx <= maxX + 10 && my >= 16 && my <= 26) ? "§f" + (isMaximized ? "🗗" : "⛶") : "§7" + (isMaximized ? "🗗" : "⛶"), maxX, 16, -1);

        if (isEditing) {
            curX = renderLink(g, "§c[退出编辑]", curX, mx, my);
            curX = renderLink(g, isDirty ? "§e[保存当前*]" : "§a[保存当前]", curX, mx, my);
        } else {
            curX = renderLink(g, "§7[复制]", curX, mx, my);
            curX = renderLink(g, "§b[日志]", curX, mx, my);
            curX = renderLink(g, "§e[定位]", curX, mx, my);
            curX = renderLink(g, "§a[编辑]", curX, mx, my);
        }
    }

    private int renderLink(GuiGraphicsExtractor g, String text, int x, int mx, int my) {
        int w = this.font.width(text);
        int targetX = x - w - 5;
        boolean hov = mx >= targetX && mx <= targetX + w && my >= 38 && my <= 48;
        g.text(this.font, text, targetX, 38, -1);
        if (hov) g.fill(targetX, 38 + this.font.lineHeight, targetX + w, 38 + this.font.lineHeight + 1, 0xFFFFFFFF);
        return targetX;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.isEditing && tabbedEditor != null) {
            if (tabbedEditor.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.activeContextMenu != null && event.isEscape()) {
            this.activeContextMenu = null; return true;
        }
        if (this.isEditing && tabbedEditor != null) {
            if (tabbedEditor.keyPressed(event)) return true;
            if (event.isEscape()) {
                this.isEditing = false;
                this.buildDashboardLayout();
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (this.activeContextMenu != null) return false;
        if (this.isEditing && tabbedEditor != null) {
            if (tabbedEditor.charTyped((char) event.codepoint())) return true;
        }
        return super.charTyped(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.activeContextMenu != null) {
            this.activeContextMenu.mouseClicked(event.x(), event.y(), event.button());
            this.activeContextMenu = null;
            return true;
        }

        if (event.y() >= 0 && event.y() <= 14) {
            int menuX = 5;
            for (MenuCategory cat : menuCategories) {
                int w = this.font.width(cat.name()) + 8;
                if (event.x() >= menuX && event.x() <= menuX + w) {
                    this.activeContextMenu = new ContextMenu(menuX, 14, cat.items());
                    return true;
                }
                menuX += w;
            }
        }

        int closeX = this.width - 25;
        if (event.x() >= closeX && event.x() <= closeX + 10 && event.y() >= 16 && event.y() <= 26) { this.onClose(); return true; }
        int maxX = closeX - 25;
        if (event.x() >= maxX && event.x() <= maxX + 10 && event.y() >= 16 && event.y() <= 26) { this.isMaximized = !this.isMaximized; this.buildDashboardLayout(); return true; }

        if (isEditing && tabbedEditor != null) {
            if (tabbedEditor.mouseClicked(event.x(), event.y(), event.button())) return true;
        }

        if (selectedError != null && event.y() >= 38 && event.y() <= 48) {
            int curX = layoutRightX + layoutRightW - 10;
            if (isEditing) {
                int cw = this.font.width("§c[退出编辑]"); curX = curX - cw - 5;
                if (event.x() >= curX && event.x() <= curX + cw) { this.isEditing = false; buildDashboardLayout(); return true; }

                boolean isDirty = tabbedEditor != null && tabbedEditor.getActiveTab() != null && tabbedEditor.getActiveTab().editor.isDirty();
                int sw = this.font.width(isDirty ? "§e[保存当前*]" : "§a[保存当前]"); curX = curX - sw - 5;
                if (event.x() >= curX && event.x() <= curX + sw) { actionSaveActiveTab(); return true; }
            } else {
                int cw1 = this.font.width("§7[复制]"); curX = curX - cw1 - 5;
                if (event.x() >= curX && event.x() <= curX + cw1) { actionCopy(); return true; }
                int cw2 = this.font.width("§b[日志]"); curX = curX - cw2 - 5;
                if (event.x() >= curX && event.x() <= curX + cw2) { actionLog(); return true; }
                int cw3 = this.font.width("§e[定位]"); curX = curX - cw3 - 5;
                if (event.x() >= curX && event.x() <= curX + cw3) { actionLocate(); return true; }

                int cw4 = this.font.width("§a[编辑]"); curX = curX - cw4 - 5;
                if (event.x() >= curX && event.x() <= curX + cw4) { openTab(selectedError.path()); return true; }
            }
        }

        if (!isMaximized) {
            int tabX = 15, tabY = 38;
            for (FilterType type : FilterType.VALUES) {
                int lw = this.font.width(filterLabels[type.ordinal()]);
                if (event.x() >= tabX && event.x() <= tabX + lw && event.y() >= tabY && event.y() <= tabY + 12) {
                    // 当切换分类时，除了更新 filter 外，我们强制调用一次鼠标点击刷新
                    this.currentFilter = type;
                    this.refreshList();
                    return true;
                }
                tabX += lw + 12;
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    private class ErrorListWidget extends ObjectSelectionList<ErrorEntry> {
        public ErrorListWidget(Minecraft mc, int w, int h, int y, int ih) { super(mc, w, h, y, ih); }
        public void add(ErrorEntry entry) { this.addEntry(entry); }
        @Override public int getRowWidth() { return this.width - 15; }
        @Override protected int scrollBarX() { return this.getX() + this.width - 6; }
        @Override protected void extractListBackground(GuiGraphicsExtractor g) {}
        @Override protected void extractListSeparators(GuiGraphicsExtractor g) {}
    }

    private class ErrorEntry extends ObjectSelectionList.Entry<ErrorEntry> {
        private final ErrorSummaryDTO dto;
        public ErrorEntry(ErrorSummaryDTO dto) { this.dto = dto; }
        @Override
        public void extractContent(GuiGraphicsExtractor g, int mx, int my, boolean isHovered, float pt) {
            boolean isSelected = selectedError != null && selectedError.id().equals(dto.id());
            boolean hov = (NekoErrorDashboardScreen.this.activeContextMenu == null) && isHovered;
            int x = this.getX(), y = this.getY(), w = this.getWidth(), h = this.getHeight() - 4;

            g.fill(x, y, x + w, y + h, isSelected ? 0x30FFFFFF : (hov ? 0x15FFFFFF : 0x08FFFFFF));
            g.fill(x + 2, y + 4, x + 4, y + h - 4, judgeErrorType(dto).color);
            g.text(NekoErrorDashboardScreen.this.font, (isSelected ? "§e" : "§f") + dto.path(), x + 8, y + 6, -1);
            g.text(NekoErrorDashboardScreen.this.font, "§7" + NekoErrorDashboardScreen.this.font.plainSubstrByWidth(dto.message(), w - 20), x + 8, y + 18, -1);
        }
        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (NekoErrorDashboardScreen.this.activeContextMenu != null) return false;
            if (event.button() == 0) {
                selectError(dto);
                if (doubleClick) {
                    NekoErrorDashboardScreen.this.openTab(dto.path());
                }
                return true;
            } else if (event.button() == 1) {
                NekoErrorDashboardScreen.this.activeContextMenu = new ContextMenu((int)event.x(), (int)event.y(), List.of(
                        new MenuItem("⛶ 全屏查看", () -> { selectError(dto); isMaximized = true; buildDashboardLayout(); }),
                        new MenuItem("📝 在标签页打开", () -> { NekoErrorDashboardScreen.this.openTab(dto.path()); }),
                        new MenuItem("📂 使用系统外部应用打开", () -> { Util.getPlatform().openFile(NekoJSPaths.ROOT.resolve(dto.path()).toFile()); showToast("§a✔ 指令已发送"); })
                ));
                return true;
            }
            return false;
        }
        @Override public Component getNarration() { return Component.literal(dto.path()); }
    }

    private class StackTraceList extends ObjectSelectionList<TextLineEntry> {
        public StackTraceList(Minecraft mc, int w, int h, int y, int ih) { super(mc, w, h, y, ih); }
        @Override public int getRowWidth() { return this.width - 20; }
        @Override protected int scrollBarX() { return this.getX() + this.width - 8; }
        @Override protected void extractListBackground(GuiGraphicsExtractor g) {}
        @Override protected void extractListSeparators(GuiGraphicsExtractor g) {}
        public int addEntry(TextLineEntry entry) { return super.addEntry(entry); }
    }

    private class TextLineEntry extends ObjectSelectionList.Entry<TextLineEntry> {
        private final String line;
        private final int color;

        public TextLineEntry(String line, int color) {
            this.line = line;
            this.color = color;
        }

        @Override
        public void extractContent(GuiGraphicsExtractor g, int mx, int my, boolean hov, float pt) {
            g.text(NekoErrorDashboardScreen.this.font, "§7" + this.line, this.getX() + 5, this.getY() + 2, this.color);
        }
        @Override public Component getNarration() { return Component.empty(); }
    }

    private class ContextMenu {
        int x, y, width, height; List<MenuItem> items;
        public ContextMenu(int sx, int sy, List<MenuItem> items) {
            this.items = items;
            this.height = items.size() * 18 + 6;
            int maxW = 120;
            for(MenuItem item : items) { maxW = Math.max(maxW, NekoErrorDashboardScreen.this.font.width(item.label) + 20); }
            this.width = maxW;
            this.x = Math.min(sx, NekoErrorDashboardScreen.this.width - width - 5);
            this.y = Math.min(sy, NekoErrorDashboardScreen.this.height - height - 5);
        }
        public void render(GuiGraphicsExtractor g, int mx, int my) {
            g.fill(x, y, x+width, y+height, 0xFF18181B); g.outline(x, y, width, height, 0xFF3F3F46);
            for (int i = 0; i < items.size(); i++) {
                int iy = y + 3 + i * 18; boolean h = mx >= x && mx <= x+width && my >= iy && my < iy+18;
                if (h) g.fill(x+2, iy, x+width-2, iy+18, 0xFF27272A);
                g.text(NekoErrorDashboardScreen.this.font, items.get(i).label, x+8, iy+5, h ? -1 : 0xFFA1A1AA);
            }
        }
        public boolean mouseClicked(double mx, double my, int b) {
            if (b == 0 && mx >= x && mx <= x+width && my >= y && my <= y+height) {
                int index = ((int)my - y - 3) / 18; if (index >= 0 && index < items.size()) items.get(index).action.run(); return true;
            } return false;
        }
    }
    private record MenuItem(String label, Runnable action) {}
}