package com.tkisor.nekojs.api.data;

import com.tkisor.nekojs.core.NekoJSPluginManager;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class NekoBindings {
    private static final Map<String, Binding> BINDINGS = new LinkedHashMap<>();
    private static boolean initialized = false;

    private NekoBindings() {}

    static void register(Binding binding) {
        BINDINGS.put(binding.getName(), binding);
    }

    public static synchronized Map<String, Binding> all() {
        if (!initialized) {
            initialize();
        }
        return Collections.unmodifiableMap(BINDINGS);
    }

    private static void initialize() {
        NekoJSPluginManager.getPlugins().forEach(plugin -> plugin.registerBindings(NekoBindings::register));
        initialized = true;
    }
}