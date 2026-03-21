package com.tkisor.nekojs.listener;

import com.tkisor.nekojs.NekoJS;
import com.tkisor.nekojs.bindings.event.EntityEvents;
import com.tkisor.nekojs.wrapper.event.entity.EntityDeathEventJS;
import com.tkisor.nekojs.wrapper.event.entity.EntityHurtPostEventJS;
import com.tkisor.nekojs.wrapper.event.entity.EntityHurtPreEventJS;
import com.tkisor.nekojs.wrapper.event.entity.EntitySpawnedEventJS;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

@EventBusSubscriber(modid = NekoJS.MODID)
public class EntityEventListener {
    @SubscribeEvent
    public static void onEntityHurtPre(LivingDamageEvent.Pre event) {
        EntityHurtPreEventJS eventJS = new EntityHurtPreEventJS(event);
        EntityEvents.HURT_PRE.post(eventJS, eventJS.getEntityId());
    }

    @SubscribeEvent
    public static void onEntityHurtPost(LivingDamageEvent.Post event) {
        EntityHurtPostEventJS eventJS = new EntityHurtPostEventJS(event);
        EntityEvents.HURT_POST.post(eventJS, eventJS.getEntityId());
    }

    @SubscribeEvent
    public static void onEntitySpawned(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide()) {
            EntityEvents.SPAWNED.post(new EntitySpawnedEventJS(event));
        }
    }

    @SubscribeEvent
    public static void onEntityDeath(LivingDeathEvent event) {
        EntityDeathEventJS eventJS = new EntityDeathEventJS(event);
        EntityEvents.DEATH.post(eventJS, eventJS.getEntityId());
    }
}
