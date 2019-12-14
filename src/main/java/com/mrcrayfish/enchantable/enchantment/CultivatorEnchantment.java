package com.mrcrayfish.enchantable.enchantment;

import com.mrcrayfish.enchantable.Enchantable;
import com.mrcrayfish.enchantable.Reference;
import com.mrcrayfish.enchantable.core.ModEnchantments;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropsBlock;
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
        super(Rarity.VERY_RARE, Enchantable.TILLABLE, new EquipmentSlotType[]{EquipmentSlotType.MAINHAND});
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

        int affectedBlocks = till(event.getWorld(), event.getPos(), event.getFace(), event.getItemStack(), event.getPlayer(), ShovelItem.field_195955_e);
        if(affectedBlocks > 0)
        {
            event.getItemStack().damageItem(affectedBlocks, event.getPlayer(), player -> player.sendBreakAnimation(event.getHand()));
            event.getPlayer().swingArm(event.getHand());
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onUseHoe(UseHoeEvent event)
    {
        ItemUseContext context = event.getContext();
        int affectedBlocks = till(context.getWorld(), context.getPos(), context.getFace(), context.getItem(), context.getPlayer(), HoeItem.HOE_LOOKUP);
        if(affectedBlocks > 0 && context.getPlayer() != null)
        {
            context.getItem().damageItem(affectedBlocks - 1, context.getPlayer(), player -> player.sendBreakAnimation(context.getHand()));
            event.setResult(Event.Result.ALLOW);
        }
    }

    @SubscribeEvent
    public static void onPlayerHarvestBlock(BlockEvent.BreakEvent event)
    {
        if(event.getState().getBlock() instanceof CropsBlock)
        {
            ItemStack heldItem = event.getPlayer().getHeldItemMainhand();
            if(heldItem.isEmpty())
                return;

            if(EnchantmentHelper.getEnchantments(heldItem).containsKey(ModEnchantments.REPLANTING))
                return;

            if(!EnchantmentHelper.getEnchantments(heldItem).containsKey(ModEnchantments.CULTIVATOR))
                return;

            World world = event.getPlayer().getEntityWorld();
            BlockPos pos = event.getPos().add(-1, 0, -1);
            for(int i = 0; i < 9; i++)
            {
                BlockPos cropPos = pos.add(i / 3, 0, i % 3);
                BlockState state = world.getBlockState(cropPos);
                if(state.getBlock() instanceof CropsBlock)
                {
                    CropsBlock crop = (CropsBlock) state.getBlock();
                    if(state.get(crop.getAgeProperty()) != crop.getMaxAge())
                        continue;

                    Block.spawnDrops(state, world, cropPos);
                    world.setBlockState(cropPos, Blocks.AIR.getDefaultState());
                    if(!cropPos.equals(event.getPos()))
                    {
                        world.playEvent(2001, cropPos, Block.getStateId(state));
                    }
                }
            }
        }
    }

    private static int till(World world, BlockPos pos, Direction face, ItemStack stack, PlayerEntity player, Map<Block, BlockState> replacementMap)
    {
        if(stack.isEmpty())
            return 0;

        if(!EnchantmentHelper.getEnchantments(stack).containsKey(ModEnchantments.CULTIVATOR))
            return 0;

        pos = pos.add(-1, 0, -1);
        if(face != Direction.DOWN)
        {
            int maxBlocks = stack.getMaxDamage() - stack.getDamage();
            int affectedBlocks = 0;
            for(int i = 0; i < 9 && i < maxBlocks; i++)
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
                        affectedBlocks++;
                        if(!air)
                        {
                            world.setBlockState(groundPos.up(), Blocks.AIR.getDefaultState());
                        }
                    }
                }
            }
            if(affectedBlocks > 0)
            {
                world.playSound(player, pos, SoundEvents.ITEM_HOE_TILL, SoundCategory.BLOCKS, 1.0F, 1.0F);
                return affectedBlocks;
            }
        }
        return 0;
    }
}
