package com.tkisor.nekojs.listener;

import com.tkisor.nekojs.NekoJS;
import com.tkisor.nekojs.bindings.event.ItemEvents;
import com.tkisor.nekojs.wrapper.event.item.ItemConsumedEventJS;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

@EventBusSubscriber(modid = NekoJS.MODID)
public class ItemEventListener {
    @SubscribeEvent
    public static void onItemConsumed(LivingEntityUseItemEvent.Finish event) {
        ItemConsumedEventJS eventJS = new ItemConsumedEventJS(event);
        ItemEvents.CONSUMED.post(eventJS, eventJS.getItemId());
    }
}
