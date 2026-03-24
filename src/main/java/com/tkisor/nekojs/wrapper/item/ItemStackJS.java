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

public class ItemStackJS implements NekoWrapper<ItemStack> {
    private final ItemStack rawStack;

    public ItemStackJS(ItemStack rawStack) {
        this.rawStack = rawStack == null ? ItemStack.EMPTY : rawStack;
    }

    public String getId() {
        return BuiltInRegistries.ITEM.getKey(rawStack.getItem()).toString();
    }

    public boolean isEmpty() {
        return rawStack.isEmpty();
    }

    public int getCount() {
        return rawStack.getCount();
    }

    public void setCount(int count) {
        rawStack.setCount(count);
    }

    public ItemStackJS withCount(int count) {
        rawStack.setCount(count);
        return this;
    }

    public String getName() {
        return rawStack.getHoverName().getString();
    }

    public void setName(String name) {
        rawStack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
    }

    public ItemStackJS withName(String name) {
        this.setName(name);
        return this;
    }

    public List<String> getLore() {
        ItemLore loreComponent = rawStack.get(DataComponents.LORE);
        if (loreComponent == null) return List.of();
        return loreComponent.lines().stream()
                .map(Component::getString)
                .collect(Collectors.toList());
    }

    public void setLore(List<String> lines) {
        List<Component> components = lines.stream()
                .map(Component::literal)
                .collect(Collectors.toList());
        rawStack.set(DataComponents.LORE, new ItemLore(components));
    }

    public ItemStackJS withLore(List<String> lines) {
        this.setLore(lines);
        return this;
    }

    public boolean isEnchanted() {
        Boolean glint = rawStack.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
        return glint != null ? glint : rawStack.isEnchanted();
    }

    public void setEnchanted(boolean enchanted) {
        if (enchanted) {
            rawStack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        } else {
            rawStack.remove(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
        }
    }

    public ItemStackJS withEnchanted(boolean enchanted) {
        this.setEnchanted(enchanted);
        return this;
    }

    public boolean isUnbreakable() {
        return rawStack.has(DataComponents.UNBREAKABLE);
    }

    public void setUnbreakable(boolean unbreakable) {
        if (unbreakable) {
            rawStack.set(DataComponents.UNBREAKABLE, Unit.INSTANCE);
        } else {
            rawStack.remove(DataComponents.UNBREAKABLE);
        }
    }

    public ItemStackJS withUnbreakable(boolean unbreakable) {
        this.setUnbreakable(unbreakable);
        return this;
    }

    public int getDamage() {
        return rawStack.getDamageValue();
    }

    public void setDamage(int damage) {
        rawStack.setDamageValue(damage);
    }

    public ItemStackJS withDamage(int damage) {
        rawStack.setDamageValue(damage);
        return this;
    }

    public int getMaxDamage() {
        return rawStack.getMaxDamage();
    }

    public void clearEnchantments() {
        rawStack.set(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
    }

    @Override
    public ItemStack unwrap() {
        return rawStack;
    }
}