package com.mrcrayfish.enchantable.client;

import com.mrcrayfish.enchantable.Enchantable;
import com.mrcrayfish.enchantable.Reference;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * Author: MrCrayfish
 */
@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public class ClientEvents
{
    private static boolean wasLookingAtEditableBlock;

    @SubscribeEvent
    public static void onRawMouseInput(InputEvent.RawMouseEvent event)
    {
        if(event.getButton() != 0 || event.getAction() != GLFW.GLFW_RELEASE)
            return;

        Minecraft mc = Minecraft.getInstance();
        if(mc.player != null)
        {
            if(mc.objectMouseOver != null && mc.objectMouseOver.getType() == RayTraceResult.Type.BLOCK)
            {
                BlockRayTraceResult result = (BlockRayTraceResult) Minecraft.getInstance().objectMouseOver;
                Enchantable.resetBreakParticles();
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event)
    {
        if(event.phase == TickEvent.Phase.START)
        {
            Minecraft mc = Minecraft.getInstance();
            if(mc.player != null)
            {
                if(mc.objectMouseOver != null && mc.objectMouseOver.getType() == RayTraceResult.Type.BLOCK)
                {
                    BlockRayTraceResult result = (BlockRayTraceResult) mc.objectMouseOver;
                    boolean canEditBlock = Enchantable.canEditBlock(mc.player, mc.world, result.getPos());
                    if(wasLookingAtEditableBlock && !canEditBlock)
                    {
                        if(!Enchantable.canEditBlock(mc.player, mc.world, result.getPos()))
                        {
                            wasLookingAtEditableBlock = false;
                            Enchantable.resetBreakParticles();
                        }
                    }
                    wasLookingAtEditableBlock = canEditBlock;
                }
            }
        }
    }
}
