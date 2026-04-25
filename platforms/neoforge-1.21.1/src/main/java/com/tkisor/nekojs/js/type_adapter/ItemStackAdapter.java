package com.tkisor.nekojs.js.type_adapter;

import com.tkisor.nekojs.api.JSTypeAdapter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import graal.graalvm.polyglot.Value;

import java.util.Optional;

public final class ItemStackAdapter implements JSTypeAdapter<ItemStack> {

    @Override
    public Class<ItemStack> getTargetClass() {
        return ItemStack.class;
    }

    @Override
    public boolean canConvert(Value value) {
        if (value.isNull()) {
            return true;
        }
        if (value.isString()) {
            return true;
        }
        if (value.isHostObject()) {
            Object obj = value.asHostObject();
            return obj instanceof ItemStack;
        }
        return false;
    }

    @Override
    public ItemStack convert(Value value) {
        if (value.isNull()) {
            return ItemStack.EMPTY;
        }

        if (value.isString()) {
            return stringToItemStack(value.asString());
        }

        if (value.isHostObject()) {
            Object obj = value.asHostObject();

            if (obj instanceof ItemStack stack) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }

    /**
     * 将字符串转换为 ItemStack。
     * 支持 "Nx item_id" 格式（例如 "2x minecraft:stick"），默认数量为 1。
     */
    static ItemStack stringToItemStack(String str) {
        if (str == null || str.trim().isEmpty()) return ItemStack.EMPTY;

        int count = 1;
        str = str.trim();

        if (str.matches("^(\\d+)x\\s+(\\S+)$")) {
            int xIndex = str.indexOf('x');
            try {
                count = Integer.parseInt(str.substring(0, xIndex).trim());
                str = str.substring(xIndex + 1).trim();
            } catch (NumberFormatException e) {
                count = 1;
            }
        }

        // 容错：如果没有冒号，自动补全 minecraft: 命名空间
        if (!str.contains(":")) {
            str = "minecraft:" + str;
        }

        ResourceLocation id = ResourceLocation.tryParse(str);
        if (id == null) {
            return ItemStack.EMPTY;
        }

        // 1.21.1: getValue() 已经被替换为 getOptional() 或 get()
        Optional<Item> itemOpt = BuiltInRegistries.ITEM.getOptional(id);

        if (itemOpt.isEmpty() && !id.getPath().equals("air")) {
            return ItemStack.EMPTY;
        }

        try {
            return new ItemStack(itemOpt.orElse(Items.AIR), count);
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }
}