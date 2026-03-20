package com.tkisor.nekojs.api.event;

import com.tkisor.nekojs.script.ScriptType;
import com.tkisor.nekojs.script.ScriptTypeFilter;
import com.tkisor.nekojs.utils.event.dispatch.DispatchKey;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author ZZZank
 */
public class EventGroupJS implements ProxyObject {
    public static EventGroupJS of(String name) {
        return new EventGroupJS(name);
    }

    private final String name;
    private final Map<String, EventBusJS<?, ?>> buses;
    private final Map<String, ScriptTypeFilter> scriptTypeFilters;

    private EventGroupJS(String name) {
        this.name = Objects.requireNonNull(name);
        this.buses = new HashMap<>();
        this.scriptTypeFilters = new HashMap<>();
    }

    public String name() {
        return name;
    }

    public Map<String, EventBusJS<?, ?>> viewBuses() {
        return Collections.unmodifiableMap(buses);
    }

    public Map<String, ScriptTypeFilter> viewScriptTypeFilters() {
        return Collections.unmodifiableMap(scriptTypeFilters);
    }

    public <E> EventBusJS<E, ?> server(String name, Class<E> type) {
        return add(name, ScriptType.SERVER, EventBusJS.of(type));
    }

    public <E> EventBusJS<E, ?> client(String name, Class<E> type) {
        return add(name, ScriptType.CLIENT, EventBusJS.of(type));
    }

    public <E> EventBusJS<E, ?> startup(String name, Class<E> type) {
        return add(name, ScriptType.STARTUP, EventBusJS.of(type));
    }

    public <E> EventBusJS<E, ?> common(String name, Class<E> type) {
        return add(name, ScriptType.COMMON, EventBusJS.of(type));
    }

    public <E, K> EventBusJS<E, K> server(String name, Class<E> type, DispatchKey<E, K> dispatchKey) {
        return add(name, ScriptType.SERVER, EventBusJS.of(type, NekoCancellableEvent.testType(type), dispatchKey));
    }

    public <E, K> EventBusJS<E, K> client(String name, Class<E> type, DispatchKey<E, K> dispatchKey) {
        return add(name, ScriptType.CLIENT, EventBusJS.of(type, NekoCancellableEvent.testType(type), dispatchKey));
    }

    public <E, K> EventBusJS<E, K> startup(String name, Class<E> type, DispatchKey<E, K> dispatchKey) {
        return add(name, ScriptType.STARTUP, EventBusJS.of(type, NekoCancellableEvent.testType(type), dispatchKey));
    }

    public <E, K> EventBusJS<E, K> common(String name, Class<E> type, DispatchKey<E, K> dispatchKey) {
        return add(name, ScriptType.COMMON, EventBusJS.of(type, NekoCancellableEvent.testType(type), dispatchKey));
    }

    public <BUS extends EventBusJS<?, ?>> BUS add(String name, ScriptTypeFilter scriptTypeFilter, BUS bus) {
        if (name == null) {
            throw new IllegalArgumentException("name == null");
        } else if (this.buses.containsKey(name)) {
            throw new IllegalArgumentException(String.format("A bus with name '%s' has already been registered", name));
        } else if (scriptTypeFilter == null) {
            throw new IllegalArgumentException("scriptTypeFilter == null");
        }
        this.buses.put(name, bus);
        this.scriptTypeFilters.put(name, scriptTypeFilter);
        return bus;
    }

    // --- ProxyObject methods start ---

    @Override
    public Object getMember(String key) {
        return this.buses.get(key);
    }

    @Override
    public Object getMemberKeys() {
        return this.buses.keySet().toArray();
    }

    @Override
    public boolean hasMember(String key) {
        return this.buses.containsKey(key);
    }

    @Override
    public void putMember(String key, Value value) {
        throw new UnsupportedOperationException("putMember(...) not supported.");
    }
    // --- ProxyObject methods end ---
}
