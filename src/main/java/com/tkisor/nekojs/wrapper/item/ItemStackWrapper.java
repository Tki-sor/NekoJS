package com.tkisor.nekojs.wrapper.item;

import com.tkisor.nekojs.wrapper.NekoWrapper;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.List;
import java.util.stream.Collectors;

public class ItemStackWrapper implements NekoWrapper<ItemStack> {
    private final ItemStack rawStack;

    public ItemStackWrapper(ItemStack rawStack) {
        this.rawStack = rawStack == null ? ItemStack.EMPTY : rawStack;
    }

    public String getId() {
        return BuiltInRegistries.ITEM.getKey(rawStack.getItem()).toString();
    }

    public int getCount() { return rawStack.getCount(); }

    public ItemStackWrapper setCount(int count) {
        rawStack.setCount(count);
        return this;
    }

    public boolean isEmpty() { return rawStack.isEmpty(); }

    public String getName() {
        return rawStack.getHoverName().getString();
    }

    public ItemStackWrapper setName(String name) {
        rawStack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return this;
    }

    public List<String> getLore() {
        ItemLore loreComponent = rawStack.get(DataComponents.LORE);
        if (loreComponent == null) return List.of();
        return loreComponent.lines().stream()
                .map(Component::getString)
                .collect(Collectors.toList());
    }

    public ItemStackWrapper setLore(List<String> lines) {
        List<Component> components = lines.stream()
                .map(Component::literal)
                .collect(Collectors.toList());
        rawStack.set(DataComponents.LORE, new ItemLore(components));
        return this;
    }

    public ItemStackWrapper setEnchanted(boolean enchanted) {
        if (enchanted) {
            rawStack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        } else {
            rawStack.remove(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
        }
        return this;
    }

    public ItemStackWrapper setUnbreakable(boolean unbreakable) {
        if (unbreakable) {
            rawStack.set(DataComponents.UNBREAKABLE, Unit.INSTANCE);
        } else {
            rawStack.remove(DataComponents.UNBREAKABLE);
        }
        return this;
    }

    public int getDamage() { return rawStack.getDamageValue(); }

    public ItemStackWrapper setDamage(int damage) {
        rawStack.setDamageValue(damage);
        return this;
    }

    public int getMaxDamage() { return rawStack.getMaxDamage(); }

    public void clearEnchantments() {
        rawStack.set(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
    }

    @Override
    public ItemStack unwrap() {
        return rawStack;
    }
}