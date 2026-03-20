package com.tkisor.nekojs.wrapper.entity;

import com.tkisor.nekojs.wrapper.item.ItemStackWrapper;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class PlayerWrapper extends LivingEntityWrapper {

    public PlayerWrapper(Player player) {
        super(player);
    }

    protected Player getPlayer() {
        return (Player) raw;
    }

    public String getUuid() {
        return getPlayer().getUUID().toString();
    }

    public void teleport(double x, double y, double z) {
        if (getPlayer() instanceof ServerPlayer sp) sp.teleportTo(x, y, z);
        else getPlayer().setPos(x, y, z);
    }

    public void sendSystemMessage(Component message) {
        getPlayer().sendSystemMessage(message);
    }

    public void sendOverlayMessage(Component message) {
        getPlayer().sendOverlayMessage(message);
    }

    public int getFoodLevel() {
        return getPlayer().getFoodData().getFoodLevel();
    }

    public void setFoodLevel(int level) {
        getPlayer().getFoodData().setFoodLevel(level);
    }

    public int getXpLevel() {
        return getPlayer().experienceLevel;
    }

    public void addXpLevel(int level) {
        getPlayer().giveExperienceLevels(level);
    }

    public boolean isOp() {
        return Commands.LEVEL_GAMEMASTERS.check(getPlayer().permissions());
    }

    public boolean isCreative() {
        return getPlayer().isCreative();
    }

    public ItemStackWrapper getMainHandItem() {
        return new ItemStackWrapper(getPlayer().getMainHandItem());
    }

    public void give(ItemStack item) {
        if (!getPlayer().getInventory().add(item)) {
            getPlayer().drop(item, false);
        }
    }

    public void addItemCooldown(Identifier itemId, int ticks) {
        Item item = BuiltInRegistries.ITEM.getValue(itemId);
        if (item != Items.AIR) {
            getPlayer().getCooldowns().addCooldown(item.getDefaultInstance(), ticks);
        }
    }

    public void playSound(Identifier soundId, float volume, float pitch) {
        SoundEvent sound = BuiltInRegistries.SOUND_EVENT.getValue(soundId);
        if (sound != null) {
            getPlayer().playSound(sound, volume, pitch);
        }
    }

    public void sendActionBar(Component text) {
        if (getPlayer() instanceof ServerPlayer sp) {
            sp.sendSystemMessage(text, true);
        }
    }

    @Override
    public Player unwrap() {
        return (Player) super.raw;
    }
}