package com.tkisor.nekojs.client;

import com.tkisor.nekojs.NekoJS;
import com.tkisor.nekojs.script.ScriptType;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import com.tkisor.nekojs.NekoJSCommon;

public class NekoJSClient {

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(NekoJSClient::onClientSetup);
        modEventBus.addListener(NekoJSClient::onClientResourceReload);
    }

    /// 某些事件需要极早期的时机，如RegisterKeyMappingsEvent
    private static void onClientSetup(FMLConstructModEvent event) {
        event.enqueueWork(() -> {
            NekoJSCommon.LOGGER.debug("[NekoJS] Client environment ready, loading CLIENT scripts...");
            NekoJS.SCRIPT_MANAGER.loadScripts(ScriptType.CLIENT);
            ScriptType.CLIENT.logger().debug("Early script injection...");
        });
    }

    // 1.21.1: 事件名改为 RegisterClientReloadListenersEvent
    private static void onClientResourceReload(RegisterClientReloadListenersEvent event) {
        // 1.21.1: NeoForge 注册重载监听器不需要手动指定 ID，直接 registerReloadListener 即可
        event.registerReloadListener((ResourceManagerReloadListener) resourceManager -> {
            NekoJSCommon.LOGGER.debug("[NekoJS] Detected client resource reload (F3 + T), reloading CLIENT scripts...");
            try {
                NekoJS.SCRIPT_MANAGER.reloadScripts(ScriptType.CLIENT);
            } catch (Exception e) {
                NekoJSCommon.LOGGER.debug("[NekoJS] CLIENT script reload failed", e);
            }
        });
    }
}