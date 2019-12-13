package com.mrcrayfish.enchantable.event;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.Cancelable;

/**
 * Author: MrCrayfish
 */
@Cancelable
public class EditBlockEvent extends PlayerEvent
{
    private World world;
    private BlockPos pos;

    public EditBlockEvent(PlayerEntity player, World world, BlockPos pos)
    {
        super(player);
        this.world = world;
        this.pos = pos;
    }

    public World getWorld()
    {
        return world;
    }

    public BlockPos getPos()
    {
        return pos;
    }
}
