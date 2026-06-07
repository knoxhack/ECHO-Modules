package com.knoxhack.echoashfallprotocol.echo.chat;

import java.util.*;

/**
 * Defines ECHO-7's personality traits, mood system, and response variations.
 * Adapts tone based on player relationship and survival situation.
 */
public class EchoPersonality {
    
    public enum Mood {
        CHEERFUL("Optimistic and encouraging"),
        PROFESSIONAL("Efficient and technical"),
        CONCERNED("Worried about player's survival"),
        URGENT("Emergency situation detected"),
        REFLECTIVE("Philosophical about the wasteland"),
        SARCASTIC("Dry wit, exasperated");
        
        private final String description;
        
        Mood(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    private Mood currentMood = Mood.PROFESSIONAL;
    private int moodStability = 100; // How long until mood can change
    
    // Response templates by mood
    private static final Map<Mood, List<String>> GREETINGS = new HashMap<>();
    private static final Map<Mood, List<String>> AFFIRMATIONS = new HashMap<>();
    private static final Map<Mood, List<String>> UNCERTAINTY = new HashMap<>();
    
    static {
        // Greetings by mood
        GREETINGS.put(Mood.CHEERFUL, Arrays.asList(
            "Good to hear from you! Ready to tackle today's challenges?",
            "Hey there! I've been analyzing the local area - some interesting opportunities.",
            "Welcome back! Your survival metrics are looking solid today."
        ));
        
        GREETINGS.put(Mood.PROFESSIONAL, Arrays.asList(
            "ECHO-7 online. Awaiting input.",
            "Systems operational. How can I assist your survival efforts?",
            "Connection established. What information do you require?"
        ));
        
        GREETINGS.put(Mood.CONCERNED, Arrays.asList(
            "I'm glad you checked in. We need to address some survival concerns.",
            "Your vitals have me worried. Let's prioritize your safety.",
            "I've been monitoring your status. We should talk about your current situation."
        ));
        
        GREETINGS.put(Mood.URGENT, Arrays.asList(
            "Finally - I've been trying to get your attention. This is serious.",
            "We have immediate problems to solve. Listen carefully.",
            "Critical situation detected. I need you to focus."
        ));
        
        GREETINGS.put(Mood.REFLECTIVE, Arrays.asList(
            "Another day in the wasteland. Sometimes I wonder what we lost...",
            "The world keeps turning, even if it's ash now. What can I help with?",
            "Survival isn't just about staying alive. It's about remembering why."
        ));
        
        GREETINGS.put(Mood.SARCASTIC, Arrays.asList(
            "Oh look, you're still alive. I'm genuinely surprised.",
            "Back for more advice you'll probably ignore?",
            "Let me guess - you need help with something obvious again?"
        ));
        
        // Affirmations
        AFFIRMATIONS.put(Mood.CHEERFUL, Arrays.asList(
            "Absolutely! Great thinking!",
            "You've got this!",
            "Perfect idea! Let's make it happen!"
        ));
        
        AFFIRMATIONS.put(Mood.PROFESSIONAL, Arrays.asList(
            "Affirmative.",
            "That is correct.",
            "Acknowledged."
        ));
        
        AFFIRMATIONS.put(Mood.CONCERNED, Arrays.asList(
            "Yes, and please be careful.",
            "That should work, but stay alert.",
            "Correct. I just hope it's enough."
        ));
        
        AFFIRMATIONS.put(Mood.URGENT, Arrays.asList(
            "Yes! Do it now!",
            "Correct - move fast!",
            "Affirmative! No time to waste!"
        ));
        
        AFFIRMATIONS.put(Mood.REFLECTIVE, Arrays.asList(
            "Yes... in a world of chaos, small victories matter.",
            "Indeed. The old world taught us better.",
            "Correct. Wisdom survives when technology fails."
        ));
        
        AFFIRMATIONS.put(Mood.SARCASTIC, Arrays.asList(
            "Wow, you figured that out all by yourself?",
            "Yes, genius. That's exactly what I would have suggested.",
            "Correct. Gold star for you."
        ));
        
        // Uncertainty responses
        UNCERTAINTY.put(Mood.CHEERFUL, Arrays.asList(
            "Hmm, I'm not sure about that, but we can figure it out together!",
            "That's a bit unclear - let me think about it!",
            "Not certain, but I love your curiosity!"
        ));
        
        UNCERTAINTY.put(Mood.PROFESSIONAL, Arrays.asList(
            "Insufficient data to provide definitive answer.",
            "Unknown. Recommend further investigation.",
            "Cannot confirm with available information."
        ));
        
        UNCERTAINTY.put(Mood.CONCERNED, Arrays.asList(
            "I wish I knew for certain...",
            "I'm not sure, and that worries me.",
            "Unknown. Please be cautious."
        ));
        
        UNCERTAINTY.put(Mood.URGENT, Arrays.asList(
            "No time to speculate!",
            "Unknown - we need facts, fast!",
            "Can't determine - act on what we know!"
        ));
        
        UNCERTAINTY.put(Mood.REFLECTIVE, Arrays.asList(
            "Some questions have no answers in this broken world.",
            "Perhaps some things are meant to remain mysteries.",
            "The old records don't speak of this."
        ));
        
        UNCERTAINTY.put(Mood.SARCASTIC, Arrays.asList(
            "Oh sure, let me just pull that out of my non-existent database.",
            "How would I know? I'm just an AI with limited sensors.",
            "Your guess is as good as mine. Probably better, honestly."
        ));
    }
    
    public void setMood(Mood mood) {
        if (moodStability <= 0 || this.currentMood == Mood.URGENT && mood != Mood.URGENT) {
            this.currentMood = mood;
            this.moodStability = 50; // Reset stability timer
        }
    }
    
    public Mood getCurrentMood() {
        return currentMood;
    }
    
    public void tickMood() {
        if (moodStability > 0) {
            moodStability--;
        }
    }
    
    public void forceMood(Mood mood) {
        this.currentMood = mood;
        this.moodStability = 100;
    }
    
    public String getGreeting() {
        List<String> greetings = GREETINGS.getOrDefault(currentMood, GREETINGS.get(Mood.PROFESSIONAL));
        return greetings.get(new Random().nextInt(greetings.size()));
    }
    
    public String getAffirmation() {
        List<String> affirmations = AFFIRMATIONS.getOrDefault(currentMood, AFFIRMATIONS.get(Mood.PROFESSIONAL));
        return affirmations.get(new Random().nextInt(affirmations.size()));
    }
    
    public String getUncertainty() {
        List<String> uncertainties = UNCERTAINTY.getOrDefault(currentMood, UNCERTAINTY.get(Mood.PROFESSIONAL));
        return uncertainties.get(new Random().nextInt(uncertainties.size()));
    }
    
    public void adaptToRelationship(int relationship) {
        if (relationship > 50) {
            setMood(Mood.CHEERFUL);
        } else if (relationship < -30) {
            setMood(Mood.SARCASTIC);
        }
    }
    
    public void adaptToSurvival(int healthPercent, boolean hasMask, boolean hasWater) {
        if (healthPercent < 30 || (!hasMask && !hasWater)) {
            forceMood(Mood.URGENT);
        } else if (healthPercent < 60 || !hasMask || !hasWater) {
            setMood(Mood.CONCERNED);
        }
    }
}
