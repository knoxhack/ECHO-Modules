package com.knoxhack.echoashfallprotocol.tools;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Professional Texture Generator for ECHO: ASHFALL PROTOCOL
 * Author-level procedural texture synthesis using Java AWT
 */
public class ProTextureGenerator {
    
    private static final int SIZE = 16;
    private static final int SCALE = 4;
    private static final String ASSETS_DIR = resolveAssetsDir();
    private static final String BLOCK_DIR = ASSETS_DIR + "/textures/block";
    private static final String ITEM_DIR = ASSETS_DIR + "/textures/item";
    
    private final Random random = new Random(42);

    private static String resolveAssetsDir() {
        File addonAssets = new File("addons/echoashfallprotocol/src/main/resources/assets/echoashfallprotocol");
        if (addonAssets.exists() || new File("addons/echoashfallprotocol").exists()) {
            return addonAssets.getPath();
        }
        return "src/main/resources/assets/echoashfallprotocol";
    }
    
    // Professional Palettes
    public static class Palette {
        final Color primary, secondary, accent, highlight, shadow, rust, grime, emissive;
        
        Palette(Color primary, Color secondary, Color accent, Color highlight, Color shadow,
                Color rust, Color grime, Color emissive) {
            this.primary = primary; this.secondary = secondary; this.accent = accent;
            this.highlight = highlight; this.shadow = shadow;
            this.rust = rust; this.grime = grime; this.emissive = emissive;
        }
    }
    
    private static final Map<String, Palette> PALETTES = new HashMap<>();
    static {
        PALETTES.put("rusty_metal", new Palette(
            new Color(90, 85, 80), new Color(110, 105, 100), new Color(70, 65, 60),
            new Color(140, 135, 130), new Color(50, 48, 45),
            new Color(140, 90, 55), new Color(60, 58, 55), null));
        
        PALETTES.put("clean_steel", new Palette(
            new Color(120, 125, 130), new Color(140, 145, 150), new Color(100, 105, 110),
            new Color(180, 185, 190), new Color(80, 83, 85),
            null, new Color(90, 92, 95), null));
        
        PALETTES.put("toxic_waste", new Palette(
            new Color(35, 140, 45), new Color(45, 160, 55), new Color(25, 120, 35),
            new Color(80, 220, 90), new Color(20, 80, 25),
            null, new Color(30, 100, 35), new Color(100, 255, 110)));
        
        PALETTES.put("radiation_hazard", new Palette(
            new Color(160, 160, 40), new Color(180, 180, 50), new Color(140, 140, 30),
            new Color(220, 220, 80), new Color(100, 100, 20),
            new Color(150, 100, 50), new Color(80, 80, 25), new Color(255, 255, 100)));
        
        PALETTES.put("tech_blue", new Palette(
            new Color(60, 100, 140), new Color(80, 130, 170), new Color(40, 80, 120),
            new Color(120, 180, 220), new Color(30, 50, 80),
            null, new Color(50, 75, 100), new Color(80, 200, 255)));
        
        PALETTES.put("medical_white", new Palette(
            new Color(200, 205, 210), new Color(220, 225, 230), new Color(180, 185, 190),
            new Color(240, 245, 250), new Color(150, 155, 160),
            null, new Color(170, 175, 180), new Color(255, 180, 200)));
        
        PALETTES.put("copper_pipes", new Palette(
            new Color(140, 100, 60), new Color(160, 120, 75), new Color(120, 85, 50),
            new Color(200, 150, 100), new Color(100, 70, 40),
            new Color(130, 70, 40), new Color(90, 65, 40), null));
        
        PALETTES.put("purple_tech", new Palette(
            new Color(100, 70, 120), new Color(120, 90, 150), new Color(80, 55, 95),
            new Color(160, 130, 200), new Color(60, 40, 75),
            null, new Color(70, 50, 85), new Color(200, 150, 255)));
        
        PALETTES.put("dark_military", new Palette(
            new Color(50, 55, 45), new Color(65, 70, 58), new Color(40, 44, 36),
            new Color(90, 95, 82), new Color(28, 32, 26),
            new Color(100, 65, 40), new Color(35, 38, 32), new Color(80, 200, 120)));
        
        PALETTES.put("orange_industrial", new Palette(
            new Color(180, 120, 50), new Color(200, 140, 65), new Color(160, 105, 40),
            new Color(240, 170, 90), new Color(130, 85, 35),
            new Color(150, 85, 40), new Color(140, 95, 45), new Color(255, 180, 80)));
    }
    
