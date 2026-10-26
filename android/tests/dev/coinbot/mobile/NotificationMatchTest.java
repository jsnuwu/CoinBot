package dev.coinbot.mobile;

import java.time.Instant;
import java.util.*;

public final class NotificationMatchTest {
    private static int checks;
    private static final long NOW=System.currentTimeMillis();
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static BrowserRules.Post post(String author,String text,long age){
        long time=NOW-age;String id=Long.toString((time-1288834974657L)<<22);
        return BrowserRules.post(author,"https://x.com/"+author+"/status/"+id,text,Instant.ofEpochMilli(time).toString(),false,false,NOW,0);
    }
    private static NotificationMatch.Request request(String text){return NotificationMatch.request("elonmusk",text,NOW,null,NOW);}
    public static void main(String[] args){
        NotificationMatch.Inbox idle=new NotificationMatch.Inbox();
        check(idle.poll(NOW)==null&&idle.poll(NOW+60000)==null,"starting and waiting does not discover work without notifications");
        BrowserRules.Post target=post("elonmusk","Starship to Mars",10000),newer=post("elonmusk","Coffee today",1000);
        check(NotificationMatch.select(request("Starship to Mars"),Arrays.asList(newer,target),NOW)==target,"choose notified text even when another post is newer");
        check(NotificationMatch.select(request("Unknown tweet"),Arrays.asList(newer,target),NOW)==null,"never substitute newest unrelated tweet");
        check(NotificationMatch.select(request("Starship to Mars"),Arrays.asList(target,post("elonmusk","Starship to Mars",20000)),NOW)==null,"duplicate text in two fresh posts stays ambiguous");
        check(NotificationMatch.select(request("Starship\n to\u00a0Mars https://t.co/abc"),Arrays.asList(target),NOW)==target,"normalize whitespace and link decoration only");
        check(NotificationMatch.select(request("Starship to Mars"),Arrays.asList(post("marionawfal","Starship to Mars",10000)),NOW)==null,"another account's identical text is not this source");
        check(NotificationMatch.select(request("Starship to Mars"),Arrays.asList(target),NOW+301000)==null,"expired notification does not trigger late matching");
        check(NotificationMatch.request("elonmusk","post",NOW-301000,null,NOW)==null,"reject stale notification");
        check(NotificationMatch.request("elonmusk","post",NOW+10000,null,NOW)==null,"reject future notification");
        check(NotificationMatch.request(null,"post",NOW,null,NOW)==null,"unknown author cannot become work");
        check(NotificationMatch.request("elonmusk","",NOW,null,NOW)==null,"neither text nor ID cannot identify post");
        String longText="Starship prepares another test flight with the crew before dawn";
        BrowserRules.Post longPost=post("elonmusk",longText,15000);
        check(NotificationMatch.select(request(longText.substring(0,48)+"…"),Arrays.asList(longPost),NOW)==longPost,"long truncated notification may match a unique prefix");
        check(NotificationMatch.select(request("Starship…"),Arrays.asList(target),NOW)==null,"short vague prefix cannot identify a post");
        Core.Candidate id=Core.candidate(target.url,Collections.singleton("elonmusk"));
        NotificationMatch.Request exact=NotificationMatch.request("elonmusk","",NOW,id,NOW);
        check(NotificationMatch.select(exact,Arrays.asList(newer,target),NOW)==target,"explicit exposed post ID goes straight to that post");
        check(NotificationMatch.select(NotificationMatch.request("elonmusk","Different post text",NOW,id,NOW),Arrays.asList(target),NOW)==null,"inline reference must not override conflicting notification content");
        check(NotificationMatch.request("marionawfal","Starship to Mars",NOW,id,NOW)==null,"mismatching named metadata author stays rejected");
        check(BrowserRules.postPage(target.url,"elonmusk",target.id),"direct post navigation is accepted for the requested author/ID");
        check(!BrowserRules.postPage(newer.url,"elonmusk",target.id),"another post detail is not an authorized match");
        check(idle.offer(request("Starship to Mars"),NOW).equals("added"),"notification creates one queued task");
        check(idle.offer(request("Starship to Mars"),NOW).equals("duplicate"),"same active notification cannot create repeated lookups");
        check(idle.poll(NOW)!=null&&idle.poll(NOW)==null,"completed work does not manufacture another profile visit");
        check(idle.offer(request("Starship to Mars"),NOW).equals("duplicate"),"seen notification remains deduplicated after processing");
        check(idle.offer(request("Other text"),NOW).equals("added")&&idle.poll(NOW+301000)==null,"queued stale event is discarded");
        NotificationMatch.Inbox bounded=new NotificationMatch.Inbox();
        for(int i=0;i<16;i++)bounded.offer(request("Post "+i),NOW);
        check(bounded.offer(request("Overflow"),NOW).equals("full"),"notification flood has a bounded queue");
        bounded.clear();check(bounded.poll(NOW)==null,"rate limit clears pending work without polling afterward");
        Core.NotificationDraft fallback=NotificationMatch.localDraft(request("Starship to Mars https://t.co/abc"),NOW);
        check(fallback!=null&&!fallback.publishable,"unmatched complete notification produces local-only draft");
        check(fallback.text.equals("Starship to Mars https://t.co/abc"),"fallback retains original notification text");
        check(NotificationMatch.localDraft(request("Starship…"),NOW)==null,"truncated notification cannot become invented complete draft");
        check(NotificationMatch.localDraft(request(longText.substring(0,48)+"…"),NOW)==null,"even long prefixes remain incomplete without verified post");
        check(NotificationMatch.localDraft(exact,NOW)==null,"ID without text cannot produce an invented theme");
        check(NotificationMatch.localDraft(request("Starship to Mars"),NOW+301000)==null,"stale fallback rejected");
        check(NotificationMatch.localDraft(null,NOW)==null,"no notification means no draft");
        check(Core.draftMetadata(Core.idea(fallback.text,fallback.id),fallback.id+".png","",false).get("source_verified").equals(false),"fallback does not claim source confirmation");
        check(!Core.draftMetadata(Core.idea(fallback.text,fallback.id),fallback.id+".png","",false).containsKey("source_post"),"fallback does not invent a post URL");
        check(NotificationMatch.localDraft(request("Starship to Mars"),NOW).id.equals(NotificationMatch.localDraft(request("Starship to Mars"),NOW).id),"repeated notification has stable deduplication key");
        check(NotificationMatch.select(request("Rocket 🚀"),Arrays.asList(post("elonmusk","Rocket 🚀\uFE0F",10000)),NOW)!=null,"emoji presentation selectors do not lose an otherwise exact match");
        check(NotificationMatch.select(request("It’s Starship"),Arrays.asList(post("elonmusk","It's Starship",10000)),NOW)!=null,"typographic apostrophe normalized");
        check(NotificationMatch.select(request("Rocket 🚀"),Arrays.asList(post("elonmusk","Rocket 🐕",10000)),NOW)==null,"different emoji still means different content");
        check(NotificationMatch.select(request("Rocket to Mars"),Arrays.asList(post("elonmusk","Rocket from Mars",10000)),NOW)==null,"word changes remain rejected");
        check(NotificationMatch.summary(request("Starship to Mars"),Arrays.asList(target,post("elonmusk","Starship to Mars",20000)),NOW).contains("passende Texte/IDs: 2"),"diagnostic reports candidate count without raw post data");
        System.out.println(checks+" notification-trigger/matching checks passed");
    }
}
