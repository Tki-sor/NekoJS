package com.tkisor.nekojs.listener;

import com.tkisor.nekojs.NekoJS;
import com.tkisor.nekojs.bindings.event.LevelEvents;
import com.tkisor.nekojs.wrapper.event.level.ExplosionStartEventJS;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ExplosionEvent;

@EventBusSubscriber(modid = NekoJS.MODID)
public class LevelEventListener {
    @SubscribeEvent
    public static void onExplosionStart(ExplosionEvent.Start event) {
        LevelEvents.EXPLOSION_START.post(event);
    }

    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        LevelEvents.EXPLOSION_DETONATE.post(event);
    }
}