    public static void main(String[] args) throws IOException {
        new File(BLOCK_DIR).mkdirs();
        new File(ITEM_DIR).mkdirs();
        
        System.out.println("=" .repeat(60));
        System.out.println("ECHO: ASHFALL PROTOCOL - Professional Texture Generator");
        System.out.println("=" .repeat(60));
        
        ProTextureGenerator gen = new ProTextureGenerator();
        gen.generateAll();
        
        System.out.println("\n" + "=" .repeat(60));
        System.out.println("Generation complete!");
        System.out.println("=" .repeat(60));
    }
    
    private void generateAll() throws IOException {
        // Environmental blocks
        System.out.println("\n[1/4] Environmental blocks...");
        saveBlock("debris_block", generateBlock(PALETTES.get("rusty_metal"), 0.6f, 0.4f, false));
        saveBlock("toxic_puddle", generateLiquid(PALETTES.get("toxic_waste"), true));
        saveBlock("radiation_block", generateBlock(PALETTES.get("radiation_hazard"), 0.5f, 0.3f, true));
        
        // Drop pod
        System.out.println("\n[2/4] Drop pod blocks...");
        saveBlock("drop_pod_hull", generateBlock(PALETTES.get("dark_military"), 0.3f, 0.2f, false));
        saveBlock("drop_pod_glass", generateGlass(PALETTES.get("tech_blue")));
        
        // Machines
        System.out.println("\n[3/4] Machine blocks...");
        saveBlock("hand_recycler", generateBlock(PALETTES.get("clean_steel"), 0.2f, 0.0f, true));
        saveBlock("thermal_burner", generateBlock(PALETTES.get("copper_pipes"), 0.5f, 0.4f, true));
        saveBlock("water_purifier", generateBlock(PALETTES.get("tech_blue"), 0.2f, 0.0f, true));
        saveBlock("micro_generator", generateBlock(PALETTES.get("rusty_metal"), 0.3f, 0.3f, true));
        saveBlock("filter_workbench", generateBlock(PALETTES.get("clean_steel"), 0.2f, 0.0f, false));
        saveBlock("battery_bank", generateBlock(PALETTES.get("tech_blue"), 0.2f, 0.0f, true));
        saveBlock("scrap_press", generateBlock(PALETTES.get("clean_steel"), 0.2f, 0.0f, false));
        saveBlock("signal_scanner", generateBlock(PALETTES.get("dark_military"), 0.3f, 0.2f, true));
        saveBlock("field_med_bay", generateBlock(PALETTES.get("medical_white"), 0.1f, 0.0f, true));
        saveBlock("atmospheric_scrubber", generateBlock(PALETTES.get("tech_blue"), 0.2f, 0.0f, true));
        saveBlock("autofeed_hopper", generateBlock(PALETTES.get("orange_industrial"), 0.2f, 0.1f, false));
        saveBlock("contaminant_condenser", generateBlock(PALETTES.get("purple_tech"), 0.2f, 0.0f, true));
        
        // Items
        System.out.println("\n[4/4] Items...");
        generateItems();
    }
    
