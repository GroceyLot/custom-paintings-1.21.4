package com.cpaintings.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.component.ComponentType;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Painting {
    @SuppressWarnings("unchecked")
    public static final ComponentType<MapIdComponent> mapIdComponentType =
            (ComponentType<MapIdComponent>) Registries.DATA_COMPONENT_TYPE.get(Identifier.of("minecraft", "map_id"));
    @SuppressWarnings("unchecked")
    public static final ComponentType<LoreComponent> loreComponentType =
            (ComponentType<LoreComponent>) Registries.DATA_COMPONENT_TYPE.get(Identifier.of("minecraft", "lore"));

    // Keeps track of used "chunk positions" so each map is unique
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

    // These are the standard Minecraft "base colors" for map color indices
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

    // These are the standard brightness levels for Minecraft maps
    private static final float[] BRIGHTNESS_LEVELS = {
            0.71f,
            0.86f,
            1.00f,
            0.53f
    };

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    CommandManager.literal("painting")
                            .then(
                                    CommandManager.argument("url", StringArgumentType.string())

                                            // 1) Just /painting <url>
                                            .executes(context -> {
                                                String url = StringArgumentType.getString(context, "url");
                                                ServerCommandSource source = context.getSource();
                                                new Thread(() -> processPainting(source, url, 1, 1)).start();
                                                return 1;
                                            })

                                            // 2) /painting <url> <blocksx>
                                            .then(
                                                    CommandManager.argument("blocksx", IntegerArgumentType.integer(1))
                                                            .executes(context -> {
                                                                String url = StringArgumentType.getString(context, "url");
                                                                int blocksx = IntegerArgumentType.getInteger(context, "blocksx");
                                                                ServerCommandSource source = context.getSource();
                                                                new Thread(() -> processPainting(source, url, blocksx, 1)).start();
                                                                return 1;
                                                            })

                                                            // 3) /painting <url> <blocksx> <blocksy>
                                                            .then(
                                                                    CommandManager.argument("blocksy", IntegerArgumentType.integer(1))
                                                                            .executes(context -> {
                                                                                String url = StringArgumentType.getString(context, "url");
                                                                                int blocksx = IntegerArgumentType.getInteger(context, "blocksx");
                                                                                int blocksy = IntegerArgumentType.getInteger(context, "blocksy");
                                                                                ServerCommandSource source = context.getSource();
                                                                                new Thread(() -> processPainting(source, url, blocksx, blocksy)).start();
                                                                                return 1;
                                                                            })
                                                            )
                                            )
                            )
            );
        });
    }

    /**
     * Helper function to check if inventory is full
     */
    public static boolean isInventoryFull(PlayerEntity player) {
        Inventory inventory = player.getInventory();

        // Main inventory slots range (0 to 35 in most cases)
        // Slot 36 and beyond are for armor, offhand, and crafting
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inventory.getStack(i);

            // If the stack is empty, there's room in the inventory
            if (stack.isEmpty()) {
                return false;
            }
        }

        // No empty slots found in the main inventory or hotbar
        return true;
    }

    /**
     * Main logic to handle painting across multiple maps.
     */
    private static void processPainting(ServerCommandSource source, String url, int blocksx, int blocksy) {
        try {
            BufferedImage originalImage = downloadImage(url);
            if (originalImage == null) {
                throw new Exception("Image could not be read (null).");
            }

            // 1) Resize the image to (128 * blocksx) by (128 * blocksy)
            int totalWidth = 128 * blocksx;
            int totalHeight = 128 * blocksy;
            BufferedImage resized = resizeImage(originalImage, totalWidth, totalHeight);

            ServerWorld world = source.getWorld();
            PlayerEntity player = source.getPlayer();
            if (player == null) {
                source.sendError(Text.literal("Player not found."));
                return;
            }

            // 2) Split into sub-images (each 128x128) and create maps
            for (int y = 0; y < blocksy; y++) {
                // Invert the Y for bottom-left = (0,0)
                int subY = (blocksy - 1 - y);
                for (int x = 0; x < blocksx; x++) {
                    // Each tile is 128x128
                    BufferedImage tile = resized.getSubimage(x * 128, subY * 128, 128, 128);

                    // Allocate chunk for each tile
                    long chunkPos = allocateChunk();

                    // Create a unique MapState for this tile
                    MapState mapState = createMapStateForTile(world, chunkPos, tile);

                    // Get a new map ID from the world
                    MapIdComponent mapId = world.increaseAndGetMapId();
                    world.putMapState(mapId, mapState);

                    // Create the filled map item stack
                    ItemStack mapItem = new ItemStack(Items.FILLED_MAP);
                    mapItem.set(mapIdComponentType, mapId);

                    LoreComponent lore = new LoreComponent(Collections.singletonList(Text.literal("[" + x + "," + y + "]")));

                    // Name it as [x,y] so player knows where it belongs (only if there's more than one)
                    if (blocksx > 1 && blocksy > 1) {
                        mapItem.set(loreComponentType, lore);
                    }

                    // If the inventory is full, drop it on the ground
                    if (isInventoryFull(player)) {
                        player.dropItem(mapItem, false);
                    } else {
                        player.getInventory().insertStack(mapItem);
                    }
                }
            }

            source.sendFeedback(
                    () -> Text.literal("Created " + (blocksx * blocksy) + " maps. Check your inventory!"),
                    false
            );
        } catch (Exception e) {
            source.sendError(Text.literal("An error occurred: " + e.getMessage()));
            e.printStackTrace();
        }
    }

    /**
     * Download an image from a URL into a BufferedImage.
     */
    private static BufferedImage downloadImage(String url) throws Exception {
        try (InputStream in = new URI(url).toURL().openStream()) {
            return ImageIO.read(in);
        }
    }

    /**
     * Resize a BufferedImage to the given width and height.
     */
    private static BufferedImage resizeImage(BufferedImage original, int targetWidth, int targetHeight) {
        BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = resized.createGraphics();
        try {
            g.drawImage(original, 0, 0, targetWidth, targetHeight, null);
        } finally {
            g.dispose();
        }
        return resized;
    }

    /**
     * Create a MapState for the given tile (128x128) and place it at a "virtual" chunk position.
     */
    private static MapState createMapStateForTile(ServerWorld world, long chunkPos, BufferedImage image) {
        int centerX = (int) (chunkPos >> 32) << 4; // chunkPos >> 32 = "high bits"
        int centerZ = (int) (chunkPos & 0xFFFFFFFFL) << 4; // chunkPos & 0xFFFFFFFF = "low bits"

        // centerX + 64, centerZ + 64 to center it roughly in the chunk
        MapState mapState = MapState.of(
                centerX + 64,
                centerZ + 64,
                (byte) 2,  // scale 1:4 (byte 2)
                false,     // tracking position
                false,     // unlimited tracking
                world.getRegistryKey()
        );

        // Convert the 128x128 tile into Minecraft map colors
        updateMapStateWithImage(mapState, image);
        return mapState;
    }

    /**
     * Convert each pixel of the 128x128 tile to a Minecraft map color index.
     */
    private static void updateMapStateWithImage(MapState mapState, BufferedImage image) {
        for (int z = 0; z < 128; z++) {
            for (int x = 0; x < 128; x++) {
                int argb = image.getRGB(x, z);
                int alpha = (argb >> 24) & 0xFF;
                if (alpha < 128) {
                    mapState.colors[x + z * 128] = 0; // transparent
                } else {
                    int colorIndex = mapColorToMapData(argb);
                    mapState.colors[x + z * 128] = (byte) colorIndex;
                }
            }
        }
        mapState.markDirty();
    }

    /**
     * Converts an ARGB color to the best matching MC map color+shade index (0..255).
     */
    private static int mapColorToMapData(int argb) {
        Color target = new Color(argb, true);

        int bestIndex = 0;
        double bestDistance = Double.MAX_VALUE;

        // Ignore fully transparent
        if (target.getAlpha() < 128) {
            return 0;
        }

        for (int baseIndex = 1; baseIndex < MINECRAFT_MAP_COLORS.length; baseIndex++) {
            Color base = new Color(MINECRAFT_MAP_COLORS[baseIndex]);
            for (int shade = 0; shade < BRIGHTNESS_LEVELS.length; shade++) {
                float factor = BRIGHTNESS_LEVELS[shade];
                int r = (int) (base.getRed()   * factor);
                int g = (int) (base.getGreen() * factor);
                int b = (int) (base.getBlue()  * factor);

                Color variant = new Color(r, g, b);
                double distance = colorDistance(target, variant);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    // formula for map color index: baseIndex*4 + shade
                    bestIndex = baseIndex * 4 + shade;
                }
            }
        }
        return bestIndex & 0xFF;
    }

    /**
     * Calculate Euclidean distance between two colors (R,G,B).
     */
    private static double colorDistance(Color c1, Color c2) {
        int redDiff   = c1.getRed()   - c2.getRed();
        int greenDiff = c1.getGreen() - c2.getGreen();
        int blueDiff  = c1.getBlue()  - c2.getBlue();
        return Math.sqrt(redDiff * redDiff + greenDiff * greenDiff + blueDiff * blueDiff);
    }
}
