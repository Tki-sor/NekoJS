package com.tkisor.nekojs.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.neoforged.fml.ModList;

public class NekoConditionEvaluator {

    /**
     * 计算 NeoForge 的 conditions 字段。
     * @return true 表示条件满足（或无条件），false 表示条件失败（缺少前置）
     */
    public static boolean test(JsonElement jsonElement) {
        if (!jsonElement.isJsonObject()) return true;
        JsonObject json = jsonElement.getAsJsonObject();

        if (json.has("neoforge:conditions")) {
            return testNeoForgeConditions(json.getAsJsonArray("neoforge:conditions"));
        }

        return true;
    }

    private static boolean testNeoForgeConditions(JsonArray conditions) {
        for (JsonElement elem : conditions) {
            if (!elem.isJsonObject()) continue;
            JsonObject cond = elem.getAsJsonObject();
            String type = cond.has("type") ? cond.get("type").getAsString() : "";

            if (type.equals("neoforge:mod_loaded")) {
                if (!ModList.get().isLoaded(cond.get("modid").getAsString())) return false;
            }

            else if (type.equals("neoforge:not")) {
                if (cond.has("value")) {
                    JsonObject value = cond.getAsJsonObject("value");
                    String valueType = value.has("type") ? value.get("type").getAsString() : "";

                    if (valueType.equals("neoforge:mod_loaded")) {
                        if (ModList.get().isLoaded(value.get("modid").getAsString())) return false;
                    }
                }
            }
        }

        return true;
    }
}