package com.tkisor.nekojs.api.recipe;

import com.google.gson.*;
import com.tkisor.nekojs.wrapper.event.server.RecipeEventJS;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;
import java.util.Map;

public class RecipeJsonBuilder {
    private final JsonObject json = new JsonObject();
    private final RecipeEventJS event;

    public RecipeJsonBuilder(RecipeEventJS event, String type) {
        this.event = event;
        json.addProperty("type", type);
    }

    public RecipeJsonBuilder input(String key, Ingredient ingredient) {
        if (ingredient != null) json.add(key, event.serializeIngredient(ingredient));
        return this;
    }

    public RecipeJsonBuilder output(String key, ItemStack result) {
        if (result != null) json.add(key, event.serializeResult(result));
        return this;
    }

    public RecipeJsonBuilder property(String key, Number value) {
        json.addProperty(key, value);
        return this;
    }

    public RecipeJsonBuilder property(String key, String value) {
        json.addProperty(key, value);
        return this;
    }

    public RecipeJsonBuilder property(String key, Boolean value) {
        json.addProperty(key, value);
        return this;
    }

    public RecipeJsonBuilder property(String key, JsonObject value) {
        json.add(key, value);
        return this;
    }

    public RecipeJsonBuilder property(String key, JsonArray value) {
        json.add(key, value);
        return this;
    }

    public RecipeJsonBuilder property(String key, List<?> value) {
        json.add(key, convertToJsonElement(value));
        return this;
    }

    public RecipeJsonBuilder property(String key, Map<String, ?> value) {
        json.add(key, convertToJsonElement(value));
        return this;
    }

    public RecipeJsonBuilder property(String key, Object value) {
        json.add(key, convertToJsonElement(value));
        return this;
    }

    private JsonElement convertToJsonElement(Object obj) {
        if (obj == null) return JsonNull.INSTANCE;

        if (obj instanceof JsonElement je) return je;

        if (obj instanceof Number n) return new JsonPrimitive(n);
        if (obj instanceof String s) return new JsonPrimitive(s);
        if (obj instanceof Boolean b) return new JsonPrimitive(b);
        if (obj instanceof Character c) return new JsonPrimitive(c);

        if (obj instanceof Ingredient ing) return event.serializeIngredient(ing);
        if (obj instanceof ItemStack stack) return event.serializeResult(stack);

        if (obj instanceof List<?> list) {
            JsonArray array = new JsonArray();
            for (Object item : list) {
                array.add(convertToJsonElement(item));
            }
            return array;
        }

        if (obj instanceof Map<?, ?> map) {
            JsonObject jsonObj = new JsonObject();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                // 🌟 递归调用！
                jsonObj.add(String.valueOf(entry.getKey()), convertToJsonElement(entry.getValue()));
            }
            return jsonObj;
        }

        return new JsonPrimitive(obj.toString());
    }

    public void register() {
        event.custom(json);
    }
}