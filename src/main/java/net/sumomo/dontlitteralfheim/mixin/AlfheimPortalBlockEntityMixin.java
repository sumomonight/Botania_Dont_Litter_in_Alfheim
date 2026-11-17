package net.sumomo.dontlitteralfheim.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.sumomo.dontlitteralfheim.api.IAlfheimPortalAnger;
import net.sumomo.dontlitteralfheim.handler.PortalItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vazkii.botania.common.block.block_entity.AlfheimPortalBlockEntity;

@Mixin(value = AlfheimPortalBlockEntity.class, remap = false)
public abstract class AlfheimPortalBlockEntityMixin implements IAlfheimPortalAnger {

    @Unique
    private static final String TAG_ANGER_CHANCE = "dontLitterAngerChance";
    @Unique
    private static final String TAG_TRASH_COUNT = "dontLitterTrashCount";
    @Unique
    private static final String TAG_CREATIVE_TRASH_COUNT = "dontLitterCreativeTrashCount";

    @Unique
    private double dontLitter$angerChance = 0.0;
    @Unique
    private int dontLitter$trashCount = 0;
    @Unique
    private int dontLitter$creativeTrashCount = 0;

    @Inject(method = "validateItemUsage", at = @At("HEAD"), cancellable = true)
    private void onValidateItemUsageHead(ItemEntity entity, CallbackInfoReturnable<Boolean> cir) {
        AlfheimPortalBlockEntity self = (AlfheimPortalBlockEntity) (Object) this;
        if (self.getLevel() instanceof ServerLevel serverLevel) {
            if (PortalItemHandler.handleBeforeTradeCheck(serverLevel, self, this, entity)) {
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = "validateItemUsage", at = @At("RETURN"))
    private void onValidateItemUsageReturn(ItemEntity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) {
            AlfheimPortalBlockEntity self = (AlfheimPortalBlockEntity) (Object) this;
            if (self.getLevel() instanceof ServerLevel serverLevel) {
                PortalItemHandler.handleNonRecipeItem(serverLevel, self, entity);
            }
        }
    }

    @Inject(method = {"saveAdditional", "m_183515_"}, at = @At("TAIL"))
    private void onSaveAdditional(CompoundTag cmp, CallbackInfo ci) {
        cmp.putDouble(TAG_ANGER_CHANCE, this.dontLitter$angerChance);
        cmp.putInt(TAG_TRASH_COUNT, this.dontLitter$trashCount);
        cmp.putInt(TAG_CREATIVE_TRASH_COUNT, this.dontLitter$creativeTrashCount);
    }

    @Inject(method = {"load", "m_142466_"}, at = @At("TAIL"))
    private void onLoad(CompoundTag cmp, CallbackInfo ci) {
        this.dontLitter$angerChance = cmp.getDouble(TAG_ANGER_CHANCE);
        this.dontLitter$trashCount = cmp.getInt(TAG_TRASH_COUNT);
        this.dontLitter$creativeTrashCount = cmp.getInt(TAG_CREATIVE_TRASH_COUNT);
    }

    @Override
    public double dontLitter$getAngerChance() {
        return this.dontLitter$angerChance;
    }

    @Override
    public void dontLitter$setAngerChance(double chance) {
        this.dontLitter$angerChance = chance;
        ((AlfheimPortalBlockEntity) (Object) this).setChanged();
    }

    @Override
    public int dontLitter$getTrashCount() {
        return this.dontLitter$trashCount;
    }

    @Override
    public void dontLitter$setTrashCount(int count) {
        this.dontLitter$trashCount = count;
        ((AlfheimPortalBlockEntity) (Object) this).setChanged();
    }

    @Override
    public int dontLitter$getCreativeTrashCount() {
        return this.dontLitter$creativeTrashCount;
    }

    @Override
    public void dontLitter$setCreativeTrashCount(int count) {
        this.dontLitter$creativeTrashCount = count;
        ((AlfheimPortalBlockEntity) (Object) this).setChanged();
    }
}