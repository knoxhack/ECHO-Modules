package com.knoxhack.echonexusprotocol.event;

import com.knoxhack.echo.adaptercore.EchoBackendWorldEventBridge;
import com.knoxhack.echonexusprotocol.Config;
import com.knoxhack.echonexusprotocol.data.NexusPlayerData;
import com.knoxhack.echonexusprotocol.item.NexusArmorItem;
import com.knoxhack.echonexusprotocol.registry.ModItems;
import com.knoxhack.echonexusprotocol.registry.ModSounds;
import com.knoxhack.echonexusprotocol.world.NexusWorldData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;

public class NexusArmorEvents {
   public void onPlayerTick(Object event) {
      ServerPlayer player = EchoBackendWorldEventBridge.postTickServerPlayer(event);
      if (player != null) {
         NexusPlayerData data = NexusPlayerData.get(player);
         int before = data.armorLockCooldown();
         data.tickArmorLockCooldown();
         boolean telemetryChanged = player.level().getGameTime() % 20L == 0L && data.refreshFieldTelemetry(player);
         if (before != data.armorLockCooldown() || telemetryChanged) {
            NexusPlayerData.saveAndSync(player, data);
         }
      }
   }
   public void onDamage(Object event) {
      ServerPlayer player = EchoBackendWorldEventBridge.livingDamageServerPlayer(event);
      if (player == null || EchoBackendWorldEventBridge.livingDamageAmount(event) < player.getHealth()) return;
      if (tryEmergencyFieldLock(player)) EchoBackendWorldEventBridge.setLivingDamageAmount(event, 0.0F);
   }

   public static boolean tryEmergencyFieldLock(ServerPlayer player) {
      NexusPlayerData data = NexusPlayerData.get(player);
      if (data.armorLockCooldown() > 0 || !hasFullNexusArmor(player) || !consumeShard(player)) return false;
      data.setArmorLockCooldown((Integer)Config.ARMOR_LOCK_COOLDOWN.get()); data.markGearUsed("nexus_armor_emergency_lock"); NexusPlayerData.saveAndSync(player, data);
      player.setHealth(Math.max(player.getHealth(), 6.0F)); player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 160, 1)); player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 120, 1));
      if (player.level() instanceof ServerLevel serverLevel) { ChunkPos chunk = player.chunkPosition(); NexusWorldData worldData = NexusWorldData.get(serverLevel); worldData.addFieldValue(chunk, (Integer)Config.ARMOR_LOCK_FIELD_GAIN.get()); worldData.addCorruptionPressure(chunk, -(Integer)Config.ARMOR_LOCK_CORRUPTION_REDUCTION.get()); serverLevel.playSound(null, player.blockPosition(), ModSounds.FIELD_STABILIZE.get(), SoundSource.PLAYERS, 0.75F, 0.85F); }
      player.sendSystemMessage(Component.literal("ECHO-7 // Emergency Field Lock consumed one Nexus Shard."));
      return true;
   }
   private static boolean hasFullNexusArmor(Player player) { return isNexusArmor(player.getItemBySlot(EquipmentSlot.HEAD)) && isNexusArmor(player.getItemBySlot(EquipmentSlot.CHEST)) && isNexusArmor(player.getItemBySlot(EquipmentSlot.LEGS)) && isNexusArmor(player.getItemBySlot(EquipmentSlot.FEET)); }
   private static boolean isNexusArmor(ItemStack stack) { return !stack.isEmpty() && stack.getItem() instanceof NexusArmorItem; }
   private static boolean consumeShard(Player player) { for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) { ItemStack stack = player.getInventory().getItem(slot); if (!stack.isEmpty() && stack.is(ModItems.NEXUS_SHARD.get())) { stack.shrink(1); return true; } } return false; }
}
