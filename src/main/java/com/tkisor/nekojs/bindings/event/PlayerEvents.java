package com.tkisor.nekojs.bindings.event;

import com.tkisor.nekojs.api.event.EventBusForgeBridge;
import com.tkisor.nekojs.api.event.EventBusJS;
import com.tkisor.nekojs.api.event.EventGroup;
import com.tkisor.nekojs.wrapper.event.player.PlayerChatEventJS;
import com.tkisor.nekojs.wrapper.event.player.PlayerLoggedInEventJS;
import com.tkisor.nekojs.wrapper.event.player.PlayerRespawnEventJS;
import com.tkisor.nekojs.wrapper.event.player.PlayerTickEventJS;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.ServerChatEvent;

public interface PlayerEvents {
    EventGroup GROUP = EventGroup.of("PlayerEvents");

    EventBusJS<PlayerLoggedInEventJS, Void> LOGGED_IN =
            GROUP.server("loggedIn", PlayerLoggedInEventJS.class);
    EventBusJS<PlayerChatEventJS, Void> CHAT =
            GROUP.server("chat", PlayerChatEventJS.class);
    EventBusJS<PlayerTickEventJS, Void> TICK =
            GROUP.server("tick", PlayerTickEventJS.class);
    EventBusJS<PlayerRespawnEventJS, Void> RESPAWNED =
            GROUP.server("respawned", PlayerRespawnEventJS.class);

    // 2. 使用 FORGE_BRIDGE 自动桥接
    EventBusForgeBridge FORGE_BRIDGE = EventBusForgeBridge.create(NeoForge.EVENT_BUS)
            .bindTransformed(
                    CHAT,                    // 目标 JS 总线
                    PlayerChatEventJS::new,  // Transformer: 也就是 new PlayerChatEventJS(event) 的简写
                    ServerChatEvent.class    // 监听的原生 Forge 事件类
            );
}