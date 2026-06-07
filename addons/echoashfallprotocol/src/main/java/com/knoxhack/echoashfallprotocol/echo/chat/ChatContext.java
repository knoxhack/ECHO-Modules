package com.knoxhack.echoashfallprotocol.echo.chat;

import java.util.*;

/**
 * Tracks conversation state and context for ECHO-7 chat sessions.
 * Maintains message history, current topic, and relationship with player.
 */
public class ChatContext {
    private final List<ChatMessage> messageHistory = new ArrayList<>();
    private static final int MAX_HISTORY = 50;
    
    private String currentTopic = "";
    private int playerRelationship = 0; // -100 to 100, affects tone
    private long sessionStartTick = 0;
    private long lastInteractionTick = 0;
    private boolean isFirstMessage = true;
    
    // Context flags for gameplay state
    private boolean playerAskedForHelp = false;
    private String lastQuestMentioned = "";
    private String lastItemMentioned = "";
    private int consecutiveGreetings = 0;
    
    public static class ChatMessage {
        public final boolean isPlayer;
        public final String content;
        public final long tick;
        public final MessageType type;
        
        public ChatMessage(boolean isPlayer, String content, long tick, MessageType type) {
            this.isPlayer = isPlayer;
            this.content = content;
            this.tick = tick;
            this.type = type;
        }
    }
    
    public enum MessageType {
        GREETING,
        QUESTION,
        STATEMENT,
        QUEST_UPDATE,
        SURVIVAL_WARNING,
        CHAT_RESPONSE,
        DRONE_COMMAND,
        SYSTEM
    }
    
    public void addMessage(boolean isPlayer, String content, long tick, MessageType type) {
        messageHistory.add(new ChatMessage(isPlayer, content, tick, type));
        if (messageHistory.size() > MAX_HISTORY) {
            messageHistory.remove(0);
        }
        lastInteractionTick = tick;
        
        if (isPlayer) {
            analyzePlayerInput(content);
        }
    }
    
    private void analyzePlayerInput(String input) {
        String lower = input.toLowerCase();
        
        // Detect greetings
        if (lower.matches("^(hi|hello|hey|greetings|yo|sup|echo).*")) {
            consecutiveGreetings++;
        } else {
            consecutiveGreetings = 0;
        }
        
        // Detect help requests
        if (lower.contains("help") || lower.contains("how") || lower.contains("what") || 
            lower.contains("where") || lower.contains("why")) {
            playerAskedForHelp = true;
        }
        
        // Update first message flag
        if (isFirstMessage) {
            isFirstMessage = false;
        }
    }
    
    public List<ChatMessage> getRecentHistory(int count) {
        int start = Math.max(0, messageHistory.size() - count);
        return new ArrayList<>(messageHistory.subList(start, messageHistory.size()));
    }
    
    public void setCurrentTopic(String topic) {
        this.currentTopic = topic;
    }
    
    public String getCurrentTopic() {
        return currentTopic;
    }
    
    public void adjustRelationship(int delta) {
        playerRelationship = Math.max(-100, Math.min(100, playerRelationship + delta));
    }
    
    public int getRelationship() {
        return playerRelationship;
    }
    
    public String getRelationshipTone() {
        if (playerRelationship > 50) return "friendly";
        if (playerRelationship > 20) return "warm";
        if (playerRelationship > -20) return "neutral";
        if (playerRelationship > -50) return "stressed";
        return "urgent";
    }
    
    public void initializeSession(long tick) {
        this.sessionStartTick = tick;
        this.lastInteractionTick = tick;
        this.isFirstMessage = true;
    }
    
    public boolean isSessionExpired(long currentTick, long timeout) {
        return (currentTick - lastInteractionTick) > timeout;
    }
    
    public boolean isFirstMessage() {
        return isFirstMessage;
    }
    
    public boolean didPlayerAskForHelp() {
        return playerAskedForHelp;
    }
    
    public void resetHelpFlag() {
        playerAskedForHelp = false;
    }
    
    public int getConsecutiveGreetings() {
        return consecutiveGreetings;
    }
    
    public void setLastQuestMentioned(String questId) {
        this.lastQuestMentioned = questId;
    }
    
    public String getLastQuestMentioned() {
        return lastQuestMentioned;
    }
    
    public void clearHistory() {
        messageHistory.clear();
        currentTopic = "";
        playerAskedForHelp = false;
        consecutiveGreetings = 0;
    }
}
