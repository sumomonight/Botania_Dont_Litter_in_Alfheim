package net.sumomo.dontlitteralfheim.compat.jei;

import net.minecraft.world.item.ItemStack;
import java.util.List;

public record SouvenirTradeJeiRecipe(
        List<ItemStack> inputs,
        List<LootEntry> outputs,
        double dropChance
) {
    public record LootEntry(ItemStack stack, double probability) {}
}