package com.tkisor.nekojs.wrapper.registry;

import lombok.Getter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

import java.util.function.Consumer;

public class ItemBuilderJS {
    @Getter
    private final ResourceLocation location;

    private int maxStackSize = 64;
    private int maxDamage = 0;
    private boolean fireResistant = false;
    private Rarity rarity = Rarity.COMMON;
    private boolean glowing = false;

    private FoodBuilderJS foodBuilder = null;

    public ItemBuilderJS(ResourceLocation location) {
        this.location = location;
    }

    public ItemBuilderJS maxStackSize(int size) { this.maxStackSize = size; return this; }
    public ItemBuilderJS maxDamage(int damage) { this.maxDamage = damage; return this; }
    public ItemBuilderJS fireResistant() { this.fireResistant = true; return this; }

    public ItemBuilderJS rarity(String rarityStr) {
        this.rarity = switch (rarityStr.toLowerCase()) {
            case "uncommon" -> Rarity.UNCOMMON;
            case "rare" -> Rarity.RARE;
            case "epic" -> Rarity.EPIC;
            default -> Rarity.COMMON;
        };
        return this;
    }

    public ItemBuilderJS glowing() { this.glowing = true; return this; }

    public ItemBuilderJS food(Consumer<FoodBuilderJS> consumer) {
        this.foodBuilder = new FoodBuilderJS();
        consumer.accept(this.foodBuilder);
        return this;
    }

    public Item createItem() {
        // 1.21.1: 直接使用 new Item.Properties()，不需要 setId()
        Item.Properties props = new Item.Properties();

        if (maxDamage > 0) {
            props.durability(maxDamage);
        } else {
            props.stacksTo(maxStackSize);
        }

        if (fireResistant) props.fireResistant();
        if (rarity != Rarity.COMMON) props.rarity(rarity);

        if (foodBuilder != null) {
            // 1.21.1 中食物相关属性全部由 food 囊括
            props.food(foodBuilder.buildFood());

            // 移除了 1.21.2+ 的 CONSUMABLE 组件逻辑，因为在 FoodBuilderJS 中已经统一处理了
        }

        if (glowing) {
            return new Item(props) {
                @Override
                public boolean isFoil(ItemStack stack) {
                    return true;
                }
            };
        }

        return new Item(props);
    }
}