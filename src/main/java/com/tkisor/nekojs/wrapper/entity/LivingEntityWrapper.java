package com.tkisor.nekojs.wrapper.entity;

import net.minecraft.world.entity.LivingEntity;

public class LivingEntityWrapper extends EntityWrapper {

    public LivingEntityWrapper(LivingEntity entity) {
        super(entity);
    }

    protected LivingEntity getLiving() { return (LivingEntity) raw; }

    public float getHealth() { return getLiving().getHealth(); }
    public void setHealth(float health) { getLiving().setHealth(health); }
    public float getMaxHealth() { return getLiving().getMaxHealth(); }

    public void setAbsorption(float amount) { getLiving().setAbsorptionAmount(amount); }
    public float getAbsorption() { return getLiving().getAbsorptionAmount(); }

    @Override
    public LivingEntity unwrap() {
        return (LivingEntity) super.raw;
    }
}