package com.tkisor.nekojs.bindings.event;

import com.tkisor.nekojs.api.event.EventBusForgeBridge;
import com.tkisor.nekojs.api.event.EventBusJS;
import com.tkisor.nekojs.api.event.EventGroup;
import com.tkisor.nekojs.utils.event.dispatch.DispatchKey;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.function.Function;

public interface EntityEvents {
    EventGroup GROUP = EventGroup.of("EntityEvents");

    Function<EntityEvent, EntityType<?>> TO_KEY = e -> e.getEntity().getType();

    EventBusJS<LivingDamageEvent.Pre, EntityType<?>> HURT_PRE =
            GROUP.server("hurtPre", LivingDamageEvent.Pre.class, DispatchKey.of(EntityType.class, TO_KEY));
    EventBusJS<LivingDamageEvent.Post, EntityType<?>> HURT_POST =
            GROUP.server("hurtPost", LivingDamageEvent.Post.class, DispatchKey.of(EntityType.class, TO_KEY));

    EventBusJS<LivingDeathEvent, EntityType<?>> DEATH =
            GROUP.server("death", LivingDeathEvent.class, DispatchKey.of(EntityType.class, TO_KEY));
    EventBusJS<EntityJoinLevelEvent, EntityType<?>> SPAWNED =
            GROUP.server("spawned", EntityJoinLevelEvent.class, DispatchKey.of(EntityType.class, TO_KEY));

    EventBusForgeBridge FORGE_BRIDGE = EventBusForgeBridge.create(NeoForge.EVENT_BUS)
        .bind(HURT_PRE)
        .bind(HURT_POST)
        .bind(DEATH)
        .bind(SPAWNED);
}