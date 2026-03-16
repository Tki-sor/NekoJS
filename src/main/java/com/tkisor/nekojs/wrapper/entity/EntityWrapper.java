package com.tkisor.nekojs.wrapper.entity;

import com.tkisor.nekojs.wrapper.NekoWrapper;
import lombok.Getter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class EntityWrapper implements NekoWrapper<Entity> {
    @Getter
    protected final Entity raw;

    public EntityWrapper(Entity entity) {
        this.raw = entity;
    }

    public static EntityWrapper of(Entity entity) {
        return switch (entity) {
            case null -> null;
            case Player p -> new PlayerWrapper(p);
            case LivingEntity le -> new LivingEntityWrapper(le);
            default -> new EntityWrapper(entity);
        };
    }

    public String getId() {
        if (raw == null) return "minecraft:empty";
        return BuiltInRegistries.ENTITY_TYPE.getKey(raw.getType()).toString();
    }

    public String getName() { return raw.getName().getString(); }
    public double getX() { return raw.getX(); }
    public double getY() { return raw.getY(); }
    public double getZ() { return raw.getZ(); }
    public Vec3 getPos() { return raw.position(); }

    public void setOnFire(int seconds) { raw.igniteForSeconds(seconds); }

    @Override
    public Entity unwrap() { return raw; }
}