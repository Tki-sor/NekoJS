package com.tkisor.nekojs.js.type_adapter;

import com.tkisor.nekojs.api.JSTypeAdapter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import graal.graalvm.polyglot.Value;

public class ItemAdapter implements JSTypeAdapter<Item> {
    @Override
    public Class<Item> getTargetClass() {
        return Item.class;
    }

    @Override
    public boolean canConvert(Value value) {
        if (value.isNull() || value.isString()) {
            return true;
        }
        if (value.isHostObject()) {
            Object obj = value.asHostObject();
            return obj instanceof Item || obj instanceof ItemStack || obj instanceof Block;
        }
        return false;
    }

    @Override
    public Item convert(Value value) {
        if (value.isNull()) {
            return Items.AIR;
        }

        if (value.isHostObject()) {
            Object obj = value.asHostObject();
            if (obj instanceof Item item) return item;
            if (obj instanceof ItemStack stack) return stack.getItem();
            // 1.21.1: 方块转物品应该使用 block.asItem()
            if (obj instanceof Block block) return block.asItem();
        }

        if (value.isString()) {
            String rawId = value.asString();

            if (rawId == null || rawId.trim().isEmpty() || rawId.startsWith("#")) {
                return Items.AIR;
            }

            String id = formatId(rawId);
            ResourceLocation loc = ResourceLocation.tryParse(id);

            if (loc != null) {
                // 1.21.1: getValue() 重命名为了 get()，并且如果找不到会自动返回 Items.AIR
                return BuiltInRegistries.ITEM.get(loc);
            }
        }

        return Items.AIR;
    }

    private String formatId(String inputStr) {
        if (inputStr.indexOf(':') != -1) {
            return inputStr;
        }
        return "minecraft:" + inputStr;
    }
}