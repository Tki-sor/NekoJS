package com.tkisor.nekojs.wrapper.entity;

import com.tkisor.nekojs.wrapper.item.ItemStackWrapper;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class PlayerWrapper extends LivingEntityWrapper {

    public PlayerWrapper(Player player) {
        super(player);
    }

    protected Player getPlayer() { return (Player) raw; }

    public String getUuid() { return getPlayer().getUUID().toString(); }

    public void teleport(double x, double y, double z) {
        if (getPlayer() instanceof ServerPlayer sp) sp.teleportTo(x, y, z);
        else getPlayer().setPos(x, y, z);
    }

    public void tell(String message) { getPlayer().sendSystemMessage(Component.literal(message)); }

    public int getFoodLevel() { return getPlayer().getFoodData().getFoodLevel(); }
    public void setFoodLevel(int level) { getPlayer().getFoodData().setFoodLevel(level); }

    public int getXpLevel() { return getPlayer().experienceLevel; }
    public void addXpLevel(int level) { getPlayer().giveExperienceLevels(level); }

    public boolean isOp() { return Commands.LEVEL_GAMEMASTERS.check(getPlayer().permissions()); }
    public boolean isCreative() { return getPlayer().isCreative(); }

    public ItemStackWrapper getMainHandItem() {
        return new ItemStackWrapper(getPlayer().getMainHandItem());
    }

    @Override
    public Player unwrap() {
        return (Player) super.raw;
    }
}