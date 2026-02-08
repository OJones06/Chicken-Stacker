package com.mobstacker.mixin;

import com.mobstacker.StackableChicken;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChickenEntity.class)
public abstract class ChickenEntityMixin extends AnimalEntity implements StackableChicken {

    @Shadow
    public int eggLayTime;

    @Unique
    private int mobstacker_stackCount = 1;

    protected ChickenEntityMixin(EntityType<? extends AnimalEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public int mobstacker_getStackCount() {
        return mobstacker_stackCount;
    }

    @Override
    public void mobstacker_setStackCount(int count) {
        mobstacker_stackCount = count;
        if (count > 1) {
            this.setCustomName(Text.literal("Chicken x" + count));
            this.setCustomNameVisible(true);
        } else {
            this.setCustomName(null);
            this.setCustomNameVisible(false);
        }
    }

    @Inject(method = "writeCustomData", at = @At("TAIL"))
    private void mobstacker_writeStackCount(WriteView view, CallbackInfo ci) {
        view.putInt("MobStackerCount", mobstacker_stackCount);
    }

    @Inject(method = "readCustomData", at = @At("TAIL"))
    private void mobstacker_readStackCount(ReadView view, CallbackInfo ci) {
        int count = view.getInt("MobStackerCount", 1);
        if (count > 1) {
            mobstacker_stackCount = count;
            this.setCustomName(Text.literal("Chicken x" + mobstacker_stackCount));
            this.setCustomNameVisible(true);
        }
    }

    @Inject(method = "tickMovement", at = @At("TAIL"))
    private void mobstacker_handleExtraEggs(CallbackInfo ci) {
        if (this.getEntityWorld() instanceof ServerWorld serverWorld && this.isAlive() && !this.isBaby() && mobstacker_stackCount > 1) {
            // Each virtual chicken has ~1/9000 chance per tick to lay an egg
            // (vanilla timer is 6000-12000 ticks, average 9000)
            int extraChickens = mobstacker_stackCount - 1;
            double expectedEggs = extraChickens / 9000.0;
            int eggsToLay = (int) expectedEggs;
            double fractional = expectedEggs - eggsToLay;
            if (this.random.nextDouble() < fractional) {
                eggsToLay++;
            }
            for (int i = 0; i < eggsToLay; i++) {
                this.dropStack(serverWorld, new ItemStack(Items.EGG));
            }
        }
    }
}
