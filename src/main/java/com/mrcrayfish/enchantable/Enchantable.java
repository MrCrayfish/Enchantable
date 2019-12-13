package com.mrcrayfish.enchantable;

import com.mojang.datafixers.util.Pair;
import com.mrcrayfish.enchantable.core.ModEnchantments;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropsBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.multiplayer.PlayerController;
import net.minecraft.client.util.PosAndRotation;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPlayerDiggingPacket;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import java.lang.reflect.Field;

/**
 * Author: MrCrayfish
 */
@Mod(Reference.MOD_ID)
public class Enchantable
{
    public static boolean canLeftClickBlock(PlayerEntity player, BlockPos pos, Direction direction)
    {
        if(player.isCreative())
            return true;

        BlockState state = player.world.getBlockState(pos);
        if(state.getBlock() instanceof CropsBlock)
        {
            ItemStack heldItem = player.getHeldItemMainhand();
            if(heldItem.isEmpty())
                return true;

            if(!EnchantmentHelper.getEnchantments(heldItem).containsKey(ModEnchantments.REPLANTING))
                return true;

            CropsBlock crop = (CropsBlock) state.getBlock();
            return state.get(CropsBlock.AGE) == crop.getMaxAge();
        }
        return true;
    }

    public static boolean canEditBlock(PlayerEntity player, World world, BlockPos pos)
    {
        if(!world.isRemote)
            return true;

        if(player.isCreative())
            return true;

        BlockState state = world.getBlockState(pos);
        if(state.getBlock() instanceof CropsBlock)
        {
            ItemStack heldItem = player.getHeldItemMainhand();
            if(heldItem.isEmpty())
                return true;

            if(!EnchantmentHelper.getEnchantments(heldItem).containsKey(ModEnchantments.REPLANTING))
                return true;

            CropsBlock crop = (CropsBlock) state.getBlock();
            return state.get(CropsBlock.AGE) == crop.getMaxAge();
        }
        return true;
    }

    public static void resetBreakParticles()
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
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}
