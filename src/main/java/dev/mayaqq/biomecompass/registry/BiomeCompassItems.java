package dev.mayaqq.biomecompass.registry;

import dev.mayaqq.biomecompass.BiomeCompass;
import dev.mayaqq.biomecompass.item.BiomeCompassItem;
import eu.pb4.polymer.core.api.item.PolymerItemGroupUtils;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;

public class BiomeCompassItems {
    public static ItemGroup ITEM_GROUP;

    public static final Item BIOME_COMPASS = register("biome_compass", new BiomeCompassItem(new Item.Settings().maxCount(1)));

    public static void register() {
        ITEM_GROUP = FabricItemGroup.builder()
                .icon(BIOME_COMPASS::getDefaultStack)
                .displayName(Text.translatable("itemGroup.biomecompass.item_group"))
                .entries((displayContext, entries) -> entries.add(BIOME_COMPASS.getDefaultStack()))
                .build();

        PolymerItemGroupUtils.registerPolymerItemGroup(BiomeCompass.id("item_group"), ITEM_GROUP);
        PolymerResourcePackUtils.addModAssets(BiomeCompass.MOD_ID);
    }

    private static Item register(String path, Item item) {
        return Registry.register(Registries.ITEM, BiomeCompass.id(path), item);
    }
}