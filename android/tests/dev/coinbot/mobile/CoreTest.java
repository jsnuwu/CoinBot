package dev.coinbot.mobile;

import java.util.*;

public final class CoreTest {
    private static int checks=0;
    private static void check(boolean condition,String reason){checks++;if(!condition)throw new AssertionError(reason);}
    public static void main(String[] args){
        Set<String> accounts=Core.accounts("@realDonaldTrump,elonmusk");
        check(accounts.contains("realdonaldtrump"),"normalize account handle");
        Core.Candidate post=Core.candidate("New post https://x.com/elonmusk/status/12345?s=20",accounts);
        check(post!=null&&post.id.equals("12345")&&post.url.equals("https://x.com/elonmusk/status/12345"),"strip tracking URL");
        check(Core.candidate("https://evil.test/elonmusk/status/123",accounts)==null,"reject other host");
        check(Core.candidate("https://x.com.evil.test/elonmusk/status/123",accounts)==null,"reject deceptive host");
        check(Core.candidate("https://x.com/random/status/123",accounts)==null,"reject unselected account");
        check(Core.candidate("https://x.com/elonmusk/status/123 https://x.com/elonmusk/status/124",accounts)==null,"reject ambiguous notifications");
        check(Core.candidate("https://x.com/elonmusk/status/12345678901234567890",accounts)==null,"reject oversized IDs without truncation");
        Core.Candidate generic=Core.candidate("https://x.com/i/web/status/123",accounts);
        check(generic!=null&&generic.handle.isEmpty(),"generic link needs subsequent author verification");
        check(Core.authorMatches("https://x.com/ElonMusk","elonmusk"),"allow verified matching author");
        check(!Core.authorMatches("https://x.com.attacker.test/elonmusk","elonmusk"),"reject spoofed author origin");
        check(!Core.authorMatches("https://x.com/random","elonmusk"),"reject mismatched author");
        long now=System.currentTimeMillis();String fresh=Long.toString((now-1288834974657L)<<22);
        check(Core.fresh(fresh,now),"accept fresh snowflake");
        check(!Core.fresh(fresh,now+301_000),"reject delayed post");
        check(!Core.fresh(fresh,now-10_000),"reject future post");
        check(!Core.fresh("99999999999999999999999",now),"reject overflow");
        Core.Idea rocket=Core.idea("Starship to Mars",fresh);
        check(rocket!=null&&rocket.theme.equals("space")&&rocket.ticker.length()<=10,"match topic and ticker limit");
        check(Core.idea("Coffee on the moon",fresh).theme.equals("coffee"),"first mentioned subject wins tie");
        check(Core.idea("Coffee coffee! Starship rocket to Mars",fresh).theme.equals("space"),"distinct subjects outweigh repetition");
        check(Core.idea("People killed near a rocket",fresh).theme.equals("news"),"neutral news illustration instead of rejecting a news topic");
        check(Core.idea("private key: rocket",fresh)==null,"do not turn access credentials into metadata");
        check(Core.idea("Read https://example.test/rocket",fresh)==null,"URL alone is not topic evidence");
        check(Core.idea("Hello @doge",fresh)==null,"mention alone is not topic evidence");
        check(Core.idea("ok thanks",fresh)==null,"do not fabricate a topic from filler");
        check(Core.idea("Starship to Mars",fresh).ticker.equals(rocket.ticker),"repeat input stable");
        check(Core.metadataLink("tweet_"+fresh).endsWith(fresh),"read explicit tweet notification tag");
        check(Core.metadataLink(fresh).contains("/i/web/status/"),"read structured post ID");
        check(Core.metadataLink("user_"+fresh).isEmpty(),"do not turn user IDs into posts");
        Set<String> mario=Core.accounts("MarioNawfal,elonmusk,realDonaldTrump");
        check("marionawfal".equals(Core.notificationAuthor("Mario Nawfal",mario)),"recognize selected notification display name locally");
        check("marionawfal".equals(Core.notificationAuthor("Mario Nawfal (@MarioNawfal)",mario)),"recognize explicit selected handle");
        check("realdonaldtrump".equals(Core.notificationAuthor("Donald J. Trump",mario)),"known display alias");
        check(Core.notificationAuthor("Someone else",mario)==null,"unknown notification author remains rejected");
        check(Core.notificationAuthor("Mario Nawfal @unselected",mario)==null,"explicit different handle overrides display-name match");
        check(Core.notificationAuthor("@MarioNawfal and @elonmusk",mario)==null,"ambiguous author stays rejected");
        check(Core.notificationAuthor("Elon Musk replied",mario)==null,"do not mistake reply title for an author");
        check("nytimes".equals(Core.notificationAuthor("The New York Times",Core.accounts("nytimes"))),"selected news profile display alias remains local-only");
        check("vitalikbuterin".equals(Core.notificationAuthor("vitalik.eth",Core.accounts("VitalikButerin"))),"selected account display alias");
        Core.NotificationDraft local=Core.notificationDraft("Mario Nawfal","A robot on Mars",mario);
        check(local!=null&&!local.publishable&&local.id.startsWith("notification_"),"notification-only evidence can never authorize publication");
        check(Core.notificationDraft("Mario Nawfal","A robot on Mars…",mario)==null,"truncated notification does not create draft");
        check(Core.notificationDraft("Mario Nawfal","",mario)==null,"empty notification does not create draft");
        check(local.id.equals(Core.notificationDraft("Mario Nawfal","A robot on Mars",mario).id),"repeated notification deduplicates independently of updates");
        check(!local.id.equals(Core.notificationDraft("Elon Musk","A robot on Mars",mario).id),"separate selected authors have separate local drafts");
        check(Core.idea(local.text,local.id)!=null,"valid received text reaches local idea generation without X network requests");
        String permalink=Core.metadataLink(fresh);
        Map<String,Object> linked=Core.draftMetadata(rocket,"meme.png",permalink,true);
        check(!local.text.contains("https://"),"original post text need not contain its own URL");
        check(permalink.equals(linked.get("twitter"))&&permalink.equals(linked.get("source_post")),"post ID supplies the exact original post social link");
        check(Boolean.TRUE.equals(linked.get("source_verified")),"confirmed source retains verification");
        check(Boolean.FALSE.equals(linked.get("publishable")),"Android draft metadata cannot enable publication");
        check("https://x.com/elonmusk/status/12345".equals(Core.postUrl("https://twitter.com/ElonMusk/status/12345?s=20")),"normalize legacy domain and strip tracking");
        for(String invalid:new String[]{null,"", "@elonmusk · ohne Post-Link","https://x.com/elonmusk","http://x.com/elonmusk/status/123","https://x.com.evil.test/elonmusk/status/123","https://evil.test/?url=https://x.com/elonmusk/status/123","https://user@x.com/elonmusk/status/123","https://x.com/elonmusk/status/12345678901234567890"}){
            Map<String,Object> absent=Core.draftMetadata(rocket,"meme.png",invalid,true);
            check(!absent.containsKey("twitter")&&!absent.containsKey("source_post")&&Boolean.FALSE.equals(absent.get("source_verified")),"missing/invalid permalink must not fabricate source evidence: "+invalid);
        }
        Core.Candidate known=Core.candidate(permalink,mario);
        String observed=Core.notificationSource(known,local);
        check(permalink.equals(observed),"retain observed notification ID even if the public embed service fails");
        check(Boolean.FALSE.equals(Core.draftMetadata(rocket,"meme.png",observed,false).get("source_verified")),"observed link never verifies notification-only content");
        check(Core.notificationSource(Core.candidate("https://x.com/elonmusk/status/123",mario),local).isEmpty(),"different named author's link is not attached to local draft");
        check(Core.notificationSource(null,local).isEmpty(),"notification text alone cannot manufacture an original post URL");
        try{
            for(String row:java.nio.file.Files.readAllLines(java.nio.file.Paths.get("topic-cases.tsv"),java.nio.charset.StandardCharsets.UTF_8)){
                if(row.startsWith("# ")||row.trim().isEmpty())continue;
                String[] cells=row.split("\\t");IdeaCheck(cells,fresh);
            }
        }catch(java.io.IOException e){throw new AssertionError(e);}
        try{Core.accounts("elonmusk,https://evil.test");throw new AssertionError("invalid account accepted");}catch(IllegalArgumentException expected){checks++;}
        System.out.println(checks+" Android core checks passed");
    }
    private static void IdeaCheck(String[] cells,String id){
        Core.Idea idea=Core.idea(cells[0],id);
        check(idea!=null,"general post must not require old meme keyword: "+cells[0]);
        check(idea.theme.equals(cells[1]),"category matches post content: "+cells[0]);
        check(idea.name.startsWith(cells[2]+" "),"name is anchored in an actual post term: "+cells[0]);
        check(idea.ticker.length()<=10&&idea.name.length()<=25,"Pump field limits");
    }
}
