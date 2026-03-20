package com.tkisor.nekojs.api.event;

import com.tkisor.nekojs.core.error.NekoErrorTracker;
import com.tkisor.nekojs.script.ScriptType;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class NekoJSEventBus {

    private static final Logger LOGGER = LoggerFactory.getLogger("NekoJS-EventBus");

    private record EventRegistration(
            ScriptType scriptType,
            String target,
            Value callback
    ) {}

    private static final Map<String, List<EventRegistration>> LISTENERS = new ConcurrentHashMap<>();

    private NekoJSEventBus() {}

    public static void register(String eventName, ScriptType scriptType, String target, Value callback) {
        LISTENERS.computeIfAbsent(eventName, k -> new CopyOnWriteArrayList<>())
                .add(new EventRegistration(scriptType, target, callback));
    }

    public static void post(String eventName, ScriptType currentEnv, Object eventObj) {
        postTargeted(eventName, currentEnv, null, eventObj);
    }

    public static void postTargeted(String eventName, ScriptType currentEnv, String eventTarget, Object eventObj) {
        List<EventRegistration> registrations = LISTENERS.get(eventName);
        if (registrations == null || registrations.isEmpty()) return;

        for (EventRegistration reg : registrations) {

            if (reg.scriptType() != ScriptType.COMMON && reg.scriptType() != currentEnv) {
                continue;
            }

            if (reg.target() != null && !reg.target().equals(eventTarget)) {
                continue;
            }

            try {
                reg.callback().executeVoid(eventObj);
            } catch (PolyglotException e) {
                LOGGER.error("脚本事件 [{}] 触发时发生错误: {}", eventName, e.getMessage());
                NekoErrorTracker.recordEventError(e);
            } catch (Exception e) {
                LOGGER.error("执行脚本事件 [{}] 时发生未知系统异常:", eventName, e);
            }
        }
    }

    public static void clearByType(ScriptType type) {
        for (List<EventRegistration> registrations : LISTENERS.values()) {
            registrations.removeIf(reg -> reg.scriptType() == type);
        }
    }
}