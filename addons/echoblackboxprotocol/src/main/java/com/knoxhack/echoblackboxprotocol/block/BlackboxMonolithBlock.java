package com.knoxhack.echoblackboxprotocol.block;

import com.knoxhack.echoblackboxprotocol.progression.BlackboxDungeon;
import com.knoxhack.echoblackboxprotocol.progression.BlackboxProgress;
import com.knoxhack.echoblackboxprotocol.registry.ModEntities;
import com.knoxhack.echoblackboxprotocol.world.DungeonSeeder;
import com.knoxhack.echoblackboxprotocol.world.ModDimensions;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.BlockHitResult;

public class BlackboxMonolithBlock extends Block {
   private final BlackboxDungeon dungeon;

   public BlackboxMonolithBlock(BlackboxDungeon dungeon, Properties properties) {
      super(properties);
      this.dungeon = dungeon;
   }

   public BlackboxDungeon dungeon() {
      return this.dungeon;
   }

   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
      if (level.isClientSide()) {
         return InteractionResult.SUCCESS;
      } else {
         BlackboxProgress progress = BlackboxProgress.get(player);
         if (!progress.canEnter(this.dungeon) && !player.hasInfiniteMaterials()) {
            player.sendSystemMessage(Component.literal("ECHO-7 // " + this.dungeon.displayName() + " locked. " + progress.lockReason(this.dungeon)));
            return InteractionResult.CONSUME;
         } else {
            if (player instanceof ServerPlayer serverPlayer) {
               ServerLevel target = ModDimensions.resolve(serverPlayer.level().getServer(), this.dungeon, serverPlayer.level());
               BlockPos arrival = new BlockPos(0, 96, 0);
               DungeonSeeder.seed(target, arrival, this.dungeon);
               spawnArrivalThreats(target, arrival, this.dungeon);
               serverPlayer.teleportTo(
                  target, arrival.getX() + 0.5, arrival.getY() + 1.0, arrival.getZ() + 0.5, Set.of(), player.getYRot(), player.getXRot(), false
               );
               player.sendSystemMessage(Component.literal("ECHO-7 // " + this.dungeon.displayName() + " route opened. The archive remembers you now."));
            }

            return InteractionResult.SUCCESS_SERVER;
         }
      }
   }

   private static void spawnArrivalThreats(ServerLevel level, BlockPos arrival, BlackboxDungeon dungeon) {
      switch (dungeon) {
         case VAULT:
            spawn(level, arrival.offset(5, 1, 4), (EntityType<?>)ModEntities.ARCHIVE_HUSK.get());
            spawn(level, arrival.offset(-4, 1, 5), (EntityType<?>)ModEntities.SECURITY_ECHO.get());
            break;
         case BUNKER:
            spawn(level, arrival.offset(6, 1, -4), (EntityType<?>)ModEntities.COMMAND_REMNANT_MINION.get());
            spawn(level, arrival.offset(-5, 1, -5), (EntityType<?>)ModEntities.BLACKBOX_SENTINEL.get());
            break;
         case LABYRINTH:
            spawn(level, arrival.offset(4, 1, 6), (EntityType<?>)ModEntities.MEMORY_PARASITE.get());
            spawn(level, arrival.offset(-4, 1, -6), (EntityType<?>)ModEntities.FALSE_ECHO_MINION.get());
            break;
         case TEMPLE:
            spawn(level, arrival.offset(0, 1, 7), (EntityType<?>)ModEntities.BLACKBOX_SENTINEL.get());
            break;
         case CORE_CHAMBER:
            spawn(level, arrival.offset(0, 1, 9), (EntityType<?>)ModEntities.NEXUS_GUARDIAN.get());
      }
   }

   private static void spawn(ServerLevel level, BlockPos pos, EntityType<?> type) {
      DungeonSeeder.spawnEncounter(level, pos, type);
   }
}
