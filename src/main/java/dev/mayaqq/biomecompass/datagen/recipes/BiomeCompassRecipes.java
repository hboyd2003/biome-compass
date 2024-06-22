package dev.mayaqq.biomecompass.datagen.recipes;

import dev.mayaqq.biomecompass.registry.BiomeCompassItems;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.data.server.recipe.RecipeExporter;
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import java.util.concurrent.CompletableFuture;

public class BiomeCompassRecipes extends FabricRecipeProvider {
    public BiomeCompassRecipes(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    public void generate(RecipeExporter exporter) {

        TagKey<Item> saplings = TagKey.of(Registries.ITEM.getKey(), mcId("saplings"));
        TagKey<Item> logs = TagKey.of(Registries.ITEM.getKey(), mcId("logs"));

        ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, BiomeCompassItems.BIOME_COMPASS)
                .pattern("#0#")
                .pattern("0C0")
                .pattern("#0#")
                .input('#', saplings)
                .input('0', logs)
                .input('C', Items.COMPASS)
                .criterion(
                        FabricRecipeProvider.hasItem(Items.COMPASS),
                        FabricRecipeProvider.conditionsFromItem(Items.COMPASS)
                )
                .offerTo(exporter);
    }

    public static Identifier mcId(String path) {
        return new Identifier("minecraft", path);
    }
}