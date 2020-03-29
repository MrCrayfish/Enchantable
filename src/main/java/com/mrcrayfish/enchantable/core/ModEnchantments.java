package com.mrcrayfish.enchantable.core;

import com.mrcrayfish.enchantable.Reference;
import com.mrcrayfish.enchantable.enchantment.CultivatorEnchantment;
import com.mrcrayfish.enchantable.enchantment.IronSkullEnchantment;
import com.mrcrayfish.enchantable.enchantment.ReplantingEnchantment;
import com.mrcrayfish.enchantable.enchantment.StompingEnchantment;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentType;
import net.minecraft.item.HoeItem;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ObjectHolder;

/**
 * Author: MrCrayfish
 */
public class ModEnchantments
{
    public static final DeferredRegister<Enchantment> REGISTER = new DeferredRegister<>(ForgeRegistries.ENCHANTMENTS, Reference.MOD_ID);

    public static final RegistryObject<Enchantment> STOMPING = REGISTER.register("stomping", StompingEnchantment::new);
    public static final RegistryObject<Enchantment> IRON_SKULL = REGISTER.register("iron_skull", IronSkullEnchantment::new);
    public static final RegistryObject<Enchantment> REPLANTING = REGISTER.register("replanting", ReplantingEnchantment::new);
    public static final RegistryObject<Enchantment> CULTIVATOR = REGISTER.register("cultivator", CultivatorEnchantment::new);
    public static final RegistryObject<Enchantment> EXCAVATOR = REGISTER.register("excavator", CultivatorEnchantment::new);
}
