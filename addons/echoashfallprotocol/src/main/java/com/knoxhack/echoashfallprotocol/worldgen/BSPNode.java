package com.knoxhack.echoashfallprotocol.worldgen;

import net.minecraft.util.RandomSource;

/**
 * Binary Space Partitioning node for procedural room generation.
 */
public class BSPNode {
    
    private int x, y, z;
    private int width, height, depth;
    private BSPNode left;
    private BSPNode right;
    private Room room;
    private boolean splitHorizontal; // true = split on X axis, false = split on Z axis
    
    public BSPNode(int x, int y, int z, int width, int height, int depth) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.left = null;
        this.right = null;
        this.room = null;
    }
    
    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getDepth() { return depth; }
    public BSPNode getLeft() { return left; }
    public BSPNode getRight() { return right; }
    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }
    public boolean isSplitHorizontal() { return splitHorizontal; }
    
    public boolean isLeaf() {
        return left == null && right == null;
    }
    
    /**
     * Recursively split this node into smaller regions
     * @param minSize Minimum size for a room
     * @param maxDepth Maximum recursion depth
     * @param currentDepth Current recursion depth
     * @param random Random source
     * @return Number of leaf nodes created
     */
    public int split(int minSize, int maxDepth, int currentDepth, RandomSource random) {
        if (currentDepth >= maxDepth || width < minSize * 2 || depth < minSize * 2) {
            // Create room in this leaf node
            createRoom(random, minSize);
            return 1;
        }
        
        // Determine split direction based on dimensions
        boolean canSplitHorizontal = width >= minSize * 2;
        boolean canSplitVertical = depth >= minSize * 2;
        
        if (!canSplitHorizontal && !canSplitVertical) {
            createRoom(random, minSize);
            return 1;
        }
        
        // Choose split direction - prefer splitting the larger dimension
        if (canSplitHorizontal && canSplitVertical) {
            splitHorizontal = width > depth ? random.nextBoolean() : random.nextBoolean();
        } else {
            splitHorizontal = canSplitHorizontal;
        }
        
        // Calculate split position (with some randomness)
        int splitPos;
        if (splitHorizontal) {
            int minSplit = x + minSize;
            int maxSplit = x + width - minSize;
            int splitRange = maxSplit - minSplit;
            splitPos = splitRange <= 0 ? minSplit : minSplit + random.nextInt(splitRange + 1);
            
            // Create left and right children
            left = new BSPNode(x, y, z, splitPos - x, height, depth);
            right = new BSPNode(splitPos, y, z, x + width - splitPos, height, depth);
        } else {
            int minSplit = z + minSize;
            int maxSplit = z + depth - minSize;
            int splitRange = maxSplit - minSplit;
            splitPos = splitRange <= 0 ? minSplit : minSplit + random.nextInt(splitRange + 1);
            
            // Create left and right children
            left = new BSPNode(x, y, z, width, height, splitPos - z);
            right = new BSPNode(x, y, splitPos, width, height, z + depth - splitPos);
        }
        
        // Recursively split children
        return left.split(minSize, maxDepth, currentDepth + 1, random) +
               right.split(minSize, maxDepth, currentDepth + 1, random);
    }
    
    /**
     * Create a room in this leaf node with random padding
     */
    private void createRoom(RandomSource random, int minSize) {
        // Keep at least a 4x4 walkable footprint so downstream random placement
        // calls never hit nextInt(0) in tiny leaves.
        int minRoomFootprint = 4;
        int maxPaddingX = Math.max((width - minRoomFootprint) / 2, 0);
        int maxPaddingZ = Math.max((depth - minRoomFootprint) / 2, 0);
        int paddingX = maxPaddingX == 0 ? 0 : random.nextInt(maxPaddingX + 1);
        int paddingZ = maxPaddingZ == 0 ? 0 : random.nextInt(maxPaddingZ + 1);

        int roomX = x + paddingX;
        int roomZ = z + paddingZ;
        int roomWidth = Math.max(minRoomFootprint, width - paddingX * 2);
        int roomDepth = Math.max(minRoomFootprint, depth - paddingZ * 2);

        roomWidth = Math.min(roomWidth, width);
        roomDepth = Math.min(roomDepth, depth);

        // Random room height
        int roomHeight = 3 + random.nextInt(3); // 3-5 blocks high
        
        this.room = new Room(roomX, y, roomZ, roomWidth, roomHeight, roomDepth, 
                Room.RoomType.MAIN_HALL);
    }
    
    /**
     * Get all rooms from leaf nodes
     */
    public void getAllRooms(java.util.List<Room> rooms) {
        if (isLeaf()) {
            if (room != null) {
                rooms.add(room);
            }
        } else {
            if (left != null) left.getAllRooms(rooms);
            if (right != null) right.getAllRooms(rooms);
        }
    }
    
    /**
     * Connect sibling rooms with corridors
     */
    public void createCorridors(java.util.List<Corridor> corridors, RandomSource random) {
        if (isLeaf()) return;
        
        // Recursively process children first
        if (left != null) left.createCorridors(corridors, random);
        if (right != null) right.createCorridors(corridors, random);
        
        // Connect left and right subtrees
        if (left != null && right != null) {
            Room leftRoom = findClosestRoom(left);
            Room rightRoom = findClosestRoom(right);
            
            if (leftRoom != null && rightRoom != null) {
                Corridor corridor = new Corridor(leftRoom, rightRoom, 
                        selectCorridorType(random), 2, 3);
                
                // Generate appropriate path shape
                switch (corridor.getType()) {
                    case STRAIGHT -> corridor.generateStraightPath();
                    case L_SHAPE -> corridor.generateLShapePath();
                    case Z_SHAPE -> corridor.generateZShapePath(random);
                    default -> corridor.generateLShapePath();
                }
                
                corridors.add(corridor);
                
                // Record connection points in rooms
                leftRoom.addConnectionPoint(leftRoom.getCenter());
                rightRoom.addConnectionPoint(rightRoom.getCenter());
            }
        }
    }
    
    /**
     * Find the closest room to this node's boundary (for corridor connection)
     */
    private Room findClosestRoom(BSPNode node) {
        if (node.isLeaf()) {
            return node.getRoom();
        }
        
        // Recursively find rooms in children
        java.util.List<Room> rooms = new java.util.ArrayList<>();
        node.getAllRooms(rooms);
        
        if (rooms.isEmpty()) return null;
        
        // Return a room closest to the split boundary
        return rooms.get(0);
    }
    
    private Corridor.CorridorType selectCorridorType(RandomSource random) {
        return switch (random.nextInt(3)) {
            case 0 -> Corridor.CorridorType.STRAIGHT;
            case 1 -> Corridor.CorridorType.L_SHAPE;
            default -> Corridor.CorridorType.Z_SHAPE;
        };
    }
    
    @Override
    public String toString() {
        return String.format("BSPNode(%d,%d,%d) size(%dx%dx%d) leaf=%s", 
                x, y, z, width, height, depth, isLeaf());
    }
}
