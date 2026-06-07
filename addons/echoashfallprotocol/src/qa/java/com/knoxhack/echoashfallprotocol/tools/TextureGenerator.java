package com.knoxhack.echoashfallprotocol.tools;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Texture Generator for ECHO: ASHFALL PROTOCOL
 * Generates procedural textures for all mod items and blocks
 */
public class TextureGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(TextureGenerator.class);

    private static final String ASSETS_DIR = resolveAssetsDir();
    private static final String BLOCK_TEXTURE_DIR = ASSETS_DIR + "/textures/block";
    private static final String ITEM_TEXTURE_DIR = ASSETS_DIR + "/textures/item";

    // Color definitions
    private static final Color GRAY_MED = new Color(120, 120, 120);
    private static final Color GRAY_LIGHT = new Color(180, 180, 180);
    private static final Color RUST = new Color(150, 100, 80);
    private static final Color COPPER = new Color(180, 120, 60);
    private static final Color TOXIC_GREEN = new Color(50, 150, 50);
    private static final Color RADIATION_YELLOW = new Color(180, 180, 50);
    private static final Color CYAN = new Color(100, 140, 160);
    private static final Color BLUE = new Color(80, 100, 140);
    private static final Color RED = new Color(160, 80, 80);
    private static final Color ORANGE = new Color(180, 140, 100);
    private static final Color PURPLE = new Color(140, 100, 160);
    private static final Color WHITE = new Color(240, 240, 240);

    private static String resolveAssetsDir() {
        File addonAssets = new File("addons/echoashfallprotocol/src/main/resources/assets/echoashfallprotocol");
        if (addonAssets.exists() || new File("addons/echoashfallprotocol").exists()) {
            return addonAssets.getPath();
        }
        return "src/main/resources/assets/echoashfallprotocol";
    }

    public static void main(String[] args) {
        System.out.println("=".repeat(50));
        System.out.println("ECHO: ASHFALL PROTOCOL Texture Generator");
        System.out.println("=".repeat(50));

        // Create directories
        new File(BLOCK_TEXTURE_DIR).mkdirs();
        new File(ITEM_TEXTURE_DIR).mkdirs();

        try {
            generateBlocks();
            generateItems();

            System.out.println("\n" + "=".repeat(50));
            System.out.println("Texture generation complete!");
            System.out.println("Blocks: " + BLOCK_TEXTURE_DIR);
            System.out.println("Items: " + ITEM_TEXTURE_DIR);
            System.out.println("=".repeat(50));
        } catch (Exception e) {
            LOGGER.error("Error generating textures", e);
        }
    }

    private static void generateBlocks() throws IOException {
        System.out.println("\n[1/2] Generating block textures...");

        // Environmental blocks
        generateDebrisBlock();
        generateToxicPuddle();
        generateRadiationBlock();

        // Drop pod blocks
        generateDropPodHull();
        generateDropPodGlass();

        // Machines
        generateMachine("hand_recycler", GRAY_MED, new Color(0, 255, 0));
        generateMachine("thermal_burner", RUST, new Color(255, 100, 0));
        generateMachine("water_purifier", CYAN, new Color(0, 150, 255));
        generateMachine("micro_generator", RED, new Color(255, 50, 50));
        generateMachine("filter_workbench", GRAY_MED, new Color(200, 200, 0));
        generateMachine("battery_bank", BLUE, new Color(0, 100, 255));
        generateMachine("scrap_press", GRAY_MED, WHITE);
        generateMachine("signal_scanner", new Color(100, 140, 100), new Color(0, 255, 100));
        generateMachine("field_med_bay", new Color(180, 180, 190), new Color(255, 100, 150));
        generateMachine("atmospheric_scrubber", CYAN, new Color(100, 255, 255));
        generateMachine("autofeed_hopper", ORANGE, new Color(255, 150, 50));
        generateMachine("contaminant_condenser", PURPLE, new Color(200, 100, 255));
    }

    private static void generateItems() throws IOException {
        System.out.println("\n[2/2] Generating item textures...");

        // Scrap materials
        generateScrapMetal();
        generateScrapWire();
        generateScrapCircuit();
        generateScrapPlastic();

        // Components
        generateSimpleItem("ash", GRAY_MED);
        generateSimpleItem("circuit_board", new Color(60, 100, 60));
        generateSimpleItem("energy_cell", BLUE);
        generateSimpleItem("filtration_membrane", GRAY_LIGHT);
        generateSimpleItem("machine_casing", GRAY_MED);

        // Bottles
        generateBottle("dirty_water_bottle", new Color(100, 120, 80));
        generateBottle("clean_water_bottle", new Color(150, 200, 255));

        // Medical
        generateVial("mutagen_vial", new Color(200, 100, 200));
        generateVial("rad_away", new Color(100, 255, 100));

        // Filters
        generateFilter("basic", GRAY_MED);
        generateFilter("advanced", CYAN);
        generateFilter("elite", new Color(160, 140, 100));

        // Tools & Armor
        generateScrapKnife();
        generateGasMask();
        generateSimpleItem("machine_upgrade_speed", new Color(60, 80, 60));
    }

    // ============ BLOCK GENERATORS ============

    private static void generateDebrisBlock() throws IOException {
        BufferedImage img = createBaseImage(16, new Color(120, 115, 110));
        Graphics2D g = img.createGraphics();

        // Add debris chunks
        for (int i = 0; i < 30; i++) {
            int x = (int)(Math.random() * 14);
            int y = (int)(Math.random() * 14);
            Color c = Math.random() < 0.5 ? new Color(90, 85, 80) : new Color(140, 135, 130);
            g.setColor(c);
            g.fillRect(x, y, 2 + (int)(Math.random() * 2), 1 + (int)(Math.random() * 2));
        }

        addBorder(g, 16, new Color(80, 75, 70));
        g.dispose();
        saveTexture(img, BLOCK_TEXTURE_DIR + "/debris_block.png");
    }

    private static void generateToxicPuddle() throws IOException {
        BufferedImage img = createBaseImage(16, TOXIC_GREEN);
        Graphics2D g = img.createGraphics();

        // Add glowing spots
        for (int i = 0; i < 20; i++) {
            int x = 1 + (int)(Math.random() * 13);
            int y = 1 + (int)(Math.random() * 13);
            g.setColor(new Color(100, 255, 100, 180));
            g.fillOval(x, y, 3, 3);
        }

        addBorder(g, 16, new Color(30, 100, 30));
        g.dispose();
        saveTexture(img, BLOCK_TEXTURE_DIR + "/toxic_puddle.png");
    }

    private static void generateRadiationBlock() throws IOException {
        BufferedImage img = createBaseImage(16, RADIATION_YELLOW);
        Graphics2D g = img.createGraphics();

        // Radiation symbol - three blades
        g.setColor(new Color(80, 80, 20));
        int center = 8;
        for (int angle = 0; angle < 360; angle += 120) {
            double rad = Math.toRadians(angle);
            int x1 = center + (int)(3 * Math.cos(rad));
            int y1 = center + (int)(3 * Math.sin(rad));
            int x2 = center + (int)(6 * Math.cos(rad));
            int y2 = center + (int)(6 * Math.sin(rad));
            g.setStroke(new BasicStroke(2));
            g.drawLine(x1, y1, x2, y2);
        }

        // Center dot
        g.fillOval(6, 6, 4, 4);

        addBorder(g, 16, new Color(140, 140, 40));
        g.dispose();
        saveTexture(img, BLOCK_TEXTURE_DIR + "/radiation_block.png");
    }

    private static void generateDropPodHull() throws IOException {
        BufferedImage img = createBaseImage(16, new Color(60, 65, 75));
        Graphics2D g = img.createGraphics();

        // Panel lines
        g.setColor(new Color(40, 45, 55));
        for (int i = 0; i <= 16; i += 4) {
            g.drawLine(i, 0, i, 15);
            g.drawLine(0, i, 15, i);
        }

        // Rivets
        g.setColor(new Color(90, 95, 105));
        for (int x = 2; x < 16; x += 4) {
            for (int y = 2; y < 16; y += 4) {
                g.fillOval(x-1, y-1, 3, 3);
            }
        }

        addBorder(g, 16, new Color(45, 50, 60));
        g.dispose();
        saveTexture(img, BLOCK_TEXTURE_DIR + "/drop_pod_hull.png");
    }

    private static void generateDropPodGlass() throws IOException {
        BufferedImage img = createBaseImage(16, new Color(150, 200, 220, 180));
        Graphics2D g = img.createGraphics();

        // Reflection streak
        g.setColor(new Color(200, 240, 255, 150));
        for (int i = 0; i < 6; i++) {
            g.drawLine(2+i, 2, 2+i, 6);
        }

        addBorder(g, 16, new Color(100, 150, 170));
        g.dispose();
        saveTexture(img, BLOCK_TEXTURE_DIR + "/drop_pod_glass.png");
    }

    private static void generateMachine(String name, Color baseColor, Color lightColor) throws IOException {
        BufferedImage img = createBaseImage(16, baseColor);
        Graphics2D g = img.createGraphics();

        // Main body
        g.setColor(baseColor);
        g.fillRect(2, 2, 12, 12);

        // Detail rectangle
        g.setColor(baseColor.darker());
        g.fillRect(4, 4, 8, 8);

        // Light/indicator
        g.setColor(lightColor);
        g.fillRect(5, 5, 3, 3);

        // Vent pattern
        g.setColor(new Color(60, 60, 60));
        for (int y = 9; y < 13; y += 2) {
            g.drawLine(4, y, 11, y);
        }

        addBorder(g, 16, new Color(80, 80, 80));

        // Highlights
        g.setColor(new Color(200, 200, 200));
        g.drawLine(2, 2, 13, 2);
        g.drawLine(2, 2, 2, 13);

        // Shadows
        g.setColor(new Color(60, 60, 60));
        g.drawLine(2, 13, 13, 13);
        g.drawLine(13, 2, 13, 13);

        g.dispose();
        saveTexture(img, BLOCK_TEXTURE_DIR + "/" + name + ".png");
    }

    // ============ ITEM GENERATORS ============

    private static void generateScrapMetal() throws IOException {
        BufferedImage img = createBaseImage(16, new Color(0, 0, 0, 0));
        Graphics2D g = img.createGraphics();

        // Metal chunks
        Color[] colors = {new Color(120, 120, 130), new Color(100, 100, 110), new Color(140, 140, 150)};
        int[][] positions = {{3, 3}, {8, 5}, {5, 9}};

        for (int i = 0; i < positions.length; i++) {
            g.setColor(colors[i]);
            g.fillRect(positions[i][0], positions[i][1], 4, 4);
        }

        // Add some shine
        g.setColor(new Color(180, 180, 190));
        g.drawLine(3, 3, 6, 3);
        g.drawLine(3, 3, 3, 6);

        g.dispose();
        saveTexture(img, ITEM_TEXTURE_DIR + "/scrap_metal.png");
    }

    private static void generateScrapWire() throws IOException {
        BufferedImage img = createBaseImage(16, new Color(0, 0, 0, 0));
        Graphics2D g = img.createGraphics();

        // Wire coils
        int[][] centers = {{5, 5}, {10, 8}};
        for (int[] center : centers) {
            g.setColor(COPPER);
            g.fillOval(center[0]-2, center[1]-2, 5, 5);
            g.setColor(new Color(200, 140, 80));
            g.fillOval(center[0]-1, center[1]-1, 3, 3);
        }

        g.dispose();
        saveTexture(img, ITEM_TEXTURE_DIR + "/scrap_wire.png");
    }

    private static void generateScrapCircuit() throws IOException {
        BufferedImage img = createBaseImage(16, new Color(0, 0, 0, 0));
        Graphics2D g = img.createGraphics();

        // Green board
        g.setColor(new Color(60, 100, 60));
        g.fillRect(3, 3, 10, 10);

        // Traces
        g.setColor(new Color(150, 150, 100));
        g.drawLine(4, 5, 11, 5);
        g.drawLine(4, 8, 11, 8);
        g.drawLine(6, 5, 6, 10);

        // Components
        g.setColor(new Color(80, 80, 80));
        g.fillRect(4, 6, 2, 2);
        g.setColor(new Color(150, 50, 50));
        g.fillRect(9, 9, 2, 2);

        g.dispose();
        saveTexture(img, ITEM_TEXTURE_DIR + "/scrap_circuit.png");
    }

    private static void generateScrapPlastic() throws IOException {
        BufferedImage img = createBaseImage(16, new Color(0, 0, 0, 0));
        Graphics2D g = img.createGraphics();

        // Plastic shards
        Color[] colors = {new Color(150, 150, 160), new Color(130, 130, 140), new Color(170, 170, 180)};
        int[][][] shards = {{{4, 4}, {7, 5}, {6, 8}, {3, 6}}, {{9, 6}, {12, 7}, {11, 10}, {8, 9}}};

        for (int i = 0; i < shards.length; i++) {
            g.setColor(colors[i]);
            int[][] shard = shards[i];
            Polygon poly = new Polygon();
            for (int[] point : shard) {
                poly.addPoint(point[0], point[1]);
            }
            g.fillPolygon(poly);
        }

        g.dispose();
        saveTexture(img, ITEM_TEXTURE_DIR + "/scrap_plastic.png");
    }

    private static void generateSimpleItem(String name, Color color) throws IOException {
        BufferedImage img = createBaseImage(16, new Color(0, 0, 0, 0));
        Graphics2D g = img.createGraphics();

        // Base shape
        g.setColor(color);
        g.fillRect(4, 4, 8, 8);

        // Detail
        g.setColor(color.darker());
        g.fillRect(6, 6, 4, 4);

        // Highlight
        g.setColor(new Color(220, 220, 220));
        g.drawLine(4, 4, 11, 4);
        g.drawLine(4, 4, 4, 11);

        g.dispose();
        saveTexture(img, ITEM_TEXTURE_DIR + "/" + name + ".png");
    }

    private static void generateBottle(String name, Color liquidColor) throws IOException {
        BufferedImage img = createBaseImage(16, new Color(0, 0, 0, 0));
        Graphics2D g = img.createGraphics();

        // Glass bottle
        g.setColor(new Color(180, 180, 190, 200));
        g.fillRect(5, 3, 6, 10);

        // Liquid
        g.setColor(liquidColor);
        g.fillRect(6, 4, 4, 7);

        // Cap
        g.setColor(new Color(120, 120, 120));
        g.fillRect(6, 2, 4, 2);

        g.dispose();
        saveTexture(img, ITEM_TEXTURE_DIR + "/" + name + ".png");
    }

    private static void generateVial(String name, Color liquidColor) throws IOException {
        BufferedImage img = createBaseImage(16, new Color(0, 0, 0, 0));
        Graphics2D g = img.createGraphics();

        // Vial
        g.setColor(new Color(150, 150, 150, 180));
        g.fillRect(6, 3, 4, 10);

        // Liquid
        g.setColor(liquidColor);
        g.fillRect(7, 5, 2, 6);

        // Cap
        g.setColor(new Color(150, 150, 150));
        g.fillRect(6, 2, 4, 2);

        g.dispose();
        saveTexture(img, ITEM_TEXTURE_DIR + "/" + name + ".png");
    }

    private static void generateFilter(String tier, Color color) throws IOException {
        BufferedImage img = createBaseImage(16, new Color(0, 0, 0, 0));
        Graphics2D g = img.createGraphics();

        // Cylinder
        g.setColor(color);
        g.fillRect(4, 3, 8, 10);

        // Filter layers
        g.setColor(color.darker());
        for (int y = 5; y < 11; y += 2) {
            g.drawLine(4, y, 11, y);
        }

        // Ends
        g.setColor(new Color(100, 100, 100));
        g.fillRect(4, 2, 8, 2);
        g.fillRect(4, 12, 8, 2);

        g.dispose();
        saveTexture(img, ITEM_TEXTURE_DIR + "/filter_cartridge_" + tier + ".png");
    }

    private static void generateScrapKnife() throws IOException {
        BufferedImage img = createBaseImage(16, new Color(0, 0, 0, 0));
        Graphics2D g = img.createGraphics();

        // Handle
        g.setColor(new Color(100, 80, 60));
        g.fillRect(4, 9, 8, 3);

        // Blade
        g.setColor(new Color(180, 180, 190));
        int[] xPoints = {7, 11, 11, 7};
        int[] yPoints = {9, 5, 9, 9};
        g.fillPolygon(xPoints, yPoints, 4);

        g.dispose();
        saveTexture(img, ITEM_TEXTURE_DIR + "/scrap_knife.png");
    }

    private static void generateGasMask() throws IOException {
        BufferedImage img = createBaseImage(16, new Color(0, 0, 0, 0));
        Graphics2D g = img.createGraphics();

        // Mask base
        g.setColor(new Color(60, 70, 80));
        g.fillRect(3, 4, 10, 8);

        // Eye lenses
        g.setColor(new Color(100, 150, 200));
        g.fillOval(5, 5, 3, 3);
        g.fillOval(8, 5, 3, 3);

        // Filter
        g.setColor(new Color(80, 80, 80));
        g.fillRect(6, 8, 4, 3);

        g.dispose();
        saveTexture(img, ITEM_TEXTURE_DIR + "/gas_mask.png");
    }

    // ============ UTILITY METHODS ============

    private static BufferedImage createBaseImage(int size, Color color) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, size, size);
        g.dispose();
        return img;
    }

    private static void addBorder(Graphics2D g, int size, Color color) {
        g.setColor(color);
        g.drawRect(0, 0, size-1, size-1);
    }

    private static void saveTexture(BufferedImage img, String path) throws IOException {
        // Scale up to 64x64 for better visibility while keeping pixel art style
        BufferedImage scaled = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(img, 0, 0, 64, 64, null);
        g.dispose();

        File file = new File(path);
        file.getParentFile().mkdirs();
        ImageIO.write(scaled, "PNG", file);
        System.out.println("Generated: " + path);
    }
}
