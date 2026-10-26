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
        check(Core.idea("People killed near a rocket",fresh)==null,"reject sensitive news");
        check(Core.idea("Read https://example.test/rocket",fresh)==null,"URL alone is not topic evidence");
        check(Core.idea("Hello @doge",fresh)==null,"mention alone is not topic evidence");
        check(Core.idea("Meeting at noon",fresh)==null,"do not fabricate a meme topic");
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
        Core.NotificationDraft local=Core.notificationDraft("Mario Nawfal","A robot on Mars",mario);
        check(local!=null&&!local.publishable&&local.id.startsWith("notification_"),"notification-only evidence can never authorize publication");
        check(Core.notificationDraft("Mario Nawfal","A robot on Mars…",mario)==null,"truncated notification does not create draft");
        check(Core.notificationDraft("Mario Nawfal","",mario)==null,"empty notification does not create draft");
        check(local.id.equals(Core.notificationDraft("Mario Nawfal","A robot on Mars",mario).id),"repeated notification deduplicates independently of updates");
        check(!local.id.equals(Core.notificationDraft("Elon Musk","A robot on Mars",mario).id),"separate selected authors have separate local drafts");
        check(Core.idea(local.text,local.id)!=null,"valid received text reaches local idea generation without X network requests");
        try{Core.accounts("elonmusk,https://evil.test");throw new AssertionError("invalid account accepted");}catch(IllegalArgumentException expected){checks++;}
        System.out.println(checks+" Android core checks passed");
    }
}
