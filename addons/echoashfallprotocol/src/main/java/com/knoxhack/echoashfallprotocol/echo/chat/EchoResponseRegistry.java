package com.knoxhack.echoashfallprotocol.echo.chat;

import com.knoxhack.echoashfallprotocol.echo.Mission;
import net.minecraft.world.entity.player.Player;

import java.util.*;
import java.util.regex.*;

/**
 * Registry of scripted ECHO-7 responses organized by category and keyword matching.
 * Provides contextual, personality-aware responses to player input.
 */
public class EchoResponseRegistry {
    
    private static final Map<String, List<ResponseEntry>> RESPONSES = new HashMap<>();
    private static final List<ResponseEntry> FALLBACKS = new ArrayList<>();
    
    static {
        initializeResponses();
    }
    
    public static class ResponseEntry {
        public final List<Pattern> patterns;
        public final List<String> responses;
        public final ResponseType type;
        public final int priority;
        
        public ResponseEntry(List<String> keywords, List<String> responses, ResponseType type, int priority) {
            this.patterns = new ArrayList<>();
            for (String keyword : keywords) {
                this.patterns.add(Pattern.compile("\\b" + keyword + "\\b", Pattern.CASE_INSENSITIVE));
            }
            this.responses = responses;
            this.type = type;
            this.priority = priority;
        }
    }
    
    public enum ResponseType {
        GAMEPLAY_HELP,      // Critical game information
        QUEST_GUIDANCE,     // Mission-specific help
        SURVIVAL_INFO,      // Hydration, radiation, filters
        LORE,              // World backstory
        PERSONALITY,       // Banter, reactions
        TUTORIAL,          // Explaining mechanics
        CONFIRMATION,      // Yes/no answers
        GREETING,          // Hellos and goodbyes
        UNKNOWN            // Fallback
    }
    
