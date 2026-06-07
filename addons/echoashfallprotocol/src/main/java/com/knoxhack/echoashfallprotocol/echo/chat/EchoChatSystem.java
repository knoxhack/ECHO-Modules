package com.knoxhack.echoashfallprotocol.echo.chat;

import net.minecraft.world.entity.player.Player;

import java.util.*;

/**
 * Main handler for ECHO-7's conversational AI system.
 * Manages chat sessions, processes player input, and routes responses.
 */
public class EchoChatSystem {
    
    private static final long SESSION_TIMEOUT = 12000L; // 10 minutes (20 ticks/second)
    private static final Map<UUID, ChatSession> activeSessions = new HashMap<>();
    
    public static class ChatSession {
        public final UUID playerId;
        public final ChatContext context;
        public final EchoPersonality personality;
        public long lastActivity;
        
        public ChatSession(UUID playerId, long startTick) {
            this.playerId = playerId;
            this.context = new ChatContext();
            this.personality = new EchoPersonality();
            this.lastActivity = startTick;
            this.context.initializeSession(startTick);
        }
    }
    
    /**
     * Process player input and return ECHO's response.
     * Creates or resumes chat session automatically.
     */
    public static String processInput(Player player, String input) {
        UUID playerId = player.getUUID();
        long currentTick = player.level().getGameTime();
        
        // Get or create session
        ChatSession session = getOrCreateSession(player, currentTick);
        session.lastActivity = currentTick;
        
        // Update personality based on context
        updatePersonality(session, player);
        
        // Add player message to history
        ChatContext.MessageType type = classifyInput(input);
        session.context.addMessage(true, input, currentTick, type);
        
        // Generate response
        String response = EchoResponseRegistry.getResponse(input, player, session.context, session.personality);
        
        // Add ECHO response to history
        session.context.addMessage(false, response, currentTick, ChatContext.MessageType.CHAT_RESPONSE);
        
        // Update relationship based on interaction
        updateRelationship(session, input, response);
        
        return response;
    }
    
    /**
     * Get a proactive message from ECHO based on game context.
     */
    public static String getProactiveMessage(Player player, ProactiveContext context) {
        ChatSession session = getOrCreateSession(player, player.level().getGameTime());
        
        return switch (context) {
            case QUEST_COMPLETED -> session.personality.getAffirmation() + " Mission objective achieved.";
            case QUEST_STARTED -> "New objective received. I'm here to help if you need guidance.";
            case LOW_HEALTH -> session.personality.getCurrentMood() == EchoPersonality.Mood.URGENT 
                ? "CRITICAL: Your health is dangerously low! Seek shelter immediately!"
                : "Your health is concerning. I recommend finding safety and healing.";
            case LOW_HYDRATION -> "Your hydration is critically low. Find water or you'll take damage.";
            case HIGH_RADIATION -> "Radiation levels detected! Use Rad-Away or find a cleanser!";
            case FILTER_DEGRADED -> "Your toxic-route filter is nearly depleted. Retreat or replace it soon!";
            case SURVIVAL_TIP -> getRandomSurvivalTip();
            case GREETING_RETURN -> session.context.isFirstMessage() 
                ? session.personality.getGreeting()
                : "Welcome back to the terminal.";
        };
    }
    
    /**
     * Send a system message directly (for quest updates, warnings, etc.)
     */
    public static String sendSystemMessage(Player player, String message, ChatContext.MessageType type) {
        ChatSession session = getOrCreateSession(player, player.level().getGameTime());
        String formatted = "§b[ECHO-7]§r " + message;
        session.context.addMessage(false, formatted, player.level().getGameTime(), type);
        return formatted;
    }
    
    private static ChatSession getOrCreateSession(Player player, long currentTick) {
        UUID playerId = player.getUUID();
        ChatSession session = activeSessions.get(playerId);
        
        if (session == null || session.context.isSessionExpired(currentTick, SESSION_TIMEOUT)) {
            session = new ChatSession(playerId, currentTick);
            activeSessions.put(playerId, session);
        }
        
        return session;
    }
    
    private static void updatePersonality(ChatSession session, Player player) {
        // Check survival stats if available
        int healthPercent = (int)((player.getHealth() / player.getMaxHealth()) * 100);
        
        // Default values if survival data not accessible
        boolean hasMask = true;
        boolean hasWater = true;
        
        session.personality.adaptToSurvival(healthPercent, hasMask, hasWater);
        session.personality.adaptToRelationship(session.context.getRelationship());
        session.personality.tickMood();
    }
    
    private static ChatContext.MessageType classifyInput(String input) {
        String lower = input.toLowerCase();
        
        if (lower.matches("^(hi|hello|hey|greetings|yo|sup).*")) {
            return ChatContext.MessageType.GREETING;
        }
        if (lower.contains("?") || lower.contains("how") || lower.contains("what") || 
            lower.contains("where") || lower.contains("why")) {
            return ChatContext.MessageType.QUESTION;
        }
        if (lower.contains("drone") || lower.contains("scout") || lower.contains("follow")) {
            return ChatContext.MessageType.DRONE_COMMAND;
        }
        
        return ChatContext.MessageType.STATEMENT;
    }
    
    private static void updateRelationship(ChatSession session, String input, String response) {
        // Positive interactions
        if (input.toLowerCase().contains("thanks") || input.toLowerCase().contains("thank you")) {
            session.context.adjustRelationship(5);
            session.personality.setMood(EchoPersonality.Mood.CHEERFUL);
        }
        
        // Negative interactions
        if (input.toLowerCase().contains("stupid") || input.toLowerCase().contains("useless") || 
            input.toLowerCase().contains("shut up")) {
            session.context.adjustRelationship(-10);
        }
        
        // Help requests show engagement
        if (session.context.didPlayerAskForHelp()) {
            session.context.adjustRelationship(2);
        }
    }
    
    private static String getRandomSurvivalTip() {
        List<String> tips = Arrays.asList(
            "Remember: Clean water can be crafted by purifying dirty water.",
            "Tip: Carry a spare filter cartridge for toxic routes.",
            "Scrap metal can be recycled into machine casings at the Hand Recycler.",
            "Place generators near machines - power flows automatically within 10 blocks.",
            "Rad-Away requires ash, scrap plastic, and clean water to craft.",
            "The wasteland has safe zones near scrubbers - radiation is lower there."
        );
        return tips.get(new Random().nextInt(tips.size()));
    }
    
    public static void clearSession(UUID playerId) {
        activeSessions.remove(playerId);
    }
    
    public static ChatSession getSession(UUID playerId) {
        return activeSessions.get(playerId);
    }
    
    public enum ProactiveContext {
        QUEST_COMPLETED,
        QUEST_STARTED,
        LOW_HEALTH,
        LOW_HYDRATION,
        HIGH_RADIATION,
        FILTER_DEGRADED,
        SURVIVAL_TIP,
        GREETING_RETURN
    }
}
