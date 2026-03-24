package com.tkisor.nekojs.wrapper.event.item;

import com.tkisor.nekojs.wrapper.entity.EntityJS;
import com.tkisor.nekojs.wrapper.entity.PlayerJS;
import com.tkisor.nekojs.wrapper.item.ItemStackJS;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

public class ItemConsumedEventJS {
    private final LivingEntityUseItemEvent.Finish rawEvent;

    public ItemConsumedEventJS(LivingEntityUseItemEvent.Finish rawEvent) {
        this.rawEvent = rawEvent;
    }

    // 获取被吃掉/喝掉的物品
    public ItemStackJS getItem() {
        return new ItemStackJS(rawEvent.getItem());
    }

    // 快捷获取物品 ID 进行判断
    public String getItemId() {
        return getItem().getId();
    }

    // 获取吃东西的人 (因为可能不是玩家，比如狐狸吃浆果，所以需要判空)
    public PlayerJS getPlayer() {
        if (rawEvent.getEntity() instanceof Player player) {
            return new PlayerJS(player);
        }
        return null;
    }

    public EntityJS getEntity() {
        return new EntityJS(rawEvent.getEntity());
    }
}