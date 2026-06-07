package com.knoxhack.signalos.service;

import com.knoxhack.signalos.SignalOS;
import com.knoxhack.signalos.api.SignalOsDataRecord;
import com.knoxhack.signalos.api.SignalOsNetLink;
import com.knoxhack.signalos.api.SignalOsNetPage;
import com.knoxhack.signalos.api.SignalOsNetSite;
import com.knoxhack.signalos.content.SignalOsContentRegistry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public final class SignalOsNetService {
    public static final String TYPE_NET_PAGE = "net_page";
    public static final String META_ADDRESS = "signalos.net.address";
    public static final String META_SITE_ID = "signalos.net.site";
    public static final String META_PATH = "signalos.net.path";
    public static final String META_REQUIRED_TIER = "signalos.net.required_tier";
    public static final String META_TAGS = "signalos.net.tags";
    public static final String META_LINKS = "signalos.net.links";

    private static final int MAX_SEARCH_RESULTS = 32;
    private static final Pattern SLUG_BAD = Pattern.compile("[^a-z0-9_.-]+");

    private SignalOsNetService() {
    }

    public static List<SignalOsNetSite> visibleSites(Player player, int accessTier) {
        int tier = Math.max(0, accessTier);
        return SignalOsContentRegistry.netSites(player).stream()
                .filter(site -> site.requiredTier() <= tier)
                .sorted(Comparator.comparingInt(SignalOsNetSite::order)
                        .thenComparing(SignalOsNetSite::address))
                .toList();
    }

    public static List<SignalOsDataRecord> records(Player player, int accessTier) {
        ArrayList<SignalOsDataRecord> records = new ArrayList<>();
        int siteIndex = 0;
        for (SignalOsNetSite site : visibleSites(player, accessTier)) {
            int pageIndex = 0;
            for (SignalOsNetPage page : site.pages()) {
                String address = site.pageAddress(page);
                SignalOsDataRecord record = new SignalOsDataRecord(
                        Identifier.fromNamespaceAndPath(SignalOS.MODID,
                                "signalnet/" + slug(site.address()) + "/" + slug(page.path())),
                        page.title(),
                        TYPE_NET_PAGE,
                        site.title(),
                        page.body().isBlank() ? site.summary() : page.body(),
                        site.order() * 1000 + siteIndex * 100 + page.order() + pageIndex,
                        false)
                        .withMetadata(META_ADDRESS, address)
                        .withMetadata(META_SITE_ID, site.id().toString())
                        .withMetadata(META_PATH, page.path())
                        .withMetadata(META_REQUIRED_TIER, Integer.toString(site.requiredTier()))
                        .withMetadata(META_TAGS, String.join(",", site.tags()))
                        .withMetadata(META_LINKS, encodedLinks(page.links()));
                records.add(record);
                pageIndex++;
            }
            siteIndex++;
        }
        return List.copyOf(records);
    }

    public static List<SignalOsDataRecord> searchRecords(List<SignalOsDataRecord> records, String query) {
        String safeQuery = normalizeSearch(query);
        return (records == null ? List.<SignalOsDataRecord>of() : records).stream()
                .filter(SignalOsNetService::isNetPage)
                .filter(record -> safeQuery.isBlank() || searchable(record).contains(safeQuery))
                .sorted(Comparator.comparingInt(SignalOsDataRecord::order)
                        .thenComparing(record -> record.metadataValue(META_ADDRESS, record.id().toString())))
                .limit(MAX_SEARCH_RESULTS)
                .toList();
    }

    public static Optional<SignalOsDataRecord> recordForAddress(Player player, int accessTier, String address) {
        String safeAddress = normalizeAddress(address);
        if (safeAddress.isBlank()) {
            return Optional.empty();
        }
        return records(player, accessTier).stream()
                .filter(record -> safeAddress.equals(record.metadataValue(META_ADDRESS, "")))
                .findFirst();
    }

    public static boolean isNetPage(SignalOsDataRecord record) {
        return record != null && TYPE_NET_PAGE.equals(record.type())
                && !record.metadataValue(META_ADDRESS, "").isBlank();
    }

    public static String normalizeAddress(String address) {
        try {
            return SignalOsNetSite.cleanAddress(address);
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    public static String slug(String value) {
        String safe = value == null ? "page" : value.strip().toLowerCase(Locale.ROOT)
                .replace('/', '_')
                .replace('.', '_');
        safe = SLUG_BAD.matcher(safe).replaceAll("_").replaceAll("^_+|_+$", "");
        return safe.isBlank() ? "page" : safe;
    }

    public static List<SignalOsNetLink> decodedLinks(SignalOsDataRecord record) {
        String encoded = record == null ? "" : record.metadataValue(META_LINKS, "");
        if (encoded.isBlank()) {
            return List.of();
        }
        ArrayList<SignalOsNetLink> links = new ArrayList<>();
        for (String entry : encoded.split("\\n")) {
            int split = entry.indexOf('|');
            if (split <= 0 || split >= entry.length() - 1) {
                continue;
            }
            try {
                links.add(new SignalOsNetLink(unescape(entry.substring(0, split)), unescape(entry.substring(split + 1))));
            } catch (RuntimeException ignored) {
            }
        }
        return List.copyOf(links);
    }

    private static String normalizeSearch(String query) {
        return query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
    }

    private static String searchable(SignalOsDataRecord record) {
        return (record.title() + " " + record.source() + " " + record.body() + " "
                + record.metadataValue(META_ADDRESS, "") + " " + record.metadataValue(META_TAGS, ""))
                .toLowerCase(Locale.ROOT);
    }

    private static String encodedLinks(List<SignalOsNetLink> links) {
        if (links == null || links.isEmpty()) {
            return "";
        }
        return links.stream()
                .limit(8)
                .map(link -> escape(link.label()) + "|" + escape(link.address()))
                .collect(java.util.stream.Collectors.joining("\n"));
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("|", "\\p").replace("\n", "\\n");
    }

    private static String unescape(String value) {
        StringBuilder out = new StringBuilder();
        boolean escaped = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (escaped) {
                out.append(c == 'p' ? '|' : c == 'n' ? '\n' : c);
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else {
                out.append(c);
            }
        }
        if (escaped) {
            out.append('\\');
        }
        return out.toString();
    }
}
