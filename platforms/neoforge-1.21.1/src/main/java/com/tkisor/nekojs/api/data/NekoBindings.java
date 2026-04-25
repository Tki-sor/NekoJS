package com.tkisor.nekojs.api.data;

import com.tkisor.nekojs.core.NekoJSPluginManager;
import com.tkisor.nekojs.script.ScriptType;
import com.tkisor.nekojs.script.ScriptTypedValue;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

import java.util.*;

public final class NekoBindings {
    private static final List<Binding> RAW_BINDINGS = new ArrayList<>();

    // 【修改点 1】: 泛型参数改为两个。第一个是枚举类型 ScriptType，第二个是存储的 Map 类型
    private static final ScriptTypedValue<ScriptType, Map<String, Binding>> ENVIRONMENT_BINDINGS =
            ScriptTypedValue.of(ScriptType.values(), type -> new LinkedHashMap<>());

    private static boolean initialized = false;

    private NekoBindings() {}

    static void register(Binding binding) {
        String name = binding.getName();
        ScriptType type = binding.scriptType();

        for (Binding existing : RAW_BINDINGS) {
            if (existing.getName().equals(name)) {
                ScriptType existingType = existing.scriptType();

                if (type == ScriptType.COMMON || existingType == ScriptType.COMMON || type == existingType) {
                    String newClassPath = binding.getType().getName();
                    String existingClassPath = existing.getType().getName();

                    throw new IllegalArgumentException(
                            "Duplicate binding name: '" + name + "'\n" +
                                    " -> New: [" + type.name() + "] (" + newClassPath + ")\n" +
                                    " -> Existing: [" + existingType.name() + "] (" + existingClassPath + ")\n" +
                                    "Possible plugin conflict or duplicate registration."
                    );
                }
            }
        }
        RAW_BINDINGS.add(binding);
    }

    /**
     * 获取当前环境的绑定集合
     */
    public static synchronized Map<String, Binding> getFor(ScriptType type) {
        if (!initialized) {
            initialize();
        }
        // 【修改点 2】: 这里的 at(type) 会根据新的泛型返回 Map<String, Binding>，无需强转
        return Collections.unmodifiableMap(ENVIRONMENT_BINDINGS.at(type));
    }

    private static void initialize() {
        var plugins = NekoJSPluginManager.getPlugins();

        plugins.forEach(plugin -> plugin.registerBindings(binding -> register((Binding) binding)));

        if (FMLEnvironment.dist == Dist.CLIENT) {
            plugins.forEach(plugin -> plugin.registerClientBindings(binding -> register((Binding) binding)));
        }

        for (Binding binding : RAW_BINDINGS) {
            for (ScriptType envType : ScriptType.all()) {
                if (binding.canApplyOn(envType) || binding.scriptType() == ScriptType.COMMON) {
                    // 【修改点 3】: ENVIRONMENT_BINDINGS.at(envType) 现在能正确识别为 Map
                    ENVIRONMENT_BINDINGS.at(envType).put(binding.getName(), binding);
                }
            }
        }

        RAW_BINDINGS.clear();
        initialized = true;
    }
}