package com.knoxhack.echoashfallprotocol.worldgen;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a corridor connecting two rooms.
 */
public class Corridor {
    
    private final Room startRoom;
    private final Room endRoom;
    private final List<BlockPos> path;
    private final CorridorType type;
    private final int width;
    private final int height;
    
    public Corridor(Room start, Room end, CorridorType type, int width, int height) {
        this.startRoom = start;
        this.endRoom = end;
        this.type = type;
        this.width = width;
        this.height = height;
        this.path = new ArrayList<>();
    }
    
    public Room getStartRoom() { return startRoom; }
    public Room getEndRoom() { return endRoom; }
    public List<BlockPos> getPath() { return path; }
    public CorridorType getType() { return type; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    
    /**
     * Generate L-shaped corridor path between two rooms
     */
    public void generateLShapePath() {
        BlockPos start = startRoom.getCenter();
        BlockPos end = endRoom.getCenter();
        
        // Determine which axis to align first
        int midX = start.getX();
        int midZ = end.getZ();
        
        // First leg (horizontal)
        generateLine(start.getX(), start.getZ(), midX, start.getZ(), start.getY());
        
        // Corner
        path.add(new BlockPos(midX, start.getY(), start.getZ()));
        
        // Second leg (vertical)
        generateLine(midX, start.getZ(), midX, midZ, start.getY());
        
        // Third leg to destination
        generateLine(midX, midZ, end.getX(), midZ, start.getY());
        generateLine(end.getX(), midZ, end.getX(), end.getZ(), start.getY());
    }
    
    /**
     * Generate straight corridor path
     */
    public void generateStraightPath() {
        BlockPos start = startRoom.getCenter();
        BlockPos end = endRoom.getCenter();
        
        // Simple line between centers
        int steps = Math.max(Math.abs(end.getX() - start.getX()), Math.abs(end.getZ() - start.getZ()));
        for (int i = 0; i <= steps; i++) {
            float t = (float) i / steps;
            int x = (int) (start.getX() + (end.getX() - start.getX()) * t);
            int z = (int) (start.getZ() + (end.getZ() - start.getZ()) * t);
            path.add(new BlockPos(x, start.getY(), z));
        }
    }
    
    /**
     * Generate Z-shaped corridor with two turns
     */
    public void generateZShapePath(net.minecraft.util.RandomSource random) {
        BlockPos start = startRoom.getCenter();
        BlockPos end = endRoom.getCenter();
        
        int midX1 = start.getX() + (end.getX() - start.getX()) / 3;
        int midX2 = start.getX() + 2 * (end.getX() - start.getX()) / 3;
        int midZ = start.getZ() + (end.getZ() - start.getZ()) / 2;
        
        // First segment
        generateLine(start.getX(), start.getZ(), midX1, start.getZ(), start.getY());
        // Second segment
        generateLine(midX1, start.getZ(), midX2, midZ, start.getY());
        // Third segment
        generateLine(midX2, midZ, end.getX(), end.getZ(), start.getY());
    }
    
    private void generateLine(int x1, int z1, int x2, int z2, int y) {
        int dx = Integer.compare(x2, x1);
        int dz = Integer.compare(z2, z1);
        
        int x = x1, z = z1;
        while (x != x2 || z != z2) {
            path.add(new BlockPos(x, y, z));
            if (x != x2) x += dx;
            if (z != z2) z += dz;
        }
        path.add(new BlockPos(x2, y, z2));
    }
    
    /**
     * Get all positions in the corridor with width expansion
     */
    public List<BlockPos> getExpandedPath() {
        List<BlockPos> expanded = new ArrayList<>();
        for (BlockPos pos : path) {
            for (int dx = -width/2; dx <= width/2; dx++) {
                for (int dz = -width/2; dz <= width/2; dz++) {
                    expanded.add(pos.offset(dx, 0, dz));
                }
            }
        }
        return expanded;
    }
    
    public enum CorridorType {
        STRAIGHT,
        L_SHAPE,
        Z_SHAPE,
        SPIRAL
    }
    
    @Override
    public String toString() {
        return String.format("Corridor[%s] %s -> %s, path:%d", 
                type, startRoom.getType(), endRoom.getType(), path.size());
    }
}
