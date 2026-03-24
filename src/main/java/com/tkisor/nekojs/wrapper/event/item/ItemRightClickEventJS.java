package com.tkisor.nekojs.wrapper.event.item;

import com.tkisor.nekojs.api.event.NekoCancellableEvent;
import com.tkisor.nekojs.bindings.player.NekoPlayerEvent;
import com.tkisor.nekojs.wrapper.entity.PlayerJS;
import com.tkisor.nekojs.wrapper.item.ItemStackJS;
import lombok.Getter;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public class ItemRightClickEventJS implements NekoPlayerEvent, NekoCancellableEvent {
    private final PlayerInteractEvent.RightClickItem rawEvent;
    @Getter
    private final PlayerJS player;
    @Getter
    private final ItemStackJS item;

    public ItemRightClickEventJS(PlayerInteractEvent.RightClickItem rawEvent) {
        this.rawEvent = rawEvent;
        this.player = new PlayerJS(rawEvent.getEntity());
        this.item = new ItemStackJS(rawEvent.getItemStack());
    }

    public MinecraftServer getServer() {
        return rawEvent.getLevel().getServer();
    }

    public Level getLevel() {
        return rawEvent.getLevel();
    }

    @Override
    public PlayerJS getEntity() {
        return player;
    }
}