package com.mrcrayfish.enchantable;

import com.mojang.datafixers.util.Pair;
import com.mrcrayfish.enchantable.core.ModEnchantments;
import com.mrcrayfish.enchantable.event.EditBlockEvent;
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
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import java.lang.reflect.Field;

/**
 * Author: MrCrayfish
 */
@Mod(Reference.MOD_ID)
public class Enchantable
{
    public Enchantable()
    {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void canPlayerEditBlock(EditBlockEvent event)
    {
        BlockState state = event.getWorld().getBlockState(event.getPos());
        if(state.getBlock() instanceof CropsBlock)
        {
            ItemStack heldItem = event.getPlayer().getHeldItemMainhand();
            if(heldItem.isEmpty())
                return;

            if(!EnchantmentHelper.getEnchantments(heldItem).containsKey(ModEnchantments.REPLANTING))
                return;

            CropsBlock crop = (CropsBlock) state.getBlock();
            if(state.get(CropsBlock.AGE) != crop.getMaxAge())
            {
                event.setCanceled(true);
            }
        }
    }

    public static boolean fireEditBlockEvent(PlayerEntity player, World world, BlockPos pos)
    {
        return MinecraftForge.EVENT_BUS.post(new EditBlockEvent(player, world, pos));
    }
}