    private BufferedImage generateBlock(Palette p, float wear, float rust, boolean emissive) {
        BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        
        // Base gradient
        for (int y = 0; y < SIZE; y++) {
            float light = 1 - (y / (float)SIZE) * 0.3f;
            Color c = adjustBrightness(p.primary, light);
            g.setColor(c);
            g.drawLine(0, y, SIZE-1, y);
        }
        
        // Surface noise
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                float n = noise(x * 0.1f, y * 0.1f);
                if (Math.abs(n) > 0.3f) {
                    Color c = blend(p.secondary, p.primary, 0.5f + n * 0.5f);
                    g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), (int)(Math.abs(n) * 200)));
                    g.drawRect(x, y, 1, 1);
                }
            }
        }
        
        // Rust
        if (rust > 0) {
            addRust(g, p, rust);
        }
        
        // Wear
        if (wear > 0) {
            addWear(g, p, wear);
        }
        
        // Rim lighting
        addRimLighting(g, p);
        
        // Ambient occlusion
        addAmbientOcclusion(g);
        
        // Emissive
        if (emissive && p.emissive != null) {
            g.setColor(p.emissive);
            g.fillRect(SIZE-6, 4, 3, 3);
        }
        
        g.dispose();
        return img;
    }
    
    private BufferedImage generateLiquid(Palette p, boolean glow) {
        BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        
        // Base liquid
        for (int y = 0; y < SIZE; y++) {
            float t = y / (float)SIZE;
            Color c = blend(p.primary, p.secondary, t);
            g.setColor(c);
            g.drawLine(0, y, SIZE-1, y);
        }
        
        // Surface ripples
        for (int x = 0; x < SIZE; x++) {
            int y = (int)(SIZE * 0.3f + Math.sin(x * 0.5f) * 2);
            g.setColor(p.highlight);
            g.drawLine(x, y, x, y+1);
        }
        
        // Glow spots
        if (glow && p.emissive != null) {
            for (int i = 0; i < 5; i++) {
                int x = random.nextInt(SIZE-4) + 2;
                int y = random.nextInt(SIZE-4) + 2;
                g.setColor(p.emissive);
                g.fillRect(x, y, 2, 2);
            }
        }
        
        g.dispose();
        return img;
    }
    
    private BufferedImage generateGlass(Palette p) {
        BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        
        // Semi-transparent base
        g.setColor(new Color(p.primary.getRed(), p.primary.getGreen(), p.primary.getBlue(), 180));
        g.fillRect(2, 2, SIZE-4, SIZE-4);
        
        // Frame
        g.setColor(p.secondary);
        g.drawRect(2, 2, SIZE-5, SIZE-5);
        
        // Glass shine
        g.setColor(new Color(220, 230, 240, 150));
        g.drawLine(3, 3, 3, 6);
        
        // Emissive interior
        if (p.emissive != null) {
            g.setColor(p.emissive);
            g.fillRect(5, 5, 6, 6);
        }
        
        g.dispose();
        return img;
    }
    
    private void generateItems() throws IOException {
        // Scrap materials
        saveItem("scrap_metal", generateItemRect(PALETTES.get("rusty_metal"), true, "chip", new Color(140, 140, 150)));
        saveItem("scrap_wire", generateItemRect(PALETTES.get("copper_pipes"), false, "circle", new Color(160, 110, 60)));
        saveItem("scrap_circuit", generateItemRect(PALETTES.get("rusty_metal"), false, "cross", new Color(60, 100, 60)));
        saveItem("scrap_plastic", generateItemRect(PALETTES.get("clean_steel"), false, "none", null));
        
        // Components
        saveItem("ash", generateItemRect(PALETTES.get("rusty_metal"), false, "none", null));
        saveItem("circuit_board", generateItemRect(PALETTES.get("rusty_metal"), false, "grid", new Color(60, 100, 60)));
        saveItem("energy_cell", generateItemRect(PALETTES.get("tech_blue"), true, "circle", new Color(80, 160, 255)));
        saveItem("filtration_membrane", generateItemRect(PALETTES.get("clean_steel"), false, "none", null));
        saveItem("machine_casing", generateItemRect(PALETTES.get("clean_steel"), true, "line", null));
        
        // Bottles
        saveItem("dirty_water_bottle", generateBottle(PALETTES.get("rusty_metal"), new Color(100, 120, 80), 0.8f));
        saveItem("clean_water_bottle", generateBottle(PALETTES.get("tech_blue"), new Color(150, 200, 255), 0.8f));
        
        // Medical
        saveItem("mutagen_vial", generateVial(PALETTES.get("purple_tech"), new Color(200, 100, 200), 0.9f, true));
        saveItem("rad_away", generateVial(PALETTES.get("toxic_waste"), new Color(100, 255, 100), 0.9f, true));
        
        // Filters
        saveItem("filter_cartridge_basic", generateItemRect(PALETTES.get("clean_steel"), false, "line", new Color(80, 80, 80)));
        saveItem("filter_cartridge_advanced", generateItemRect(PALETTES.get("tech_blue"), false, "line", new Color(80, 120, 160)));
        saveItem("filter_cartridge_elite", generateItemRect(PALETTES.get("rusty_metal"), true, "line", new Color(160, 140, 100)));
        
        // Tools
        saveItem("scrap_knife", generateBlade(PALETTES.get("clean_steel")));
        saveItem("gas_mask", generateMask(PALETTES.get("dark_military")));
        saveItem("machine_upgrade_speed", generateItemRect(PALETTES.get("rusty_metal"), true, "chip", new Color(150, 50, 50)));
    }
    
    private BufferedImage generateItemRect(Palette p, boolean metallic, String detailType, Color detailColor) {
        BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        
        // Main body
        int x1 = 4, y1 = 4, x2 = 11, y2 = 11;
        for (int y = y1; y <= y2; y++) {
            float t = (y - y1) / (float)(y2 - y1);
            Color c = blend(p.primary, p.secondary, t);
            g.setColor(c);
            g.drawLine(x1, y, x2, y);
        }
        
        // Detail
        if (detailColor != null && !detailType.equals("none")) {
            g.setColor(detailColor);
            switch (detailType) {
                case "chip" -> g.fillRect(6, 6, 3, 3);
                case "circle" -> g.fillOval(6, 6, 3, 3);
                case "line" -> {
                    g.drawLine(x1, 8, x2, 8);
                    g.drawLine(x1, 8, x2, 8);
                }
                case "cross" -> {
                    int cx = (x1 + x2) / 2, cy = (y1 + y2) / 2;
                    g.drawLine(cx, y1+1, cx, y2-1);
                    g.drawLine(x1+1, cy, x2-1, cy);
                }
                case "grid" -> {
                    for (int gx = 6; gx <= 8; gx += 2) {
                        for (int gy = 6; gy <= 8; gy += 2) {
                            g.drawRect(gx, gy, 1, 1);
                        }
                    }
                }
            }
        }
        
        // Shading
        addRimLighting(g, p);
        
        g.dispose();
        return img;
    }
    
    private BufferedImage generateVial(Palette p, Color liquid, float level, boolean glow) {
        BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        
        // Glass
        int x1 = 6, y1 = 2, x2 = 9, y2 = 13;
        g.setColor(new Color(180, 185, 190, 200));
        for (int y = y1; y <= y2; y++) {
            g.drawLine(x1, y, x2, y);
        }
        
        // Liquid
        int liquidY = (int)(y2 - (y2 - y1) * level);
        g.setColor(liquid);
        for (int y = liquidY; y < y2; y++) {
            g.drawLine(x1+1, y, x2-1, y);
        }
        
        // Cap
        g.setColor(new Color(120, 120, 120));
        g.fillRect(x1, y1-1, x2-x1+1, 1);
        
        // Shine
        g.setColor(new Color(220, 230, 240, 150));
        g.drawLine(x1, y1+2, x1, y1+5);
        
        g.dispose();
        return img;
    }
    
    private BufferedImage generateBottle(Palette p, Color liquid, float level) {
        BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        
        // Body
        int bx1 = 5, by1 = 4, bx2 = 10, by2 = 12;
        for (int y = by1; y <= by2; y++) {
            float t = (y - by1) / (float)(by2 - by1);
            g.setColor(blend(p.primary, p.secondary, t));
            g.drawLine(bx1, y, bx2, y);
        }
        
        // Neck
        g.setColor(new Color(p.highlight.getRed(), p.highlight.getGreen(), p.highlight.getBlue(), 200));
        g.drawLine(6, 2, 9, 2);
        
        // Cap
        g.setColor(new Color(100, 100, 100));
        g.fillRect(6, 2, 4, 1);
        
        // Reflection
        g.setColor(new Color(240, 245, 250, 180));
        g.drawLine(bx1, by1+1, bx1, by1+4);
        
        g.dispose();
        return img;
    }
    
    private BufferedImage generateBlade(Palette p) {
        BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        
        // Handle
        g.setColor(p.secondary);
        g.fillRect(4, 9, 8, 3);
        
        // Blade
        int[] x = {7, 11, 11, 7};
        int[] y = {9, 5, 9, 9};
        g.setColor(p.highlight);
        g.fillPolygon(x, y, 4);
        
        // Sharp edge
        g.setColor(new Color(220, 220, 230));
        g.drawLine(7, 9, 11, 5);
        
        g.dispose();
        return img;
    }
    
    private BufferedImage generateMask(Palette p) {
        BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        
        // Face plate
        g.setColor(p.primary);
        g.fillRect(3, 4, 10, 8);
        
        // Eyes
        g.setColor(new Color(100, 150, 200));
        g.fillOval(5, 5, 2, 2);
        g.fillOval(8, 5, 2, 2);
        
        // Filter
        g.setColor(p.accent);
        g.fillRect(6, 8, 4, 2);
        
        g.dispose();
        return img;
    }
    
    // Helper methods
    private void addRust(Graphics2D g, Palette p, float amount) {
        Color rustBase = p.rust != null ? p.rust : new Color(140, 80, 40);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                if (noise(x * 0.15f, y * 0.15f) > 1 - amount * 2) {
                    g.setColor(new Color(rustBase.getRed(), rustBase.getGreen(), rustBase.getBlue(), 180));
                    g.drawRect(x, y, 1, 1);
                }
            }
        }
    }
    
    private void addWear(Graphics2D g, Palette p, float amount) {
        Color grime = p.grime != null ? p.grime : p.shadow;
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                float n = noise(x * 0.2f, y * 0.2f);
                if (n > 1 - amount) {
                    g.setColor(new Color(grime.getRed(), grime.getGreen(), grime.getBlue(), 150));
                    g.drawRect(x, y, 1, 1);
                }
            }
        }
    }
    
    private void addRimLighting(Graphics2D g, Palette p) {
        // Top/left (light)
        g.setColor(new Color(p.highlight.getRed(), p.highlight.getGreen(), p.highlight.getBlue(), 200));
        for (int x = 0; x < SIZE; x++) {
            g.drawRect(x, 0, 1, 1);
        }
        for (int y = 0; y < SIZE; y++) {
            g.drawRect(0, y, 1, 1);
        }
        
        // Bottom/right (shadow)
        g.setColor(new Color(p.shadow.getRed(), p.shadow.getGreen(), p.shadow.getBlue(), 200));
        for (int x = 0; x < SIZE; x++) {
            g.drawRect(x, SIZE-1, 1, 1);
        }
        for (int y = 0; y < SIZE; y++) {
            g.drawRect(SIZE-1, y, 1, 1);
        }
    }
    
    private void addAmbientOcclusion(Graphics2D g) {
        // Corner darkening
        int[][] corners = {{0, 0}, {SIZE-1, 0}, {0, SIZE-1}, {SIZE-1, SIZE-1}};
        for (int[] c : corners) {
            for (int dy = 0; dy < 4; dy++) {
                for (int dx = 0; dx < 4; dx++) {
                    int x = c[0] + dx - 2, y = c[1] + dy - 2;
                    if (x >= 0 && x < SIZE && y >= 0 && y < SIZE) {
                        float dist = (float)Math.sqrt(dx*dx + dy*dy);
                        int alpha = Math.max(0, Math.min(255, (int)(100 * (1 - dist / 4))));
                        g.setColor(new Color(0, 0, 0, alpha));
                        g.drawRect(x, y, 1, 1);
                    }
                }
            }
        }
    }
    
    private float noise(float x, float y) {
        return (float)(Math.sin(x * 12.9898 + y * 78.233) * 43758.5453 % 1);
    }
    
    private Color adjustBrightness(Color c, float factor) {
        return new Color(
            Math.min(255, (int)(c.getRed() * factor)),
            Math.min(255, (int)(c.getGreen() * factor)),
            Math.min(255, (int)(c.getBlue() * factor))
        );
    }
    
    private Color blend(Color c1, Color c2, float t) {
        return new Color(
            (int)(c1.getRed() + (c2.getRed() - c1.getRed()) * t),
            (int)(c1.getGreen() + (c2.getGreen() - c1.getGreen()) * t),
            (int)(c1.getBlue() + (c2.getBlue() - c1.getBlue()) * t)
        );
    }
    
    private void saveBlock(String name, BufferedImage img) throws IOException {
        saveTexture(img, BLOCK_DIR + "/" + name + ".png");
    }
    
    private void saveItem(String name, BufferedImage img) throws IOException {
        saveTexture(img, ITEM_DIR + "/" + name + ".png");
    }
    
    private void saveTexture(BufferedImage img, String path) throws IOException {
        // Scale with nearest neighbor
        BufferedImage scaled = new BufferedImage(SIZE * SCALE, SIZE * SCALE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(img, 0, 0, SIZE * SCALE, SIZE * SCALE, null);
        g.dispose();
        
        ImageIO.write(scaled, "PNG", new File(path));
        System.out.println("  Generated: " + path);
    }
}
