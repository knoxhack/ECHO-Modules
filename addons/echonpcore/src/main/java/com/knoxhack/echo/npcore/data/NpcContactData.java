package com.knoxhack.echo.npcore.data;

import com.knoxhack.echo.npcore.profile.EchoNpcProfile;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class NpcContactData {
    private static final String ROOT_KEY = "echonpcore_contacts";
    private static final String DISCOVERED_KEY = "discovered";
    private static final String LAST_INTERACTION_KEY = "lastInteraction";
    private static final String DISPLAY_KEY = "displayName";
    private static final String ROLE_KEY = "role";
    private static final String FACTION_KEY = "faction";

    private NpcContactData() {
    }

    public static void discover(ServerPlayer player, EchoNpcProfile profile, long gameTime) {
        if (player == null || profile == null || !profile.integrations().terminalContact()) {
            return;
        }
        CompoundTag tag = contactTag(player, profile.id(), true);
        tag.putBoolean(DISCOVERED_KEY, true);
        tag.putLong(LAST_INTERACTION_KEY, Math.max(0L, gameTime));
        tag.putString(DISPLAY_KEY, profile.displayName());
        tag.putString(ROLE_KEY, profile.role());
        tag.putString(FACTION_KEY, profile.faction().toString());
        persist(player, profile.id(), tag);
    }

    public static boolean discovered(Player player, Identifier profileId) {
        return contactTag(player, profileId, false).getBooleanOr(DISCOVERED_KEY, false);
    }

    public static Contact contact(Player player, EchoNpcProfile profile) {
        CompoundTag tag = contactTag(player, profile.id(), false);
        boolean discovered = tag.getBooleanOr(DISCOVERED_KEY, false);
        return new Contact(profile.id(), profile.displayName(), profile.role(), profile.faction(),
                discovered, tag.getLongOr(LAST_INTERACTION_KEY, 0L));
    }

    public static List<Contact> discoveredContacts(Player player, Iterable<EchoNpcProfile> profiles) {
        List<Contact> contacts = new ArrayList<>();
        for (EchoNpcProfile profile : profiles) {
            Contact contact = contact(player, profile);
            if (contact.discovered()) {
                contacts.add(contact);
            }
        }
        contacts.sort(Comparator.comparing(Contact::displayName));
        return List.copyOf(contacts);
    }

    private static CompoundTag contactTag(Player player, Identifier profileId, boolean create) {
        if (player == null || profileId == null) {
            return new CompoundTag();
        }
        CompoundTag root = rootTag(player, create);
        CompoundTag tag = root.getCompoundOrEmpty(profileId.toString());
        if (create && !root.contains(profileId.toString())) {
            root.put(profileId.toString(), tag);
            player.getPersistentData().put(ROOT_KEY, root);
        }
        return tag;
    }

    private static CompoundTag rootTag(Player player, boolean create) {
        if (player == null) {
            return new CompoundTag();
        }
        CompoundTag persistent = player.getPersistentData();
        CompoundTag root = persistent.getCompoundOrEmpty(ROOT_KEY);
        if (create && !persistent.contains(ROOT_KEY)) {
            persistent.put(ROOT_KEY, root);
        }
        return root;
    }

    private static void persist(Player player, Identifier profileId, CompoundTag tag) {
        CompoundTag root = rootTag(player, true);
        root.put(profileId.toString(), tag);
        player.getPersistentData().put(ROOT_KEY, root);
    }

    public record Contact(
            Identifier profileId,
            String displayName,
            String role,
            Identifier factionId,
            boolean discovered,
            long lastInteractionTick) {
    }
}
