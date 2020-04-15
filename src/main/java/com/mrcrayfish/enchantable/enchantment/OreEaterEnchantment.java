package com.mrcrayfish.enchantable.enchantment;

import com.mrcrayfish.enchantable.Enchantable;
import com.mrcrayfish.enchantable.Reference;
import com.mrcrayfish.enchantable.core.ModEnchantments;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.IFluidState;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ToolItem;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
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
        return super.canApplyTogether(enchantment) && enchantment != ModEnchantments.EXCAVATOR.get();
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
        if(!state.getBlock().isIn(Tags.Blocks.ORES))
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

        int damageAmount = 0;
        int durability = heldItem.getMaxDamage() - heldItem.getDamage();
        Block targetBlock = state.getBlock();
        Set<BlockPos> orePositions = gatherBlocks(targetBlock, world, pos, 2 + Math.max(0, level - 1) * 2);
        for(BlockPos orePos : orePositions)
        {
            if(destroyBlock(world, toolTypes, orePos, true, heldItem, player))
            {
                damageAmount++;
            }
            if(damageAmount >= durability)
            {
                break;
            }
        }

        /* Handles applying damage to the tool and considers if it has an unbreaking enchantment */
        heldItem.damageItem(damageAmount, player, player1 -> player1.sendBreakAnimation(Hand.MAIN_HAND));
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
            if(spawnDrops && !player.isCreative())
            {
                TileEntity tileEntity = blockState.hasTileEntity() ? world.getTileEntity(pos) : null;
                blockState.getBlock().harvestBlock(world, player, pos, blockState, tileEntity, stack);
            }
            int fortuneLevel = EnchantmentHelper.getEnchantmentLevel(Enchantments.FORTUNE, stack);
            int silkLevel = EnchantmentHelper.getEnchantmentLevel(Enchantments.SILK_TOUCH, stack);
            int exp = blockState.getExpDrop(world, pos, fortuneLevel, silkLevel);
            if(world.setBlockState(pos, fluidState.getBlockState(), 3))
            {
                if(!player.isCreative())
                {
                    blockState.getBlock().dropXpOnBlockBreak(world, pos, exp);
                }
            }
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
     * @param pos    the position of the block being targeted
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

        if(!state.getBlock().isIn(Tags.Blocks.ORES))
        {
            return 0;
        }

        float totalDigSpeed = 0;
        int totalBlocks = 0;
        int durability = heldItem.getMaxDamage() - heldItem.getDamage();
        Block targetBlock = state.getBlock();
        Set<BlockPos> orePositions = gatherBlocks(targetBlock, world, pos, 2 + Math.max(0, level - 1) * 2);
        for(BlockPos orePos : orePositions)
        {
            state = world.getBlockState(orePos);
            if(ExcavatorEnchantment.isToolEffective(toolTypes, state, player, world, orePos))
            {
                totalDigSpeed += ExcavatorEnchantment.getDigSpeed(player, state, orePos);
                totalBlocks++;
            }
            if(totalBlocks >= durability)
            {
                break;
            }
        }

        if(totalBlocks <= 0)
        {
            return 0;
        }

        EffectInstance instance = player.getActivePotionEffect(Effects.HASTE);
        if(instance != null)
        {
            totalBlocks = (int) (totalBlocks * (instance.getAmplifier() * 0.5));
        }

        return (totalDigSpeed / (float) totalBlocks) / (float) totalBlocks;
    }

    /**
     * Gathers a set of surrounding blocks of the same type starting from an initial block position.
     * The search uses a modified breadth first search that limits the depth.
     *
     * @param targetBlock The block to search for. It can be null to search for all blocks
     * @param world       The world to search for blocks
     * @param pos         The starting position of the search
     * @param depth       The depth limit of the search
     * @return A set of block positions
     */
    private static Set<BlockPos> gatherBlocks(@Nullable Block targetBlock, World world, BlockPos pos, int depth)
    {
        Queue<BlockEntry> queue = new LinkedList<>();
        Set<BlockPos> explored = new LinkedHashSet<>();
        queue.add(new BlockEntry(pos, depth));
        explored.add(pos);
        while(!queue.isEmpty())
        {
            BlockEntry entry = queue.remove();
            if(entry.depth - 1 > 0)
            {
                addMatchingBlockToQueue(targetBlock, world, entry.pos.north(), queue, explored, entry.depth - 1);
                addMatchingBlockToQueue(targetBlock, world, entry.pos.east(), queue, explored, entry.depth - 1);
                addMatchingBlockToQueue(targetBlock, world, entry.pos.south(), queue, explored, entry.depth - 1);
                addMatchingBlockToQueue(targetBlock, world, entry.pos.west(), queue, explored, entry.depth - 1);
                addMatchingBlockToQueue(targetBlock, world, entry.pos.up(), queue, explored, entry.depth - 1);
                addMatchingBlockToQueue(targetBlock, world, entry.pos.down(), queue, explored, entry.depth - 1);
            }
            explored.add(entry.pos);
        }
        return explored;
    }

    /**
     * Adds the matching block at the specified position to the provided queue. This is used in
     * conjunction with {@link OreEaterEnchantment#gatherBlocks} to search for blocks.
     *
     * @param targetBlock The block to search for. It can be null to search for all blocks
     * @param world       The world to search for blocks
     * @param pos         The position to test
     * @param queue       A queue of block entries that have not been searched
     * @param explored    A set of block position that have been searched
     * @param depth       The new depth limit
     */
    private static void addMatchingBlockToQueue(@Nullable Block targetBlock, World world, BlockPos pos, Queue<BlockEntry> queue, Set<BlockPos> explored, int depth)
    {
        BlockState state = world.getBlockState(pos);
        if(state.isAir(world, pos))
        {
            return;
        }
        if(targetBlock == null || state.getBlock() == targetBlock)
        {
            if(!explored.contains(pos))
            {
                queue.offer(new BlockEntry(pos, depth));
            }
        }
    }

    private static class BlockEntry
    {
        private BlockPos pos;
        private int depth;

        private BlockEntry(BlockPos pos, int depth)
        {
            this.pos = pos;
            this.depth = depth;
        }

        public BlockPos getPos()
        {
            return pos;
        }

        public int getDepth()
        {
            return depth;
        }

        @Override
        public boolean equals(Object o)
        {
            if(this == o) return true;
            if(o == null || getClass() != o.getClass()) return false;
            BlockEntry blockEntry = (BlockEntry) o;
            return Objects.equals(pos, blockEntry.pos);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(pos);
        }
    }
}
