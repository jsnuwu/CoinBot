package dev.coinbot.mobile;

import android.app.Notification;
import android.content.*;
import android.os.Bundle;
import android.service.notification.*;
import java.io.File;
import java.util.*;
import java.util.concurrent.*;

public final class XListener extends NotificationListenerService {
    private final ExecutorService queue = new ThreadPoolExecutor(1,1,0,TimeUnit.SECONDS,new ArrayBlockingQueue<Runnable>(32));
    public void onListenerConnected(){getSharedPreferences("settings",0).edit().putBoolean("listener_connected",true).apply();}
    public void onListenerDisconnected(){getSharedPreferences("settings",0).edit().putBoolean("listener_connected",false).apply();}
    public void onDestroy(){queue.shutdownNow();super.onDestroy();}

    public void onNotificationPosted(StatusBarNotification event){
        if(!"com.twitter.android".equals(event.getPackageName()))return;
        SharedPreferences prefs=getSharedPreferences("settings",0);
        if(!prefs.getBoolean("enabled",false))return;
        Notification n=event.getNotification();
        if((n.flags&Notification.FLAG_GROUP_SUMMARY)!=0 || Notification.CATEGORY_MESSAGE.equals(n.category))return;
        Bundle extras=n.extras;if(extras==null || extras.containsKey(Notification.EXTRA_MESSAGES))return;
        Set<String> allowed;
        try{allowed=Core.accounts(prefs.getString("accounts","realDonaldTrump,elonmusk"));}catch(IllegalArgumentException e){record("Account-Einstellungen ungültig");return;}
        StringBuilder visible=new StringBuilder();
        visible.append(Core.metadataLink(event.getTag())).append('\n');
        visible.append(Core.metadataLink(n.getShortcutId())).append('\n');
        for(String key:extras.keySet()){
            Object value;
            try{value=extras.get(key);}catch(RuntimeException e){continue;}
            if(value instanceof CharSequence)visible.append(value).append('\n');
            else if(value instanceof CharSequence[])for(CharSequence line:(CharSequence[])value)visible.append(line).append('\n');
            else if(value instanceof android.net.Uri)visible.append(value.toString()).append('\n');
            String normalizedKey=key.replace("_","").toLowerCase(Locale.ROOT);
            if(normalizedKey.equals("tweetid")||normalizedKey.equals("statusid"))visible.append(Core.metadataLink(String.valueOf(value))).append('\n');
            if(visible.length()>16000)break;
        }
        Core.Candidate candidate=Core.candidate(visible.toString(),allowed);
        if(candidate==null){record("X-Meldung erhalten, aber kein eindeutiger Post-Link eines beobachteten Accounts vorhanden");return;}
        if(!Core.fresh(candidate.id,System.currentTimeMillis())){record("Post zu alt oder Zeitstempel unplausibel; kein Entwurf");return;}
        if(System.currentTimeMillis()<prefs.getLong("x_blocked_until",0)){record("X-Zugriff in Wartezeit nach Quellenfehler");return;}
        try{queue.execute(()->process(candidate,allowed));}catch(RejectedExecutionException e){record("Warteschlange voll; kein weiterer Auftrag angenommen");}
    }
    private void process(Core.Candidate candidate,Set<String> allowed){
        if(!getSharedPreferences("settings",0).getBoolean("enabled",false))return;
        try(LocalStore store=new LocalStore(this)){
            store.prune();
            if(!store.claim(candidate))return;
            store.recordEvent(this,"X-Post-Link erhalten; Quelle wird geprüft");
            try{
                if(!Core.fresh(candidate.id,System.currentTimeMillis()))throw new XReader.SourceError("Post in Warteschlange veraltet");
                if(!store.canPrepare())throw new XReader.SourceError("Vorschlagslimit erreicht: 20 pro Tag und 60 Sekunden Mindestabstand");
                String text=XReader.text(candidate,allowed);
                if(!Core.fresh(candidate.id,System.currentTimeMillis()))throw new XReader.SourceError("Post während der Verarbeitung veraltet");
                Core.Idea idea=Core.idea(text,candidate.id);
                if(idea==null){store.finish(candidate.id,"Kein passendes Meme-Thema",null);return;}
                File folder=new File(getFilesDir(),"drafts");if(!folder.exists()&&!folder.mkdirs())throw new java.io.IOException();
                Art.save(idea,new File(folder,candidate.id+".png"));
                store.finish(candidate.id,"Entwurf bereit · Pump.fun-Konto noch nicht verbunden",idea);
                store.recordEvent(this,"Automatisch vorbereitet: "+idea.name+" / $"+idea.ticker);
            }catch(XReader.SourceError e){
                store.finish(candidate.id,e.getMessage(),null);store.recordEvent(this,e.getMessage());
                if(e.getMessage().contains("HTTP 429")||e.getMessage().contains("HTTP 403"))getSharedPreferences("settings",0).edit().putLong("x_blocked_until",System.currentTimeMillis()+900_000).apply();
            }catch(Exception e){store.finish(candidate.id,"Quelle/Verarbeitung fehlgeschlagen; kein Coin erstellt",null);store.recordEvent(this,"Verarbeitung fehlgeschlagen; kein Coin erstellt");}
        }
    }
    private void record(String message){try(LocalStore store=new LocalStore(this)){store.recordEvent(this,message);}}
}
