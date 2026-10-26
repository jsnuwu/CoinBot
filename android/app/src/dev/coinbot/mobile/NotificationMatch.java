package dev.coinbot.mobile;

import java.text.Normalizer;
import java.time.Instant;
import java.util.*;

/** An X notification is the only work item. No account rotation or periodic discovery. */
public final class NotificationMatch {
    private NotificationMatch(){}
    private static String normalized(String text){return Normalizer.normalize(text,Normalizer.Form.NFKC).replaceAll("https?://\\S+","").replaceAll("[\uFE0E\uFE0F\u200B\u200E\u200F]","").replace('\u2019','\'').replace('\u2018','\'').replaceAll("[\\s\\p{Z}]+"," ").trim();}
    public static final class Request {
        public final String author,text,postId,key;
        private final String originalText;
        public final long receivedAt;
        private final boolean truncated;
        private Request(String author,String text,String postId,long time){
            this.originalText=text;this.author=author;this.text=normalized(text);this.postId=postId;this.receivedAt=time;
            this.truncated=this.text.endsWith("…")||this.text.endsWith("...");
            this.key=author+"\n"+postId+"\n"+this.text;
        }
        public boolean fresh(long now){return receivedAt<=now+5000&&now-receivedAt<=Core.MAX_AGE_MS;}
        private boolean matches(BrowserRules.Post post){
            if(!author.equals(post.author))return false;
            long created=Instant.parse(post.createdAt).toEpochMilli();
            if(created>receivedAt+5000||receivedAt-created>Core.MAX_AGE_MS)return false;
            if(!postId.isEmpty()&&!postId.equals(post.id))return false;
            if(!postId.isEmpty()&&text.isEmpty())return true;
            String body=normalized(post.text);
            if(!truncated)return !text.isEmpty()&&body.equals(text);
            String prefix=text.replaceFirst("(?:…|\\.\\.\\.)$","").trim();
            return prefix.length()>=(!postId.isEmpty()?15:40)&&body.startsWith(prefix);
        }
    }
    public static Request request(String author,String text,long receivedAt,Core.Candidate candidate,long now){
        if(author==null||!author.matches("[a-z0-9_]{1,15}")||text==null||text.length()>4000)return null;
        if(candidate!=null&&(!candidate.handle.isEmpty()&&!candidate.handle.equals(author)||!Core.fresh(candidate.id,now)))return null;
        Request result=new Request(author,text,candidate==null?"":candidate.id,receivedAt);
        if(!result.fresh(now)||result.postId.isEmpty()&&result.text.isEmpty())return null;
        return result;
    }
    /** Only complete notification text can produce an explicitly unverified local fallback. */
    public static Core.NotificationDraft localDraft(Request request,long now){
        if(request==null||!request.fresh(now))return null;
        return Core.notificationDraft("@"+request.author,request.originalText,Collections.singleton(request.author));
    }
    public static BrowserRules.Post select(Request request,List<BrowserRules.Post> posts,long now){
        if(request==null||!request.fresh(now))return null;
        BrowserRules.Post result=null;
        for(BrowserRules.Post post:posts)if(request.matches(post)){
            if(result!=null&&!result.id.equals(post.id))return null;
            result=post;
        }
        return result;
    }
    public static String summary(Request request,List<BrowserRules.Post> posts,long now){
        Set<String> ids=new HashSet<>();if(request!=null&&request.fresh(now))for(BrowserRules.Post post:posts)if(request.matches(post))ids.add(post.id);int count=ids.size();
        return "Gültige Posts: "+posts.size()+"; passende Texte/IDs: "+count+"; "+
            (request==null||!request.fresh(now)?"Auftrag veraltet":count==0?"kein passender Text/Zeitraum":count>1?"mehrdeutig, kein Link bestätigt":"eindeutig passend");
    }
    public static final class Inbox {
        private final Deque<Request> requests=new ArrayDeque<>();
        private final Map<String,Long> seen=new LinkedHashMap<>();
        public String offer(Request request,long now){
            seen.entrySet().removeIf(entry->now-entry.getValue()>Core.MAX_AGE_MS);
            requests.removeIf(item->!item.fresh(now));
            if(request==null||!request.fresh(now))return "expired";
            if(seen.containsKey(request.key))return "duplicate";
            if(requests.size()>=16||seen.size()>=128)return "full";
            requests.add(request);seen.put(request.key,now);return "added";
        }
        public Request poll(long now){while(!requests.isEmpty()){Request next=requests.remove();if(next.fresh(now))return next;}return null;}
        public void clear(){requests.clear();}
    }
}
