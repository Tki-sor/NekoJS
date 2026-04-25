package com.tkisor.nekojs.client.gui.components;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;
import java.util.List;

public class NekoMenuBar {
    public record MenuCategory(String name, List<NekoContextMenu.MenuItem> items) {}

    private final List<MenuCategory> categories;
    private NekoContextMenu activeContextMenu = null;

    public NekoMenuBar(List<MenuCategory> categories) {
        this.categories = categories;
    }

    // 1.21.1: 参数改为 GuiGraphics
    public void render(GuiGraphics g, Font font, int mouseX, int mouseY, int startX, int startY) {
        int menuX = startX;
        for (MenuCategory cat : categories) {
            int w = font.width(cat.name());
            boolean hov = mouseX >= menuX && mouseX <= menuX + w + 8 && mouseY >= startY && mouseY <= startY + 14;
            if (hov && this.activeContextMenu == null) {
                g.fill(menuX, startY, menuX + w + 8, startY + 14, 0xFF3E3E42);
            }
            // 1.21.1: 替换为 drawString，false 代表不带文字阴影
            g.drawString(font, cat.name(), menuX + 4, startY + 3, 0xFFCCCCCC, false);
            menuX += w + 8;
        }
        if (this.activeContextMenu != null) {
            this.activeContextMenu.render(g, mouseX, mouseY);
        }
    }

    public boolean mouseClicked(double mx, double my, int button, Font font, int screenW, int screenH, int startX, int startY) {
        if (this.activeContextMenu != null) {
            this.activeContextMenu.mouseClicked(mx, my, button);
            this.activeContextMenu = null;
            return true;
        }
        if (my >= startY && my <= startY + 14) {
            int menuX = startX;
            for (MenuCategory cat : categories) {
                int w = font.width(cat.name()) + 8;
                if (mx >= menuX && mx <= menuX + w) {
                    this.activeContextMenu = new NekoContextMenu(font, menuX, startY + 14, screenW, screenH, cat.items());
                    return true;
                }
                menuX += w;
            }
        }
        return false;
    }

    // 1.21.1: 将 KeyEvent 替换为底层按键参数，并使用 GLFW 常量判断 ESC
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.activeContextMenu != null && keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.activeContextMenu = null;
            return true;
        }
        return false;
    }

    public boolean isMenuOpen() {
        return this.activeContextMenu != null;
    }
}