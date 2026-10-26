package dev.coinbot.mobile;

import java.time.Instant;
import java.util.*;

public final class BrowserRulesTest {
    private static int checks;
    private static void check(boolean value,String reason){checks++;if(!value)throw new AssertionError(reason);}
    public static void main(String[] args){
        long now=System.currentTimeMillis(),created=now-1000,started=now-2000;
        String id=Long.toString((created-1288834974657L)<<22),url="https://x.com/elonmusk/status/"+id,date=Instant.ofEpochMilli(created).toString();
        check(BrowserRules.xPage("https://x.com/i/flow/login"),"user can log in directly on X");
        check(BrowserRules.profilePage("https://x.com/ElonMusk","elonmusk"),"case insensitive exact selected profile");
        for(String bad:new String[]{"https://x.com.evil.test/elonmusk","https://user@x.com/elonmusk","http://x.com/elonmusk","file:///elonmusk","https://evil.test/?x=https://x.com/elonmusk"})check(!BrowserRules.xPage(bad),"reject untrusted navigation origin");
        check(!BrowserRules.profilePage("https://x.com/elonmusk/status/123","elonmusk"),"do not treat detail or unrelated screen as selected profile");
        check(!BrowserRules.profilePage("https://x.com/notifications","elonmusk"),"never collect private notifications screen");
        BrowserRules.Post post=BrowserRules.post("elonmusk",url,"Starship to Mars",date,false,false,now,started);
        check(post!=null&&post.url.equals(url),"actual profile post supplies source permalink without one in the text");
        Core.Idea idea=BrowserRules.idea(post);
        check(idea.name.equals("Starship Sprint")&&!idea.name.matches(".*[0-9A-F]{5}$"),"readable post-related title instead of hexadecimal name");
        check(idea.ticker.length()<=10&&idea.ticker.matches("[A-Z0-9]+"),"bounded deterministic ticker");
        check(idea.description.contains("Starship to Mars")&&idea.description.contains("@elonmusk")&&idea.description.contains("Unofficial"),"description explains actual public source and unofficial status");
        check(BrowserRules.idea(post).ticker.equals(idea.ticker),"same post gives same draft");
        Map<String,Object> metadata=Core.draftMetadata(idea,"meme.png",post.url,true);
        check(url.equals(metadata.get("twitter"))&&url.equals(metadata.get("source_post"))&&Boolean.FALSE.equals(metadata.get("publishable")),"linked browser draft is still not a publication");
        check(BrowserRules.post("elonmusk",url,"Starship",date,true,false,now,started)==null,"reject social context/quotes/reposts/promotions");
        check(BrowserRules.post("elonmusk",url,"Starship",date,false,true,now,started)==null,"reject known truncated text");
        check(BrowserRules.post("elonmusk",url,"Starship…",date,false,false,now,started)==null,"reject ellipsis truncation");
        check(BrowserRules.post("elonmusk",url.replace("elonmusk","random"),"Starship",date,false,false,now,started)==null,"reject different author permalink");
        check(BrowserRules.post("elonmusk",Core.metadataLink(id),"Starship",date,false,false,now,started)==null,"generic ID alone does not establish profile author");
        check(BrowserRules.post("elonmusk",url,"Starship",date,false,false,now,now)==null,"do not replay posts older than monitor start");
        check(BrowserRules.post("elonmusk",url,"Starship",date,false,false,now+301000,started)==null,"do not prepare stale posts");
        check(BrowserRules.post("elonmusk",url,"Starship",Instant.ofEpochMilli(now+10000).toString(),false,false,now,started)==null,"reject future timestamp");
        check(BrowserRules.post("elonmusk",url,"Starship","garbage",false,false,now,started)==null,"require explicit valid post date");
        BrowserRules.Post sensitive=BrowserRules.post("elonmusk",url,"private key: starship",date,false,false,now,started);
        check(sensitive!=null&&BrowserRules.idea(sensitive)==null,"credentials never become metadata despite a source link");
        check(BrowserRules.idea(BrowserRules.post("elonmusk",url,"Coffee and bitcoin",date,false,false,now,started)).theme.equals("coffee"),"post title subject and illustration keep selected main theme");
        System.out.println(checks+" browser source/draft rules passed");
    }
}
