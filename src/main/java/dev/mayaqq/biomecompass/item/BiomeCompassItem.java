package dev.mayaqq.biomecompass.item;

import dev.mayaqq.biomecompass.BiomeCompass;
import dev.mayaqq.biomecompass.gui.BiomeSelectionGui;
import dev.mayaqq.biomecompass.helper.TextHelper;
import dev.mayaqq.biomecompass.registry.BiomeCompassItems;
import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.minecraft.client.item.TooltipType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LodestoneTrackerComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtIntArray;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.zip.DataFormatException;

public class BiomeCompassItem extends Item implements PolymerItem {
    public static final String BIOME_NAME_KEY = BiomeCompass.id("biome_name").toString();
    public static final String BIOME_DIMENSION_KEY = BiomeCompass.id("biome_dimension").toString();
    public static final String BIOME_POS_KEY = BiomeCompass.id("biome_pos").toString();

    private final int modelData;

    public BiomeCompassItem(Settings settings) {
        super(settings);
        this.modelData = PolymerResourcePackUtils.requestModel(Items.COMPASS, BiomeCompass.id("item/biome_compass")).value();
    }

//    private static boolean hasBiome(ItemStack stack) {
//        NbtCompound nbt = stack.get()
//        return nbt != null && nbt.contains(BIOME_NAME_KEY);
//    }

    private static Optional<RegistryKey<World>> getBiomeDimension(NbtCompound nbt) {
        return World.CODEC.parse(NbtOps.INSTANCE, nbt.get(BIOME_DIMENSION_KEY)).result();
    }

    private void addComponents(RegistryKey<World> worldKey, BlockPos pos, ItemStack stack, String biomeName) {
        if (biomeName != null) {
            // Lodestone component
            stack.set(DataComponentTypes.LODESTONE_TRACKER,
                    new LodestoneTrackerComponent(Optional.of(GlobalPos.create(worldKey, pos)), true));

            // Add biome name, dimension and biome positon to NBT via custom data component
            stack.apply(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT, comp -> comp.apply(currentNbt -> {
                currentNbt.putString(BIOME_NAME_KEY, biomeName);
                World.CODEC.encodeStart(NbtOps.INSTANCE, worldKey).resultOrPartial(BiomeCompass.LOGGER::error).ifPresent(nbtElement -> currentNbt.put(BIOME_DIMENSION_KEY, nbtElement));
                currentNbt.put(BIOME_POS_KEY, NbtHelper.fromBlockPos(pos));
            }));
        }
    }


    public void track(BlockPos pos, World world, PlayerEntity player, ItemStack oldCompass, String biomeName) {
        world.playSound(null, player.getBlockPos(), SoundEvents.ITEM_LODESTONE_COMPASS_LOCK, SoundCategory.PLAYERS, 1.0F, 1.0F);
        boolean bl = oldCompass.getCount() == 1;
        if (bl) {
            this.addComponents(world.getRegistryKey(), pos, oldCompass, biomeName);
        } else {
            oldCompass.decrement(1);
            ItemStack newCompass = oldCompass.copyComponentsToNewStack(BiomeCompassItems.BIOME_COMPASS, 1);

            this.addComponents(world.getRegistryKey(), pos, newCompass, biomeName);
            if (!player.getInventory().insertStack(newCompass)) {
                player.dropItem(newCompass, false);
            }
        }
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        Optional.ofNullable(stack.get(DataComponentTypes.CUSTOM_DATA)).ifPresent(nbtComponent -> {
            if (nbtComponent.contains(BIOME_NAME_KEY) && nbtComponent.contains(BIOME_POS_KEY)) {
                tooltip.add(Text.translatable("item.biomecompass.biome_compass.tooltip.biome_name", TextHelper.getBiomeNameFormatted(nbtComponent.copyNbt().getString(BIOME_NAME_KEY))));
                NbtIntArray nbtIntArray = null;
                try {
                    nbtIntArray = Optional.ofNullable((NbtIntArray) nbtComponent.copyNbt().get(BIOME_POS_KEY)).orElseThrow(DataFormatException::new);
                    if (nbtIntArray.size() != 3) throw new DataFormatException("Expected 3 integers, got " + nbtIntArray.size());
                } catch (DataFormatException e) {
                    BiomeCompass.LOGGER.error("Error while reading biome position from NBT", e);
                    return;
                }
                BlockPos pos = new BlockPos(nbtIntArray.get(0).intValue(), nbtIntArray.get(1).intValue(), nbtIntArray.get(2).intValue());
                tooltip.add(Text.translatable("item.biomecompass.biome_compass.tooltip.biome_pos", TextHelper.getBlockPosFormatted(pos)));
            }
        });
        super.appendTooltip(stack, context, tooltip, type);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        super.use(world, user, hand);

        if (user instanceof ServerPlayerEntity player) {
            if (player.isCreative() && player.isSneaking() && player.getStackInHand(hand).contains(DataComponentTypes.LODESTONE_TRACKER)) {
                BlockPos pos = Objects.requireNonNull(player.getStackInHand(hand).get(DataComponentTypes.LODESTONE_TRACKER)).target().get().pos();
                // TODO: somehow get top block at position? (y=120 is usually safe)
                player.requestTeleport(pos.getX(), 120, pos.getZ());
                return TypedActionResult.success(user.getStackInHand(hand));
            }

            BiomeSelectionGui.open(player, 0, hand);
            return TypedActionResult.success(user.getStackInHand(hand));
        } else {
            return TypedActionResult.pass(user.getStackInHand(hand));
        }
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
        return Items.COMPASS;
    }

    @Override
    public ItemStack getPolymerItemStack(ItemStack itemStack, TooltipType tooltipType, RegistryWrapper.WrapperLookup lookup, @Nullable ServerPlayerEntity player) {
        ItemStack fake = PolymerItem.super.getPolymerItemStack(itemStack, tooltipType, lookup, player);

        if (!PolymerResourcePackUtils.hasPack(player, player.getUuid())) {
            fake.addEnchantment(Enchantments.INFINITY, 0);
        }

        return fake;
    }

    @Override
    public int getPolymerCustomModelData(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
        return this.modelData;
    }
}
