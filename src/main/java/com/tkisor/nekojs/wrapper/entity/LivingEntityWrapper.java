package com.tkisor.nekojs.wrapper.entity;

import com.tkisor.nekojs.wrapper.item.ItemStackWrapper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

public class LivingEntityWrapper extends EntityWrapper {

    public LivingEntityWrapper(LivingEntity entity) {
        super(entity);
    }

    protected LivingEntity getLiving() {
        return (LivingEntity) raw;
    }

    public float getHealth() {
        return getLiving().getHealth();
    }

    public void setHealth(float health) {
        getLiving().setHealth(health);
    }

    public float getMaxHealth() {
        return getLiving().getMaxHealth();
    }

    public void heal(float amount) {
        getLiving().heal(amount);
    }

    public void setAbsorption(float amount) {
        getLiving().setAbsorptionAmount(amount);
    }

    public float getAbsorption() {
        return getLiving().getAbsorptionAmount();
    }

    public void addEffect(Identifier effectId, int durationTicks, int amplifier) {
        MobEffect effect = BuiltInRegistries.MOB_EFFECT.getValue(effectId);
        if (effect != null) {
            getLiving().addEffect(new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect), durationTicks, amplifier));
        }
    }

    public void removeEffect(Identifier effectId) {
        MobEffect effect = BuiltInRegistries.MOB_EFFECT.getValue(effectId);
        if (effect != null) {
            getLiving().removeEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect));
        }
    }

    public void removeAllEffects() {
        getLiving().removeAllEffects();
    }

    public ItemStackWrapper getOffHandItem() {
        return new ItemStackWrapper(getLiving().getOffhandItem());
    }

    public ItemStackWrapper getHeadArmor() {
        return new ItemStackWrapper(getLiving().getItemBySlot(EquipmentSlot.HEAD));
    }

    public ItemStackWrapper getChestArmor() {
        return new ItemStackWrapper(getLiving().getItemBySlot(EquipmentSlot.CHEST));
    }

    public ItemStackWrapper getLegsArmor() {
        return new ItemStackWrapper(getLiving().getItemBySlot(EquipmentSlot.LEGS));
    }

    public ItemStackWrapper getFeetArmor() {
        return new ItemStackWrapper(getLiving().getItemBySlot(EquipmentSlot.FEET));
    }

    @Override
    public LivingEntity unwrap() {
        return (LivingEntity) super.raw;
    }
}