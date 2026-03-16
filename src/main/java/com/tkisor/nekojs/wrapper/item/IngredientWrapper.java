package com.tkisor.nekojs.wrapper.item;

import com.tkisor.nekojs.wrapper.NekoWrapper;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.List;

public class IngredientWrapper implements NekoWrapper<Ingredient> {

    private final List<String> rawIds = new ArrayList<>();

    public IngredientWrapper(String... ids) {
        for (String id : ids) {
            this.rawIds.add(formatId(id));
        }
    }

    public IngredientWrapper or(String id) {
        this.rawIds.add(formatId(id));
        return this;
    }

    public IngredientWrapper or(IngredientWrapper other) {
        this.rawIds.addAll(other.rawIds);
        return this;
    }

    private String formatId(String inputStr) {
        if (inputStr == null || inputStr.isBlank()) {
            throw new IllegalArgumentException("[NekoJS] 材料 ID 不能为空！");
        }
        if (inputStr.startsWith("#")) {
            return inputStr.contains(":") ? inputStr : "#minecraft:" + inputStr.substring(1);
        } else {
            return inputStr.contains(":") ? inputStr : "minecraft:" + inputStr;
        }
    }

    @Override
    public Ingredient unwrap() {
        if (this.rawIds.isEmpty()) return Ingredient.of(Items.BARRIER);

        List<Holder<Item>> allHolders = new java.util.ArrayList<>();

        for (String rawId : this.rawIds) {
            boolean isTag = rawId.startsWith("#");
            Identifier loc = Identifier.tryParse(isTag ? rawId.substring(1) : rawId);
            if (loc == null) continue;

            if (isTag) {
                TagKey<Item> tagKey =
                        TagKey.create(Registries.ITEM, loc);

                BuiltInRegistries.ITEM.get(tagKey).ifPresent(holders -> {
                    for (net.minecraft.core.Holder<net.minecraft.world.item.Item> h : holders) {
                        allHolders.add(h);
                    }
                });
            } else {
                Item item = BuiltInRegistries.ITEM.getValue(loc);
                if (item != Items.AIR) {
                    allHolders.add(item.builtInRegistryHolder());
                }
            }
        }

        if (allHolders.isEmpty()) {
            return Ingredient.of(Items.BARRIER);
        }

        return Ingredient.of(HolderSet.direct(allHolders));
    }
}