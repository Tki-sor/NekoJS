package com.tkisor.nekojs.mixin;

import com.tkisor.nekojs.NekoJS;
import com.tkisor.nekojs.bindings.event.ServerEvents;
import com.tkisor.nekojs.mixin_api.IRecipeManagerExtension;
import com.tkisor.nekojs.wrapper.event.server.RecipeEventJS;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(RecipeManager.class)
public abstract class RecipeManagerMixin implements IRecipeManagerExtension {

    @Final
    @Shadow private HolderLookup.Provider registries;

    @Shadow private RecipeMap recipes;

    @Unique
    @Override
    public void nekojs$applyScripts() {
        RecipeEventJS eventJS = new RecipeEventJS(this.recipes.values(), this.registries);

        try {
            ServerEvents.RECIPES.post(eventJS);
        } catch (Exception e) {
            NekoJS.LOGGER.error("[NekoJS] 配方脚本执行崩溃: ", e);
        }

        this.recipes = eventJS.getFinalMap();
        NekoJS.LOGGER.info("[NekoJS] 脚本执行完毕，当前配方总数: {}", this.recipes.values().size());
    }
}