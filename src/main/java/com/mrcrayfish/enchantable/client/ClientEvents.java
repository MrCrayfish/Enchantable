package com.mrcrayfish.enchantable.client;

import com.mrcrayfish.enchantable.Enchantable;
import com.mrcrayfish.enchantable.Reference;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.CPlayerDiggingPacket;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Author: MrCrayfish
 */
@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public class ClientEvents
{
    private static boolean needsResetting = true;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event)
    {
        if(event.phase == TickEvent.Phase.START)
        {
            Minecraft mc = Minecraft.getInstance();
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
