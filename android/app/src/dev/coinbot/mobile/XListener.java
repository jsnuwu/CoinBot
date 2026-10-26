package dev.coinbot.mobile;

import android.app.Notification;
import android.content.*;
import android.os.Bundle;
import android.service.notification.*;
import java.util.*;
import java.util.concurrent.*;

public final class XListener extends NotificationListenerService {
    private static volatile XListener live;
    private final ExecutorService queue = new ThreadPoolExecutor(1,1,0,TimeUnit.SECONDS,new ArrayBlockingQueue<Runnable>(32));
    public static boolean attached(){return live!=null;}
    static boolean hasAccess(Context context){
        String value=android.provider.Settings.Secure.getString(context.getContentResolver(),"enabled_notification_listeners");
        if(value==null)return false;ComponentName own=new ComponentName(context,XListener.class);
        for(String entry:value.split(":"))if(own.equals(ComponentName.unflattenFromString(entry)))return true;
        return false;
    }
    public static void refreshActive(){XListener current=live;if(current!=null)current.scanActive();}
    public void onListenerConnected(){live=this;note(null,"Benachrichtigungsdienst verbunden");scanActive();}
    public void onListenerDisconnected(){if(live==this)live=null;note(null,"Benachrichtigungsdienst getrennt");}
    public void onDestroy(){if(live==this)live=null;queue.shutdownNow();super.onDestroy();}
    private void scanActive(){
        if(!getSharedPreferences("settings",0).getBoolean("enabled",false))return;
        try{StatusBarNotification[] active=getActiveNotifications();if(active!=null)for(StatusBarNotification event:active)onNotificationPosted(event);}
        catch(SecurityException e){note(null,"Benachrichtigungszugriff konnte nicht verwendet werden");}
    }
    private static String field(Bundle extras,String key){Object v=extras.get(key);return v instanceof CharSequence?v.toString():"";}
    public void onNotificationPosted(StatusBarNotification event){
        if(!"com.twitter.android".equals(event.getPackageName()))return;
        SharedPreferences prefs=getSharedPreferences("settings",0);
        if(!prefs.getBoolean("enabled",false))return;
        note("received","X-Meldung empfangen");
        Notification n=event.getNotification();
        if((n.flags&Notification.FLAG_GROUP_SUMMARY)!=0){note("summary","X-Sammelmeldung ignoriert");return;}
        Bundle extras=n.extras;
        if(Notification.CATEGORY_MESSAGE.equals(n.category)||(extras!=null&&extras.containsKey(Notification.EXTRA_MESSAGES))){note("private","X-Meldung als Nachrichtenformat aussortiert; kein Inhalt gespeichert");return;}
        if(extras==null){note("incomplete","X-Meldung ohne lesbare Textfelder");return;}
        long age=System.currentTimeMillis()-event.getPostTime();
        if(age>Core.MAX_AGE_MS||age < -5000){note(null,"X-Benachrichtigung zu alt oder Zeitstempel unplausibel");return;}
        Set<String> allowed;
        try{allowed=Core.accounts(prefs.getString("accounts","realDonaldTrump,elonmusk"));}catch(IllegalArgumentException e){note(null,"Account-Einstellungen ungültig");return;}
        String title=field(extras,Notification.EXTRA_TITLE_BIG);if(title.isEmpty())title=field(extras,Notification.EXTRA_TITLE);
        String body=field(extras,Notification.EXTRA_BIG_TEXT);if(body.isEmpty())body=field(extras,Notification.EXTRA_TEXT);
        Core.NotificationDraft local=Core.notificationDraft(title,body,allowed);
        NotificationLinks links=new NotificationLinks(n,event.getTag());
        Core.Candidate candidate=links.candidate(allowed);
        Diagnostics.links(this,links.summary(candidate!=null));
        if(prefs.getBoolean("browser_mode",false)){
            String author=Core.notificationAuthor(title,allowed);
            NotificationMatch.Request request=NotificationMatch.request(author,body,event.getPostTime(),candidate,System.currentTimeMillis());
            if(request!=null&&BrowserMonitor.requestNotification(request))note(null,"Linksuche für empfangene X-Mitteilung von @"+author+" eingereiht");
            else note(null,"Keine Linksuche: Auslöserdienst fehlt oder Mitteilung nicht zuordenbar");
            return;
        }
        if(candidate==null){
            note("no_link","Keine eindeutige Post-ID oder URL in X-Mitteilung; lokaler Entwurf ohne Quellenlink");
            if(local==null){
                if(Core.notificationAuthor(title,allowed)==null)note("unknown_author","Absender aus X-Mitteilung nicht zur Beobachtungsliste zugeordnet");
                else note("incomplete","X-Mitteilung enthält keinen vollständigen verwertbaren Text");
                return;
            }
        }else if(!Core.fresh(candidate.id,System.currentTimeMillis())){note(null,"Verlinkter Post zu alt oder Zeitstempel unplausibel");return;}
        final Core.Candidate target=candidate;final Core.NotificationDraft fallback=local;
        try{queue.execute(()->process(target,fallback,allowed));}catch(RejectedExecutionException e){note(null,"Warteschlange voll; kein weiterer Auftrag angenommen");}
    }
    private void process(Core.Candidate candidate,Core.NotificationDraft fallback,Set<String> allowed){
        if(!getSharedPreferences("settings",0).getBoolean("enabled",false))return;
        try(LocalStore store=new LocalStore(this)){
            store.prune();
            if(candidate==null){prepareNotification(store,fallback,null);return;}
            if(!store.claim(candidate)){note("duplicate","Doppelte Post-Meldung ignoriert");return;}
            if(!Core.fresh(candidate.id,System.currentTimeMillis())){store.finish(candidate.id,"Post in Warteschlange veraltet",null);note(null,"Post in Warteschlange veraltet");return;}
            if(System.currentTimeMillis()<getSharedPreferences("settings",0).getLong("x_blocked_until",0)){
                store.finish(candidate.id,"X-Quelle in Wartezeit",null);prepareNotification(store,fallback,candidate);return;
            }
            try{
                if(!store.canPrepare())throw new XReader.SourceError("Vorschlagslimit erreicht: 20 pro Tag und 60 Sekunden Mindestabstand");
                String text=XReader.text(candidate,allowed);
                if(!Core.fresh(candidate.id,System.currentTimeMillis()))throw new XReader.SourceError("Post während der Verarbeitung veraltet");
                store.source(candidate.id,"post_link_verified");prepare(store,candidate.id,text,true,candidate.url);
            }catch(XReader.SourceError e){
                store.finish(candidate.id,e.getMessage(),null);note("source_failed",e.getMessage());
                if(e.getMessage().contains("HTTP 429")||e.getMessage().contains("HTTP 403"))getSharedPreferences("settings",0).edit().putLong("x_blocked_until",System.currentTimeMillis()+900_000).apply();
                prepareNotification(store,fallback,candidate);
            }catch(Exception e){store.finish(candidate.id,"Quellenprüfung fehlgeschlagen",null);note("source_failed","Quellenprüfung fehlgeschlagen");prepareNotification(store,fallback,candidate);}
        }catch(Exception e){note(null,"Lokale Verarbeitung fehlgeschlagen; kein Coin erstellt");}
    }
    private void prepareNotification(LocalStore store,Core.NotificationDraft draft,Core.Candidate candidate)throws Exception{
        if(draft==null){note(null,"Kein vollständiger lokaler Mitteilungstext für einen Entwurf vorhanden");return;}
        String url=Core.notificationSource(candidate,draft);
        if(!store.claimNotification(draft,url)){note("duplicate","Doppelte X-Mitteilung ignoriert");return;}
        prepare(store,draft.id,draft.text,false,url);
    }
    private void prepare(LocalStore store,String id,String text,boolean verified,String sourceUrl)throws Exception{
        Core.Idea idea=Core.idea(text,id);
        if(idea==null){store.finish(id,"Kein verwertbarer Themenbegriff oder vertraulicher Inhalt",null);note("no_topic","Kein verwertbarer Themenbegriff oder Text enthält Zugangsdaten");return;}
        if(!DraftWriter.save(this,store,id,idea,sourceUrl,verified,verified?"post_link_verified":"notification_unverified",null)){note(null,"Vorschlagslimit erreicht: 20 pro Tag und 60 Sekunden Mindestabstand");return;}
        String status=verified?"Entwurf bereit · Post-Link bestätigt · Pump.fun fehlt":"Lokaler Entwurf · X-Mitteilung nicht per Link verifiziert · keine Veröffentlichung";
        note(verified?"draft_verified":"draft_notification",status+": "+idea.name);
    }
    private void note(String key,String message){Diagnostics.note(this,key,message);}
}
