package net.sumomo.dontlitteralfheim.api;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import vazkii.botania.api.mana.ManaItemHandler;

public abstract class AbstractBotaniaAttack {
    private final int manaCost;
    private final int cooldownTicks;

    protected AbstractBotaniaAttack(int manaCost, int cooldownTicks) {
        this.manaCost = Math.max(0, manaCost);
        this.cooldownTicks = Math.max(0, cooldownTicks);
    }

    public boolean playerExecute(AttackContext ctx) {
        if (!(ctx.caster() instanceof Player player)) {
            return false;
        }

        if (this.manaCost > 0 && !player.isCreative()) {
            ItemStack stack = ctx.stack();
            if (!ManaItemHandler.instance().requestManaExactForTool(stack, player, this.manaCost, true)) {
                return false;
            }
        }

        this.perform(ctx);

        if (this.cooldownTicks > 0 && !ctx.stack().isEmpty()) {
            player.getCooldowns().addCooldown(ctx.stack().getItem(), this.cooldownTicks);
        }

        return true;
    }

    public void aiExecute(AttackContext ctx) {
        this.perform(ctx);
    }

    protected abstract void perform(AttackContext ctx);

    public int getManaCost() {
        return this.manaCost;
    }

    public int getCooldownTicks() {
        return this.cooldownTicks;
    }
}