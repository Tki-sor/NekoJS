package com.tkisor.nekojs.listener;

import com.tkisor.nekojs.NekoJS;
import com.tkisor.nekojs.bindings.event.EntityEvents;
import com.tkisor.nekojs.bindings.event.ItemEvents;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = NekoJS.MODID)
public class LivingEventListener {
    @SubscribeEvent
    public static void onUseStartUseStart(LivingEntityUseItemEvent.Start event) {
        ItemEvents.USE_START.post(event, event.getItem());
    }

    @SubscribeEvent
    public static void onUseStopUseStop(LivingEntityUseItemEvent.Stop event) {
        ItemEvents.USE_STOP.post(event, event.getItem());
    }

    @SubscribeEvent
    public static void onUseFinishUseFinish(LivingEntityUseItemEvent.Finish event) {
        ItemEvents.USE_FINISHED.post(event, event.getItem());
    }

    @SubscribeEvent
    public static void onDamagePre(LivingDamageEvent.Pre event) {
        EntityEvents.DAMAGE_PRE.post(event);
    }

    @SubscribeEvent
    public static void onDamagePost(LivingDamageEvent.Post event) {
        EntityEvents.DAMAGE_POST.post(event);
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        EntityEvents.DEATH.post(event);
    }

    @SubscribeEvent
    public static void onTickPre(EntityTickEvent.Pre event) {
        EntityEvents.TICK_Pre.post(event);
    }

    @SubscribeEvent
    public static void onTickPost(EntityTickEvent.Post event) {
        EntityEvents.TICK_Post.post(event);
    }

    @SubscribeEvent
    public static void onJoinLevel(EntityJoinLevelEvent event) {
        EntityEvents.JOIN_LEVEL.post(event);
    }

    @SubscribeEvent
    public static void onLeaveLevel(EntityLeaveLevelEvent event) {
        EntityEvents.LEAVE_LEVEL.post(event);
    }
}
