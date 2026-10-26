package dev.coinbot.mobile;

import android.app.Notification;
import android.content.*;
import android.os.Bundle;
import android.service.notification.*;
import java.io.File;
import java.util.*;
import java.util.concurrent.*;

public final class XListener extends NotificationListenerService {
    private static volatile XListener live;
    private final ExecutorService queue = new ThreadPoolExecutor(1,1,0,TimeUnit.SECONDS,new ArrayBlockingQueue<Runnable>(32));
    public static boolean attached(){return live!=null;}
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
        StringBuilder visible=new StringBuilder();
        visible.append(Core.metadataLink(event.getTag())).append('\n');visible.append(Core.metadataLink(n.getShortcutId())).append('\n');
        for(String key:extras.keySet()){
            Object value;try{value=extras.get(key);}catch(RuntimeException e){continue;}
            if(value instanceof CharSequence)visible.append(value).append('\n');
            else if(value instanceof CharSequence[])for(CharSequence line:(CharSequence[])value)visible.append(line).append('\n');
            else if(value instanceof android.net.Uri)visible.append(value.toString()).append('\n');
            String normalizedKey=key.replace("_","").toLowerCase(Locale.ROOT);
            if(normalizedKey.equals("tweetid")||normalizedKey.equals("statusid"))visible.append(Core.metadataLink(String.valueOf(value))).append('\n');
            if(visible.length()>16000)break;
        }
        Core.Candidate candidate=Core.candidate(visible.toString(),allowed);
        if(candidate==null){
            note("no_link","Kein passender Post-Link; lokale Mitteilung wird geprüft");
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
            if(candidate==null){prepareNotification(store,fallback);return;}
            if(!store.claim(candidate)){note("duplicate","Doppelte Post-Meldung ignoriert");return;}
            if(!Core.fresh(candidate.id,System.currentTimeMillis())){store.finish(candidate.id,"Post in Warteschlange veraltet",null);note(null,"Post in Warteschlange veraltet");return;}
            if(System.currentTimeMillis()<getSharedPreferences("settings",0).getLong("x_blocked_until",0)){
                store.finish(candidate.id,"X-Quelle in Wartezeit",null);prepareNotification(store,fallback);return;
            }
            try{
                if(!store.canPrepare())throw new XReader.SourceError("Vorschlagslimit erreicht: 20 pro Tag und 60 Sekunden Mindestabstand");
                String text=XReader.text(candidate,allowed);
                if(!Core.fresh(candidate.id,System.currentTimeMillis()))throw new XReader.SourceError("Post während der Verarbeitung veraltet");
                store.source(candidate.id,"post_link_verified");prepare(store,candidate.id,text,true);
            }catch(XReader.SourceError e){
                store.finish(candidate.id,e.getMessage(),null);note("source_failed",e.getMessage());
                if(e.getMessage().contains("HTTP 429")||e.getMessage().contains("HTTP 403"))getSharedPreferences("settings",0).edit().putLong("x_blocked_until",System.currentTimeMillis()+900_000).apply();
                prepareNotification(store,fallback);
            }catch(Exception e){store.finish(candidate.id,"Quellenprüfung fehlgeschlagen",null);note("source_failed","Quellenprüfung fehlgeschlagen");prepareNotification(store,fallback);}
        }catch(Exception e){note(null,"Lokale Verarbeitung fehlgeschlagen; kein Coin erstellt");}
    }
    private void prepareNotification(LocalStore store,Core.NotificationDraft draft)throws Exception{
        if(draft==null){note(null,"Kein vollständiger lokaler Mitteilungstext für einen Entwurf vorhanden");return;}
        if(!store.claimNotification(draft)){note("duplicate","Doppelte X-Mitteilung ignoriert");return;}
        prepare(store,draft.id,draft.text,false);
    }
    private void prepare(LocalStore store,String id,String text,boolean verified)throws Exception{
        if(!store.canPrepare()){store.finish(id,"Vorschlagslimit erreicht",null);note(null,"Vorschlagslimit erreicht: 20 pro Tag und 60 Sekunden Mindestabstand");return;}
        Core.Idea idea=Core.idea(text,id);
        if(idea==null){store.finish(id,"Kein verwertbarer Themenbegriff oder vertraulicher Inhalt",null);note("no_topic","Kein verwertbarer Themenbegriff oder Text enthält Zugangsdaten");return;}
        File folder=new File(getFilesDir(),"drafts");if(!folder.exists()&&!folder.mkdirs())throw new java.io.IOException();
        Art.save(idea,new File(folder,id+".png"));
        String status=verified?"Entwurf bereit · Post-Link bestätigt · Pump.fun fehlt":"Lokaler Entwurf · X-Mitteilung nicht per Link verifiziert · keine Veröffentlichung";
        store.finish(id,status,idea);note(verified?"draft_verified":"draft_notification",status+": "+idea.name);
    }
    private void note(String key,String message){Diagnostics.note(this,key,message);}
}
