package net.sumomo.dontlitteralfheim.api;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public record AttackContext(
        Level level,
        LivingEntity caster,
        @Nullable LivingEntity target,
        Vec3 targetPos,
        ItemStack stack
) {
    public static AttackContext ofAi(LivingEntity caster, @Nullable LivingEntity target, Vec3 targetPos) {
        return new AttackContext(caster.level(), caster, target, targetPos, ItemStack.EMPTY);
    }

    public static AttackContext ofPlayer(LivingEntity caster, Vec3 targetPos, ItemStack stack) {
        return new AttackContext(caster.level(), caster, null, targetPos, stack);
    }
}