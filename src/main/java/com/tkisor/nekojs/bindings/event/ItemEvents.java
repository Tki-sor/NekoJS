package com.tkisor.nekojs.bindings.event;

import com.tkisor.nekojs.api.event.EventBusForgeBridge;
import com.tkisor.nekojs.api.event.EventBusJS;
import com.tkisor.nekojs.api.event.EventGroup;
import com.tkisor.nekojs.utils.event.dispatch.DispatchKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.function.Function;

public interface ItemEvents {
    EventGroup GROUP = EventGroup.of("ItemEvents");

    EventBusJS<PlayerInteractEvent.RightClickItem, ItemStack> RIGHT_CLICKED =
            GROUP.server("rightClicked", PlayerInteractEvent.RightClickItem.class, DispatchKey.itemStack());
    EventBusJS<ItemTooltipEvent, ItemStack> TOOLTIP =
            GROUP.client("tooltip", ItemTooltipEvent.class, DispatchKey.itemStack());

    EventBusJS<PlayerEvent.ItemCraftedEvent, ItemStack> CRAFTED =
            GROUP.server("crafted", PlayerEvent.ItemCraftedEvent.class, DispatchKey.itemStack());


    EventBusJS<LivingEntityUseItemEvent.Start, ItemStack> USE_START =
            GROUP.server("useStarted", LivingEntityUseItemEvent.Start.class, DispatchKey.itemStack());
    EventBusJS<LivingEntityUseItemEvent.Stop, ItemStack> USE_STOP =
            GROUP.server("useStopped", LivingEntityUseItemEvent.Stop.class, DispatchKey.itemStack());
    EventBusJS<LivingEntityUseItemEvent.Finish, ItemStack> USE_FINISHED =
            GROUP.server("useFinished", LivingEntityUseItemEvent.Finish.class, DispatchKey.itemStack());

    EventBusForgeBridge FORGE_BRIDGE = EventBusForgeBridge.create(NeoForge.EVENT_BUS)
            .bind(RIGHT_CLICKED)
            .bind(TOOLTIP)
            .bind(CRAFTED)
            .bind(USE_START)
            .bind(USE_STOP)
            .bind(USE_FINISHED);
}