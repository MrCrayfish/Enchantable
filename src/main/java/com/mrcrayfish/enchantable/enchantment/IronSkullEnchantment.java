package com.mrcrayfish.enchantable.enchantment;

import com.mrcrayfish.enchantable.Reference;
import com.mrcrayfish.enchantable.core.ModEnchantments;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnchantmentType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;

/**
 * Author: MrCrayfish
 */
@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public class IronSkullEnchantment extends Enchantment
{
    public IronSkullEnchantment()
    {
        super(Rarity.UNCOMMON, EnchantmentType.ARMOR_HEAD, new EquipmentSlotType[]{EquipmentSlotType.HEAD});
        this.setRegistryName(new ResourceLocation(Reference.MOD_ID, "iron_skull"));
    }

    @Override
    public int getMinEnchantability(int level)
    {
        return 10;
    }

    @Override
    public int getMaxEnchantability(int level)
    {
        return this.getMinEnchantability(level) + 30;
    }

    @SubscribeEvent
    public static void onPlayerFallDamage(LivingDamageEvent event)
    {
        if(event.getSource() == DamageSource.FALLING_BLOCK || event.getSource() == DamageSource.FLY_INTO_WALL)
        {
            LivingEntity livingEntity = event.getEntityLiving();
            if(livingEntity instanceof PlayerEntity)
            {
                PlayerEntity player = (PlayerEntity) livingEntity;
                ItemStack stack = player.getItemStackFromSlot(EquipmentSlotType.HEAD);
                if(!stack.isEmpty())
                {
                    Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(stack);
                    if(enchantments.containsKey(ModEnchantments.IRON_SKULL))
                    {
                        event.setCanceled(true);
                        stack.damageItem((int) event.getAmount(), player, entity -> {
                            entity.sendBreakAnimation(EquipmentSlotType.fromSlotTypeAndIndex(EquipmentSlotType.Group.ARMOR, EquipmentSlotType.HEAD.getIndex()));
                        });
                    }
                }
            }
        }
    }
}
