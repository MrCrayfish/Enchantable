package com.mrcrayfish.enchantable.core;

import com.mrcrayfish.enchantable.Reference;
import com.mrcrayfish.enchantable.enchantment.*;
import net.minecraft.enchantment.Enchantment;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Author: MrCrayfish
 */
public class ModEnchantments
{
    public static final DeferredRegister<Enchantment> REGISTER = new DeferredRegister<>(ForgeRegistries.ENCHANTMENTS, Reference.MOD_ID);

    public static final RegistryObject<Enchantment> STOMPING = REGISTER.register("stomping", StompingEnchantment::new);
    public static final RegistryObject<Enchantment> IRON_SKULL = REGISTER.register("iron_skull", IronSkullEnchantment::new);
    public static final RegistryObject<Enchantment> SEEDER = REGISTER.register("replanting", SeederEnchantment::new);
    public static final RegistryObject<Enchantment> CULTIVATOR = REGISTER.register("cultivator", CultivatorEnchantment::new);
    public static final RegistryObject<Enchantment> EXCAVATOR = REGISTER.register("excavator", ExcavatorEnchantment::new);
}
