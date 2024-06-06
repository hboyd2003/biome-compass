package dev.mayaqq.biomecompass.gui;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import dev.mayaqq.biomecompass.helper.TextHelper;
import dev.mayaqq.biomecompass.item.BiomeCompassItem;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SignGui;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.command.argument.RegistryPredicateArgumentType;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class BiomeSelectionGui {

    public static Registry<Biome> getRegistry(ServerWorld world) {
        return world.getRegistryManager().get(RegistryKeys.BIOME);
    }

    public static void open(ServerPlayerEntity player, int page, Hand hand) {
        open(player, page, hand, "");
    }

    public static void open(ServerPlayerEntity player, int page, Hand hand, String filter) {
        SimpleGui gui = new SimpleGui(ScreenHandlerType.GENERIC_9X6, player, false);

        gui.setTitle(Text.translatable("gui.biomecompass.biome_compass.title"));

        Registry<Biome> biomes = getRegistry(player.getServerWorld());

        ArrayList<Biome> biomesList = new ArrayList<>(biomes.stream().toList());

        for (int i = 0; i < biomesList.size(); i++) {
            Biome biome = biomesList.get(i);
            Identifier biomeId = biomes.getId(biome);
            String name = TextHelper.getBiomeNameFormatted(biomeId).getString();
            if (!name.contains(filter) && !biomeId.toString().contains(filter)) {
                biomesList.remove(i);
                i--;
            }
        }

        for (int i = page * 45; i < biomesList.size(); i++) {
            if (i >= 45 * page + 45) {
                break;
            }
            Biome biome = biomesList.get(i);
            Identifier biomeId = biomes.getId(biome);

            String biomeColor = biome.getTemperature() > 1.0 ? "§c" : biome.getTemperature() < 0.15 ? "§b" : "§a";
            String biomeNameString = biomeId.getPath().toString().replaceAll("_", " ");
            Text biomeName = Text.of(biomeColor + biomeNameString);

            Item biomeItem = biome.getTemperature() > 1.0 ? Items.LAVA_BUCKET : biome.getTemperature() < 0.15 ? Items.SNOWBALL : Items.GRASS_BLOCK;

            gui.setSlot(i - 45 * page, new GuiElementBuilder()
                    .setItem(biomeItem)
                    .setName(biomeName)
                    .setLore(new ArrayList<>(List.of(
                            Text.translatable("gui.biomecompass.biome_compass.identifier", biomeId.toString()).formatted(Formatting.GRAY),
                            Text.translatable("gui.biomecompass.biome_compass.warmth", String.valueOf(biome.getTemperature())).formatted(Formatting.GRAY),
                            Text.translatable("gui.biomecompass.biome_compass.precipitation", biome.getPrecipitation(BlockPos.ORIGIN).toString()).formatted(Formatting.GRAY)
                    )))
                    .setCallback((index, type, action) -> {
                        gui.close();
                        try {
                            Pair<BlockPos, RegistryEntry<Biome>> pair = executeLocateBiome(player.getBlockPos(), player.getServerWorld(), biome);
                            player.sendMessage(Text.translatable("gui.biomecompass.biome_compass.found", biomeName, TextHelper.getBlockPosFormatted(pair.getFirst()), TextHelper.getDistanceFromPlayer(player, pair.getFirst())));
                            ((BiomeCompassItem) player.getStackInHand(hand).getItem()).track(pair.getFirst(), player.getServerWorld(), player, player.getStackInHand(hand), biomeName.getString());
                        } catch (Exception e) {
                            player.sendMessage(Text.translatable("gui.biomecompass.biome_compass.not_found", biomeName).formatted(Formatting.DARK_RED), false);
                        }
                    })
            );
        }

        for (int i = 45; i < 54; i++) {
            gui.setSlot(i, new GuiElementBuilder()
                    .setItem(Items.BLACK_STAINED_GLASS_PANE)
                    .setName(Text.of(""))
            );
        }

        gui.setSlot(50, new GuiElementBuilder()
                .setItem(Items.OAK_SIGN)
                .setName(Text.translatable("gui.biomecompass.biome_compass.search"))
                .addLoreLine(Text.translatable("gui.biomecompass.biome_compass.current_filter", filter).formatted(Formatting.GRAY))
                .addLoreLine(Text.translatable("gui.biomecompass.biome_compass.filter_reset").formatted(Formatting.GRAY))
                .setCallback((index, type, action) -> {
                    if (type.isRight) {
                        open(player, page, hand);
                    } else {
                        filterInput(player, hand);
                    }
                })
        );
        gui.setSlot(49, new GuiElementBuilder()
                .setItem(Items.BARRIER)
                .setName(Text.translatable("gui.biomecompass.biome_compass.close").formatted(Formatting.DARK_RED))
                .setLore(new ArrayList<>())
                .setCallback((index, type, action) -> gui.close())
        );

        if (page > 0) {
            gui.setSlot(45, new GuiElementBuilder()
                    .setItem(Items.ARROW)
                    .setName(Text.translatable("gui.biomecompass.biome_compass.previous_page"))
                    .setLore(new ArrayList<>())
                    .setCallback((index, type, action) -> open(player, page - 1, hand))
            );
        }

        if (page < biomesList.size() / 45) {
            gui.setSlot(53, new GuiElementBuilder()
                    .setItem(Items.ARROW)
                    .setName(Text.translatable("gui.biomecompass.biome_compass.next_page"))
                    .setLore(new ArrayList<>())
                    .setCallback((index, type, action) -> open(player, page + 1, hand))
            );
        }

        gui.open();
    }

    private static Pair<BlockPos, RegistryEntry<Biome>> executeLocateBiome(BlockPos pos, ServerWorld world, Biome biome) {
        Predicate<RegistryEntry<Biome>> predicate = new RegistryKeyBased<>(RegistryKey.of(RegistryKeys.BIOME, getRegistry(world).getId(biome)));
        return world.locateBiome(predicate, pos, 6400, 32, 64);
    }

    record RegistryKeyBased<T>(RegistryKey<T> key) implements RegistryPredicateArgumentType.RegistryPredicate<T> {

        public Either<RegistryKey<T>, TagKey<T>> getKey() {
            return Either.left(this.key);
        }

        public <E> Optional<RegistryPredicateArgumentType.RegistryPredicate<E>> tryCast(RegistryKey<? extends Registry<E>> registryRef) {
            return this.key.tryCast(registryRef).map(RegistryKeyBased::new);
        }

        public boolean test(RegistryEntry<T> registryEntry) {
            return registryEntry.matchesKey(this.key);
        }

        public String asString() {
            return this.key.getValue().toString();
        }
    }

    private static void filterInput(ServerPlayerEntity player, Hand hand) {
        try {
            SignGui gui = new SignGui(player) {
                @Override
                public void onClose() {
                    BiomeSelectionGui.open(player, 0, hand, this.getLine(0).getString());
                }
            };
            gui.open();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
