package com.mrcrayfish.enchantable.enchantment;

import com.mrcrayfish.enchantable.Reference;
import com.mrcrayfish.enchantable.core.ModEnchantments;
import com.mrcrayfish.enchantable.core.ModSounds;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnchantmentType;
import net.minecraft.enchantment.ProtectionEnchantment;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.particles.BlockParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.LogicalSidedProvider;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Map;

/**
 * Author: MrCrayfish
 */
@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public class StompingEnchantment extends Enchantment
{
    public StompingEnchantment()
    {
        super(Rarity.UNCOMMON, EnchantmentType.ARMOR_FEET, new EquipmentSlotType[]{EquipmentSlotType.FEET});
        this.setRegistryName(new ResourceLocation(Reference.MOD_ID, "stomping"));
    }

    @Override
    public int getMinLevel()
    {
        return 1;
    }

    @Override
    public int getMaxLevel()
    {
        return 4;
    }

    @Override
    protected boolean canApplyTogether(Enchantment enchantment)
    {
        if(enchantment instanceof ProtectionEnchantment)
        {
            ProtectionEnchantment protection = (ProtectionEnchantment) enchantment;
            return protection.protectionType != ProtectionEnchantment.Type.FALL;
        }
        return true;
    }

    @SubscribeEvent
    public static void onPlayerFallDamage(LivingDamageEvent event)
    {
        if(event.getSource() == DamageSource.FALL)
        {
            LivingEntity entity = event.getEntityLiving();
            if(entity instanceof PlayerEntity)
            {
                PlayerEntity player = (PlayerEntity) entity;
                ItemStack stack = player.getItemStackFromSlot(EquipmentSlotType.FEET);
                if(!stack.isEmpty())
                {
                    Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(stack);
                    if(enchantments.containsKey(ModEnchantments.STOMPING))
                    {
                        int level = enchantments.get(ModEnchantments.STOMPING);
                        float strengthFactor = 0.8F * (level / 4.0F);
                        List<LivingEntity> entities = player.world.getEntitiesWithinAABB(LivingEntity.class, player.getBoundingBox().grow(5, 0, 5), LivingEntity::isAlive);
                        entities.remove(player);
                        if(entities.size() > 0)
                        {
                            float fallDamage = event.getAmount();
                            event.setAmount(Math.max(0F, fallDamage - fallDamage * strengthFactor));
                            for(LivingEntity livingEntity : entities)
                            {
                                /* If PVP is not enabled, prevent stomping from damaging players */
                                if(livingEntity instanceof PlayerEntity)
                                {
                                    MinecraftServer server = LogicalSidedProvider.INSTANCE.get(LogicalSide.SERVER);
                                    if(!server.isPVPEnabled())
                                    {
                                        continue;
                                    }
                                }

                                float distance = livingEntity.getDistance(player);
                                float distanceFactor = Math.max(0.5F, 1.0F - distance / 5.0F);
                                livingEntity.attackEntityFrom(DamageSource.GENERIC, fallDamage * strengthFactor * distanceFactor * 2.0F);
                                livingEntity.setRevengeTarget(player);
                                if(livingEntity.world instanceof ServerWorld)
                                {
                                    BlockState state = livingEntity.world.getBlockState(livingEntity.getPosition().down());
                                    ServerWorld serverWorld = (ServerWorld) livingEntity.world;
                                    serverWorld.spawnParticle(new BlockParticleData(ParticleTypes.BLOCK, state), livingEntity.posX, livingEntity.posY, livingEntity.posZ, 50, 0, 0, 0, (double) 0.15F);
                                    serverWorld.playSound(null, livingEntity.posX, livingEntity.posY, livingEntity.posZ, ModSounds.ENTITY_PLAYER_STOMP, SoundCategory.PLAYERS, 1.0F, 1.0F);
                                }

                                /* Cause the entity to bop up into the air */
                                double stompStrength = 0.3 * (level / 4.0);
                                Vec3d direction = new Vec3d(livingEntity.posX - player.posX, 0, livingEntity.posZ - player.posZ).normalize();
                                livingEntity.setMotion(direction.x * stompStrength, stompStrength, direction.z * stompStrength);
                                livingEntity.addVelocity(direction.x * stompStrength, stompStrength, direction.z * stompStrength);
                                livingEntity.velocityChanged = true;
                            }
                            stack.damageItem(entities.size(), player, entity1 -> {
                                entity1.sendBreakAnimation(EquipmentSlotType.func_220318_a(EquipmentSlotType.Group.ARMOR, EquipmentSlotType.FEET.getIndex()));
                            });
                        }
                    }
                }
            }
        }
    }
}
