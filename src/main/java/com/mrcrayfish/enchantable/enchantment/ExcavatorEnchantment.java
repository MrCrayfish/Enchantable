package com.mrcrayfish.enchantable.enchantment;

import com.mrcrayfish.enchantable.Reference;
import com.mrcrayfish.enchantable.core.ModEnchantments;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.OreBlock;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnchantmentType;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.IFluidState;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolItem;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

/**
 * Author: MrCrayfish
 */
@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public class ExcavatorEnchantment extends Enchantment
{
    protected ExcavatorEnchantment()
    {
        super(Rarity.VERY_RARE, EnchantmentType.DIGGER, new EquipmentSlotType[]{EquipmentSlotType.MAINHAND});
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
    public boolean canApplyTogether(Enchantment enchantment)
    {
        return super.canApplyTogether(enchantment) && enchantment != Enchantments.FORTUNE && enchantment != Enchantments.SILK_TOUCH;
    }

    @SubscribeEvent
    public static void onPlayerBreak(BlockEvent.BreakEvent event)
    {
        ItemStack heldItem = event.getPlayer().getHeldItemMainhand();
        if(heldItem.isEmpty()) return;

        if(!EnchantmentHelper.getEnchantments(heldItem).containsKey(ModEnchantments.EXCAVATOR.get()))
        {
            return;
        }

        PlayerEntity player = event.getPlayer();
        Direction direction = Direction.getFacingDirections(event.getPlayer())[0];
        World world = event.getPlayer().getEntityWorld();
        BlockPos pos = event.getPos();

        Set<ToolType> toolTypes = new HashSet<>();
        BlockState source = event.getState();
        if(heldItem.getItem() instanceof ToolItem)
        {
            toolTypes = heldItem.getItem().getToolTypes(heldItem);
        }

        int size = 3;
        Direction.Axis axis = direction.getAxis();
        if(axis.isHorizontal())
        {
            direction = direction.rotateY();
            for(int i = 0; i < size; i++)
            {
                for(int j = 0; j < size; j++)
                {
                    destroyBlock(world, toolTypes, pos.add(direction.getAxis().getCoordinate(i - (size - 1) / 2, 0, 0), j - (size - 1) / 2, direction.getAxis().getCoordinate(0, 0, i - (size - 1) / 2)), true, player);
                }
            }
        }
        else
        {
            for(int i = 0; i < size; i++)
            {
                for(int j = 0; j < size; j++)
                {
                    destroyBlock(world, toolTypes, pos.add(i - (size - 1) / 2, 0, j - (size - 1) / 2), true, player);
                }
            }
        }
    }

    private static boolean destroyBlock(World world, Set<ToolType> toolTypes, BlockPos pos, boolean spawnDrops, Entity entity)
    {
        BlockState blockState = world.getBlockState(pos);
        if(blockState.isAir(world, pos))
        {
            return false;
        }
        if(toolTypes.stream().anyMatch(toolType -> blockState.getBlock().isToolEffective(blockState, toolType)))
        {
            boolean ignoreOre = toolTypes.contains(ToolType.PICKAXE);
            if(blockState.getBlock() instanceof OreBlock && ignoreOre)
            {
                return false;
            }
            IFluidState fluidState = world.getFluidState(pos);
            if(spawnDrops)
            {
                TileEntity tileEntity = blockState.hasTileEntity() ? world.getTileEntity(pos) : null;
                Block.spawnDrops(blockState, world, pos, tileEntity, entity, ItemStack.EMPTY);
            }
            return world.setBlockState(pos, fluidState.getBlockState(), 3);
        }
        return false;
    }
}
