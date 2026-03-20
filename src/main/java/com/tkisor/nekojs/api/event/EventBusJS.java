package com.tkisor.nekojs.api.event;

import com.tkisor.nekojs.utils.event.CancellableEventBus;
import com.tkisor.nekojs.utils.event.EventBus;
import com.tkisor.nekojs.utils.event.EventListenerToken;
import com.tkisor.nekojs.utils.event.dispatch.DispatchCancellableEventBus;
import com.tkisor.nekojs.utils.event.dispatch.DispatchEventBus;
import com.tkisor.nekojs.utils.event.dispatch.DispatchKey;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author ZZZank
 */
public class EventBusJS<EVENT, KEY> implements ProxyExecutable {
    public static <E, K> EventBusJS<E, K> of(EventBus<E> bus, Function<Object, K> keyTransformer) {
        return new EventBusJS<>(bus, keyTransformer);
    }

    public static <E> EventBusJS<E, Void> of(Class<E> eventType) {
        return new EventBusJS<>(EventBus.create(eventType), null);
    }

    public static <E> EventBusJS<E, Void> ofCancellable(Class<E> eventType) {
        return new EventBusJS<>(CancellableEventBus.create(eventType), null);
    }

    public static <E, K> EventBusJS<E, K> of(
        Class<E> eventType,
        boolean cancellable,
        DispatchKey<E, K> dispatchKey,
        Function<Object, K> keyTransformer
    ) {
        EventBus<E> bus;
        if (cancellable) {
            bus = dispatchKey != null
                ? DispatchCancellableEventBus.create(eventType, dispatchKey)
                : CancellableEventBus.create(eventType);
        } else {
            bus = dispatchKey != null
                ? DispatchEventBus.create(eventType, dispatchKey)
                : EventBus.create(eventType);
        }
        return new EventBusJS<>(bus, keyTransformer);
    }

    private final EventBus<EVENT> bus;
    private final List<EventListenerToken<EVENT>> tokens;
    private final Function<Object, KEY> keyTransformer;

    protected EventBusJS(EventBus<EVENT> bus, Function<Object, KEY> keyTransformer) {
        this.bus = Objects.requireNonNull(bus);
        this.tokens = new ArrayList<>();
        this.keyTransformer = keyTransformer;
    }

    public boolean canCancel() {
        return bus instanceof CancellableEventBus<?>;
    }

    public boolean canDispatch() {
        return bus instanceof DispatchEventBus<?, ?>;
    }

    public EventBus<EVENT> bus() {
        return bus;
    }

    public List<EventListenerToken<EVENT>> tokens() {
        return tokens;
    }

    public boolean post(EVENT event) {
        return this.bus.post(event);
    }

    public boolean post(EVENT event, KEY key) {
        if (canDispatch()) {
            return ((DispatchEventBus<EVENT, KEY>) bus).post(event, key);
        }
        throw new IllegalStateException("This bus is not dispatchable");
    }

    @Override
    public Object execute(Value... args) {
        if (args.length == 0) {
            throw new IllegalArgumentException("EventBus requires at least one arg");
        }
        EventListenerToken<EVENT> token;
        if (canDispatch()) {
            if (canCancel()) {
                token = args.length > 1
                    ? registerDispatchCancellable(args[1], args[0]) // listen("key", (e) => true)
                    : registerCancellable(args[0]); // listen((e) => true)
            } else {
                token = args.length > 1
                    ? registerDispatch(args[1], args[0]) // listen("key", (e) => {})
                    : register(args[0]); // listen((e) => {})
            }
        } else {
            if (canCancel()) {
                token = registerCancellable(args[0]); // listen((e) => true)
            } else {
                token = register(args[0]); // listen((e) => {})
            }
        }
        return this.tokens.add(token);
    }

    private EventListenerToken<EVENT> register(Value listener) {
        var bus = this.bus;
        return bus.listen((Consumer<EVENT>) listener.as(Consumer.class));
    }

    private EventListenerToken<EVENT> registerCancellable(Value listener) {
        var bus = (CancellableEventBus<EVENT>) this.bus;
        return bus.listen((Predicate<EVENT>) listener.as(Predicate.class));
    }

    private EventListenerToken<EVENT> registerDispatch(Value listener, Value key) {
        var bus = (DispatchEventBus<EVENT, KEY>) this.bus;
        return bus.listen(
            this.keyTransformer.apply(key),
            (Consumer<EVENT>) listener.as(Consumer.class)
        );
    }

    private EventListenerToken<EVENT> registerDispatchCancellable(Value listener, Value key) {
        var bus = (DispatchCancellableEventBus<EVENT, KEY>) this.bus;
        return bus.listen(
            this.keyTransformer.apply(key),
            (Predicate<EVENT>) listener.as(Predicate.class)
        );
    }
}