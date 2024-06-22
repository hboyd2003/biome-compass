package dev.mayaqq.biomecompass.helper;

import dev.mayaqq.biomecompass.item.BiomeCompassItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

public class TextHelper {
    public static Text getBiomeNameFormatted(NbtCompound nbt) {
        return getBiomeNameFormatted(nbt.getString(BiomeCompassItem.BIOME_NAME_KEY));
    }

    public static Text getBiomeNameFormatted(String name) {
        return Text.literal(name).formatted(Formatting.DARK_GREEN);
    }

    public static Text getBiomeNameFormatted(Identifier id) {
        return Text.translatable("biome." + id.getNamespace() + "." + id.getPath()).formatted(Formatting.DARK_GREEN);
    }

    public static Text getBlockPosFormatted(BlockPos pos) {
        return Text.empty()
                .append((Text.literal(String.valueOf(pos.getX()))).formatted(Formatting.GOLD))
                .append(", ")
                .append((Text.literal(String.valueOf(pos.getY()))).formatted(Formatting.GOLD))
                .append(", ")
                .append((Text.literal(String.valueOf(pos.getZ()))).formatted(Formatting.GOLD));
    }

    public static Text getDistanceFromPlayer(PlayerEntity player, BlockPos pos) {
        return Text.literal(String.valueOf(
                (int) getDistance(
                        player.getBlockX(),
                        player.getBlockZ(),
                        pos.getX(),
                        pos.getZ()
                )
        )).formatted(Formatting.GOLD);
    }

    private static float getDistance(int x1, int y1, int x2, int y2) {
        int i = x2 - x1;
        int j = y2 - y1;
        return MathHelper.sqrt((float)(i * i + j * j));
    }
}
