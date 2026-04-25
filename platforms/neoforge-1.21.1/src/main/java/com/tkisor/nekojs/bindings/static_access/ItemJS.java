package com.tkisor.nekojs.bindings.static_access;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Optional;

public class ItemJS {

    /**
     * 核心工厂方法
     * @param id 物品 ID (如 "minecraft:stone")
     * @param count 数量
     * @return ItemStack
     */
    public ItemStack of(String id, int count) {
        ResourceLocation location = ResourceLocation.parse(id);

        // 1.21.1: 使用 getOptional 返回 Optional<Item>
        Optional<Item> itemOpt = BuiltInRegistries.ITEM.getOptional(location);

        if (itemOpt.isEmpty() && !id.equals("minecraft:air")) {
            throw new IllegalArgumentException("Invalid item id '" + id + "'");
        }

        // 获取 Item 实例构建 ItemStack，兜底使用 AIR
        return new ItemStack(itemOpt.orElse(Items.AIR), count);
    }

    /**
     * 简易重载：默认数量为 1
     */
    public ItemStack of(String id) {
        return of(id, 1);
    }
}