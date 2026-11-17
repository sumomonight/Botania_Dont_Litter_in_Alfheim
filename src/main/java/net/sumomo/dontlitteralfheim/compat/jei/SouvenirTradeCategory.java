package net.sumomo.dontlitteralfheim.compat.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.sumomo.dontlitteralfheim.registry.ModItems;
import vazkii.botania.common.block.BotaniaBlocks;

import java.util.List;

public class SouvenirTradeCategory implements IRecipeCategory<SouvenirTradeJeiRecipe> {

    public static final RecipeType<SouvenirTradeJeiRecipe> RECIPE_TYPE =
            RecipeType.create("dont_litter_in_alfheim", "souvenir_trade", SouvenirTradeJeiRecipe.class);

    private static final String CYCLING_SLOT_NAME = "loot_cycling_output";

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable slotDrawable;

    public SouvenirTradeCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(162, 116);
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModItems.PRESENT.get()));
        this.slotDrawable = guiHelper.getSlotDrawable();
    }

    @Override
    public RecipeType<SouvenirTradeJeiRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.dont_litter_in_alfheim.category.souvenir_trade");
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, SouvenirTradeJeiRecipe recipe, IFocusGroup focuses) {
        // Input slot (Present / tagged items)
        builder.addSlot(RecipeIngredientRole.INPUT, 10, 14)
                .setBackground(slotDrawable, -1, -1)
                .addItemStacks(recipe.inputs());

        // Catalyst (Alfheim Portal core block)
        builder.addSlot(RecipeIngredientRole.CATALYST, 44, 14)
                .setBackground(slotDrawable, -1, -1)
                .addItemStack(new ItemStack(BotaniaBlocks.alfPortal));

        List<ItemStack> outputStacks = recipe.outputs().stream()
                .map(SouvenirTradeJeiRecipe.LootEntry::stack)
                .toList();

        // 1. Cycling output slot directly below Present (with probability text next to it)
        builder.addSlot(RecipeIngredientRole.OUTPUT, 10, 40)
                .setSlotName(CYCLING_SLOT_NAME)
                .setBackground(slotDrawable, -1, -1)
                .addItemStacks(outputStacks);

        // 2. Full overview grid (9x3) below the cycling row
        int cols = 9;
        for (int i = 0; i < Math.min(outputStacks.size(), 27); i++) {
            int col = i % cols;
            int row = i / cols;
            builder.addSlot(RecipeIngredientRole.OUTPUT, 1 + col * 18, 62 + row * 18)
                    .setBackground(slotDrawable, -1, -1)
                    .addItemStack(outputStacks.get(i));
        }
    }

    @Override
    public void draw(SouvenirTradeJeiRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;

        // Top row: Arrow & overall souvenir drop chance
        String dropChanceText = String.format("%.1f%%", recipe.dropChance() * 100.0);
        Component dropLabel = Component.translatable("jei.dont_litter_in_alfheim.drop_chance", dropChanceText);
        guiGraphics.drawString(font, "->", 30, 18, 0x404040, false);
        guiGraphics.drawString(font, labelOrFallback(dropLabel), 68, 18, 0x2E8B57, false);

        // Middle row: Probability of the currently displayed cycling item
        recipeSlotsView.findSlotByName(CYCLING_SLOT_NAME)
                .flatMap(slot -> slot.getDisplayedIngredient(VanillaTypes.ITEM_STACK))
                .ifPresent(displayedStack -> {
                    for (SouvenirTradeJeiRecipe.LootEntry entry : recipe.outputs()) {
                        if (ItemStack.isSameItemSameTags(entry.stack(), displayedStack)) {
                            String itemProbText = String.format("%.1f%%", entry.probability() * 100.0);
                            guiGraphics.drawString(font, itemProbText, 32, 44, 0x404040, false);
                            break;
                        }
                    }
                });
    }

    private Component labelOrFallback(Component label) {
        return label;
    }
}