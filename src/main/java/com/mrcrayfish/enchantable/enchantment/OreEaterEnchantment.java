package com.mrcrayfish.enchantable.enchantment;

import com.mrcrayfish.enchantable.Enchantable;
import com.mrcrayfish.enchantable.Reference;
import com.mrcrayfish.enchantable.core.ModEnchantments;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.OreBlock;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.fluid.IFluidState;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ToolItem;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

/**
 * Author: MrCrayfish
 */
@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public class OreEaterEnchantment extends Enchantment
{
    public OreEaterEnchantment()
    {
        super(Rarity.VERY_RARE, Enchantable.PICKAXE, new EquipmentSlotType[]{EquipmentSlotType.MAINHAND});
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

    @Override
    public int getMaxLevel()
    {
        return 3;
    }

    @Override
    public boolean canApplyTogether(Enchantment enchantment)
    {
        return super.canApplyTogether(enchantment) && enchantment != Enchantments.SILK_TOUCH && enchantment != ModEnchantments.EXCAVATOR.get();
    }

    @SubscribeEvent
    public static void onPlayerBreak(BlockEvent.BreakEvent event)
    {
        ItemStack heldItem = event.getPlayer().getHeldItemMainhand();
        if(heldItem.isEmpty()) return;

        if(!EnchantmentHelper.getEnchantments(heldItem).containsKey(ModEnchantments.ORE_EATER.get()))
        {
            return;
        }

        int level = EnchantmentHelper.getEnchantments(heldItem).get(ModEnchantments.ORE_EATER.get());
        PlayerEntity player = event.getPlayer();
        World world = event.getPlayer().getEntityWorld();
        BlockPos pos = event.getPos();

        BlockState state = world.getBlockState(pos);
        if(!(state.getBlock() instanceof OreBlock))
        {
            return;
        }

        Set<ToolType> toolTypes = new HashSet<>();
        if(heldItem.getItem() instanceof ToolItem)
        {
            toolTypes = heldItem.getItem().getToolTypes(heldItem);
        }

        if(!ExcavatorEnchantment.isToolEffective(toolTypes, state, player, world, pos))
        {
            return;
        }

        Block targetBlock = state.getBlock();
        Queue<OreEntry> queue = new LinkedList<>();
        Set<BlockPos> explored = new HashSet<>();
        OreEntry start = new OreEntry(pos, 2 + Math.max(0, level - 1) * 2);
        queue.add(start);
        explored.add(pos);

        while(!queue.isEmpty())
        {
            OreEntry oreEntry = queue.remove();
            if(oreEntry.distance - 1 > 0)
            {
                getNeighbouringOre(targetBlock, world, oreEntry.pos.north(), queue, explored, oreEntry.distance - 1);
                getNeighbouringOre(targetBlock, world, oreEntry.pos.east(), queue, explored, oreEntry.distance - 1);
                getNeighbouringOre(targetBlock, world, oreEntry.pos.south(), queue, explored, oreEntry.distance - 1);
                getNeighbouringOre(targetBlock, world, oreEntry.pos.west(), queue, explored, oreEntry.distance - 1);
                getNeighbouringOre(targetBlock, world, oreEntry.pos.up(), queue, explored, oreEntry.distance - 1);
                getNeighbouringOre(targetBlock, world, oreEntry.pos.down(), queue, explored, oreEntry.distance - 1);
            }
            explored.add(oreEntry.pos);
        }

        int damageAmount = 0;
        for(BlockPos orePos : explored)
        {
            if(destroyBlock(world, toolTypes, orePos, true, heldItem, player))
            {
                damageAmount++;
            }
        }

        /* Handles applying damage to the tool and considers if it has an unbreaking enchantment */
        heldItem.attemptDamageItem(damageAmount, world.rand, (ServerPlayerEntity) player);
    }

    private static void getNeighbouringOre(Block targetBlock, World world, BlockPos pos, Queue<OreEntry> queue, Set<BlockPos> explored, int distance)
    {
        BlockState state = world.getBlockState(pos);
        if(state.getBlock() == targetBlock)
        {
            if(!explored.contains(pos))
            {
                queue.offer(new OreEntry(pos, distance));
            }
        }
    }



    private static boolean destroyBlock(World world, Set<ToolType> toolTypes, BlockPos pos, boolean spawnDrops, ItemStack stack, PlayerEntity player)
    {
        BlockState blockState = world.getBlockState(pos);
        if(blockState.isAir(world, pos))
        {
            return false;
        }
        if(ExcavatorEnchantment.isToolEffective(toolTypes, blockState, player, world, pos))
        {
            IFluidState fluidState = world.getFluidState(pos);
            if(spawnDrops)
            {
                TileEntity tileEntity = blockState.hasTileEntity() ? world.getTileEntity(pos) : null;
                Block.spawnDrops(blockState, world, pos, tileEntity, player, stack);
            }
            /*MinecraftServer server = world.getServer();
            server.enqueue(new TickDelayedTask());*/
            return world.setBlockState(pos, fluidState.getBlockState(), 3);
        }
        return false;
    }

    @SubscribeEvent
    public static void onPlayerMineSpeed(PlayerEvent.BreakSpeed event)
    {
        float effectiveSpeed = getEffectiveDigSpeed(event.getPlayer(), event.getPos());
        if(effectiveSpeed > 0)
        {
            event.setNewSpeed(effectiveSpeed);
        }
    }

    /**
     * Gets the effective speed when using the excavator enchantment. Essentially gets the average
     * speed and divides it by the number of blocks it can effectively mine.
     *
     * @param player the player mining the blocks
     * @param pos the position of the block being targeted
     * @return the effective speed
     */
    private static float getEffectiveDigSpeed(PlayerEntity player, BlockPos pos)
    {
        ItemStack heldItem = player.getHeldItemMainhand();
        if(heldItem.isEmpty()) return 0;

        if(!EnchantmentHelper.getEnchantments(heldItem).containsKey(ModEnchantments.ORE_EATER.get()))
        {
            return 0;
        }

        if(!(heldItem.getItem() instanceof PickaxeItem))
        {
            return 0;
        }

        int level = EnchantmentHelper.getEnchantments(heldItem).get(ModEnchantments.ORE_EATER.get());
        World world = player.getEntityWorld();
        Set<ToolType> toolTypes = heldItem.getItem().getToolTypes(heldItem);

        BlockState state = world.getBlockState(pos);
        if(!ExcavatorEnchantment.isToolEffective(toolTypes, state, player, world, pos))
        {
            return 0;
        }

        if(!(state.getBlock() instanceof OreBlock))
        {
            return 0;
        }

        Block targetBlock = state.getBlock();
        Queue<OreEntry> queue = new LinkedList<>();
        Set<BlockPos> explored = new HashSet<>();
        OreEntry start = new OreEntry(pos, 2 + Math.max(0, level - 1) * 2);
        queue.add(start);
        explored.add(pos);

        while(!queue.isEmpty())
        {
            OreEntry oreEntry = queue.remove();
            if(oreEntry.distance - 1 > 0)
            {
                getNeighbouringOre(targetBlock, world, oreEntry.pos.north(), queue, explored, oreEntry.distance - 1);
                getNeighbouringOre(targetBlock, world, oreEntry.pos.east(), queue, explored, oreEntry.distance - 1);
                getNeighbouringOre(targetBlock, world, oreEntry.pos.south(), queue, explored, oreEntry.distance - 1);
                getNeighbouringOre(targetBlock, world, oreEntry.pos.west(), queue, explored, oreEntry.distance - 1);
                getNeighbouringOre(targetBlock, world, oreEntry.pos.up(), queue, explored, oreEntry.distance - 1);
                getNeighbouringOre(targetBlock, world, oreEntry.pos.down(), queue, explored, oreEntry.distance - 1);
            }
            explored.add(oreEntry.pos);
        }

        float totalDigSpeed = 0;
        int totalBlocks = 0;
        for(BlockPos orePos : explored)
        {
            state = world.getBlockState(orePos);
            if(ExcavatorEnchantment.isToolEffective(toolTypes, state, player, world, orePos))
            {
                totalDigSpeed += ExcavatorEnchantment.getDigSpeed(player, state, orePos);
                totalBlocks++;
            }
        }

        if(totalBlocks <= 0)
        {
            return 0;
        }
        return (totalDigSpeed / (float) totalBlocks) / (float) totalBlocks;
    }

    private static class OreEntry
    {
        private BlockPos pos;
        private int distance;

        public OreEntry(BlockPos pos, int distance)
        {
            this.pos = pos;
            this.distance = distance;
        }

        public BlockPos getPos()
        {
            return pos;
        }

        public int getDistance()
        {
            return distance;
        }

        @Override
        public boolean equals(Object o)
        {
            if(this == o) return true;
            if(o == null || getClass() != o.getClass()) return false;
            OreEntry oreEntry = (OreEntry) o;
            return Objects.equals(pos, oreEntry.pos);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(pos);
        }
    }
}
