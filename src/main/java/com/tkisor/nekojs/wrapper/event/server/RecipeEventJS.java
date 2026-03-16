package com.tkisor.nekojs.wrapper.event.server;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.JsonOps;
import com.tkisor.nekojs.NekoJS;
import com.tkisor.nekojs.api.recipe.RecipeFilter;
import com.tkisor.nekojs.bindings.event.NekoEvent;
import com.tkisor.nekojs.wrapper.item.ItemStackWrapper;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.*;
import org.graalvm.polyglot.Value;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class RecipeEventJS implements NekoEvent {

    private final List<RecipeHolder<?>> holders;
    private final HolderLookup.Provider registries;
    private int recipeCounter = 0;

    public RecipeEventJS(Collection<RecipeHolder<?>> originalHolders, HolderLookup.Provider registries) {
        this.holders = new ArrayList<>(originalHolders);
        this.registries = registries;
    }

    // --- 📥 核心序列化助手 (利用原版 CODEC 保证 100% 准确) ---

    private JsonElement serializeResult(ItemStackWrapper wrapper) {
        ItemStack stack = wrapper.unwrap();
        // 自动处理 DataComponents (1.21.1 的附魔、名字、Lore 等)
        return ItemStack.CODEC.encodeStart(registries.createSerializationContext(JsonOps.INSTANCE), stack)
                .getOrThrow(JsonParseException::new);
    }

    private JsonElement serializeIngredient(Ingredient ingredient) {
        // 自动处理物品、标签以及多物品 OR 逻辑
        return Ingredient.CODEC.encodeStart(registries.createSerializationContext(JsonOps.INSTANCE), ingredient)
                .getOrThrow(JsonParseException::new);
    }

    // --- 🛠️ 内部注册逻辑 ---

    private void register(JsonObject json, String prefix) {
        Identifier loc = Identifier.fromNamespaceAndPath("nekojs", prefix + "_" + (recipeCounter++));
        ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, loc);
        try {
            // 将拼装好的 Json 对象转换为真正的配方实例
            Recipe<?> recipe = Recipe.CODEC.parse(registries.createSerializationContext(JsonOps.INSTANCE), json)
                    .getOrThrow(JsonParseException::new);
            holders.add(new RecipeHolder<>(key, recipe));
        } catch (Exception e) {
            NekoJS.LOGGER.error("[NekoJS] 动态配方构建失败 ({}): {} \nJSON: {}", prefix, e.getMessage(), json);
        }
    }

    // --- 📜 有序合成 (Shaped) ---

    public void shaped(ItemStackWrapper result, Value pattern, Value keys) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "minecraft:crafting_shaped");

        // 1. 处理形状
        JsonArray patternArray = new JsonArray();
        for (int i = 0; i < pattern.getArraySize(); i++) {
            patternArray.add(pattern.getArrayElement(i).asString());
        }
        json.add("pattern", patternArray);

        // 2. 处理材料映射 (利用适配器自动转换)
        JsonObject keyObj = new JsonObject();
        for (String key : keys.getMemberKeys()) {
            Ingredient ing = keys.getMember(key).as(Ingredient.class);
            // ✨ 这里也要用 .add()
            keyObj.add(key, serializeIngredient(ing));
        }
        json.add("key", keyObj);

        // 3. 处理产出
        json.add("result", serializeResult(result));

        register(json, "shaped");
    }

    // --- 🌀 无序合成 (Shapeless) ---

    public void shapeless(ItemStackWrapper result, List<Ingredient> ingredients) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "minecraft:crafting_shapeless");

        JsonArray ingredientsArray = new JsonArray();
        if (ingredients != null) {
            for (Ingredient ing : ingredients) {
                ingredientsArray.add(serializeIngredient(ing));
            }
        }

        json.add("ingredients", ingredientsArray);
        json.add("result", serializeResult(result));

        register(json, "shapeless");
    }

    // --- 🍳 烹饪类配方 (熔炉、高炉、烟熏炉、营火) ---

    private void createCookingRecipe(String type, ItemStackWrapper result, Ingredient ingredient, float xp, int cookTime, String prefix) {
        JsonObject json = new JsonObject();
        json.addProperty("type", type);

        json.add("ingredient", serializeIngredient(ingredient));
        json.add("result", serializeResult(result));

        json.addProperty("experience", xp);
        json.addProperty("cookingtime", cookTime);

        register(json, prefix);
    }

    public void smelting(ItemStackWrapper result, Ingredient ingredient) {
        createCookingRecipe("minecraft:smelting", result, ingredient, 0.1f, 200, "smelting");
    }

    public void smelting(ItemStackWrapper result, Ingredient ingredient, float xp, int cookTime) {
        createCookingRecipe("minecraft:smelting", result, ingredient, xp, cookTime, "smelting");
    }

    public void blasting(ItemStackWrapper result, Ingredient ingredient) {
        createCookingRecipe("minecraft:blasting", result, ingredient, 0.1f, 100, "blasting");
    }

    public void blasting(ItemStackWrapper result, Ingredient ingredient, float xp, int cookTime) {
        createCookingRecipe("minecraft:blasting", result, ingredient, xp, cookTime, "blasting");
    }

    public void smoking(ItemStackWrapper result, Ingredient ingredient) {
        createCookingRecipe("minecraft:smoking", result, ingredient, 0.1f, 100, "smoking");
    }

    public void campfireCooking(ItemStackWrapper result, Ingredient ingredient) {
        createCookingRecipe("minecraft:campfire_cooking", result, ingredient, 0.1f, 600, "campfire");
    }

    // --- 🧹 移除逻辑 ---

    public void remove(RecipeFilter filter) {
        if (filter == null) return;
        int before = holders.size();
        holders.removeIf(holder -> filter.test(holder, this.registries));
        int removed = before - holders.size();
        NekoJS.LOGGER.info("[NekoJS] 过滤器匹配并移除了 {} 个配方", removed);
    }

    // --- 🔍 工具方法 ---

    public RecipeMap getFinalMap() {
        return RecipeMap.create(this.holders);
    }

    public static String getRecipeOutputId(Recipe<?> recipe) {
        if (recipe instanceof ShapedRecipe shaped) return getIdFromTemplate(shaped.result);
        if (recipe instanceof ShapelessRecipe shapeless) return getIdFromTemplate(shapeless.result);
        if (recipe instanceof SingleItemRecipe single) return getIdFromTemplate(single.result());
        if (recipe instanceof AbstractCookingRecipe cooking) return getIdFromTemplate(cooking.result);
        return null;
    }

    private static String getIdFromTemplate(ItemStackTemplate template) {
        return BuiltInRegistries.ITEM.getKey(template.item().value()).toString();
    }

    public static List<Ingredient> getIngredients(Recipe<?> recipe) {
        if (recipe instanceof ShapedRecipe shaped) {
            return shaped.getIngredients().stream().flatMap(Optional::stream).collect(Collectors.toList());
        }
        if (recipe instanceof ShapelessRecipe shapeless) return shapeless.ingredients;
        if (recipe instanceof SingleItemRecipe single) return List.of(single.input());
        if (recipe instanceof AbstractCookingRecipe cooking) return List.of(cooking.input());
        return List.of();
    }
}