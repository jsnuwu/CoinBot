package dev.coinbot.mobile;

import java.net.URI;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.*;

/** Pure local selection rules. No wallet or transaction methods. */
public final class Core {
    private Core() {}
    public static final long MAX_AGE_MS = 300_000;
    private static final Pattern URL = Pattern.compile("https://(?:x\\.com|twitter\\.com)/(?:(?:i/web|i)/status/|([A-Za-z0-9_]{1,15})/status/)([0-9]{1,19})(?![0-9])(?:[/?#][^\\s<>\"']*)?");
    private static final Pattern SENSITIVE = Pattern.compile("\\b(killed|dead|death|shooting|attack|war|suicide|murder|rape|seed phrase|private key|guaranteed profit)\\b", Pattern.CASE_INSENSITIVE);
    private static final String[][] THEMES = {
        {"space", "moon|rocket|rockets|space|mars|starship|spacex|mond|rakete", "Lunar", "MOON"},
        {"coffee", "coffee|covfefe|kaffee", "Coffee Comet", "JAVA"},
        {"dog", "dog|dogs|doge|shiba|hund", "Disco Dog", "DOG"},
        {"cat", "cat|cats|kitten|katze", "Cosmic Cat", "CAT"},
        {"robot", "robot|robots|ai|optimus|roboter", "Robot Raccoon", "ROBO"},
        {"pizza", "pizza", "Pizza Planet", "PIZZA"},
        {"taco", "taco|tacos", "Taco Orbit", "TACO"},
        {"gold", "gold|golden", "Golden Goose", "GOOSE"},
        {"penguin", "penguin|penguins|pinguin", "Pixel Penguin", "PENG"},
        {"tariff", "tariff|tariffs|zoll", "Parcel Party", "BOX"}
    };
    public static final class Candidate {
        public final String handle, id, url;
        Candidate(String handle, String id) { this.handle = handle; this.id = id; this.url = "https://x.com/" + (handle.isEmpty() ? "i/web" : handle) + "/status/" + id; }
    }
    public static final class Idea {
        public final String name, ticker, theme, description;
        Idea(String name, String ticker, String theme) { this.name = name; this.ticker = ticker; this.theme = theme;
            this.description = "Independent community meme inspired by " + theme + ". Unofficial; not affiliated with or endorsed by the post author. No promised value or returns.";
        }
    }
    public static Set<String> accounts(String input) {
        Set<String> result = new LinkedHashSet<>();
        for (String part : input.split(",")) {
            String h = part.trim().replaceFirst("^@", "").toLowerCase(Locale.ROOT);
            if (!h.matches("[a-z0-9_]{1,15}")) throw new IllegalArgumentException("Bitte gültige X-Nutzernamen mit Kommas trennen.");
            result.add(h);
        }
        if (result.isEmpty() || result.size() > 10) throw new IllegalArgumentException("Bitte 1 bis 10 Accounts einstellen.");
        return result;
    }
    public static Candidate candidate(String input, Set<String> allowed) {
        Matcher m = URL.matcher(input);
        Candidate result = null;
        while (m.find()) {
            String handle = m.group(1) == null ? "" : m.group(1).toLowerCase(Locale.ROOT);
            if (!handle.isEmpty() && !allowed.contains(handle)) continue;
            Candidate next = new Candidate(handle, m.group(2));
            if (result != null && !result.id.equals(next.id)) return null;
            result = next;
        }
        return result;
    }
    public static boolean fresh(String id, long now) {
        try {
            long created = (Long.parseLong(id) >>> 22) + 1288834974657L;
            return created <= now + 5_000 && now - created <= MAX_AGE_MS;
        } catch (NumberFormatException e) { return false; }
    }
    public static String metadataLink(String value) {
        if(value==null)return "";
        Matcher m=Pattern.compile("^(?:(?:tweet|status)[_:/-]?)?([0-9]{15,19})$").matcher(value);
        return m.matches()?"https://x.com/i/web/status/"+m.group(1):"";
    }
    public static boolean authorMatches(String authorUrl, String handle) {
        try {
            URI u = new URI(authorUrl);
            return "https".equals(u.getScheme()) && u.getUserInfo() == null && u.getPort() == -1 &&
                ("x.com".equals(u.getHost()) || "twitter.com".equals(u.getHost())) &&
                ("/" + handle).equalsIgnoreCase(u.getPath());
        } catch (Exception e) { return false; }
    }
    public static Idea idea(String text, String id) {
        if (SENSITIVE.matcher(text).find()) return null;
        String clean = text.replaceAll("https?://\\S+|@[A-Za-z0-9_]+", " ");
        String[] best = null;
        int bestCount = 0, bestStart = Integer.MAX_VALUE;
        for (String[] theme : THEMES) {
            Matcher m = Pattern.compile("\\b(" + theme[1] + ")\\b", Pattern.CASE_INSENSITIVE).matcher(clean);
            Set<String> words = new HashSet<>(); int first = Integer.MAX_VALUE;
            while (m.find()) { words.add(m.group().toLowerCase(Locale.ROOT)); first = Math.min(first, m.start()); }
            if (words.size() > bestCount || (words.size() == bestCount && first < bestStart)) {
                best = theme; bestCount = words.size(); bestStart = first;
            }
        }
        if (best == null || bestCount == 0) return null;
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(id.getBytes("UTF-8"));
            String suffix = String.format(Locale.ROOT, "%02X%02X%02X", hash[0] & 255, hash[1] & 255, hash[2] & 255).substring(0, 5);
            return new Idea(best[2] + " " + suffix, best[3] + suffix, best[0]);
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
}
