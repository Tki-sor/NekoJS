package com.tkisor.nekojs.wrapper.event.item;

import com.tkisor.nekojs.wrapper.entity.EntityWrapper;
import com.tkisor.nekojs.wrapper.entity.PlayerWrapper;
import com.tkisor.nekojs.wrapper.item.ItemStackWrapper;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

public class ItemConsumedEventJS {
    private final LivingEntityUseItemEvent.Finish rawEvent;

    public ItemConsumedEventJS(LivingEntityUseItemEvent.Finish rawEvent) {
        this.rawEvent = rawEvent;
    }

    // 获取被吃掉/喝掉的物品
    public ItemStackWrapper getItem() {
        return new ItemStackWrapper(rawEvent.getItem());
    }

    // 快捷获取物品 ID 进行判断
    public String getItemId() {
        return getItem().getId();
    }

    // 获取吃东西的人 (因为可能不是玩家，比如狐狸吃浆果，所以需要判空)
    public PlayerWrapper getPlayer() {
        if (rawEvent.getEntity() instanceof Player player) {
            return new PlayerWrapper(player);
        }
        return null;
    }

    public EntityWrapper getEntity() {
        return new EntityWrapper(rawEvent.getEntity());
    }
}