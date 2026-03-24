package com.tkisor.nekojs.wrapper.event.item;

import com.tkisor.nekojs.wrapper.entity.PlayerJS;
import com.tkisor.nekojs.wrapper.item.ItemStackJS;
import net.minecraft.world.Container;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public class ItemCraftedEventJS {

    private final PlayerEvent.ItemCraftedEvent rawEvent;

    public ItemCraftedEventJS(PlayerEvent.ItemCraftedEvent rawEvent) {
        this.rawEvent = rawEvent;
    }

    /**
     * 获取合成该物品的玩家
     * JS 侧: event.player
     */
    public PlayerJS getPlayer() {
        return new PlayerJS(rawEvent.getEntity());
    }

    /**
     * 获取合成出来的物品堆 (可以修改数量或 NBT)
     * JS 侧: event.item
     */
    public ItemStackJS getItem() {
        return new ItemStackJS(rawEvent.getCrafting());
    }

    /**
     * 获取合成网格的容器 (用于读取消耗的材料)
     * JS 侧: event.inventory
     */
    public Container getInventory() {
        return rawEvent.getInventory();
    }
}