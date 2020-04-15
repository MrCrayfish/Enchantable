package com.mrcrayfish.enchantable.enchantment;

import com.mrcrayfish.enchantable.Reference;
import com.mrcrayfish.enchantable.core.ModEnchantments;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnchantmentType;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.IFluidState;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolItem;
import net.minecraft.potion.EffectUtils;
import net.minecraft.potion.Effects;
import net.minecraft.tags.FluidTags;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

/**
 * Author: MrCrayfish
 */
@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public class ExcavatorEnchantment extends Enchantment
{
    public static final int BASE_SIZE = 3;

    public ExcavatorEnchantment()
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
        return super.canApplyTogether(enchantment) && enchantment != Enchantments.FORTUNE && enchantment != ModEnchantments.ORE_EATER.get();
    }

    @Override
    public int getMaxLevel()
    {
        return 2;
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

        if(!EnchantmentHelper.getEnchantments(heldItem).containsKey(ModEnchantments.EXCAVATOR.get()))
        {
            return 0;
        }

        int level = EnchantmentHelper.getEnchantments(heldItem).get(ModEnchantments.EXCAVATOR.get());
        int size = BASE_SIZE + Math.max(0, level - 1) * 2;

        World world = player.getEntityWorld();
        Direction direction = Direction.getFacingDirections(player)[0];
        double reach = player.getAttribute(PlayerEntity.REACH_DISTANCE).getValue();
        RayTraceResult result = player.pick(reach, 0, false);
        if(result.getType() == RayTraceResult.Type.BLOCK)
        {
            BlockRayTraceResult blockResult = (BlockRayTraceResult) result;
            direction = blockResult.getFace();
        }

        Set<ToolType> toolTypes = new HashSet<>();
        if(heldItem.getItem() instanceof ToolItem)
        {
            toolTypes = heldItem.getItem().getToolTypes(heldItem);
        }

        BlockState blockState = world.getBlockState(pos);
        if(!net.minecraftforge.common.ForgeHooks.canHarvestBlock(blockState, player, world, pos))
        {
            return 0;
        }

        if(blockState.getBlock().isIn(Tags.Blocks.ORES))
        {
            return 0;
        }

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

        Pair<Float, Integer> pair = getDestroySpeed(world, player, size, toolTypes, heldItem, function);
        float totalDigSpeed = pair.getLeft();
        int totalBlocks = pair.getRight();
        if(totalBlocks <= 0)
        {
            return 0;
        }
        return (totalDigSpeed / (float) totalBlocks) / (float) totalBlocks;
    }

    private static Pair<Float, Integer> getDestroySpeed(World world, PlayerEntity player, int size, Set<ToolType> toolTypes, ItemStack stack, Function<Pair<Integer, Integer>, BlockPos> function)
    {
        int durability = stack.getMaxDamage() - stack.getDamage();
        float totalDigSpeed = 0;
        int totalBlocks = 0;
        for(int i = 0; i < size; i++)
        {
            for(int j = 0; j < size; j++)
            {
                BlockPos blockPos = function.apply(Pair.of(i, j));
                BlockState blockState = world.getBlockState(blockPos);
                if(blockState.isAir(world, blockPos))
                {
                    continue;
                }
                if(isToolEffective(toolTypes, blockState, player, world, blockPos))
                {
                    if(blockState.getBlock().isIn(Tags.Blocks.ORES))
                    {
                        continue;
                    }
                    totalDigSpeed += getDigSpeed(player, blockState, blockPos);
                    totalBlocks++;

                    if(totalBlocks >= durability)
                    {
                        return Pair.of(totalDigSpeed, totalBlocks);
                    }
                }
            }
        }
        return Pair.of(totalDigSpeed, totalBlocks);
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

        int level = EnchantmentHelper.getEnchantments(heldItem).get(ModEnchantments.EXCAVATOR.get());
        int size = BASE_SIZE + Math.max(0, level - 1) * 2;

        PlayerEntity player = event.getPlayer();
        Direction direction = Direction.getFacingDirections(event.getPlayer())[0];
        double reach = player.getAttribute(PlayerEntity.REACH_DISTANCE).getValue();
        RayTraceResult result = player.pick(reach, 0, false);
        if(result.getType() == RayTraceResult.Type.BLOCK)
        {
            BlockRayTraceResult blockResult = (BlockRayTraceResult) result;
            direction = blockResult.getFace();
        }
        World world = event.getPlayer().getEntityWorld();
        BlockPos pos = event.getPos();

        Set<ToolType> toolTypes = new HashSet<>();
        if(heldItem.getItem() instanceof ToolItem)
        {
            toolTypes = heldItem.getItem().getToolTypes(heldItem);
        }

        BlockState blockState = world.getBlockState(pos);
        if(!isToolEffective(toolTypes, blockState, player, world, pos))
        {
            return;
        }

        if(blockState.getBlock().isIn(Tags.Blocks.ORES))
        {
            return;
        }

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

        int durability = heldItem.getMaxDamage() - heldItem.getDamage();
        if(durability > 1) //No point breaking blocks if only one durability left
        {
            int damageAmount = destroyBlocks(world, pos, player, size, toolTypes, heldItem, function);

            /* Handles applying damage to the tool and considers if it has an unbreaking enchantment */
            heldItem.damageItem(damageAmount, player, player1 -> player1.sendBreakAnimation(Hand.MAIN_HAND));
        }
    }

    private static int destroyBlocks(World world, BlockPos source, PlayerEntity player, int size, Set<ToolType> toolTypes, ItemStack stack, Function<Pair<Integer, Integer>, BlockPos> function)
    {
        int durability = stack.getMaxDamage() - stack.getDamage();
        int damageAmount = 0;
        for(int i = 0; i < size; i++)
        {
            for(int j = 0; j < size; j++)
            {
                BlockPos newPos = function.apply(Pair.of(i, j));
                if(newPos.equals(source))
                {
                    continue;
                }
                if(destroyBlock(world, toolTypes, newPos, true, stack, player))
                {
                    damageAmount++;
                }
                if(damageAmount >= durability)
                {
                    return damageAmount;
                }
            }
        }
        return damageAmount;
    }

    private static boolean destroyBlock(World world, Set<ToolType> toolTypes, BlockPos pos, boolean spawnDrops, ItemStack stack, PlayerEntity player)
    {
        BlockState blockState = world.getBlockState(pos);
        if(blockState.isAir(world, pos))
        {
            return false;
        }
        if(isToolEffective(toolTypes, blockState, player, world, pos))
        {
            if(blockState.getBlock().isIn(Tags.Blocks.ORES))
            {
                return false;
            }
            IFluidState fluidState = world.getFluidState(pos);
            if(spawnDrops)
            {
                TileEntity tileEntity = blockState.hasTileEntity() ? world.getTileEntity(pos) : null;
                Block.spawnDrops(blockState, world, pos, tileEntity, player, stack);
            }
            return world.setBlockState(pos, fluidState.getBlockState(), 3);
        }
        return false;
    }

    public static boolean isToolEffective(Set<ToolType> toolTypes, BlockState state, PlayerEntity player, World world, BlockPos pos)
    {
        if(toolTypes.stream().noneMatch(toolType -> state.getBlock().isToolEffective(state, toolType)))
        {
            if(state.getMaterial().isToolNotRequired())
            {
                return false;
            }
        }
        return net.minecraftforge.common.ForgeHooks.canHarvestBlock(state, player, world, pos);
    }

    public static float getDigSpeed(PlayerEntity player, BlockState state, @Nullable BlockPos pos)
    {
        float destroySpeed = player.inventory.getDestroySpeed(state);
        if(destroySpeed > 1.0F)
        {
            int efficiencyModifier = EnchantmentHelper.getEfficiencyModifier(player);
            ItemStack heldItem = player.getHeldItemMainhand();
            if(efficiencyModifier > 0 && !heldItem.isEmpty())
            {
                destroySpeed += (float) (efficiencyModifier * efficiencyModifier + 1);
            }
        }

        if(EffectUtils.hasMiningSpeedup(player))
        {
            destroySpeed *= 1.0F + (float) (EffectUtils.getMiningSpeedup(player) + 1) * 0.2F;
        }

        if(player.isPotionActive(Effects.MINING_FATIGUE))
        {
            float multiplier;
            switch(player.getActivePotionEffect(Effects.MINING_FATIGUE).getAmplifier())
            {
                case 0:
                    multiplier = 0.3F;
                    break;
                case 1:
                    multiplier = 0.09F;
                    break;
                case 2:
                    multiplier = 0.0027F;
                    break;
                case 3:
                default:
                    multiplier = 8.1E-4F;
            }

            destroySpeed *= multiplier;
        }

        if(player.areEyesInFluid(FluidTags.WATER) && !EnchantmentHelper.hasAquaAffinity(player))
        {
            destroySpeed /= 5.0F;
        }

        if(!player.onGround)
        {
            destroySpeed /= 5.0F;
        }
        return destroySpeed;
    }
}
