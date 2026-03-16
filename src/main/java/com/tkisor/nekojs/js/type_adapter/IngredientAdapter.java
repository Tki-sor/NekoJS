package com.tkisor.nekojs.js.type_adapter;

import com.tkisor.nekojs.NekoJS;
import com.tkisor.nekojs.api.JSTypeAdapter;
import com.tkisor.nekojs.wrapper.item.IngredientWrapper;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import org.graalvm.polyglot.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class IngredientAdapter implements JSTypeAdapter<Ingredient> {
    @Override
    public Class<Ingredient> getTargetClass() { return Ingredient.class; }

    @Override
    public boolean canConvert(Value value) {
        return value != null && (value.isString() || value.hasArrayElements() ||
                (value.isHostObject() && value.asHostObject() instanceof IngredientWrapper));
    }

    @Override
    public Ingredient convert(Value value) {
        if (value == null || value.isNull()) return fallback();

        if (value.isHostObject() && value.asHostObject() instanceof IngredientWrapper wrapper) {
            return wrapper.unwrap();
        }

        try {
            List<Holder<Item>> holders = new ArrayList<>();

            if (value.isString()) {
                resolveToHolders(value.asString(), holders);
            }
            else if (value.hasArrayElements()) {
                for (long i = 0; i < value.getArraySize(); i++) {
                    resolveToHolders(value.getArrayElement(i).asString(), holders);
                }
            }

            if (!holders.isEmpty()) {
                return Ingredient.of(HolderSet.direct(holders));
            }

        } catch (Exception e) {
            NekoJS.LOGGER.error("[NekoJS] 材料转换失败: {}", e.getMessage());
        }

        return fallback();
    }

    private void resolveToHolders(String rawId, List<Holder<Item>> out) {
        String id = formatId(rawId);
        Identifier loc = Identifier.tryParse(id.startsWith("#") ? id.substring(1) : id);
        if (loc == null) return;

        if (id.startsWith("#")) {
            TagKey<Item> tagKey = TagKey.create(Registries.ITEM, loc);
            BuiltInRegistries.ITEM.get(tagKey).ifPresent(holders -> {
                for (Holder<Item> h : holders) {
                    out.add(h);
                }
            });
        } else {
            Item item = BuiltInRegistries.ITEM.getValue(loc);

            if (item != null && item != Items.AIR) {
                out.add(item.builtInRegistryHolder());
            }
        }
    }

    private String formatId(String inputStr) {
        if (inputStr.startsWith("#")) {
            return inputStr.contains(":") ? inputStr : "#minecraft:" + inputStr.substring(1);
        } else {
            return inputStr.contains(":") ? inputStr : "minecraft:" + inputStr;
        }
    }

    private Ingredient fallback() {
        return Ingredient.of(Items.BARRIER);
    }
}