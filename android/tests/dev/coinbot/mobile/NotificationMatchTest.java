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
        System.out.println(checks+" notification-trigger/matching checks passed");
    }
}
