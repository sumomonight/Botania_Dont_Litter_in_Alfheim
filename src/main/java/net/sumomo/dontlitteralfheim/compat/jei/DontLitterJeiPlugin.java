package net.sumomo.dontlitteralfheim.compat.jei;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import net.sumomo.dontlitteralfheim.config.DontLitterConfig;
import net.sumomo.dontlitteralfheim.handler.PortalItemHandler;
import net.sumomo.dontlitteralfheim.registry.ModItems;
import vazkii.botania.common.block.BotaniaBlocks;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@JeiPlugin
public class DontLitterJeiPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation("dont_litter_in_alfheim", "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new SouvenirTradeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(BotaniaBlocks.alfPortal), SouvenirTradeCategory.RECIPE_TYPE);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        // 1. Souvenir Trade Category
        List<ItemStack> souvenirInputs = collectSouvenirTradeItems();
        List<SouvenirTradeJeiRecipe.LootEntry> souvenirOutputs = collectLootTableOutputs();
        double dropChance = DontLitterConfig.SOUVENIR_DROP_CHANCE.get();

        if (!souvenirInputs.isEmpty() && !souvenirOutputs.isEmpty()) {
            registration.addRecipes(
                    SouvenirTradeCategory.RECIPE_TYPE,
                    List.of(new SouvenirTradeJeiRecipe(souvenirInputs, souvenirOutputs, dropChance))
            );
        }

        // 2. Normal Trash Info
        List<ItemStack> normalTrash = collectNormalTrashItems();
        if (!normalTrash.isEmpty()) {
            registration.addIngredientInfo(
                    normalTrash,
                    VanillaTypes.ITEM_STACK,
                    Component.translatable("jei.dont_litter_in_alfheim.trash_info")
            );
        }

        // 3. Creative Trash Info
        List<ItemStack> creativeTrash = collectCreativeTrashItems();
        if (!creativeTrash.isEmpty()) {
            registration.addIngredientInfo(
                    creativeTrash,
                    VanillaTypes.ITEM_STACK,
                    Component.translatable("jei.dont_litter_in_alfheim.creative_trash_info")
            );
        }

        // 4. Normal Apology Letter Info
        List<ItemStack> normalLetters = collectNormalApologyItems();
        if (!normalLetters.isEmpty()) {
            registration.addIngredientInfo(
                    normalLetters,
                    VanillaTypes.ITEM_STACK,
                    Component.translatable("jei.dont_litter_in_alfheim.apology_info")
            );
        }

        // 5. Creative Apology Letter Info
        List<ItemStack> creativeLetters = collectCreativeApologyItems();
        if (!creativeLetters.isEmpty()) {
            registration.addIngredientInfo(
                    creativeLetters,
                    VanillaTypes.ITEM_STACK,
                    Component.translatable("jei.dont_litter_in_alfheim.creative_apology_info")
            );
        }

        // 6. Present / Souvenir Trade Info
        if (!souvenirInputs.isEmpty()) {
            registration.addIngredientInfo(
                    souvenirInputs,
                    VanillaTypes.ITEM_STACK,
                    Component.translatable("jei.dont_litter_in_alfheim.present_info")
            );
        }
    }

    // --- Collection Helper Methods ---

    private List<ItemStack> collectItemsWithTag(Item defaultItem, TagKey<Item> tag) {
        Set<Item> items = new LinkedHashSet<>();
        items.add(defaultItem);

        var tagManager = ForgeRegistries.ITEMS.tags();
        if (tagManager != null) {
            for (Item item : tagManager.getTag(tag)) {
                items.add(item);
            }
        }
        return items.stream().map(ItemStack::new).toList();
    }

    private List<ItemStack> collectSouvenirTradeItems() {
        return collectItemsWithTag(ModItems.PRESENT.get(), PortalItemHandler.SOUVENIR_TRADE_TAG);
    }

    private List<ItemStack> collectNormalTrashItems() {
        return collectItemsWithTag(ModItems.TRASH.get(), PortalItemHandler.TRASH_TAG);
    }

    private List<ItemStack> collectCreativeTrashItems() {
        return collectItemsWithTag(ModItems.CREATIVE_TRASH.get(), PortalItemHandler.CREATIVE_TRASH_TAG);
    }

    private List<ItemStack> collectNormalApologyItems() {
        return collectItemsWithTag(ModItems.APOLOGY_LETTER.get(), PortalItemHandler.RESET_ITEM_TAG);
    }

    private List<ItemStack> collectCreativeApologyItems() {
        return collectItemsWithTag(ModItems.CREATIVE_APOLOGY_LETTER.get(), PortalItemHandler.CREATIVE_RESET_ITEM_TAG);
    }

    private List<SouvenirTradeJeiRecipe.LootEntry> collectLootTableOutputs() {
        Map<Item, Integer> itemWeights = new LinkedHashMap<>();

        String[] subPath = ModList.get().isLoaded("mythicbotany")
                ? new String[]{"data", "dont_litter_in_alfheim", "loot_tables", "gameplay", "alfheim_souvenir_mythic.json"}
                : new String[]{"data", "dont_litter_in_alfheim", "loot_tables", "gameplay", "alfheim_souvenir.json"};

        try {
            var modFile = ModList.get().getModFileById("dont_litter_in_alfheim").getFile();
            java.nio.file.Path jsonPath = modFile.findResource(subPath);
            if (java.nio.file.Files.exists(jsonPath)) {
                try (var reader = java.nio.file.Files.newBufferedReader(jsonPath, StandardCharsets.UTF_8)) {
                    JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                    extractWeightedItemsFromPools(root, itemWeights);
                }
            }
        } catch (Exception ignored) {
        }

        int totalWeight = itemWeights.values().stream().mapToInt(Integer::intValue).sum();
        if (totalWeight <= 0) {
            return List.of();
        }

        List<SouvenirTradeJeiRecipe.LootEntry> entries = new ArrayList<>();
        for (Map.Entry<Item, Integer> entry : itemWeights.entrySet()) {
            double probability = (double) entry.getValue() / totalWeight;
            entries.add(new SouvenirTradeJeiRecipe.LootEntry(new ItemStack(entry.getKey()), probability));
        }
        return entries;
    }

    private void extractWeightedItemsFromPools(JsonObject root, Map<Item, Integer> out) {
        if (!root.has("pools")) return;
        JsonArray pools = root.getAsJsonArray("pools");
        for (JsonElement poolEl : pools) {
            JsonObject pool = poolEl.getAsJsonObject();
            if (!pool.has("entries")) continue;
            for (JsonElement entryEl : pool.getAsJsonArray("entries")) {
                JsonObject entry = entryEl.getAsJsonObject();
                String type = entry.has("type") ? entry.get("type").getAsString() : "";
                if ("minecraft:item".equals(type) && entry.has("name")) {
                    ResourceLocation id = ResourceLocation.tryParse(entry.get("name").getAsString());
                    int weight = entry.has("weight") ? entry.get("weight").getAsInt() : 1;
                    if (id != null && weight > 0) {
                        Item item = ForgeRegistries.ITEMS.getValue(id);
                        if (item != null && item != Items.AIR) {
                            out.merge(item, weight, Integer::sum);
                        }
                    }
                }
            }
        }
    }
}