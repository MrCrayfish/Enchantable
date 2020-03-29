package com.mrcrayfish.enchantable.enchantment;

import com.mrcrayfish.enchantable.Enchantable;
import com.mrcrayfish.enchantable.Reference;
import com.mrcrayfish.enchantable.core.ModEnchantments;
import net.minecraft.block.*;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnchantmentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.HoeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.concurrent.TickDelayedTask;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.LogicalSidedProvider;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Author: MrCrayfish
 */
@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public class ReplantingEnchantment extends Enchantment
{
    public ReplantingEnchantment()
    {
        super(Rarity.RARE, Enchantable.HOE, new EquipmentSlotType[]{EquipmentSlotType.MAINHAND});
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
    public static void onPlayerHarvestBlock(BlockEvent.BreakEvent event)
    {
        if(event.getState().getBlock() instanceof CropsBlock)
        {
            ItemStack heldItem = event.getPlayer().getHeldItemMainhand();
            if(heldItem.isEmpty())
                return;

            if(!EnchantmentHelper.getEnchantments(heldItem).containsKey(ModEnchantments.REPLANTING))
                return;

            World world = event.getPlayer().getEntityWorld();
            if(EnchantmentHelper.getEnchantments(heldItem).containsKey(ModEnchantments.CULTIVATOR))
            {
                BlockPos pos = event.getPos().add(-1, 0, -1);
                for(int i = 0; i < 9; i++)
                {
                    BlockPos cropPos = pos.add(i / 3, 0, i % 3);
                    BlockState state = world.getBlockState(cropPos);
                    ReplantingEnchantment.replantCrop(state, world, cropPos, event.getPlayer(), event.getPos());
                }
            }
            else
            {
                ReplantingEnchantment.replantCrop(event.getState(), world, event.getPos(), event.getPlayer(), event.getPos());
            }
        }
    }

    private static void replantCrop(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockPos originalPos)
    {
        if(state.getBlock() instanceof CropsBlock)
        {
            CropsBlock crop = (CropsBlock) state.getBlock();
            if(state.get(crop.getAgeProperty()) != crop.getMaxAge())
                return;

            ItemStack stack = crop.getItem(world, pos, state);
            if(stack.getItem() instanceof BlockItem)
            {
                BlockItem blockItem = (BlockItem) stack.getItem();
                if(blockItem.getBlock() instanceof CropsBlock)
                {
                    ItemStack seeds = ItemStack.EMPTY;
                    List<ItemStack> drops = Block.getDrops(state, (ServerWorld) world, pos, null);
                    for(ItemStack drop : drops)
                    {
                        if(drop.getItem() == stack.getItem())
                        {
                            seeds = drop.split(1);
                            break;
                        }
                    }
                    if(seeds.isEmpty())
                    {
                        seeds = findSeeds(player, stack.getItem());
                    }

                    drops.forEach(drop -> Block.spawnAsEntity(world, pos, drop));
                    state.spawnAdditionalDrops(world, pos, ItemStack.EMPTY);
                    world.setBlockState(pos, Blocks.AIR.getDefaultState());

                    if(!pos.equals(originalPos))
                    {
                        world.playEvent(2001, pos, Block.getStateId(state));
                    }

                    BlockState seedState = blockItem.getBlock().getDefaultState();
                    if(!seeds.isEmpty() & blockItem.getBlock().isValidPosition(seedState, world, pos))
                    {
                        MinecraftServer server = LogicalSidedProvider.INSTANCE.get(LogicalSide.SERVER);
                        server.enqueue(new TickDelayedTask(0, () -> world.setBlockState(pos, seedState, 3)));
                        seeds.shrink(1);
                    }
                }
            }
        }
    }

    private static ItemStack findSeeds(PlayerEntity player, Item item)
    {
        for(int i = 0; i < player.inventory.getSizeInventory(); i++)
        {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if(stack.getItem() == item)
            {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}
