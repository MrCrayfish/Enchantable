package com.mrcrayfish.enchantable.enchantment;

import com.mrcrayfish.enchantable.Enchantable;
import com.mrcrayfish.enchantable.Reference;
import com.mrcrayfish.enchantable.core.ModEnchantments;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnchantmentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.*;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.UseHoeEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;

/**
 * Author: MrCrayfish
 */
@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public class CultivatorEnchantment extends Enchantment
{
    public CultivatorEnchantment()
    {
        super(Rarity.RARE, Enchantable.TILLABLE, new EquipmentSlotType[]{EquipmentSlotType.MAINHAND});
        this.setRegistryName(new ResourceLocation(Reference.MOD_ID, "cultivator"));
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack)
    {
        return stack.getItem() instanceof HoeItem || stack.getItem() instanceof ShovelItem;
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
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event)
    {
        if(!(event.getItemStack().getItem() instanceof ShovelItem))
            return;

        if(till(event.getWorld(), event.getPos(), event.getFace(), event.getItemStack(), event.getPlayer(), ShovelItem.field_195955_e))
        {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onUseHoe(UseHoeEvent event)
    {
        ItemUseContext context = event.getContext();
        if(till(context.getWorld(), context.getPos(), context.getFace(), context.getItem(), context.getPlayer(), HoeItem.HOE_LOOKUP))
        {
            event.setResult(Event.Result.ALLOW);
        }
    }

    private static boolean till(World world, BlockPos pos, Direction face, ItemStack stack, PlayerEntity player, Map<Block, BlockState> replacementMap)
    {
        if(stack.isEmpty())
            return false;

        if(!EnchantmentHelper.getEnchantments(stack).containsKey(ModEnchantments.CULTIVATOR))
            return false;

        pos = pos.add(-1, 0, -1);
        if(face != Direction.DOWN)
        {
            boolean tilled = false;
            for(int i = 0; i < 9; i++)
            {
                BlockPos groundPos = pos.add(i / 3, 0, i % 3);
                boolean air = world.isAirBlock(groundPos.up());
                boolean replaceable = world.getBlockState(groundPos.up()).getMaterial().isReplaceable();
                if(air || replaceable)
                {
                    BlockState groundState = replacementMap.get(world.getBlockState(groundPos).getBlock());
                    if(groundState != null)
                    {
                        world.setBlockState(groundPos, groundState, 11);
                        tilled = true;
                        if(!air)
                        {
                            world.setBlockState(groundPos.up(), Blocks.AIR.getDefaultState());
                        }
                    }
                }
            }
            if(tilled)
            {
                world.playSound(player, pos, SoundEvents.ITEM_HOE_TILL, SoundCategory.BLOCKS, 1.0F, 1.0F);
                return true;
            }
        }
        return false;
    }
}
