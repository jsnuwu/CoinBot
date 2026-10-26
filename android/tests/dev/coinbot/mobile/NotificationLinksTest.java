package dev.coinbot.mobile;

import android.app.Notification;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import java.util.*;

/** Synthetic exposed metadata, not an emulator or real-device test. */
public final class NotificationLinksTest {
    private static int checks;
    private static final Set<String> ALLOWED=Core.accounts("elonmusk");
    private static void check(boolean ok,String reason){checks++;if(!ok)throw new AssertionError(reason);}
    private static Core.Candidate candidate(Notification n){return new NotificationLinks(n,null).candidate(ALLOWED);}
    public static void main(String[] args){
        String id="2114220571831799953";
        Notification plain=new Notification();plain.extras.put("android.text","Mars launch today");
        plain.contentIntent=new Object(){public String toString(){throw new AssertionError("opaque opening target inspected");}};
        NotificationLinks empty=new NotificationLinks(plain,null);
        check(empty.candidate(ALLOWED)==null,"notification body and opaque open target cannot invent post URL");
        check(empty.summary(false).contains("Öffnungsziel vorhanden, nicht auslesbar"),"explain why normal X notification can open but bot cannot read its target");
        check(!empty.summary(false).contains("Mars"),"diagnostics exclude notification text");

        Notification nested=new Notification();
        nested.extras.put("payload",new Bundle().put("tweet_id",Long.valueOf(id)));
        check(candidate(nested)!=null&&candidate(nested).id.equals(id),"nested numeric tweet ID needs no inline post URL");

        Notification uri=new Notification();uri.extras.put("uri",Uri.parse("twitter://status?status_id="+id));
        check(candidate(uri)!=null&&candidate(uri).url.equals(Core.metadataLink(id)),"read app deep link in exposed URI");
        check(new NotificationLinks(uri,null).summary(true).contains("App-Postlinks: 1"),"diagnostics report app link shape without raw values");

        Notification intent=new Notification();
        intent.extras.put("data",new Object[]{new Intent(Uri.parse("https://x.com/elonmusk/status/"+id))});
        check(candidate(intent)!=null&&candidate(intent).handle.equals("elonmusk"),"read exposed Intent data inside array without executing it");

        Notification actions=new Notification();
        actions.actions=new Notification.Action[]{new Notification.Action(new Bundle().put("post_id",id))};
        check(candidate(actions)!=null,"read explicitly exposed action metadata");

        Notification list=new Notification();
        list.extras.put("items",new ArrayList<Object>(Arrays.asList(new Bundle().put("statusId",id))));
        check(candidate(list)!=null,"read structured ID inside exposed list");
        list.extras.put("other",new Bundle().put("tweet_id","2114220571831799954"));
        check(candidate(list)==null,"different post IDs stay ambiguous");

        Notification unrelated=new Notification();unrelated.extras.put("user_id",id).put("id",id).put("notification_id",id);
        check(candidate(unrelated)==null,"never reinterpret arbitrary account/notification numbers as posts");
        unrelated.extras.put("url","https://x.com/random/status/"+id);
        check(candidate(unrelated)==null,"unwatched named author stays rejected");

        Notification cycle=new Notification();cycle.extras.put("self",cycle.extras).put("tweet_id",id);
        check(candidate(cycle)!=null,"cyclic exposed bundle graph cannot hang traversal");
        Notification huge=new Notification();
        char[] data=new char[16001];Arrays.fill(data,'x');huge.extras.put("text",new String(data)).put("tweet_id",id);
        check(candidate(huge)==null,"partial scan cannot confirm a possibly ambiguous source");
        check(new NotificationLinks(huge,null).summary(false).contains("Leselimit erreicht"),"bounded parsing has an explicit reason");
        System.out.println(checks+" notification metadata checks passed (JVM fixtures, no device)");
    }
}
