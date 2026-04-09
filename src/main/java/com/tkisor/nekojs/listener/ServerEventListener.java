package com.tkisor.nekojs.listener;

import com.tkisor.nekojs.NekoJS;
import com.tkisor.nekojs.bindings.event.ServerEvents;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.server.*;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = NekoJS.MODID)
public class ServerEventListener {
    @SubscribeEvent
    public static void onTickPre(ServerTickEvent.Pre event) {
        ServerEvents.TICK_PRE.post(event);
    }

    @SubscribeEvent
    public static void onTickPost(ServerTickEvent.Post event) {
        ServerEvents.TICK_POST.post(event);
    }

    @SubscribeEvent
    public static void onAboutToStart(ServerAboutToStartEvent event) {
        ServerEvents.ABOUT_TO_START.post(event);
    }

    @SubscribeEvent
    public static void onStarting(ServerStartingEvent event) {
        ServerEvents.STARTING.post(event);
    }

    @SubscribeEvent
    public static void onStarted(ServerStartedEvent event) {
        ServerEvents.STARTED.post(event);
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent event) {
        ServerEvents.STOPPING.post(event);
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent event) {
        ServerEvents.STOPPED.post(event);
    }
}
