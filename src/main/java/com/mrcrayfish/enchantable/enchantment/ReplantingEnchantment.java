package com.mrcrayfish.enchantable.enchantment;

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
        super(Rarity.RARE, EnchantmentType.DIGGER, new EquipmentSlotType[]{EquipmentSlotType.MAINHAND});
        this.setRegistryName(new ResourceLocation(Reference.MOD_ID, "replanting"));
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

            CropsBlock crop = (CropsBlock) event.getState().getBlock();
            if(event.getState().get(crop.getAgeProperty()) != crop.getMaxAge())
                return;

            World world = event.getPlayer().getEntityWorld();

            ItemStack stack = crop.getItem(event.getWorld(), event.getPos(), event.getState());
            if(stack.getItem() instanceof BlockItem)
            {
                BlockItem blockItem = (BlockItem) stack.getItem();
                if(blockItem.getBlock() instanceof CropsBlock)
                {
                    ItemStack seeds = ItemStack.EMPTY;
                    List<ItemStack> drops = Block.getDrops(event.getState(), (ServerWorld) world, event.getPos(), null);
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
                        seeds = findSeeds(event.getPlayer(), stack.getItem());
                    }

                    drops.forEach(drop -> Block.spawnAsEntity(world, event.getPos(), drop));
                    event.getState().spawnAdditionalDrops(world, event.getPos(), ItemStack.EMPTY);
                    world.setBlockState(event.getPos(), Blocks.AIR.getDefaultState());

                    BlockState state = blockItem.getBlock().getDefaultState();
                    if(!seeds.isEmpty() & blockItem.getBlock().isValidPosition(state, world, event.getPos()))
                    {
                        MinecraftServer server = LogicalSidedProvider.INSTANCE.get(LogicalSide.SERVER);
                        server.enqueue(new TickDelayedTask(0, () -> world.setBlockState(event.getPos(), state, 3)));
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
