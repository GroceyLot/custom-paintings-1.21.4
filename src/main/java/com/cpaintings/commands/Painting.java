package com.cpaintings.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.component.ComponentType;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.map.MapState;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.entity.player.PlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Painting {
    public static final ComponentType<MapIdComponent> mapIdComponentType =
            (ComponentType<MapIdComponent>) Registries.DATA_COMPONENT_TYPE.get(Identifier.of("minecraft", "map_id"));

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("painting")
                    .then(CommandManager.argument("url", StringArgumentType.string())
                            .executes(context -> {
                                String url = StringArgumentType.getString(context, "url");
                                ServerCommandSource source = context.getSource();

                                new Thread(() -> processPainting(source, url)).start();
                                return 1;
                            })
                    )
            );
        });
    }

    private static BufferedImage downloadAndResizeImage(String url) throws Exception {
        BufferedImage original;
        try (InputStream in = new URI(url).toURL().openStream()) {
            original = ImageIO.read(in);
            if (original == null) {
                throw new Exception("Unsupported image format or invalid image.");
            }
        }

        int targetWidth = 128;
        int targetHeight = 128;

        BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = resized.createGraphics();
        try {
            g.drawImage(original, 0, 0, targetWidth, targetHeight, null);
        } finally {
            g.dispose();
        }

        return resized;
    }

    private static final Set<Long> usedChunks = new HashSet<>();

    private static long allocateChunk() {
        long base = 100000;
        long chunk;
        do {
            chunk = base++;
        } while (usedChunks.contains(chunk));
        usedChunks.add(chunk);
        return chunk;
    }

    private static final int[] MINECRAFT_MAP_COLORS = {
            0x000000, // 0  (NONE / Transparent)
            0x7FB238, // 1  (GRASS)
            0xF7E9A3, // 2  (SAND)
            0xC7C7C7, // 3  (WOOL)
            0xFF0000, // 4  (FIRE)
            0xA0A0FF, // 5  (ICE)
            0xA7A7A7, // 6  (METAL)
            0x007C00, // 7  (PLANT)
            0xFFFFFF, // 8  (SNOW)
            0xA4A8B8, // 9  (CLAY)
            0x976D4D, // 10 (DIRT)
            0x707070, // 11 (STONE)
            0x4040FF, // 12 (WATER)
            0x8F7748, // 13 (WOOD)
            0xFFFCF5, // 14 (QUARTZ)
            0xD87F33, // 15 (COLOR_ORANGE)
            0xB24CD8, // 16 (COLOR_MAGENTA)
            0x6699D8, // 17 (COLOR_LIGHT_BLUE)
            0xE5E533, // 18 (COLOR_YELLOW)
            0x7FCC19, // 19 (COLOR_LIGHT_GREEN)
            0xF27FA5, // 20 (COLOR_PINK)
            0x4C4C4C, // 21 (COLOR_GRAY)
            0x999999, // 22 (COLOR_LIGHT_GRAY)
            0x4C7F99, // 23 (COLOR_CYAN)
            0x7F3FB2, // 24 (COLOR_PURPLE)
            0x334CB2, // 25 (COLOR_BLUE)
            0x664C33, // 26 (COLOR_BROWN)
            0x667F33, // 27 (COLOR_GREEN)
            0x993333, // 28 (COLOR_RED)
            0x191919, // 29 (COLOR_BLACK)
            0xFAEE4D, // 30 (GOLD)
            0x5CDBD5, // 31 (DIAMOND)
            0x4A80FF, // 32 (LAPIS)
            0x00D93A, // 33 (EMERALD)
            0x815631, // 34 (PODZOL / SPRUCE)
            0x700200, // 35 (NETHER)
            0xD1B1A1, // 36 (TERRACOTTA_WHITE)
            0x9F5224, // 37 (TERRACOTTA_ORANGE)
            0x95576C, // 38 (TERRACOTTA_MAGENTA)
            0x706C8A, // 39 (TERRACOTTA_LIGHT_BLUE)
            0xBA8524, // 40 (TERRACOTTA_YELLOW)
            0x677535, // 41 (TERRACOTTA_LIGHT_GREEN)
            0xA04D4E, // 42 (TERRACOTTA_PINK)
            0x392923, // 43 (TERRACOTTA_GRAY)
            0x876B62, // 44 (TERRACOTTA_LIGHT_GRAY)
            0x575C5C, // 45 (TERRACOTTA_CYAN)
            0x7A4958, // 46 (TERRACOTTA_PURPLE)
            0x4C3E5C, // 47 (TERRACOTTA_BLUE)
            0x4C3223, // 48 (TERRACOTTA_BROWN)
            0x4C522A, // 49 (TERRACOTTA_GREEN)
            0x8E3C2E, // 50 (TERRACOTTA_RED)
            0x251610, // 51 (TERRACOTTA_BLACK)
            0xBD3031, // 52 (CRIMSON_NYLIUM)
            0x943F61, // 53 (CRIMSON_STEM)
            0x5C191D, // 54 (CRIMSON_HYPHAE)
            0x167E86, // 55 (WARPED_NYLIUM)
            0x3A8E8C, // 56 (WARPED_STEM)
            0x562C3E, // 57 (WARPED_HYPHAE)
            0x14B485, // 58 (WARPED_WART_BLOCK)
            0x646464, // 59 (DEEPSLATE)
            0xD8AF93, // 60 (RAW_IRON)
            0x7FA796  // 61 (GLOW_LICHEN)
    };

    private static final float[] BRIGHTNESS_LEVELS = {
            0.71f,
            0.86f,
            1.00f,
            0.53f
    };

    private static int mapColorToMapData(int argb) {
        int alpha = (argb >> 24) & 0xFF;

        if (alpha < 128) {
            return 0;
        }

        Color target = new Color(argb, true);

        int bestIndex = 0;
        double bestDistance = Double.MAX_VALUE;

        for (int baseIndex = 1; baseIndex < MINECRAFT_MAP_COLORS.length; baseIndex++) {
            Color base = new Color(MINECRAFT_MAP_COLORS[baseIndex]);

            for (int shade = 0; shade < BRIGHTNESS_LEVELS.length; shade++) {
                float factor = BRIGHTNESS_LEVELS[shade];

                int r = (int) (base.getRed()   * factor);
                int g = (int) (base.getGreen() * factor);
                int b = (int) (base.getBlue()  * factor);

                Color variant = new Color(r, g, b);
                double distance = calculateColorDistance(target, variant);

                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestIndex = baseIndex * 4 + shade;
                }
            }
        }

        return bestIndex & 0xFF;
    }

    private static double calculateColorDistance(Color c1, Color c2) {
        int redDiff = c1.getRed() - c2.getRed();
        int greenDiff = c1.getGreen() - c2.getGreen();
        int blueDiff = c1.getBlue() - c2.getBlue();
        return Math.sqrt(redDiff * redDiff + greenDiff * greenDiff + blueDiff * blueDiff);
    }

    private static void updateMapStateWithImage(MapState mapState, BufferedImage image) {
        for (int z = 0; z < 128; z++) {
            for (int x = 0; x < 128; x++) {
                int argb = image.getRGB(x, z);
                int alpha = (argb >> 24) & 0xFF;

                if (alpha < 128) {
                    mapState.colors[x + z * 128] = 0;
                } else {
                    int colorIndex = mapColorToMapData(argb);
                    mapState.colors[x + z * 128] = (byte) colorIndex;
                }
            }
        }
        mapState.markDirty();
    }

    private static ItemStack createMap(ServerWorld world, long chunkPos, BufferedImage image) {
        int startX = (int) (chunkPos >> 32) << 4;
        int startZ = (int) (chunkPos & 0xFFFFFFFFL) << 4;

        MapState mapState = MapState.of(
                startX + 64,
                startZ + 64,
                (byte) 2,
                false,
                false,
                world.getRegistryKey()
        );

        updateMapStateWithImage(mapState, image);

        MapIdComponent mapId = world.increaseAndGetMapId();
        world.putMapState(mapId, mapState);

        ItemStack map = new ItemStack(Items.FILLED_MAP);
        map.set(mapIdComponentType, mapId);

        return map;
    }

    private static void giveMapToPlayer(ServerCommandSource source, ItemStack map) {
        PlayerEntity player = source.getPlayer();
        if (player != null) {
            player.getInventory().insertStack(map);
            source.sendFeedback(() -> Text.literal("Map added to your inventory!"), false);
        } else {
            source.sendError(Text.literal("Player not found."));
        }
    }

    private static void processPainting(ServerCommandSource source, String url) {
        try {
            BufferedImage image = downloadAndResizeImage(url);
            ServerWorld world = source.getWorld();
            long chunkPos = allocateChunk();

            ItemStack map = createMap(world, chunkPos, image);
            giveMapToPlayer(source, map);
        } catch (Exception e) {
            source.sendError(Text.literal("An error occurred: " + e.getMessage()));
            e.printStackTrace();
        }
    }
}
