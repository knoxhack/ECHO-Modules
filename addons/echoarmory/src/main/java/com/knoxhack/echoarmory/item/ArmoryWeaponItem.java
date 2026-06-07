package com.knoxhack.echoarmory.item;

import com.knoxhack.echoarmory.content.ArmoryContent;
import com.knoxhack.echoarmory.content.FiringModeDefinition;
import com.knoxhack.echoarmory.content.GearDefinition;
import com.knoxhack.echoarmory.content.ModuleDefinition;
import com.knoxhack.echoarmory.data.ArmoryStance;
import com.knoxhack.echoarmory.data.EnergyState;
import com.knoxhack.echoarmory.data.InstalledModules;
import com.knoxhack.echoarmory.data.InstabilityState;
import com.knoxhack.echoarmory.entity.ArmoryProjectileEntity;
import com.knoxhack.echoarmory.registry.ModDataComponents;
import com.knoxhack.echoarmory.registry.ModItems;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ArmoryWeaponItem extends Item implements ArmoryGearItem {
   private final String gearId;

   public ArmoryWeaponItem(String gearId, Properties properties) {
      super(properties);
      this.gearId = gearId;
   }

   @Override
   public String gearId() {
      return gearId;
   }

   @Override
   public ArmoryGearKind gearKind() {
      return ArmoryGearKind.WEAPON;
   }

   @Override
   public InteractionResult use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      ArmoryData.initialize(stack);
      GearDefinition gear = ArmoryContent.gear(gearId).orElse(null);
      if (gear == null) {
         return InteractionResult.PASS;
      }
      if (!ArmoryData.factionGateSatisfied(player, gear)) {
         if (!level.isClientSide()) {
            player.sendSystemMessage(Component.literal("ECHO ARMORY // " + ArmoryData.factionGateLine(gear)));
         }
         player.getCooldowns().addCooldown(stack, 20);
         return InteractionResult.CONSUME;
      }
      if (player.isShiftKeyDown()) {
         ArmoryStance stance = stack.getOrDefault(ModDataComponents.STANCE.get(), ArmoryStance.BALANCED).next();
         stack.set(ModDataComponents.STANCE.get(), stance);
         if (!level.isClientSide()) {
            player.sendSystemMessage(Component.literal("ECHO ARMORY // Stance set to " + stance.label() + "."));
         }
         return InteractionResult.SUCCESS_SERVER;
      }
      if (!level.isClientSide() && level instanceof ServerLevel serverLevel && isRanged(gear)) {
         return fireEnergyShot(serverLevel, player, hand, stack, gear);
      }
      return InteractionResult.SUCCESS;
   }

   @Override
   public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
      ArmoryData.initialize(stack);
      if (!(attacker.level() instanceof ServerLevel serverLevel)) {
         return;
      }
      GearDefinition gear = ArmoryContent.gear(gearId).orElse(null);
      if (gear == null) {
         return;
      }
      if (attacker instanceof Player player && !ArmoryData.factionGateSatisfied(player, gear)) {
         player.sendSystemMessage(Component.literal("ECHO ARMORY // " + ArmoryData.factionGateLine(gear)));
         player.getCooldowns().addCooldown(stack, 20);
         return;
      }
      ArmoryStance stance = stack.getOrDefault(ModDataComponents.STANCE.get(), ArmoryStance.BALANCED);
      float extraDamage = Math.max(1.0F, (gear.baseDamage() + ArmoryData.damageBonus(stack)) * stance.damageScale() * 0.65F);
      target.hurtServer(serverLevel, attacker instanceof Player player ? player.damageSources().playerAttack(player) : attacker.damageSources().magic(), extraDamage);
      applyModuleEffects(serverLevel, stack, target, attacker);
      stack.hurtAndBreak(1, attacker, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
      if (attacker instanceof Player player) {
         player.getCooldowns().addCooldown(stack, stance.cooldownTicks());
      }
   }

   @Override
   public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
      ArmoryData.initialize(stack);
      ArmoryContent.gear(gearId).ifPresent(gear -> appendGearTooltip(stack, gear, tooltip));
   }

   private static boolean isRanged(GearDefinition gear) {
      return gear.baseType().equals("ranged") || gear.tags().contains("ranged") || gear.tags().contains("energy");
   }

   private InteractionResult fireEnergyShot(ServerLevel level, Player player, InteractionHand hand, ItemStack stack, GearDefinition gear) {
      FiringModeDefinition mode = ArmoryContent.firingModeFor(gear)
         .orElseGet(() -> new FiringModeDefinition(gear.id(), gear.title() + " Bolt", "energy",
            FiringModeDefinition.ProjectileKind.ENERGY_BOLT, 1, 12, 24, rangeFor(gear), 0.8D, 1.0F, 0, List.of("ranged")));
      int energyCost = Math.max(mode.energyCost(), ArmoryData.moduleDefinitions(stack).stream().mapToInt(ModuleDefinition::energyCost).sum());
      LivingEntity target = findTarget(level, player, mode.range());
      if (target == null) {
         player.sendSystemMessage(Component.literal("ECHO ARMORY // " + gear.title() + " found no firing solution."));
         player.getCooldowns().addCooldown(stack, 12);
         return InteractionResult.CONSUME;
      }
      if (!consumeAmmo(player, mode.ammoCost()) && !ArmoryData.spendEnergy(stack, energyCost)) {
         player.sendSystemMessage(Component.literal("ECHO ARMORY // " + gear.title() + " requires Ammo Crystals or charged energy."));
         player.getCooldowns().addCooldown(stack, 18);
         return InteractionResult.CONSUME;
      }
      ArmoryStance stance = stack.getOrDefault(ModDataComponents.STANCE.get(), ArmoryStance.BALANCED);
      float damage = (gear.baseDamage() + ArmoryData.damageBonus(stack)) * stance.damageScale();
      boolean overload = player.isSprinting() || stack.getOrDefault(ModDataComponents.ENERGY_STATE.get(), EnergyState.EMPTY).overloaded();
      if (overload) {
         damage *= 1.55F;
         addInstability(stack, 14, 80);
      }
      ArmoryProjectileEntity projectile = ArmoryProjectileEntity.create(level, player, stack, mode, Math.max(2.0F, damage * mode.damageScale()));
      level.addFreshEntity(projectile);
      if (mode.instability() > 0) {
         addInstability(stack, mode.instability(), mode.cooldownTicks() * 2);
      }
      level.playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.45F, 1.3F);
      stack.hurtAndBreak(1, player, hand);
      player.getCooldowns().addCooldown(stack, Math.max(mode.cooldownTicks(), stance.cooldownTicks()));
      return InteractionResult.SUCCESS_SERVER;
   }

   public static void applyModuleEffects(ServerLevel level, ItemStack stack, LivingEntity target, LivingEntity attacker) {
      List<ModuleDefinition> modules = ArmoryData.moduleDefinitions(stack);
      for (ModuleDefinition module : modules) {
         switch (module.effectType()) {
            case "frost" -> {
               target.setTicksFrozen(Math.min(target.getTicksRequiredToFreeze() + 40, target.getTicksFrozen() + 80));
               target.setDeltaMovement(target.getDeltaMovement().scale(0.55D));
            }
            case "fire" -> target.igniteForSeconds(4.0F);
            case "lightning" -> {
               if (target.distanceToSqr(attacker) < 36.0D) {
                  target.hurtServer(level, attacker.damageSources().magic(), 2.0F);
               }
            }
            case "void" -> addInstability(stack, 8, 60);
            case "life_leech" -> attacker.heal(1.0F);
            default -> {
            }
         }
      }
      if (modules.stream().anyMatch(module -> module.synergyTags().contains("frost"))) {
         AABB area = target.getBoundingBox().inflate(2.0D);
         for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class, area, entity -> entity != attacker && entity != target)) {
            nearby.setTicksFrozen(Math.min(nearby.getTicksRequiredToFreeze() + 20, nearby.getTicksFrozen() + 40));
         }
      }
   }

   private static void addInstability(ItemStack stack, int amount, int cooldown) {
      InstabilityState state = stack.getOrDefault(ModDataComponents.INSTABILITY_STATE.get(), InstabilityState.STABLE);
      stack.set(ModDataComponents.INSTABILITY_STATE.get(), new InstabilityState(state.instability() + amount, Math.max(state.cooldownTicks(), cooldown)));
   }

   private static boolean consumeAmmo(Player player, int amount) {
      if (amount <= 0) {
         return true;
      }
      if (player.getAbilities().instabuild) {
         return true;
      }
      int available = 0;
      for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
         ItemStack slot = player.getInventory().getItem(i);
         if (slot.is(ModItems.AMMO_CRYSTALS.get())) {
            available += slot.getCount();
         }
      }
      if (available < amount) {
         return false;
      }
      int remaining = amount;
      for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
         ItemStack slot = player.getInventory().getItem(i);
         if (slot.is(ModItems.AMMO_CRYSTALS.get())) {
            int consumed = Math.min(remaining, slot.getCount());
            slot.shrink(consumed);
            remaining -= consumed;
            if (remaining <= 0) {
               return true;
            }
         }
      }
      return remaining <= 0;
   }

   private static LivingEntity findTarget(Level level, Player player, double range) {
      Vec3 eye = player.getEyePosition();
      Vec3 look = player.getLookAngle().normalize();
      return level.getEntities(player, player.getBoundingBox().inflate(range), entity -> entity instanceof LivingEntity living
            && living.isAlive()
            && living != player
            && living.distanceToSqr(player) <= range * range)
         .stream()
         .map(entity -> (LivingEntity)entity)
         .filter(entity -> entity.getEyePosition().subtract(eye).normalize().dot(look) >= 0.45D)
         .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(player)))
         .orElse(null);
   }

   private static double rangeFor(GearDefinition gear) {
      if (gear.id().getPath().contains("rifle") || gear.id().getPath().contains("gun")) {
         return 16.0D;
      }
      if (gear.id().getPath().contains("bow")) {
         return 13.0D;
      }
      return 8.0D;
   }

   private static void appendGearTooltip(ItemStack stack, GearDefinition gear, Consumer<Component> tooltip) {
      InstalledModules modules = ArmoryData.modules(stack);
      ArmoryStance stance = stack.getOrDefault(ModDataComponents.STANCE.get(), ArmoryStance.BALANCED);
      tooltip.accept(Component.literal("Tier " + gear.tier() + " " + gear.craftingStage() + " | " + stance.label()));
      tooltip.accept(Component.literal("Damage " + (gear.baseDamage() + ArmoryData.damageBonus(stack)) + " | Modules " + modules.modules().size() + "/" + gear.moduleSlots()));
      EnergyState energy = stack.get(ModDataComponents.ENERGY_STATE.get());
      if (energy != null && energy.capacity() > 0) {
         tooltip.accept(Component.literal("Energy " + energy.stored() + "/" + energy.capacity()));
      }
      if (!gear.factionGate().isBlank()) {
         tooltip.accept(Component.literal("Faction gate: " + gear.factionGate() + " rep " + ArmoryData.requiredReputation(gear)));
      }
      for (ModuleDefinition module : ArmoryData.moduleDefinitions(stack)) {
         tooltip.accept(Component.literal("Module: " + module.title()));
      }
   }
}
