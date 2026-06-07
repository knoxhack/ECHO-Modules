package com.knoxhack.echonexusprotocol.entity;

import com.knoxhack.echonexusprotocol.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ArchiveSeekerEntity extends NexusMobEntity {
   private ItemStack stolenStack = ItemStack.EMPTY;

   public ArchiveSeekerEntity(EntityType<? extends ArchiveSeekerEntity> type, Level level) { super(type, level); }
   public void tick() { super.tick(); if (!this.level().isClientSide() && this.tickCount % 120 == 0) { for (Player player : this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(4.0D))) { ItemStack stolen = stealMemoryItem(player); if (!stolen.isEmpty()) { this.stolenStack = stolen.copy(); player.sendSystemMessage(Component.literal("ECHO-7 // Archive Seeker stole " + stolen.getHoverName().getString() + ". Kill it to recover the memory.")); if (this.level() instanceof ServerLevel serverLevel) { serverLevel.sendParticles(ParticleTypes.ENCHANT, this.getX(), this.getY() + 1.1D, this.getZ(), 18, 0.35D, 0.45D, 0.35D, 0.12D); serverLevel.playSound(null, this.blockPosition(), com.knoxhack.echonexusprotocol.registry.ModSounds.REALITY_TEAR_PULSE.get(), SoundSource.HOSTILE, 0.5F, 1.35F); } } this.teleportTo(this.getX() + (this.random.nextDouble() - 0.5D) * 8.0D, this.getY(), this.getZ() + (this.random.nextDouble() - 0.5D) * 8.0D); return; } } }
   public void die(DamageSource source) { if (!this.level().isClientSide() && !this.stolenStack.isEmpty() && this.level() instanceof ServerLevel serverLevel) { this.spawnAtLocation(serverLevel, this.stolenStack.copy()); this.stolenStack = ItemStack.EMPTY; } super.die(source); }
   public static ItemStack stealMemoryItem(Player player) { for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) { ItemStack stack = player.getInventory().getItem(slot); if (stack.is(ModItems.MEMORY_SHARD.get()) || stack.is(ModItems.BLACKBOX_FRAGMENT.get()) || stack.is(ModItems.DATA_FRAGMENT.get())) { ItemStack stolen = stack.copyWithCount(1); stack.shrink(1); return stolen; } } return ItemStack.EMPTY; }
}
