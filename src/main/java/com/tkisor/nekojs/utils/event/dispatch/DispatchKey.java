package com.tkisor.nekojs.utils.event.dispatch;

import com.tkisor.nekojs.utils.event.impl.dispatch.DispatchKeyImpl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.function.Function;

/**
 * @author ZZZank
 */
public interface DispatchKey<E, K> {
    static <E, K> DispatchKey<E, K> of(Class<? super K> keyType, Function<? super E, K> toKey) {
        return new DispatchKeyImpl<>((Class<K>) keyType, toKey);
    }

    static <E, K> DispatchKey<E, K> of(Class<? super K> keyType) {
        return of(keyType, (ignored) -> null);
    }

    static <E> DispatchKey<E, String> string() {
        return of(String.class);
    }

    static <E> DispatchKey<E, ItemStack> itemStack() {
        return of(ItemStack.class);
    }

    static <E> DispatchKey<E, Block> block() {
        return of(Block.class);
    }

    Class<K> keyType();

    K eventToKey(E event);
}
