package com.tkisor.nekojs.api.event;

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

    private EventGroupJS(String name) {
        this.name = Objects.requireNonNull(name);
        this.buses = new HashMap<>();
    }

    public String name() {
        return name;
    }

    public Map<String, EventBusJS<?, ?>> viewBuses() {
        return Collections.unmodifiableMap(buses);
    }

    public <E, K> EventBusJS<E, K> addBus(String name, EventBusJS<E, K> bus) {
        return addBusImpl(name, bus);
    }

    public <E> EventBusJS<E, Void> addBus(String name, Class<E> eventType) {
        return addBusImpl(name, EventBusJS.of(eventType));
    }

    public <E> EventBusJS<E, Void> addBus(String name, Class<E> eventType, boolean cancellable) {
        return addBusImpl(name, cancellable ? EventBusJS.ofCancellable(eventType) : EventBusJS.of(eventType));
    }

    private <BUS extends EventBusJS<?, ?>> BUS addBusImpl(String name, BUS bus) {
        if (name == null) {
            throw new IllegalArgumentException("'name' should not be null");
        } else if (this.buses.containsKey(name)) {
            throw new IllegalArgumentException(String.format("A bus with name '%s' has already been registered", name));
        }
        this.buses.put(name, bus);
        return bus;
    }

    private static <E> void unregisterBus(EventBusJS<E, ?> busJS) {
        var bus = busJS.bus();
        for (var token : busJS.tokens()) {
            bus.unregister(token);
        }
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