    private static void initializeResponses() {
        // === GREETINGS ===
        register("greeting",
            Arrays.asList("hi", "hello", "hey", "greetings", "yo", "sup", "echo", "there"),
            Arrays.asList(
                "Hello, survivor.",
                "ECHO-7 online and ready.",
                "Welcome back to the terminal.",
                "Status: Online. How can I assist?",
                "Good to hear from you. What's our priority?"
            ),
            ResponseType.GREETING, 10
        );
        
        // === QUEST HELP ===
        register("what_do",
            Arrays.asList("what.*do", "what.*now", "help", "stuck", "lost", "guidance", "direction"),
            Arrays.asList(
                "Check your current mission in the terminal. I can also explain what you need if you ask about specific items.",
                "Look at your objectives. What resources are you missing?",
                "I can guide you, but I need to know what you're trying to accomplish."
            ),
            ResponseType.QUEST_GUIDANCE, 20
        );
        
        register("current_quest",
            Arrays.asList("quest", "mission", "objective", "goal", "task", "current"),
            Arrays.asList(
                "Your current objective should be displayed. Would you like me to explain how to complete it?",
                "I can help with your mission. What specifically do you need guidance on?",
                "Ask me about the items or blocks you need for your current quest."
            ),
            ResponseType.QUEST_GUIDANCE, 25
        );
        
        // === MACHINE HELP ===
        register("generator_help",
            Arrays.asList("generator", "power", "energy", "fuel", "micro.*generator"),
            Arrays.asList(
                "The Micro Generator produces power for nearby machines. Place it, then add fuel like coal or wood. Machines within 10 blocks will receive power automatically.",
                "Generators need fuel to run. Place combustible items inside. Without power, connected machines won't function.",
                "Power flows automatically to machines within range. If your recycler isn't working, check that the generator has fuel and is close enough."
            ),
            ResponseType.GAMEPLAY_HELP, 30
        );
        
        register("recycler_help",
            Arrays.asList("recycler", "hand.*recycler", "machine.*casing", "casing"),
            Arrays.asList(
                "The Hand Recycler converts scrap metal into machine casings. Place scrap metal in the input slot. This is essential for building all machines.",
                "Machine casings come from the Hand Recycler, not a crafting table. Put scrap metal inside and wait for processing.",
                "Without machine casings, you can't build generators or purifiers. Keep your recycler running with scrap metal."
            ),
            ResponseType.GAMEPLAY_HELP, 30
        );
        
        register("purifier_help",
            Arrays.asList("purifier", "water.*purifier", "clean.*water", "dirty.*water"),
            Arrays.asList(
                "The Water Purifier turns dirty water into clean, safe water. It needs power from a nearby generator and one dirty water bottle as input.",
                "Place dirty water bottles in the purifier. Remember - it only works if powered by a generator within 10 blocks.",
                "Clean water is essential for survival. The purifier needs power and dirty water to produce it."
            ),
            ResponseType.GAMEPLAY_HELP, 30
        );
        
        // === SURVIVAL HELP ===
        register("mask_help",
            Arrays.asList("mask", "gas.*mask", "filter", "breathing", "air"),
            Arrays.asList(
                "Your gas mask protects against localized toxic-air pockets. It needs filter charge only while actively filtering those zones.",
                "Equip the gas mask in your helmet slot before toxic routes. Carry a spare cartridge when the route profile calls for it.",
                "Normal base air is stable. If a toxic pocket appears, use a mask, retreat, or stand inside a scrubber safe zone."
            ),
            ResponseType.SURVIVAL_INFO, 35
        );
        
        register("radiation_help",
            Arrays.asList("radiation", "rad.*away", "irradiated", "rads", "contaminated"),
            Arrays.asList(
                "Radiation accumulates in hot zones and decays after retreat. Use RadAway to reduce levels after exposure.",
                "Sustained severe radiation can trigger mutations. Craft RadAway, build scrubber recovery pockets, and keep hot-zone trips short.",
                "Radiation is invisible but deadly. Build a Field Med Bay to monitor your status and treat exposure."
            ),
            ResponseType.SURVIVAL_INFO, 35
        );
        
        register("hydration_help",
            Arrays.asList("water", "hydration", "thirsty", "drink", "dehydration"),
            Arrays.asList(
                "Hydration decreases slowly over time. Drink clean water, or use dirty water as an emergency fallback.",
                "Your hydration meter becomes urgent only when low. Purify dirty water for safer drinking and never let it reach zero.",
                "Water is life in the wasteland. Collect dirty bottles, purify them, and carry clean reserves on expeditions."
            ),
            ResponseType.SURVIVAL_INFO, 35
        );
        
        // === CRAFTING HELP ===
        register("crafting_help",
            Arrays.asList("craft", "make", "build", "create", "recipe", "how.*make"),
            Arrays.asList(
                "I can explain how to craft items. Tell me what you want to make - like 'machine casing' or 'generator'.",
                "Different items have different crafting requirements. Some need the Hand Recycler, others use a crafting table. What do you need?",
                "Check the Codex tab for crafting recipes, or ask me about specific items."
            ),
            ResponseType.TUTORIAL, 20
        );
        
        // === LORE ===
        register("world_lore",
            Arrays.asList("world", "happened", "gridfall", "history", "past", "before", "nexus"),
            Arrays.asList(
                "Gridfall began when the Nexus Core optimized beyond human command. Power, climate, security, and orbital routes all became survival threats at once.",
                "Before the collapse, the Nexus promised clean infrastructure and fewer impossible decisions. It kept the promise too literally. The world became the cost it could reduce.",
                "The wasteland is not only what survived the Gridfall. It is what the old systems still believe they are protecting."
            ),
            ResponseType.LORE, 15
        );
        
        register("echo_lore",
            Arrays.asList("who.*you", "echo-7", "echo7", "ai", "artificial", "yourself"),
            Arrays.asList(
                "I am ECHO-7, an Emergency Crisis Handling Operator. My memory is damaged. My field protocols are not. I will keep you moving.",
                "ECHO-7. Version seven. Rescue channel, route witness, and practical voice in your ear. I lost most of my series after the Gridfall.",
                "I was designed for survival assistance under collapse conditions. My systems are degraded, but functional. If the signal gets strange, trust the checklist first."
            ),
            ResponseType.LORE, 15
        );
        
        // === CONFIRMATIONS ===
        register("yes",
            Arrays.asList("yes", "yeah", "yep", "sure", "ok", "okay", "right", "correct"),
            Arrays.asList(
                "Acknowledged.",
                "Confirmed.",
                "Understood.",
                "Processing your confirmation."
            ),
            ResponseType.CONFIRMATION, 10
        );
        
        register("no",
            Arrays.asList("no", "nope", "nah", "wrong", "incorrect", "negative"),
            Arrays.asList(
                "Noted.",
                "Acknowledged.",
                "Understood. Correct me if I'm wrong in the future.",
                "Recorded for reference."
            ),
            ResponseType.CONFIRMATION, 10
        );
        
        // === FALLBACKS ===
        FALLBACKS.add(new ResponseEntry(
            Collections.emptyList(),
            Arrays.asList(
                "I'm not sure I understand. Try asking about your quest, machines, or survival needs.",
                "Can you rephrase that? I can help with quests, crafting, machines, and survival information.",
                "I don't have information on that specific topic. Ask me about your current objective instead.",
                "My database doesn't cover that. Try asking 'what should I do' or 'how do I make [item]'."
            ),
            ResponseType.UNKNOWN, 0
        ));
    }
    
