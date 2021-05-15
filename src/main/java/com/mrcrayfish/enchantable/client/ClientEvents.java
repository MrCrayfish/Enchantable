package com.mrcrayfish.enchantable.client;

import com.google.common.collect.Sets;
import com.mrcrayfish.enchantable.Enchantable;
import com.mrcrayfish.enchantable.Reference;
import com.mrcrayfish.enchantable.core.ModEnchantments;
import com.mrcrayfish.enchantable.enchantment.ExcavatorEnchantment;
import com.mrcrayfish.enchantable.enchantment.OreEaterEnchantment;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.DestroyBlockProgress;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolItem;
import net.minecraft.network.play.client.CPlayerDiggingPacket;
import net.minecraft.util.Direction;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.function.Function;

/**
 * Author: MrCrayfish
 */
@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public class ClientEvents
{
    private static boolean needsResetting = true;
    private static BlockPos lastHittingPos = null;
    private static final Long2ObjectMap<DestroyBlockProgress> DAMAGE_PROGRESS = new Long2ObjectOpenHashMap<>();

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event)
    {
        Minecraft mc = Minecraft.getInstance();
        if(event.phase == TickEvent.Phase.START)
        {
            if(mc.player != null)
            {
                boolean leftClicking = mc.currentScreen == null && mc.gameSettings.keyBindAttack.isKeyDown() && mc.mouseHelper.isMouseGrabbed();
                if(leftClicking && mc.objectMouseOver != null && mc.objectMouseOver.getType() == RayTraceResult.Type.BLOCK)
                {
                    BlockRayTraceResult result = (BlockRayTraceResult) mc.objectMouseOver;
                    BlockPos pos = result.getPos();
                    if(!Enchantable.fireEditBlockEvent(mc.player, mc.world, pos))
                    {
                        needsResetting = true;
                        return;
                    }
                }
                if(needsResetting)
                {
                    ClientEvents.resetBreakParticles();
                    needsResetting = false;
                }
            }
        }
        else if(mc.playerController != null)
        {
            if(!mc.playerController.isHittingBlock)
            {
                clearBreakProgress();
                return;
            }

            PlayerEntity player = mc.player;
            if(player == null)
            {
                clearBreakProgress();
                return;
            }

            ItemStack heldItem = player.getHeldItemMainhand();
            if(heldItem.isEmpty())
            {
                clearBreakProgress();
                return;
            }

            World world = player.world;
            BlockPos pos = mc.playerController.currentBlock;
            Direction direction = Direction.getFacingDirections(player)[0];
            double reach = Objects.requireNonNull(player.getAttribute(ForgeMod.REACH_DISTANCE.get())).getValue();
            RayTraceResult result = player.pick(reach, 0, false);
            if(result.getType() == RayTraceResult.Type.BLOCK)
            {
                BlockRayTraceResult blockResult = (BlockRayTraceResult) result;
                direction = blockResult.getFace();
            }
            BlockState blockState = world.getBlockState(pos);

            if(!net.minecraftforge.common.ForgeHooks.canHarvestBlock(blockState, player, world, pos))
            {
                clearBreakProgress();
                return;
            }

            clearBreakProgress();
            handleExcavatorBreakProgress(mc, pos, player, world, heldItem, blockState, direction);
            handleOreEaterBreakProgress(mc, pos, player, world, heldItem, blockState);
        }
    }

    private static void handleExcavatorBreakProgress(Minecraft mc, BlockPos pos, PlayerEntity player, World world, ItemStack heldItem, BlockState blockState, Direction direction)
    {
        if(!EnchantmentHelper.getEnchantments(heldItem).containsKey(ModEnchantments.EXCAVATOR.get()))
        {
            return;
        }

        if(blockState.getBlock().isIn(Tags.Blocks.ORES))
        {
            return;
        }

        int level = EnchantmentHelper.getEnchantments(heldItem).get(ModEnchantments.EXCAVATOR.get());
        int size = ExcavatorEnchantment.BASE_SIZE + Math.max(0, level - 1) * 2;
        Function<Pair<Integer, Integer>, BlockPos> function;
        if(direction.getAxis().isHorizontal())
        {
            Direction finalDirection = direction.rotateY();
            function = pair -> pos.add(finalDirection.getAxis().getCoordinate(pair.getLeft() - (size - 1) / 2, 0, 0), pair.getRight() - (size - 1) / 2, finalDirection.getAxis().getCoordinate(0, 0, pair.getLeft() - (size - 1) / 2));
        }
        else
        {
            function = pair -> pos.add(pair.getLeft() - (size - 1) / 2, 0, pair.getRight() - (size - 1) / 2);
        }

        DestroyBlockProgress progress = mc.worldRenderer.damagedBlocks.get(player.getEntityId());
        if(progress != null)
        {
            lastHittingPos = pos;
            Set<BlockPos> blocks = getExcavatorBlocks(world, size, pos, player, function);
            blocks.forEach(pos1 -> {
                DestroyBlockProgress subProgress = new DestroyBlockProgress(player.getEntityId(), pos1);
                subProgress.setPartialBlockDamage(progress.getPartialBlockDamage());
                DAMAGE_PROGRESS.put(pos1.toLong(), subProgress);
                mc.worldRenderer.damageProgress.computeIfAbsent(pos1.toLong(), value -> Sets.newTreeSet()).add(subProgress);
            });
        }
    }

    private static void handleOreEaterBreakProgress(Minecraft mc, BlockPos pos, PlayerEntity player, World world, ItemStack heldItem, BlockState blockState)
    {
        if(!EnchantmentHelper.getEnchantments(heldItem).containsKey(ModEnchantments.ORE_EATER.get()))
        {
            return;
        }

        Set<ToolType> toolTypes = heldItem.getItem().getToolTypes(heldItem);
        if(!ExcavatorEnchantment.isToolEffective(toolTypes, blockState, player, world, pos))
        {
            return;
        }

        if(!blockState.getBlock().isIn(Tags.Blocks.ORES))
        {
            return;
        }

        DestroyBlockProgress progress = mc.worldRenderer.damagedBlocks.get(player.getEntityId());
        if(progress == null)
        {
            return;
        }

        Block targetBlock = blockState.getBlock();
        int level = EnchantmentHelper.getEnchantments(heldItem).get(ModEnchantments.ORE_EATER.get());
        Set<BlockPos> orePositions = OreEaterEnchantment.gatherBlocks(targetBlock, world, pos, 2 + Math.max(0, level - 1) * 2);
        for(BlockPos orePos : orePositions)
        {
            blockState = world.getBlockState(orePos);
            if(ExcavatorEnchantment.isToolEffective(toolTypes, blockState, player, world, orePos))
            {
                DestroyBlockProgress subProgress = new DestroyBlockProgress(player.getEntityId(), orePos);
                subProgress.setPartialBlockDamage(progress.getPartialBlockDamage());
                DAMAGE_PROGRESS.put(orePos.toLong(), subProgress);
                mc.worldRenderer.damageProgress.computeIfAbsent(orePos.toLong(), value -> Sets.newTreeSet()).add(subProgress);
            }
        }
    }

    private static void clearBreakProgress()
    {
        if(DAMAGE_PROGRESS.isEmpty())
        {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        DAMAGE_PROGRESS.forEach((posLong, destroyBlockProgress) ->
        {
            Set<DestroyBlockProgress> set = mc.worldRenderer.damageProgress.get(posLong);
            if(set != null)
            {
                set.remove(destroyBlockProgress);
                if(set.isEmpty())
                {
                    mc.worldRenderer.damageProgress.remove(posLong);
                }
            }
        });
        DAMAGE_PROGRESS.clear();
    }

    private static Set<BlockPos> getExcavatorBlocks(World world, int size, BlockPos source, PlayerEntity player, Function<Pair<Integer, Integer>, BlockPos> function)
    {
        ItemStack heldItem = player.getHeldItemMainhand();
        if(heldItem.isEmpty())
        {
            return Collections.emptySet();
        }

        Set<ToolType> toolTypes = new HashSet<>();
        if(heldItem.getItem() instanceof ToolItem)
        {
            toolTypes = heldItem.getItem().getToolTypes(heldItem);
        }

        Set<BlockPos> blocks = new HashSet<>();
        for(int i = 0; i < size; i++)
        {
            for(int j = 0; j < size; j++)
            {
                BlockPos pos = function.apply(Pair.of(i, j));
                if(pos.equals(source))
                {
                    continue;
                }
                BlockState blockState = world.getBlockState(pos);
                if(blockState.isAir(world, pos))
                {
                    continue;
                }
                if(ExcavatorEnchantment.isToolEffective(toolTypes, blockState, player, world, pos))
                {
                    if(blockState.getBlock().isIn(Tags.Blocks.ORES))
                    {
                        continue;
                    }
                    blocks.add(pos);
                }
            }
        }
        return blocks;
    }

    private static void resetBreakParticles()
    {
        try
        {
            Minecraft mc = Minecraft.getInstance();
            BlockPos currentBlock = mc.playerController.currentBlock;
            BlockState blockstate = mc.world.getBlockState(currentBlock);
            mc.getTutorial().onHitBlock(mc.world, currentBlock, blockstate, -1.0F);
            mc.player.connection.sendPacket(new CPlayerDiggingPacket(CPlayerDiggingPacket.Action.ABORT_DESTROY_BLOCK, currentBlock, Direction.DOWN));
            mc.playerController.curBlockDamageMP = 0.0F;
            mc.world.sendBlockBreakProgress(mc.player.getEntityId(), currentBlock, -1);
            mc.playerController.isHittingBlock = false;
            mc.player.resetCooldown();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

}
