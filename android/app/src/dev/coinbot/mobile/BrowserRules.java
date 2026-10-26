package dev.coinbot.mobile;

import java.net.URI;
import java.time.Instant;
import java.util.*;

/** Rules for posts actually read from an allowlisted X profile page. No network or credentials. */
public final class BrowserRules {
    private BrowserRules(){}
    public static boolean xPage(String value){
        try{URI u=new URI(value);return "https".equals(u.getScheme())&&u.getUserInfo()==null&&u.getPort()==-1&&
            Arrays.asList("x.com","twitter.com","mobile.twitter.com").contains(u.getHost());}catch(Exception e){return false;}
    }
    public static boolean profilePage(String value,String handle){
        try{return xPage(value)&&("/"+handle).equalsIgnoreCase(new URI(value).getPath());}catch(Exception e){return false;}
    }
    public static boolean postPage(String value,String handle,String id){
        String canonical=Core.postUrl(value);
        Core.Candidate candidate=Core.candidate(canonical,Collections.singleton(handle));
        return candidate!=null&&candidate.id.equals(id);
    }
    public static final class Post {
        public final String id,author,text,url,createdAt;
        private Post(Core.Candidate c,String author,String text,String createdAt){this.id=c.id;this.author=author;this.text=text;this.url=c.url;this.createdAt=createdAt;}
    }
    public static Post post(String expected,String url,String text,String createdAt,boolean context,boolean truncated,long now,long startedAt){
        if(expected==null||!expected.matches("[a-z0-9_]{1,15}")||context||truncated||text==null||text.length()>4000||!Core.completeNotificationText(text))return null;
        String canonical=Core.postUrl(url);if(canonical.isEmpty())return null;
        Core.Candidate c=Core.candidate(canonical,Collections.singleton(expected));
        if(c==null||!c.handle.equals(expected)||!Core.fresh(c.id,now))return null;
        try{
            long created=Instant.parse(createdAt).toEpochMilli();
            long snowflake=(Long.parseLong(c.id)>>>22)+1288834974657L;
            if(created<startedAt||created>now+5000||now-created>Core.MAX_AGE_MS||Math.abs(created-snowflake)>60_000)return null;
        }catch(Exception e){return null;}
        return new Post(c,expected,text.trim(),createdAt);
    }
    public static Core.Idea idea(Post post){
        Core.Idea base=Core.idea(post.text,post.id);if(base==null)return null;
        String name=base.name.replaceFirst(" [0-9A-F]{5}$","");
        // Keep recognizable post subjects rather than a random mascot or hexadecimal name.
        String[][] subjects={{"starship","Starship Sprint","space"},{"spacex","Rocket Relay","space"},{"mars","Mars Echo","space"},
            {"optimus","Optimus Echo","robot"},{"bitcoin","Bitcoin Pulse","finance"},{"ethereum","Ether Echo","finance"},
            {"solana","Solana Signal","finance"},{"coffee","Coffee Comet","coffee"},{"covfefe","Covfefe Club","coffee"}};
        if(!post.text.matches("(?is).*\\b(war|killed|attack|krieg|angriff)\\b.*"))for(String[] subject:subjects){
            if(base.theme.equals(subject[2])&&java.util.regex.Pattern.compile("\\b"+subject[0]+"\\b",java.util.regex.Pattern.CASE_INSENSITIVE).matcher(post.text).find()){name=subject[1];break;}
        }
        String stem=name.split(" ")[0].replaceAll("[^A-Za-z0-9]","").toUpperCase(Locale.ROOT);
        if(stem.length()>7)stem=stem.substring(0,7);
        String suffix=base.ticker.substring(base.ticker.length()-5,base.ticker.length()-2);
        String excerpt=post.text.replaceAll("https?://\\S+"," ").replaceAll("\\s+"," ").trim();
        if(excerpt.length()>200)excerpt=excerpt.substring(0,197)+"…";
        String description="Community concept inspired by @"+post.author+"'s post: “"+excerpt+"” " +
            "Unofficial; not affiliated with or endorsed by the author. No promised value or returns.";
        return new Core.Idea(name,stem+suffix,base.theme,description);
    }
}