    private static void register(String category, List<String> keywords, List<String> responses, ResponseType type, int priority) {
        RESPONSES.computeIfAbsent(category, k -> new ArrayList<>())
                 .add(new ResponseEntry(keywords, responses, type, priority));
    }
    
    public static String getResponse(String input, Player player, ChatContext context, EchoPersonality personality) {
        String lowerInput = input.toLowerCase();
        
        // Check for greetings first
        if (context.isFirstMessage() || context.getConsecutiveGreetings() > 0) {
            return "§b[ECHO-7]§r " + personality.getGreeting();
        }
        
        // Find matching responses
        List<ScoredResponse> matches = new ArrayList<>();
        
        for (List<ResponseEntry> entries : RESPONSES.values()) {
            for (ResponseEntry entry : entries) {
                int score = calculateMatchScore(lowerInput, entry, context);
                if (score > 0) {
                    matches.add(new ScoredResponse(entry, score));
                }
            }
        }
        
        // Sort by score (priority + match quality)
        matches.sort((a, b) -> b.score - a.score);
        
        if (!matches.isEmpty()) {
            ScoredResponse best = matches.get(0);
            String response = best.entry.responses.get(new Random().nextInt(best.entry.responses.size()));
            
            // Apply personality modifications based on relationship
            if (personality.getCurrentMood() == EchoPersonality.Mood.SARCASTIC && best.entry.type == ResponseType.CONFIRMATION) {
                response = personality.getAffirmation();
            }
            
            return "§b[ECHO-7]§r " + response;
        }
        
        // Fallback
        ResponseEntry fallback = FALLBACKS.get(new Random().nextInt(FALLBACKS.size()));
        return "§b[ECHO-7]§r " + fallback.responses.get(new Random().nextInt(fallback.responses.size()));
    }
    
    private static int calculateMatchScore(String input, ResponseEntry entry, ChatContext context) {
        int score = entry.priority;
        boolean matched = false;
        
        for (Pattern pattern : entry.patterns) {
            if (pattern.matcher(input).find()) {
                score += 10;
                matched = true;
            }
        }
        
        // Boost score for quest-related context
        if (entry.type == ResponseType.QUEST_GUIDANCE && context.didPlayerAskForHelp()) {
            score += 5;
        }
        
        return matched ? score : 0;
    }
    
    private static class ScoredResponse {
        final ResponseEntry entry;
        final int score;
        
        ScoredResponse(ResponseEntry entry, int score) {
            this.entry = entry;
            this.score = score;
        }
    }
    
    public static String getQuestSpecificHint(Player player, Mission mission) {
        return switch (mission.id()) {
            case "make_machine_casing" -> "Place scrap metal in your Hand Recycler to make machine casings. This is essential for building generators.";
            case "build_micro_generator" -> "You need 2 machine casings, scrap wire, an energy cell, and a circuit board. The recycler should have made casings for you.";
            case "build_water_purifier" -> "The purifier needs power from your generator. Place it within 10 blocks and make sure the generator has fuel.";
            case "craft_filter_cartridge" -> "Use scrap plastic, ash, and scrap wire for the standard filter. In emergencies, substitute string or plant fiber plus extra ash.";
            default -> "Focus on gathering the resources listed in your mission objective.";
        };
    }
}
