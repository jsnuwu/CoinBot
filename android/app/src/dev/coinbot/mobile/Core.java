package dev.coinbot.mobile;

import java.net.URI;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.*;

/** Pure local selection rules. No wallet or transaction methods. */
public final class Core {
    private Core() {}
    public static final String DEFAULT_ACCOUNTS="realDonaldTrump,elonmusk,cz_binance,FoxNews,VitalikButerin,nytimes,TheRoaringKitty,MarioNawfal";
    public static final long MAX_AGE_MS = 300_000;
    private static final Pattern URL = Pattern.compile("https://(?:x\\.com|twitter\\.com)/(?:(?:i/web|i)/status/|([A-Za-z0-9_]{1,15})/status/)([0-9]{1,19})(?![0-9])(?:[/?#][^\\s<>\"']*)?");
    private static final Pattern PRIVATE_CONTENT = Pattern.compile("\\b(seed phrase|recovery phrase|private key)\\b|\\b(password|api[_ -]?key)\\s*[:=]", Pattern.CASE_INSENSITIVE);
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
    private static final Set<String> STOP = new HashSet<>(Arrays.asList((
        "the a an and or but to of in on at for from with without by as is are was were be been being " +
        "this that these those it its they them their we us our you your he she his her i my me " +
        "new latest breaking just news update says said say will would could should may might can " +
        "has have had do does did not no yes ok okay hey now today tonight tomorrow yesterday very more most " +
        "some any all much many other another about over under into out than then here there " +
        "announces announced announce opens opened launches launched reveals revealed after before " +
        "please follow subscribe retweet share like thanks thank hello good morning evening see read " +
        "official endorsed guarantee guaranteed profit investment free claim wallet seed password " +
        "trump donald president elon musk mario nawfal " +
        "der die das ein eine einer eines einem einen und oder aber mit ohne von vom zum zur am im " +
        "ist sind war waren wird werden wurde wurden hat haben hatte den dem des dieser diese dieses " +
        "heute jetzt gestern morgen neu neue neuen neues sagt sagte meldet berichtet bekannt gibt " +
        "bitte danke hallo mehr auch noch nur nicht kein keine fuer fur uber ueber"
    ).split(" +")));
    private static final String[][] GENRES = {
        {"sports","football|soccer|basketball|tennis|olympics|champion|tournament|sport|fussball","Kick"},
        {"transport","train|railway|rail|railroad|metro|airport|airline|flight|transport|bahn|zug","Rail"},
        {"finance","bitcoin|ethereum|crypto|bank|banking|stock|stocks|inflation|market|markets|economy|gdp|finance|wirtschaft","Pulse"},
        {"science","quantum|fusion|physics|research|scientist|science|energy|forschung","Lab"},
        {"weather","storm|hurricane|flood|weather|rain|snow|climate|wetter|sturm","Cloud"},
        {"health","hospital|vaccine|medicine|medical|health|healthcare|clinic|gesundheit","Care"},
        {"politics","senate|election|elections|government|parliament|congress|politics|minister|wahl|wahlen|regierung|parlament|abstimmung|midterms|verteidigungsminister","Brief"},
        {"technology","chip|chips|software|smartphone|iphone|android|computer|technology|technologie|internet","Byte"},
        {"news",".","Signal"}
    };
    private static final Set<String> ENTITIES=new HashSet<>(Arrays.asList(("iran israel usa china japan india indien russia russland ukraine germany deutschland seoul berlin france frankreich brazil brasilien canada kanada london paris tokyo bitcoin ethereum solana nvidia samsung tesla apple google openai nato nasa").split(" +")));
    private static String ascii(String text){return java.text.Normalizer.normalize(text,java.text.Normalizer.Form.NFD).replaceAll("\\p{M}","").replace("ß","ss");}
    private static boolean seriousNews(String text){return Pattern.compile("\\b(killed|dead|death|shooting|attack|war|suicide|murder|rape|krieg|get[oö]tet|angriff|angreifen|vergeltung)\\b",Pattern.CASE_INSENSITIVE).matcher(text).find();}
    public static final class Candidate {
        public final String handle, id, url;
        Candidate(String handle, String id) { this.handle = handle; this.id = id; this.url = "https://x.com/" + (handle.isEmpty() ? "i/web" : handle) + "/status/" + id; }
    }
    public static final class Idea {
        public final String name, ticker, theme, description;
        Idea(String name, String ticker, String theme) { this(name,ticker,theme,"Independent community meme inspired by " + theme + ". Unofficial; not affiliated with or endorsed by the post author. No promised value or returns."); }
        Idea(String name,String ticker,String theme,String description){this.name=name;this.ticker=ticker;this.theme=theme;this.description=description;}
    }
    public static final class NotificationDraft {
        public final String id, author, text;
        public final boolean publishable = false;
        NotificationDraft(String id, String author, String text){this.id=id;this.author=author;this.text=text;}
    }
    /** Display-name/notification matching is NOT identity verification. Local drafts only. */
    public static String notificationAuthor(String title, Set<String> allowed){
        Matcher explicit=Pattern.compile("@([A-Za-z0-9_]{1,15})(?![A-Za-z0-9_])").matcher(title);
        Set<String> handles=new HashSet<>();
        while(explicit.find())handles.add(explicit.group(1).toLowerCase(Locale.ROOT));
        if(!handles.isEmpty()){
            if(handles.size()!=1)return null;
            String h=handles.iterator().next();return allowed.contains(h)?h:null;
        }
        String normalized=title.toLowerCase(Locale.ROOT)
            .replaceFirst("\\s+(posted|tweeted|hat gepostet|hat getwittert)\\s*$", "")
            .replaceAll("[^a-z0-9_]", "");
        for(String h:allowed){
            if(normalized.equals(h.replace("_",""))||normalized.equals(h))return h;
            if(h.equals("realdonaldtrump")&&(normalized.equals("donaldjtrump")||normalized.equals("donaldtrump")))return h;
            if(h.equals("nytimes")&&normalized.equals("thenewyorktimes"))return h;
            if(h.equals("vitalikbuterin")&&normalized.equals("vitaliketh"))return h;
            if(h.equals("theroaringkitty")&&normalized.equals("roaringkitty"))return h;
            if(h.equals("cz_binance")&&(normalized.equals("cz")||normalized.equals("czbnb")))return h;
        }
        return null;
    }
    public static boolean completeNotificationText(String text){
        String value=text.replaceAll("https?://\\S+", "").trim();
        return !value.isEmpty()&&text.length()<=4000&&!value.endsWith("…")&&!value.endsWith("...");
    }
    public static NotificationDraft notificationDraft(String title,String text,Set<String> allowed){
        String author=notificationAuthor(title,allowed);
        if(author==null||!completeNotificationText(text))return null;
        try{
            String normalized=text.trim().replaceAll("\\s+"," ");
            byte[] hash=MessageDigest.getInstance("SHA-256").digest((author+"\n"+normalized).getBytes("UTF-8"));
            StringBuilder id=new StringBuilder("notification_");
            for(int i=0;i<12;i++)id.append(String.format(Locale.ROOT,"%02x",hash[i]&255));
            return new NotificationDraft(id.toString(),author,text.trim());
        }catch(Exception e){throw new IllegalStateException(e);}
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
    /** X app deep links are metadata, not URLs which need to be present in a post's text. */
    public static String appPostLink(String value){
        if(value==null)return "";
        try{
            URI uri=new URI(value);
            if(!"twitter".equals(uri.getScheme())||!"status".equals(uri.getHost())||uri.getUserInfo()!=null||uri.getPort()!=-1)return "";
            if(uri.getPath()!=null&&!uri.getPath().isEmpty()&&!"/".equals(uri.getPath()))return "";
            String query=uri.getRawQuery();if(query==null)return "";
            String result="";
            for(String part:query.split("&")){
                String[] pair=part.split("=",2);
                if(pair.length!=2||!(pair[0].equals("status_id")||pair[0].equals("tweet_id")||pair[0].equals("id")))continue;
                String next=metadataLink(pair[1]);
                if(next.isEmpty()||!result.isEmpty()&&!result.equals(next))return "";
                result=next;
            }
            return result;
        }catch(Exception e){return "";}
    }
    public static boolean postIdKey(String key){
        return key!=null&&key.replace("_","").toLowerCase(Locale.ROOT).matches("tweetid|statusid|postid");
    }
    /** Exact post permalink, never a profile or a guessed text-search result. */
    public static String postUrl(String value){
        if(value==null)return "";
        try{
            URI u=new URI(value);
            if(!"https".equals(u.getScheme())||u.getUserInfo()!=null||u.getPort()!=-1||
               !("x.com".equals(u.getHost())||"twitter.com".equals(u.getHost())))return "";
            Matcher m=Pattern.compile("^/(?:(?:i/web|i)/status/|([A-Za-z0-9_]{1,15})/status/)([0-9]{1,19})/?$").matcher(u.getPath());
            return m.matches()?new Candidate(m.group(1)==null?"":m.group(1).toLowerCase(Locale.ROOT),m.group(2)).url:"";
        }catch(Exception e){return "";}
    }
    public static String notificationSource(Candidate candidate,NotificationDraft draft){
        if(candidate==null||draft==null)return "";
        // A link for a different named author is not evidence for this notification draft.
        return candidate.handle.isEmpty()||candidate.handle.equals(draft.author)?candidate.url:"";
    }
    public static Map<String,Object> draftMetadata(Idea idea,String image,String sourceUrl,boolean verified){
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("name",idea.name);result.put("symbol",idea.ticker);result.put("description",idea.description);
        result.put("image",image);result.put("showName",true);
        String url=postUrl(sourceUrl);
        if(!url.isEmpty()){result.put("twitter",url);result.put("source_post",url);}
        result.put("source_verified",verified&&!url.isEmpty());
        result.put("publishable",false); // This Android version only prepares local drafts.
        return result;
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
        if (PRIVATE_CONTENT.matcher(text).find()) return null;
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
        if (best == null || bestCount == 0 || seriousNews(clean)) return generalIdea(clean,id);
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(id.getBytes("UTF-8"));
            String suffix = String.format(Locale.ROOT, "%02X%02X%02X", hash[0] & 255, hash[1] & 255, hash[2] & 255).substring(0, 5);
            return new Idea(best[2] + " " + suffix, best[3] + suffix, best[0]);
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
    private static Idea generalIdea(String text,String id){
        String normalized=ascii(text);
        Matcher words=Pattern.compile("#?[A-Za-z][A-Za-z0-9]{1,23}").matcher(normalized);
        String subject=null;int best=-1;
        while(words.find()){
            String raw=words.group(),word=raw.replace("#","");String lower=word.toLowerCase(Locale.ROOT);
            if(STOP.contains(lower)||word.length()<3&&!word.matches("[A-Z]{2,6}"))continue;
            int score=(raw.startsWith("#")?40:0)+(word.matches("[A-Z]{2,6}")?15:0)+(word.matches("[A-Z][a-z]+")?20:0)+Math.min(word.length(),10);
            if(ENTITIES.contains(lower))score=75;
            if(raw.startsWith("#"))score+=100;
            // Stable first-mention tie breaking. No claim of semantic/viral prediction.
            if(score>best){best=score;subject=word;}
        }
        if(subject==null)return null;
        String[] genre=GENRES[GENRES.length-1];
        for(String[] g:GENRES){if(seriousNews(text)&&!g[0].equals("politics"))continue;if(Pattern.compile("\\b("+g[1]+")\\b",Pattern.CASE_INSENSITIVE).matcher(normalized).find()){genre=g;break;}}
        String suffix;
        try{byte[] h=MessageDigest.getInstance("SHA-256").digest(id.getBytes("UTF-8"));suffix=String.format(Locale.ROOT,"%02X%02X%02X",h[0]&255,h[1]&255,h[2]&255).substring(0,5);}catch(Exception e){throw new IllegalStateException(e);}
        int limit=25-genre[2].length()-7;
        String stem=subject.substring(0,Math.min(subject.length(),limit));
        if(!stem.matches("[A-Z]{2,6}"))stem=stem.substring(0,1).toUpperCase(Locale.ROOT)+stem.substring(1).toLowerCase(Locale.ROOT);
        String ticker=subject.substring(0,Math.min(subject.length(),5)).toUpperCase(Locale.ROOT)+suffix;
        return new Idea(stem+" "+genre[2]+" "+suffix,ticker,genre[0]);
    }

}
