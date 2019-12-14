package com.mrcrayfish.enchantable.enchantment;

import com.mrcrayfish.enchantable.Reference;
import com.mrcrayfish.enchantable.core.ModEnchantments;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnchantmentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.HoeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.UseHoeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Author: MrCrayfish
 */
@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public class CultivatorEnchantment extends Enchantment
{
    public CultivatorEnchantment()
    {
        super(Rarity.RARE, EnchantmentType.DIGGER, new EquipmentSlotType[]{EquipmentSlotType.MAINHAND});
        this.setRegistryName(new ResourceLocation(Reference.MOD_ID, "cultivator"));
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack)
    {
        return stack.getItem() instanceof HoeItem;
    }

    @Override
    public int getMinEnchantability(int level)
    {
        return 15;
    }

    @Override
    public int getMaxEnchantability(int level)
    {
        return super.getMinEnchantability(level) + 50;
    }

    @SubscribeEvent
    public static void onUseHoe(UseHoeEvent event)
    {
        ItemUseContext context = event.getContext();

        ItemStack stack = context.getItem();
        if(stack.isEmpty())
            return;

        if(!EnchantmentHelper.getEnchantments(stack).containsKey(ModEnchantments.CULTIVATOR))
            return;

        World world = context.getWorld();
        BlockPos pos = context.getPos().add(-1, 0, -1);
        if(context.getFace() != Direction.DOWN)
        {
            boolean tilled = false;
            for(int i = 0; i < 9; i++)
            {
                BlockPos groundPos = pos.add(i / 3, 0, i % 3);
                if(world.isAirBlock(groundPos.up()))
                {
                    BlockState groundState = HoeItem.HOE_LOOKUP.get(world.getBlockState(groundPos).getBlock());
                    if(groundState != null)
                    {
                        world.setBlockState(groundPos, groundState, 11);
                        tilled = true;
                    }
                }
            }
            if(tilled)
            {
                PlayerEntity player = context.getPlayer();
                world.playSound(player, pos, SoundEvents.ITEM_HOE_TILL, SoundCategory.BLOCKS, 1.0F, 1.0F);
                event.setResult(Event.Result.ALLOW);
            }
        }
    }
}
