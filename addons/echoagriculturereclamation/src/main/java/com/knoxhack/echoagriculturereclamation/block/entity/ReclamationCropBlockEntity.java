package com.knoxhack.echoagriculturereclamation.block.entity;

import com.knoxhack.echoagriculturereclamation.content.CropSpec;
import com.knoxhack.echoagriculturereclamation.content.ReclamationCropLogic;
import com.knoxhack.echoagriculturereclamation.content.SeedProfile;
import com.knoxhack.echoagriculturereclamation.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class ReclamationCropBlockEntity extends BlockEntity {
   private SeedProfile profile;

   public ReclamationCropBlockEntity(BlockPos pos, BlockState state) {
      super(ModBlockEntities.CROP.get(), pos, state);
   }

   public void setProfile(SeedProfile profile) {
      this.profile = profile;
      setChanged();
   }

   public SeedProfile profileOrFallback(CropSpec spec, boolean stable) {
      return profile == null || !profile.cropId().equals(spec.path())
         ? ReclamationCropLogic.fallbackProfile(spec, stable)
         : profile;
   }

   public SeedProfile profile() {
      return profile;
   }

   @Override
   protected void loadAdditional(ValueInput input) {
      super.loadAdditional(input);
      String cropId = input.getStringOr("crop_id", "");
      if (cropId.isBlank()) {
         profile = null;
      } else {
         profile = new SeedProfile(cropId, input.getIntOr("contamination", 1), input.getIntOr("stability", 35));
      }
   }

   @Override
   protected void saveAdditional(ValueOutput output) {
      super.saveAdditional(output);
      if (profile != null) {
         output.putString("crop_id", profile.cropId());
         output.putInt("contamination", profile.contaminationTier());
         output.putInt("stability", profile.stability());
      } else {
         output.putString("crop_id", "");
      }
   }
}
