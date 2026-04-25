package com.tkisor.nekojs.core;

import com.tkisor.nekojs.NekoJS;
import com.tkisor.nekojs.api.data.Binding;
import com.tkisor.nekojs.api.data.NekoBindings;
import com.tkisor.nekojs.api.event.NekoEventGroups;
import com.tkisor.nekojs.api.event.EventGroupJS;
import com.tkisor.nekojs.core.error.NekoErrorTracker;
import com.tkisor.nekojs.core.fs.NekoJSPaths;
import com.tkisor.nekojs.script.ScriptContainer;
import com.tkisor.nekojs.script.ScriptType;
import com.tkisor.nekojs.script.ScriptTypedValue;
import com.tkisor.nekojs.script.prop.ScriptProperty;
import com.tkisor.nekojs.script.prop.ScriptPropertyRegistry;
import graal.graalvm.polyglot.Context;
import graal.graalvm.polyglot.PolyglotException;
import graal.graalvm.polyglot.Value;

import java.nio.file.Path;
import java.util.*;

/**
 * NekoJS 脚本引擎核心生命周期调度器
 */
public final class NekoJSScriptManager {

    // 修复点：明确指定泛型为 <ScriptType, Context> 和 <ScriptType, List<ScriptContainer>>
    private final ScriptTypedValue<ScriptType, Context> contexts =
            ScriptTypedValue.ofNullable(ScriptType.values(), this::initContext);

    private final ScriptTypedValue<ScriptType, List<ScriptContainer>> scripts =
            ScriptTypedValue.of(ScriptType.values(), type -> new ArrayList<>());

    private final ScriptPropertyRegistry scriptPropertyRegistry = new ScriptPropertyRegistry.Impl();

    private static final Map<Context, ScriptType> CONTEXT_TYPE_MAP = Collections.synchronizedMap(new WeakHashMap<>());

    public NekoJSScriptManager() {
    }

    public void registerScriptProperty() {
        for (var plugin : NekoJSPluginManager.getPlugins()) {
            plugin.registerScriptProperty(scriptPropertyRegistry);
        }
    }

    public void discoverScripts() {
        for (ScriptType type : ScriptType.all()) {
            discoverScripts(type);
        }
    }

    public void discoverScripts(ScriptType type) {
        List<ScriptContainer> discovered = ScriptLocator.discover(type, scriptPropertyRegistry);
        scripts.set(type, discovered);
        type.logger().info("发现了 {} 个 {} 脚本。", discovered.size(), type.name());
    }

    public void loadScripts(ScriptType type) {
        List<ScriptContainer> scriptList = this.scripts.at(type);
        Context ctx = contexts.at(type);

        for (var script : scriptList) {
            script.preload();
        }

        scriptList.sort((s1, s2) -> {
            int p1 = s1.properties.getOrDefault(ScriptProperty.PRIORITY);
            int p2 = s2.properties.getOrDefault(ScriptProperty.PRIORITY);
            return Integer.compare(p2, p1);
        });

        for (ScriptContainer script : scriptList) {
            if (script.shouldRun()) {
                runScript(ctx, script);
            }
        }
    }

    private Context initContext(ScriptType type) {
        Context ctx = NekoSandboxBuilder.build(type);
        CONTEXT_TYPE_MAP.put(ctx, type);

        var bindings = ctx.getBindings("js");
        var values = NekoEventGroups.all().values();

        NekoJS.LOGGER.info("正在为 {} 注册 {} 个事件组...", type.name(), values.size());
        for (var group : values) {
            bindings.putMember(group.name(), new EventGroupJS(group, type));
        }

        Map<String, Binding> environmentBindings = NekoBindings.getFor(type);
        environmentBindings.forEach((name, binding) -> {
            Object obj = binding.getObject();
            if (binding.isStaticClass()) {
                Value javaType = bindings.getMember("Java").invokeMember("type", ((Class<?>) obj).getName());
                bindings.putMember(name, javaType);
            } else {
                bindings.putMember(name, obj);
            }
        });

        return ctx;
    }

    private void runScript(Context ctx, ScriptContainer script) {
        try {
            Path relativePath = NekoJSPaths.ROOT.relativize(script.path);
            String requirePath = "./" + relativePath.toString().replace("\\", "/");

            ctx.eval("js", "require").execute(requirePath);

            NekoErrorTracker.clear(script.id);
            script.disabled = false;
            script.lastError = null;

        } catch (Throwable t) {
            script.disabled = true;
            script.lastError = t;
            NekoErrorTracker.record(script, t);

            if (t instanceof PolyglotException polyglotException) {
                String cleanTrace = NekoErrorTracker.getMappedStackTrace(polyglotException);
                script.type.logger().error("脚本执行失败: {}\n{}", script.id.toString(), cleanTrace);
            } else {
                script.type.logger().error("脚本内部环境崩溃: {}", script.id.toString(), t);
            }
        }
    }

    public void reloadScripts(ScriptType type) {
        type.logger().info("正在重载 {} 脚本...", type.name());

        for (var group : NekoEventGroups.all().values()) {
            group.clearListeners(type);
        }

        Context oldContext = contexts.set(type, null);
        if (oldContext != null) {
            try {
                oldContext.close();
            } catch (Exception e) {
                type.logger().warn("关闭旧上下文时发生异常", e);
            }
        }

        discoverScripts(type);
        loadScripts(type);
        type.logger().info("{} 脚本重载完毕。", type.name());
    }

    public boolean hasScripts(ScriptType type) {
        List<ScriptContainer> typeScripts = scripts.at(type);
        return typeScripts != null && !typeScripts.isEmpty();
    }

    public static ScriptType getTypeFromContext(Context context) {
        return CONTEXT_TYPE_MAP.get(context);
    }
}