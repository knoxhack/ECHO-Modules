package com.knoxhack.echoashfallprotocol.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a room in a procedural structure.
 */
public class Room {
    
    private final int x, y, z;
    private final int width, depth;
    private int height;
    private RoomType type;
    private final List<BlockPos> connectionPoints;
    private boolean isEntrance;
    private boolean isMainLootRoom;
    private int floorIndex; // 0 = ground floor; 1+ = upper floors (Pass 1)
    
    public Room(int x, int y, int z, int width, int height, int depth, RoomType type) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.type = type;
        this.connectionPoints = new ArrayList<>();
        this.isEntrance = false;
        this.isMainLootRoom = false;
    }
    
    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }
    public int getDepth() { return depth; }
    public RoomType getType() { return type; }
    public void setType(RoomType type) { this.type = type; }
    
    public int getCenterX() { return x + width / 2; }
    public int getCenterY() { return y + height / 2; }
    public int getCenterZ() { return z + depth / 2; }
    
    public BlockPos getCenter() {
        return new BlockPos(getCenterX(), getCenterY(), getCenterZ());
    }
    
    public boolean isEntrance() { return isEntrance; }
    public void setEntrance(boolean entrance) { isEntrance = entrance; }
    
    public boolean isMainLootRoom() { return isMainLootRoom; }
    public void setMainLootRoom(boolean mainLootRoom) { isMainLootRoom = mainLootRoom; }

    public int getFloorIndex() { return floorIndex; }
    public void setFloorIndex(int floorIndex) { this.floorIndex = floorIndex; }
    
    public List<BlockPos> getConnectionPoints() { return connectionPoints; }
    
    public void addConnectionPoint(BlockPos pos) {
        connectionPoints.add(pos);
    }
    
    /**
     * Check if a position is within this room (with wall thickness)
     */
    public boolean contains(int px, int py, int pz, int wallThickness) {
        return px >= x - wallThickness && px < x + width + wallThickness &&
               py >= y && py < y + height &&
               pz >= z - wallThickness && pz < z + depth + wallThickness;
    }
    
    /**
     * Check if this room intersects with another
     */
    public boolean intersects(Room other, int padding) {
        return x < other.x + other.width + padding && x + width + padding > other.x &&
               y < other.y + other.height && y + height > other.y &&
               z < other.z + other.depth + padding && z + depth + padding > other.z;
    }
    
    /**
     * Get random position inside room
     */
    public BlockPos getRandomPosition(net.minecraft.util.RandomSource random, int padding) {
        int usableWidth = Math.max(1, width - padding * 2);
        int usableDepth = Math.max(1, depth - padding * 2);
        int offsetX = Math.min(padding, Math.max(0, width - 1));
        int offsetZ = Math.min(padding, Math.max(0, depth - 1));
        int rx = x + offsetX + random.nextInt(usableWidth);
        int ry = y + 1;
        int rz = z + offsetZ + random.nextInt(usableDepth);
        return new BlockPos(rx, ry, rz);
    }
    
    /**
     * Get center position of room
     */
    public BlockPos getCenterPosition() {
        int cx = x + width / 2;
        int cy = y + height / 2;
        int cz = z + depth / 2;
        return new BlockPos(cx, cy, cz);
    }
    
    public enum RoomType {
        ENTRANCE,
        MAIN_HALL,
        STORAGE,
        LABORATORY,
        CONTAINMENT,
        SERVER_ROOM,
        ARMORY,
        BARRACKS,
        REACTOR_CORE,
        CONTROL_ROOM,
        MEDBAY,
        HALLWAY,
        STAIRCASE,
        HIDDEN_ROOM,
        // === EXPLORATION 1.1 ROOM TYPES ===
        MARKET,       // Crashbreak Salvage Yard
        OFFICE,       // Administrative spaces
        RECEPTION,    // Entry/greeting areas
        GREENHOUSE,   // Sporebound Sanctum
        WORKSHOP,     // Derelict Workshop
        SHAFT,        // Abandoned Mine
        ORE_VEIN,     // Abandoned Mine rich areas
        OBSERVATORY,   // Observation Post
        // === VANILLA VILLAGE HOUSE INTEGRATION ===
        VANILLA_HOUSE_SMALL,   // 5-7 block wide vanilla house
        VANILLA_HOUSE_MEDIUM,  // 7-9 block wide vanilla house
        VANILLA_HOUSE_LARGE    // 9-11 block wide vanilla house
    }
    
    @Override
    public String toString() {
        return String.format("Room[%s](%d,%d,%d) size(%dx%dx%d)", 
                type, x, y, z, width, height, depth);
    }
}
