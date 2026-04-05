package com.tkisor.nekojs.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import com.tkisor.nekojs.client.gui.components.NekoCodeEditor;
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
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class NekoWorkspaceScreen extends Screen {

    private NekoTabbedEditor tabbedEditor;
    private FileListWidget fileListWidget;
    private EditBox searchBox;
    private EditBox replaceBox;

    private boolean isMatchCase = false;
    private boolean isMatchWord = false;
    private boolean isRegex = false;
    private boolean isPreserveCase = false;

    private List<String> allLocalFiles = new ArrayList<>();
    private Map<String, String> fileContentCache = new HashMap<>();

    private FileNode treeRoot;
    private FileNode searchRoot;
    private String selectedFilePath = null;

    private int selectedMatchStart = -1;

    private boolean isSidebarOpen = true;
    private int activeActivity = 0;

    private double listScrollX = 0;
    private int maxListContentWidth = 0;

    private final int actBarW = 24;
    private int sidebarW;
    private int leftW;
    private int rightX;
    private final int topY = 20;

    private String toastMessage = "";
    private long toastTime = 0;

    private ContextMenu activeContextMenu = null;
    private boolean isModalOpen = false;
    private String modalTitle = "";
    private EditBox modalInput;
    private Consumer<String> modalCallback;

    public NekoWorkspaceScreen() {
        super(Component.literal("NekoJS Workspace"));
    }

    // 🌟 新增：替代被移除的 Screen.hasShiftDown()
    private static boolean hasShiftDown() {
        Window window = Minecraft.getInstance().getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT) ||
                InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    @Override
    protected void init() {
        super.init();
        scanLocalFiles();
        buildWorkspaceLayout();
    }

    private void buildWorkspaceLayout() {
        String oldSearch = this.searchBox != null ? this.searchBox.getValue() : "";
        String oldReplace = this.replaceBox != null ? this.replaceBox.getValue() : "";
        double oldScroll = this.fileListWidget != null ? this.fileListWidget.scrollAmount() : 0;
        this.clearWidgets();

        this.sidebarW = isSidebarOpen ? Math.max(160, Math.min(250, (int) (this.width * 0.18))) : 0;
        this.leftW = actBarW + sidebarW;
        this.rightX = leftW + 1;

        int contentH = this.height - topY;

        if (isSidebarOpen) {
            if (activeActivity == 0) {
                this.fileListWidget = new FileListWidget(this.minecraft, sidebarW, contentH - 30, topY + 24, 14);
                this.fileListWidget.setX(actBarW);
                refreshFileList();
                this.fileListWidget.setScrollAmount(oldScroll);
                this.addRenderableWidget(this.fileListWidget);
            } else if (activeActivity == 1) {
                int inputWidth = sidebarW - 12 - 50;

                this.searchBox = new EditBox(this.font, actBarW + 6, topY + 24, inputWidth, 18, Component.literal("搜索"));
                this.searchBox.setHint(Component.literal("§8搜索..."));
                this.searchBox.setValue(oldSearch);
                this.searchBox.setTextColor(0xFFFFFFFF);

                this.searchBox.setResponder(s -> { buildSearchTree(); refreshFileList(); });
                this.addRenderableWidget(this.searchBox);

                this.replaceBox = new EditBox(this.font, actBarW + 6, topY + 46, inputWidth, 18, Component.literal("替换"));
                this.replaceBox.setHint(Component.literal("§8替换为..."));
                this.replaceBox.setValue(oldReplace);
                this.replaceBox.setTextColor(0xFFFFFFFF);
                this.addRenderableWidget(this.replaceBox);

                buildSearchTree();

                this.fileListWidget = new FileListWidget(this.minecraft, sidebarW, contentH - 74, topY + 68, 14);
                this.fileListWidget.setX(actBarW);
                refreshFileList();
                this.fileListWidget.setScrollAmount(oldScroll);
                this.addRenderableWidget(this.fileListWidget);

                this.setFocused(this.searchBox);
            }
        }

        if (this.tabbedEditor == null) {
            this.tabbedEditor = new NekoTabbedEditor(this.font, rightX, topY, this.width - rightX, contentH,
                    this::doSaveTab,
                    this::buildWorkspaceLayout,
                    this::buildWorkspaceLayout);
        } else {
            this.tabbedEditor.setBounds(rightX, topY, this.width - rightX, contentH);
        }

        if (this.tabbedEditor.getActiveTab() != null && this.tabbedEditor.getActiveTab().editor != null) {
            this.addRenderableWidget(this.tabbedEditor.getActiveTab().editor.getWidget());
            if (activeActivity != 1 && !isModalOpen) {
                this.setFocused(this.tabbedEditor.getActiveTab().editor.getWidget());
            }
        }

        int mw = 240;
        int mh = 80;
        int mx_ = this.width / 2 - mw / 2;
        int my_ = this.height / 2 - mh / 2;
        this.modalInput = new EditBox(this.font, mx_ + 10, my_ + 28, mw - 20, 18, Component.empty());
        this.modalInput.setTextColor(0xFFFFFFFF);
        this.modalInput.visible = isModalOpen;
        this.modalInput.active = isModalOpen;
        this.addRenderableWidget(this.modalInput);
        if (isModalOpen) this.setFocused(this.modalInput);
    }

    private void collectExpandedPaths(FileNode node, Set<String> set) {
        if (node.isExpanded && !node.path.isEmpty()) set.add(node.path);
        for (FileNode child : node.children) collectExpandedPaths(child, set);
    }

    private void scanLocalFiles() {
        Set<String> expandedPaths = new HashSet<>();
        if (this.treeRoot != null) collectExpandedPaths(this.treeRoot, expandedPaths);

        this.allLocalFiles.clear();
        this.fileContentCache.clear();
        this.treeRoot = new FileNode("root", "", true, -1);
        this.treeRoot.isExpanded = true;

        Path root = NekoJSPaths.ROOT;
        if (!Files.exists(root)) return;

        try (Stream<Path> stream = Files.walk(root)) {
            stream.forEach(p -> {
                if (p.equals(root)) return;

                String relPath = root.relativize(p).toString().replace('\\', '/');
                boolean isActualDir = Files.isDirectory(p);

                if (!isActualDir) {
                    this.allLocalFiles.add(relPath);
                    try {
                        if (Files.size(p) < 1024 * 1024) {
                            fileContentCache.put(relPath, Files.readString(p));
                        }
                    } catch (Exception ignored) {}
                }

                String[] parts = relPath.split("/");
                FileNode current = treeRoot;
                StringBuilder pathBuilder = new StringBuilder();

                for (int i = 0; i < parts.length; i++) {
                    if (pathBuilder.length() > 0) pathBuilder.append("/");
                    pathBuilder.append(parts[i]);
                    String currentFullPath = pathBuilder.toString();

                    FileNode child = null;
                    for (FileNode c : current.children) {
                        if (c.name.equals(parts[i])) {
                            child = c; break;
                        }
                    }

                    if (child == null) {
                        boolean isDirNode = (i < parts.length - 1) || isActualDir;
                        child = new FileNode(parts[i], currentFullPath, isDirNode, current.depth + 1);
                        if (expandedPaths.contains(currentFullPath)) child.isExpanded = true;
                        current.children.add(child);
                    }
                    current = child;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.allLocalFiles.sort(String::compareToIgnoreCase);
        sortTree(treeRoot);
        buildSearchTree();
    }

    private Pattern getSearchPattern(String text) {
        if (text == null || text.isEmpty()) return null;
        String regex = isRegex ? text : Pattern.quote(text);
        if (isMatchWord) regex = "\\b" + regex + "\\b";
        int flags = isMatchCase ? 0 : Pattern.CASE_INSENSITIVE;
        try { return Pattern.compile(regex, flags); } catch (Exception e) { return null; }
    }

    private void buildSearchTree() {
        this.searchRoot = new FileNode("search_root", "", true, -1);
        this.searchRoot.isExpanded = true;

        String filter = searchBox != null ? searchBox.getValue() : "";
        if (filter.isEmpty()) return;

        Pattern pattern = getSearchPattern(filter);
        if (pattern == null) return;

        for (String path : allLocalFiles) {
            String content = fileContentCache.getOrDefault(path, "");
            String[] lines = content.replace("\r", "").split("\n", -1);

            FileNode fileNode = new FileNode(path, path, true, 0);
            fileNode.isExpanded = true;

            int globalOffset = 0;
            int matchCount = 0;

            for (int i = 0; i < lines.length; i++) {
                String rawLine = lines[i];
                String trimmedLine = rawLine.trim();
                int trimOffset = rawLine.indexOf(trimmedLine);
                if (trimOffset == -1) trimOffset = 0;

                Matcher m = pattern.matcher(rawLine);
                List<int[]> highlights = new ArrayList<>();

                int firstMatchAbsoluteStart = -1;
                int firstMatchAbsoluteEnd = -1;

                while (m.find()) {
                    matchCount++;
                    if (firstMatchAbsoluteStart == -1) {
                        firstMatchAbsoluteStart = globalOffset + m.start();
                        firstMatchAbsoluteEnd = globalOffset + m.end();
                    }

                    int localStart = m.start() - trimOffset;
                    int localEnd = m.end() - trimOffset;
                    localStart = Math.max(0, localStart);
                    localEnd = Math.min(trimmedLine.length(), localEnd);
                    highlights.add(new int[]{localStart, localEnd});
                }

                if (!highlights.isEmpty()) {
                    int firstMatchStart = highlights.get(0)[0];
                    String displayLine = trimmedLine;
                    int shift = 0;
                    boolean addedPrefix = false;

                    if (displayLine.length() > 60) {
                        if (firstMatchStart > 20) {
                            shift = firstMatchStart - 15;
                            int end = Math.min(displayLine.length(), shift + 50);
                            displayLine = "..." + displayLine.substring(shift, end) + (end < displayLine.length() ? "..." : "");
                            addedPrefix = true;
                        } else {
                            displayLine = displayLine.substring(0, 60) + "...";
                        }
                    }

                    List<int[]> finalHighlights = new ArrayList<>();
                    for (int[] hl : highlights) {
                        int s = hl[0] - shift + (addedPrefix ? 3 : 0);
                        int e = hl[1] - shift + (addedPrefix ? 3 : 0);
                        s = Math.max(0, Math.min(s, displayLine.length()));
                        e = Math.max(0, Math.min(e, displayLine.length()));
                        if (s < e) {
                            finalHighlights.add(new int[]{s, e});
                        }
                    }

                    SearchMatchNode matchNode = new SearchMatchNode(displayLine, path, finalHighlights, firstMatchAbsoluteStart, firstMatchAbsoluteEnd);
                    fileNode.children.add(matchNode);
                }
                globalOffset += rawLine.length() + 1;
            }

            if (matchCount > 0) {
                String[] parts = path.split("/");
                String fileName = parts[parts.length - 1];
                fileNode.name = fileName + " (" + matchCount + ")  §8" + getParentDir(path);
                searchRoot.children.add(fileNode);
            }
        }
    }

    private void refreshFileList() {
        if (fileListWidget == null || treeRoot == null) return;
        double oldScroll = fileListWidget.scrollAmount();
        fileListWidget.clearEntries();
        maxListContentWidth = 0;

        if (activeActivity == 0) {
            addNodesToWidget(treeRoot);
        } else if (activeActivity == 1) {
            if (searchRoot != null) {
                addNodesToWidget(searchRoot);
            }
        }
        fileListWidget.setScrollAmount(oldScroll);

        int visibleW = sidebarW - 12;
        int maxScrollX = Math.max(0, maxListContentWidth - visibleW);
        listScrollX = Mth.clamp(listScrollX, 0, maxScrollX);
    }

    private void addNodesToWidget(FileNode node) {
        if (node != treeRoot && node != searchRoot) {
            fileListWidget.addEntry(new FileEntry(node, false));
            int indent = node.depth * 10 + 4;
            int width = indent + 20 + this.font.width(node.name);
            maxListContentWidth = Math.max(maxListContentWidth, width);
        }
        if (node == treeRoot || node == searchRoot || (node.isDir && node.isExpanded)) {
            for (FileNode child : node.children) {
                addNodesToWidget(child);
            }
        }
    }

    private void doGlobalReplaceAll() {
        if (searchBox == null || replaceBox == null) return;
        String query = searchBox.getValue();
        String replacement = replaceBox.getValue();

        Pattern pattern = getSearchPattern(query);
        if (pattern == null) { showToast("§c✖ 搜索内容为空或正则错误"); return; }

        int affectedFiles = 0;
        int totalReplaced = 0;

        for (String path : allLocalFiles) {
            String content = fileContentCache.getOrDefault(path, "");
            Matcher matcher = pattern.matcher(content);

            if (matcher.find()) {
                String newContent;
                if (isPreserveCase) {
                    StringBuffer sb = new StringBuffer();
                    matcher.reset();
                    while (matcher.find()) {
                        String matchStr = matcher.group();
                        String actualReplacement = replacement;
                        if (matchStr.toUpperCase().equals(matchStr)) actualReplacement = replacement.toUpperCase();
                        else if (Character.isUpperCase(matchStr.charAt(0))) actualReplacement = replacement.substring(0, 1).toUpperCase() + (replacement.length() > 1 ? replacement.substring(1) : "");
                        else actualReplacement = replacement.toLowerCase();
                        matcher.appendReplacement(sb, Matcher.quoteReplacement(actualReplacement));
                        totalReplaced++;
                    }
                    matcher.appendTail(sb);
                    newContent = sb.toString();
                } else {
                    matcher.reset();
                    newContent = matcher.replaceAll(Matcher.quoteReplacement(replacement));
                    Matcher countMatcher = pattern.matcher(content);
                    while(countMatcher.find()) totalReplaced++;
                }

                if (!content.equals(newContent)) {
                    try {
                        Files.writeString(NekoJSPaths.ROOT.resolve(path), newContent);
                        fileContentCache.put(path, newContent);
                        affectedFiles++;
                        if (tabbedEditor != null) tabbedEditor.openTab(path, newContent);
                    } catch (Exception e) { showToast("§c✖ 替换文件失败: " + path); }
                }
            }
        }
        buildSearchTree();
        refreshFileList();
        showToast("§a✔ 在 " + affectedFiles + " 个文件中替换了 " + totalReplaced + " 处内容");
    }

    private void sortTree(FileNode node) { node.children.sort((a, b) -> { if (a.isDir != b.isDir) return a.isDir ? -1 : 1; return a.name.compareToIgnoreCase(b.name); }); for (FileNode c : node.children) { if (c.isDir) sortTree(c); } }
    private void createNewItem(String targetDir, String name, boolean isDir) { try { if (name == null || name.trim().isEmpty()) return; String fullPath = targetDir.isEmpty() ? name : targetDir + "/" + name; Path p = NekoJSPaths.ROOT.resolve(fullPath); if (isDir) { Files.createDirectories(p); } else { if (p.getParent() != null) Files.createDirectories(p.getParent()); if (!Files.exists(p)) Files.createFile(p); } scanLocalFiles(); if (!targetDir.isEmpty()) { expandNodeByPath(treeRoot, targetDir); } refreshFileList(); showToast("§a✔ 创建成功: " + name); } catch (Exception e) { showToast("§c✖ 创建失败: " + e.getMessage()); } }
    private void expandNodeByPath(FileNode root, String path) { if (path == null || path.isEmpty()) return; String[] parts = path.split("/"); FileNode current = root; for (String part : parts) { for (FileNode child : current.children) { if (child.name.equals(part)) { child.isExpanded = true; current = child; break; } } } }
    private void deleteItem(String path) { try { Path target = NekoJSPaths.ROOT.resolve(path); if (Files.exists(target)) { if (Files.isDirectory(target)) { try (Stream<Path> walk = Files.walk(target)) { walk.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete); } } else { Files.delete(target); } } if (selectedFilePath != null && selectedFilePath.startsWith(path)) { selectedFilePath = null; } scanLocalFiles(); refreshFileList(); showToast("§a✔ 已删除 " + path); } catch (Exception e) { showToast("§c✖ 删除失败: " + e.getMessage()); } }
    private String getParentDir(String path) { int idx = path.lastIndexOf('/'); return (idx == -1) ? "" : path.substring(0, idx); }
    private void openModal(String title, Consumer<String> callback) { this.modalTitle = title; this.modalCallback = callback; if (this.modalInput != null) { this.modalInput.setValue(""); this.modalInput.visible = true; this.modalInput.active = true; this.setFocused(this.modalInput); } this.isModalOpen = true; this.activeContextMenu = null; }
    private void closeModal() { this.isModalOpen = false; if (this.modalInput != null) { this.modalInput.visible = false; this.modalInput.active = false; } this.setFocused(null); }

    private void openFileInEditor(String path) {
        openFileInEditor(path, -1, -1);
    }

    private void openFileInEditor(String path, int selectionStart, int selectionEnd) {
        try {
            Path p = NekoJSPaths.ROOT.resolve(path);
            String text = Files.exists(p) ? Files.readString(p) : "";
            if (tabbedEditor == null) buildWorkspaceLayout();
            tabbedEditor.openTab(path, text);

            if (selectionStart != -1 && selectionEnd != -1) {
                if (tabbedEditor.getActiveTab() != null && tabbedEditor.getActiveTab().editor != null) {
                    tabbedEditor.getActiveTab().editor.setHighlightAndScroll(selectionStart, selectionEnd);
                }
            }

        } catch (Exception e) { showToast("§c✖ 无法读取文件: " + e.getMessage()); }
    }

    private void doSaveTab(NekoTabbedEditor.Tab tab) { if (tab == null || tab.editor == null) return; try { Path path = NekoJSPaths.ROOT.resolve(tab.path); Files.writeString(path, tab.editor.getValue()); showToast("§a✔ 已保存 " + tab.path); tab.editor.markSaved(); } catch (Exception e) { showToast("§c✖ 保存失败: " + e.getMessage()); } }
    public void showToast(String msg) { this.toastMessage = msg; this.toastTime = System.currentTimeMillis() + 2000; }
    public void onSyncFeedback(boolean success, String message) { String prefix = success ? "§a✔ " : "§c✖ "; this.showToast(prefix + message); if (success && tabbedEditor != null && tabbedEditor.getActiveTab() != null) { tabbedEditor.getActiveTab().editor.markSaved(); } }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fillGradient(0, 0, this.width, this.height, 0xFF1E1E1E, 0xFF1E1E1E);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, topY, 0xFF333333);
        graphics.text(this.font, "§cNEKO§fJS §8Workspace", 10, 6, -1);
        int closeX = this.width - 20;
        graphics.text(this.font, (mouseX >= closeX && mouseX <= closeX + 10 && mouseY >= 4 && mouseY <= 16) ? "§c✖" : "§7✖", closeX, 6, -1);
        renderTopMenuBar(graphics, mouseX, mouseY);
        graphics.fill(0, topY, actBarW, this.height, 0xFF333333);

        int iconCenterYOffset = (actBarW - this.font.lineHeight) / 2;
        int iconCenterX1 = (actBarW - this.font.width("📂")) / 2;
        int iconCenterX2 = (actBarW - this.font.width("🔍")) / 2;
        int iconCenterX3 = (actBarW - this.font.width("⚙")) / 2;

        if (activeActivity == 0 && isSidebarOpen) { graphics.fill(0, topY, actBarW, topY + actBarW, 0xFF444444); graphics.fill(0, topY, 2, topY + actBarW, 0xFF007ACC); graphics.text(this.font, "§f📂", iconCenterX1, topY + iconCenterYOffset, -1); } else { boolean hov = mouseX >= 0 && mouseX <= actBarW && mouseY >= topY && mouseY < topY + actBarW; graphics.text(this.font, "§7📂", iconCenterX1, topY + iconCenterYOffset, hov ? 0xFFFFFFFF : -1); }
        int btn2Y = topY + actBarW;
        if (activeActivity == 1 && isSidebarOpen) { graphics.fill(0, btn2Y, actBarW, btn2Y + actBarW, 0xFF444444); graphics.fill(0, btn2Y, 2, btn2Y + actBarW, 0xFF007ACC); graphics.text(this.font, "§f🔍", iconCenterX2, btn2Y + iconCenterYOffset, -1); } else { boolean hov = mouseX >= 0 && mouseX <= actBarW && mouseY >= btn2Y && mouseY < btn2Y + actBarW; graphics.text(this.font, "§7🔍", iconCenterX2, btn2Y + iconCenterYOffset, hov ? 0xFFFFFFFF : -1); }
        int btnSettingsY = this.height - actBarW; boolean hovSettings = mouseX >= 0 && mouseX <= actBarW && mouseY >= btnSettingsY && mouseY < this.height; graphics.text(this.font, "§7⚙", iconCenterX3, btnSettingsY + iconCenterYOffset, hovSettings ? 0xFFFFFFFF : -1);

        if (isSidebarOpen) {
            graphics.fill(actBarW, topY, leftW, this.height, 0xFF252526);
            if (activeActivity == 0) graphics.text(this.font, "资源管理器", actBarW + 10, topY + 10, 0xFFBBBBBB);
            else if (activeActivity == 1) graphics.text(this.font, "搜索与替换", actBarW + 10, topY + 10, 0xFFBBBBBB);

            if (fileListWidget != null) {
                int visibleW = sidebarW - 12;
                int maxScrollX = Math.max(0, maxListContentWidth - visibleW);

                if (maxScrollX > 0) {
                    int trackW = sidebarW - 6;
                    int handleW = Math.max(15, (int) ((visibleW / (float) maxListContentWidth) * trackW));
                    int handleX = actBarW + 2 + (int) ((listScrollX / maxScrollX) * (trackW - handleW));
                    int scrollY = fileListWidget.getY() + fileListWidget.getHeight() + 2;

                    graphics.fill(actBarW + 2, scrollY, actBarW + 2 + trackW, scrollY + 4, 0xFF1E1E1E);
                    graphics.fill(handleX, scrollY, handleX + handleW, scrollY + 4, 0xFF4A4A4A);
                }
            }
        }

        graphics.fill(leftW, topY, rightX, this.height, 0xFF1E1E1E);
        if (tabbedEditor != null && !tabbedEditor.isEmpty()) { tabbedEditor.renderUnderlay(graphics, mouseX, mouseY); } else { graphics.fill(rightX, topY, this.width, this.height, 0xFF1E1E1E); graphics.centeredText(this.font, "§7从左侧选择文件以开始编辑 (双击打开)", rightX + (this.width - rightX) / 2, this.height / 2, -1); }
        boolean blockEditorHover = isModalOpen || (activeContextMenu != null) || (tabbedEditor != null && tabbedEditor.isHoveringDropdown(mouseX, mouseY));

        super.extractRenderState(graphics, blockEditorHover ? -999 : mouseX, blockEditorHover ? -999 : mouseY, partialTick);

        String activeTooltip = null;
        if (isSidebarOpen && activeActivity == 1) {
            if (this.searchBox != null) this.searchBox.extractRenderState(graphics, mouseX, mouseY, partialTick);
            if (this.replaceBox != null) this.replaceBox.extractRenderState(graphics, mouseX, mouseY, partialTick);

            int inputWidth = sidebarW - 12 - 50;
            int iconStartX = actBarW + 6 + inputWidth + 4;

            int sy = topY + 24;
            activeTooltip = drawToggleIcon(graphics, "Aa", iconStartX, sy, isMatchCase, mouseX, mouseY, activeTooltip, "区分大小写");
            activeTooltip = drawToggleIcon(graphics, "\"\"", iconStartX + 16, sy, isMatchWord, mouseX, mouseY, activeTooltip, "全字匹配");
            activeTooltip = drawToggleIcon(graphics, ".*", iconStartX + 32, sy, isRegex, mouseX, mouseY, activeTooltip, "使用正则表达式");

            int ry = topY + 46;
            activeTooltip = drawToggleIcon(graphics, "AB", iconStartX, ry, isPreserveCase, mouseX, mouseY, activeTooltip, "保留大小写");
            activeTooltip = drawToggleIcon(graphics, "ALL", iconStartX + 20, ry, false, mouseX, mouseY, activeTooltip, "全部替换");
        }

        if (System.currentTimeMillis() < toastTime) { float yAnim = Mth.clamp((2000f - (toastTime - System.currentTimeMillis())) / 150f, 0, 1); int tw = this.font.width(toastMessage) + 20; int ty = this.height - 30 - (int)(15 * yAnim); graphics.fill(this.width/2-tw/2, ty, this.width/2+tw/2, ty+16, 0xCC000000); graphics.outline(this.width/2-tw/2, ty, tw, 16, 0xFF44FF44); graphics.centeredText(this.font, toastMessage, this.width / 2, ty + 4, -1); }
        if (this.activeContextMenu != null) this.activeContextMenu.render(graphics, mouseX, mouseY);

        if (this.isModalOpen) {
            int mw = 240; int mh = 80; int mx_ = this.width / 2 - mw / 2; int my_ = this.height / 2 - mh / 2;
            graphics.fill(0, 0, this.width, this.height, 0x88000000); graphics.fill(mx_, my_, mx_ + mw, my_ + mh, 0xFF1E1E1E); graphics.outline(mx_, my_, mw, mh, 0xFF454545); graphics.centeredText(this.font, modalTitle, this.width / 2, my_ + 8, 0xFFFFFFFF);
            if (this.modalInput != null) this.modalInput.extractRenderState(graphics, mouseX, mouseY, partialTick);
            int btnY = my_ + 55; boolean hovC = mouseX >= mx_ + mw - 110 && mouseX <= mx_ + mw - 60 && mouseY >= btnY && mouseY <= btnY + 16; graphics.fill(mx_ + mw - 110, btnY, mx_ + mw - 60, btnY + 16, hovC ? 0xFF007ACC : 0xFF094771); graphics.centeredText(this.font, "确定", mx_ + mw - 85, btnY + 4, 0xFFFFFFFF); boolean hovX = mouseX >= mx_ + mw - 55 && mouseX <= mx_ + mw - 10 && mouseY >= btnY && mouseY <= btnY + 16; graphics.fill(mx_ + mw - 55, btnY, mx_ + mw - 10, btnY + 16, hovX ? 0xFF555555 : 0xFF333333); graphics.centeredText(this.font, "取消", mx_ + mw - 32, btnY + 4, 0xFFFFFFFF);
        }

        if (activeTooltip != null) {
            int tw = this.font.width(activeTooltip) + 8;
            graphics.fill(mouseX + 10, mouseY + 10, mouseX + 10 + tw, mouseY + 24, 0xFF111111);
            graphics.outline(mouseX + 10, mouseY + 10, tw, 14, 0xFF454545);
            graphics.text(this.font, activeTooltip, mouseX + 14, mouseY + 13, 0xFFCCCCCC);
        }
    }

    private String drawToggleIcon(GuiGraphicsExtractor g, String text, int x, int y, boolean isActive, int mx, int my, String currentTooltip, String tooltipStr) {
        int w = this.font.width(text) + 4; int h = 18; boolean isHovered = mx >= x && mx <= x + w && my >= y && my <= y + h;
        if (isActive) { g.fill(x, y, x + w, y + h, 0xFF094771); g.outline(x, y, w, h, 0xFF007ACC); } else if (isHovered) { g.fill(x, y, x + w, y + h, 0xFF333333); }
        g.centeredText(this.font, text, x + w / 2, y + 5, isActive ? 0xFFFFFFFF : 0xFFAAAAAA);
        return isHovered ? tooltipStr : currentTooltip;
    }

    private void renderTopMenuBar(GuiGraphicsExtractor g, int mx, int my) { int curX = this.width - 40; if (tabbedEditor != null && !tabbedEditor.isEmpty() && tabbedEditor.getActiveTab() != null) { boolean isDirty = tabbedEditor.getActiveTab().editor.isDirty(); curX = renderTopButton(g, isDirty ? "§e[保存当前*]" : "§a[保存当前]", curX, mx, my, () -> doSaveTab(tabbedEditor.getActiveTab())); curX = renderTopButton(g, "§e[↑ 推送当前]", curX, mx, my, () -> { ClientPacketDistributor.sendToServer(new SaveScriptPacket(tabbedEditor.getActiveTab().path, tabbedEditor.getActiveTab().editor.getValue())); showToast("§e正在推送当前文件..."); }); } curX -= 10; curX = renderTopButton(g, "§b[↓↓ 拉取所有]", curX, mx, my, () -> { ClientPacketDistributor.sendToServer(new FetchAllScriptsRequestPacket()); showToast("§b请求覆盖拉取服务端所有代码..."); }); curX = renderTopButton(g, "§e[↑↑ 推送所有]", curX, mx, my, () -> { Map<String, String> localFiles = NekoJSNetwork.collectAllValidScripts(NekoJSPaths.ROOT); ClientPacketDistributor.sendToServer(new UploadAllScriptsPacket(localFiles)); showToast("§e正在打包推送所有本地代码..."); }); curX -= 10; curX = renderTopButton(g, "§7[打开本地目录]", curX, mx, my, () -> Util.getPlatform().openFile(NekoJSPaths.ROOT.toFile())); curX = renderTopButton(g, "§7[刷新列表]", curX, mx, my, () -> { scanLocalFiles(); refreshFileList(); showToast("§a本地文件列表已刷新"); }); }
    private int renderTopButton(GuiGraphicsExtractor g, String text, int x, int mx, int my, Runnable action) { int w = this.font.width(text); int targetX = x - w - 5; boolean hov = mx >= targetX && mx <= targetX + w && my >= 4 && my <= 16; g.text(this.font, text, targetX, 6, -1); if (hov) g.fill(targetX, 6 + this.font.lineHeight, targetX + w, 6 + this.font.lineHeight + 1, 0xFFFFFFFF); return targetX; }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (isModalOpen) { int mw = 240; int mh = 80; int mx_ = this.width / 2 - mw / 2; int my_ = this.height / 2 - mh / 2; int btnY = my_ + 55; if (event.button() == 0 && event.y() >= btnY && event.y() <= btnY + 16) { if (event.x() >= mx_ + mw - 110 && event.x() <= mx_ + mw - 60) { if (modalCallback != null) modalCallback.accept(modalInput.getValue()); closeModal(); return true; } if (event.x() >= mx_ + mw - 55 && event.x() <= mx_ + mw - 10) { closeModal(); return true; } } if (modalInput != null && modalInput.mouseClicked(event, false)) return true; return true; }
        if (activeContextMenu != null) { if (activeContextMenu.mouseClicked(event.x(), event.y(), event.button())) return true; activeContextMenu = null; return true; }
        if (event.y() >= 4 && event.y() <= 16) { int closeX = this.width - 20; if (event.x() >= closeX && event.x() <= closeX + 10) { this.onClose(); return true; } }

        if (event.x() >= 0 && event.x() <= actBarW && event.y() >= topY) {
            if (event.y() < topY + actBarW) { if (activeActivity == 0 && isSidebarOpen) isSidebarOpen = false; else { activeActivity = 0; isSidebarOpen = true; } buildWorkspaceLayout(); return true; }
            else if (event.y() < topY + actBarW * 2) { if (activeActivity == 1 && isSidebarOpen) isSidebarOpen = false; else { activeActivity = 1; isSidebarOpen = true; } buildWorkspaceLayout(); return true; }
        }

        if (isSidebarOpen && activeActivity == 1) {
            int inputWidth = sidebarW - 12 - 50;
            int iconStartX = actBarW + 6 + inputWidth + 4;
            int sy = topY + 24; int ry = topY + 46;

            if (event.y() >= sy && event.y() <= sy + 18) {
                if (event.x() >= iconStartX && event.x() <= iconStartX + this.font.width("Aa") + 4) { isMatchCase = !isMatchCase; buildSearchTree(); refreshFileList(); return true; }
                if (event.x() >= iconStartX + 16 && event.x() <= iconStartX + 16 + this.font.width("\"\"") + 4) { isMatchWord = !isMatchWord; buildSearchTree(); refreshFileList(); return true; }
                if (event.x() >= iconStartX + 32 && event.x() <= iconStartX + 32 + this.font.width(".*") + 4) { isRegex = !isRegex; buildSearchTree(); refreshFileList(); return true; }
            }
            if (event.y() >= ry && event.y() <= ry + 18) {
                if (event.x() >= iconStartX && event.x() <= iconStartX + this.font.width("AB") + 4) { isPreserveCase = !isPreserveCase; return true; }
                if (event.x() >= iconStartX + 20 && event.x() <= iconStartX + 20 + this.font.width("ALL") + 4) { doGlobalReplaceAll(); return true; }
            }
        }

        if (event.y() >= 0 && event.y() <= topY) { int curX = this.width - 40; if (tabbedEditor != null && !tabbedEditor.isEmpty() && tabbedEditor.getActiveTab() != null) { boolean isDirty = tabbedEditor.getActiveTab().editor.isDirty(); int cw2 = this.font.width(isDirty ? "§e[保存当前*]" : "§a[保存当前]"); curX -= (cw2 + 5); if (event.x() >= curX && event.x() <= curX + cw2) { doSaveTab(tabbedEditor.getActiveTab()); return true; } int cw3 = this.font.width("§e[↑ 推送当前]"); curX -= (cw3 + 5); if (event.x() >= curX && event.x() <= curX + cw3) { ClientPacketDistributor.sendToServer(new SaveScriptPacket(tabbedEditor.getActiveTab().path, tabbedEditor.getActiveTab().editor.getValue())); showToast("§e正在推送当前文件..."); return true; } } curX -= 10; int cw4 = this.font.width("§b[↓↓ 拉取所有]"); curX -= (cw4 + 5); if (event.x() >= curX && event.x() <= curX + cw4) { ClientPacketDistributor.sendToServer(new FetchAllScriptsRequestPacket()); showToast("§b请求覆盖拉取服务端所有代码..."); return true; } int cw5 = this.font.width("§e[↑↑ 推送所有]"); curX -= (cw5 + 5); if (event.x() >= curX && event.x() <= curX + cw5) { Map<String, String> localFiles = NekoJSNetwork.collectAllValidScripts(NekoJSPaths.ROOT); ClientPacketDistributor.sendToServer(new UploadAllScriptsPacket(localFiles)); showToast("§e正在打包推送所有本地代码..."); return true; } curX -= 10; int cw6 = this.font.width("§7[打开本地目录]"); curX -= (cw6 + 5); if (event.x() >= curX && event.x() <= curX + cw6) { Util.getPlatform().openFile(NekoJSPaths.ROOT.toFile()); return true; } int cw7 = this.font.width("§7[刷新列表]"); curX -= (cw7 + 5); if (event.x() >= curX && event.x() <= curX + cw7) { scanLocalFiles(); refreshFileList(); showToast("§a本地文件列表已刷新"); return true; } }

        boolean handled = super.mouseClicked(event, doubleClick);
        if (!handled && event.button() == 1 && isSidebarOpen && activeActivity == 0) { if (event.x() > actBarW && event.x() < leftW && event.y() > topY) { List<MenuItem> items = List.of( new MenuItem("📄 新建文件", () -> openModal("新建文件 (根目录)", name -> createNewItem("", name, false))), new MenuItem("📁 新建文件夹", () -> openModal("新建文件夹 (根目录)", name -> createNewItem("", name, true))), new MenuItem("🔄 刷新列表", () -> { scanLocalFiles(); refreshFileList(); showToast("§a刷新成功"); }) ); this.activeContextMenu = new ContextMenu((int)event.x(), (int)event.y(), items); return true; } }
        if (tabbedEditor != null && tabbedEditor.mouseClicked(event.x(), event.y(), event.button())) return true;
        return handled;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isModalOpen || activeContextMenu != null) return true;
        if (tabbedEditor != null && tabbedEditor.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) return true;

        if (isSidebarOpen && mouseX >= actBarW && mouseX <= leftW && mouseY >= topY) {
            if (hasShiftDown()) {
                double delta = (scrollX != 0 ? scrollX : scrollY) * 20;
                int visibleW = sidebarW - 12;
                int maxScrollX = Math.max(0, maxListContentWidth - visibleW);

                listScrollX -= delta;
                listScrollX = Mth.clamp(listScrollX, 0, maxScrollX);
                return true;
            }
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (isModalOpen) { if (event.key() == GLFW.GLFW_KEY_ESCAPE) { closeModal(); return true; } if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) { if (modalCallback != null) modalCallback.accept(modalInput.getValue()); closeModal(); return true; } return super.keyPressed(event); }
        if (this.activeContextMenu != null && event.isEscape()) { this.activeContextMenu = null; return true; }
        if (tabbedEditor != null && tabbedEditor.keyPressed(event)) return true;
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (isModalOpen) return super.charTyped(event);
        if (this.activeContextMenu != null) return false;
        if (tabbedEditor != null && tabbedEditor.charTyped((char) event.codepoint())) return true;
        return super.charTyped(event);
    }

    private static class FileNode { String name; String path; boolean isDir; int depth; boolean isExpanded = false; List<FileNode> children = new ArrayList<>(); public FileNode(String name, String path, boolean isDir, int depth) { this.name = name; this.path = path; this.isDir = isDir; this.depth = depth; } }

    private static class SearchMatchNode extends FileNode {
        List<int[]> highlights; int globalStart; int globalEnd;
        public SearchMatchNode(String lineText, String path, List<int[]> highlights, int globalStart, int globalEnd) {
            super(lineText, path, false, 1);
            this.highlights = highlights; this.globalStart = globalStart; this.globalEnd = globalEnd;
        }
    }

    private class FileListWidget extends ObjectSelectionList<FileEntry> {
        public FileListWidget(Minecraft mc, int w, int h, int y, int ih) { super(mc, w, h, y, ih); }
        @Override public int getRowWidth() { return this.width - 12; }
        @Override protected int scrollBarX() { return this.getX() + this.width - 6; }
        @Override protected void extractListBackground(GuiGraphicsExtractor g) {}
        @Override protected void extractListSeparators(GuiGraphicsExtractor g) {}
        @Override public int addEntry(FileEntry entry) { return super.addEntry(entry); }
        @Override public int getItemCount() { return super.getItemCount(); }
    }

    private class FileEntry extends ObjectSelectionList.Entry<FileEntry> {
        private final FileNode node; private final boolean isFlatSearch;
        public FileEntry(FileNode node, boolean isFlatSearch) { this.node = node; this.isFlatSearch = isFlatSearch; }

        @Override
        public void extractContent(GuiGraphicsExtractor g, int mx, int my, boolean isHovered, float pt) {
            boolean isSelected = false;

            if (activeActivity == 1) {
                if (node instanceof SearchMatchNode match) {
                    isSelected = node.path.equals(NekoWorkspaceScreen.this.selectedFilePath)
                            && match.globalStart == NekoWorkspaceScreen.this.selectedMatchStart;
                } else {
                    isSelected = node.path.equals(NekoWorkspaceScreen.this.selectedFilePath)
                            && NekoWorkspaceScreen.this.selectedMatchStart == -1;
                }
            } else {
                isSelected = node.path.equals(selectedFilePath);
                boolean isEditing = (tabbedEditor != null && tabbedEditor.getActiveTab() != null && tabbedEditor.getActiveTab().path.equals(node.path));
                if (isEditing) isSelected = true;
            }

            if (isSelected) g.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), 0xFF094771);
            else if (isHovered) g.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), 0x33FFFFFF);

            int scrollX = (int) NekoWorkspaceScreen.this.listScrollX;
            int baseRenderX = this.getX() - scrollX;

            int indent = (node.depth * 10 + 4);
            int color = isSelected ? 0xFFFFFFFF : 0xFFCCCCCC;

            if (node instanceof SearchMatchNode match) {
                int xOffset = baseRenderX + indent;
                int yOffset = this.getY() + 3;
                String text = match.name;
                int currentIndex = 0;

                for (int[] hl : match.highlights) {
                    int safeHl0 = Math.max(0, Math.min(hl[0], text.length()));
                    int safeHl1 = Math.max(0, Math.min(hl[1], text.length()));
                    int safeCurrent = Math.max(0, Math.min(currentIndex, text.length()));

                    if (safeHl0 > safeCurrent) {
                        String pre = text.substring(safeCurrent, safeHl0);
                        g.text(NekoWorkspaceScreen.this.font, pre, xOffset, yOffset, 0xFFCCCCCC);
                        xOffset += NekoWorkspaceScreen.this.font.width(pre);
                    }
                    if (safeHl1 > safeHl0) {
                        String hi = text.substring(safeHl0, safeHl1);
                        g.fill(xOffset, yOffset - 1, xOffset + NekoWorkspaceScreen.this.font.width(hi), yOffset + NekoWorkspaceScreen.this.font.lineHeight, 0x66FFAA00);
                        g.text(NekoWorkspaceScreen.this.font, hi, xOffset, yOffset, 0xFFFFCC00);
                        xOffset += NekoWorkspaceScreen.this.font.width(hi);
                    }
                    currentIndex = Math.max(currentIndex, safeHl1);
                }

                int safeCurrentFinal = Math.max(0, Math.min(currentIndex, text.length()));
                if (safeCurrentFinal < text.length()) {
                    String post = text.substring(safeCurrentFinal);
                    g.text(NekoWorkspaceScreen.this.font, post, xOffset, yOffset, 0xFFCCCCCC);
                }
            } else if (node.isDir) {
                String prefix = node.isExpanded ? "v" : ">";
                g.text(NekoWorkspaceScreen.this.font, prefix, baseRenderX + indent, this.getY() + 3, 0xFF888888);
                g.text(NekoWorkspaceScreen.this.font, node.name, baseRenderX + indent + 10, this.getY() + 3, color);
            } else {
                g.text(NekoWorkspaceScreen.this.font, node.name, baseRenderX + indent, this.getY() + 3, color);
            }
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (event.button() == 0) {
                if (node.isDir) {
                    int indent = (node.depth * 10 + 4);
                    if (event.x() < this.getX() - (int) NekoWorkspaceScreen.this.listScrollX + indent + 12) {
                        node.isExpanded = !node.isExpanded;
                        NekoWorkspaceScreen.this.selectedFilePath = node.path;
                        NekoWorkspaceScreen.this.selectedMatchStart = -1;
                    } else {
                        node.isExpanded = !node.isExpanded;
                        NekoWorkspaceScreen.this.selectedFilePath = node.path;
                        NekoWorkspaceScreen.this.selectedMatchStart = -1;
                    }
                    NekoWorkspaceScreen.this.refreshFileList();
                    return true;
                } else {
                    NekoWorkspaceScreen.this.selectedFilePath = node.path;

                    if (node instanceof SearchMatchNode match) {
                        NekoWorkspaceScreen.this.selectedMatchStart = match.globalStart;
                        NekoWorkspaceScreen.this.openFileInEditor(node.path, match.globalStart, match.globalEnd);
                    } else {
                        NekoWorkspaceScreen.this.selectedMatchStart = -1;
                        if (doubleClick) NekoWorkspaceScreen.this.openFileInEditor(node.path);
                    }
                    NekoWorkspaceScreen.this.refreshFileList();
                    return true;
                }
            } else if (event.button() == 1) {
                String targetDir = node.isDir ? node.path : getParentDir(node.path);
                String displayDir = targetDir.isEmpty() ? "根目录" : targetDir;
                List<MenuItem> items = List.of(
                        new MenuItem("📄 新建文件", () -> NekoWorkspaceScreen.this.openModal("新建文件 (" + displayDir + ")", name -> NekoWorkspaceScreen.this.createNewItem(targetDir, name, false))),
                        new MenuItem("📁 新建文件夹", () -> NekoWorkspaceScreen.this.openModal("新建文件夹 (" + displayDir + ")", name -> NekoWorkspaceScreen.this.createNewItem(targetDir, name, true))),
                        new MenuItem("🗑 删除 " + node.name, () -> NekoWorkspaceScreen.this.deleteItem(node.path))
                );
                NekoWorkspaceScreen.this.activeContextMenu = new ContextMenu((int)event.x(), (int)event.y(), items);
                return true;
            }
            return false;
        }
        @Override public Component getNarration() { return Component.literal(this.node.path); }
    }

    private class ContextMenu { int x, y, width, height; List<MenuItem> items; public ContextMenu(int sx, int sy, List<MenuItem> items) { this.items = items; this.height = items.size() * 18 + 6; int maxW = 120; for(MenuItem item : items) maxW = Math.max(maxW, NekoWorkspaceScreen.this.font.width(item.label) + 20); this.width = maxW; this.x = Math.min(sx, NekoWorkspaceScreen.this.width - width - 5); this.y = Math.min(sy, NekoWorkspaceScreen.this.height - height - 5); } public void render(GuiGraphicsExtractor g, int mx, int my) { g.fill(x, y, x+width, y+height, 0xFF18181B); g.outline(x, y, width, height, 0xFF3F3F46); for (int i = 0; i < items.size(); i++) { int iy = y + 3 + i * 18; boolean h = mx >= x && mx <= x+width && my >= iy && my < iy+18; if (h) g.fill(x+2, iy, x+width-2, iy+18, 0xFF27272A); g.text(NekoWorkspaceScreen.this.font, items.get(i).label, x+8, iy+5, h ? -1 : 0xFFA1A1AA); } } public boolean mouseClicked(double mx, double my, int b) { if (b == 0 && mx >= x && mx <= x+width && my >= y && my <= y+height) { int index = ((int)my - y - 3) / 18; if (index >= 0 && index < items.size()) items.get(index).action.run(); return true; } return false; } }
    private record MenuItem(String label, Runnable action) {}
}