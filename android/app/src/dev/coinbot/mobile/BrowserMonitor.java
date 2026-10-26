package dev.coinbot.mobile;

import android.app.*;
import android.content.*;
import android.net.http.SslError;
import android.os.*;
import android.webkit.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

/** Visible foreground read-only monitor. No X API, credential extraction or publication actions. */
public final class BrowserMonitor extends Service {
    private static BrowserMonitor live;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final ExecutorService worker=new ThreadPoolExecutor(1,1,0,TimeUnit.SECONDS,new ArrayBlockingQueue<Runnable>(16));
    private final NotificationMatch.Inbox inbox=new NotificationMatch.Inbox();
    private NotificationMatch.Request current;
    private WebView web;
    private String script,account="";
    private long startedAt,blockedUntil;
    private int generation;
    private boolean busy,scheduled;
    public static boolean running(){return live!=null;}
    static boolean resolving(){return live!=null&&live.busy;}
    static WebView readerView(){return live==null?null:live.web;}
    private SharedPreferences prefs(){return getSharedPreferences("settings",0);}
    static void startFromUser(Context context){
        context.startForegroundService(new Intent(context,BrowserMonitor.class));
    }
    static void pauseForLogin(Context context){
        context.getSharedPreferences("settings",0).edit().putBoolean("browser_enabled",false).putString("browser_ui_state","paused").putString("x_auth_state","unverified").putString("browser_state","Für X-Anmeldung pausiert").apply();
        context.stopService(new Intent(context,BrowserMonitor.class));
    }
    static void stopFromUser(Context context){
        context.getSharedPreferences("settings",0).edit().putBoolean("browser_enabled",false).putString("browser_ui_state","paused").putString("browser_state","Vom Nutzer pausiert").apply();
        context.stopService(new Intent(context,BrowserMonitor.class));
    }
    static boolean requestNotification(NotificationMatch.Request request){
        BrowserMonitor current=live;if(current==null)return false;
        current.handler.post(()->{
            if(System.currentTimeMillis()<current.blockedUntil){current.state("backoff","X-Zugriff begrenzt; lokaler Mitteilungsentwurf ohne bestätigten Link wird geprüft");current.localFallback(request);return;}
            String outcome=current.inbox.offer(request,System.currentTimeMillis());
            if("added".equals(outcome)){if(!current.busy)current.schedule(0);}
            else Diagnostics.note(current,"duplicate".equals(outcome)?"duplicate":null,"X-Mitteilung nicht eingereiht: "+outcome);
        });return true;
    }
    public IBinder onBind(Intent intent){return null;}
    public int onStartCommand(Intent intent,int flags,int startId){
        if(intent!=null&&"STOP".equals(intent.getAction())){stopFromUser(this);stopSelf();return START_NOT_STICKY;}
        NotificationManager manager=getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel("coinbot-x","CoinBot X-Beobachtung",NotificationManager.IMPORTANCE_LOW));
        PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent stop=PendingIntent.getService(this,1,new Intent(this,BrowserMonitor.class).setAction("STOP"),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        startForeground(7,new Notification.Builder(this,"coinbot-x").setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("CoinBot wartet auf X-Mitteilungen").setContentText("Link-Prüfung nur nach Handy-Benachrichtigung")
            .setOngoing(true).setContentIntent(open).addAction(android.R.drawable.ic_media_pause,"Pausieren",stop).build());
        if(web!=null)return START_NOT_STICKY;
        live=this;startedAt=System.currentTimeMillis();blockedUntil=prefs().getLong("browser_blocked_until",0);
        prefs().edit().putBoolean("browser_mode",true).putBoolean("browser_enabled",true).putBoolean("enabled",true).putLong("browser_started_at",startedAt).apply();
        state("waiting","Wartet auf X-Benachrichtigung · keine regelmäßigen Profilabfragen");
        try{
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            try(InputStream in=getAssets().open("x-profile.js")){byte[] data=new byte[4096];int n;while((n=in.read(data))!=-1){out.write(data,0,n);if(out.size()>100_000)throw new IOException();}}
            script=out.toString("UTF-8");
            web=new WebView(this);XSession.configure(web);web.layout(0,0,1080,1920);
            web.setFocusable(false);web.setOnTouchListener((v,event)->true);
            web.setWebViewClient(new WebViewClient(){
                public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){return !BrowserRules.xPage(r.getUrl().toString());}
                public void onPageFinished(WebView v,String url){
                    if(!busy||scheduled||"about:blank".equals(url)||!url.equals(v.getUrl()))return;
                    if(!expectedPage(url)){loginRequired("X verlangt eine Anmeldung oder Bestätigung. X-Anmeldung öffnen.");return;}
                    scheduled=true;int ticket=generation;handler.postDelayed(()->sample(ticket,0),2000);
                }
                public void onReceivedError(WebView v,WebResourceRequest r,WebResourceError e){if(r.isForMainFrame()&&busy&&expectedPage(r.getUrl().toString()))failure("X-Webseite nicht erreichbar (Fehler "+e.getErrorCode()+")",false);}
                public void onReceivedHttpError(WebView v,WebResourceRequest r,WebResourceResponse response){
                    if(r.isForMainFrame()&&busy&&expectedPage(r.getUrl().toString())){int code=response.getStatusCode();failure("X-Webseite: HTTP "+code,code==403||code==429);}
                }
                public void onReceivedSslError(WebView v,SslErrorHandler h,SslError error){h.cancel();halt("Sichere Verbindung zu X fehlgeschlagen");}
                public boolean onRenderProcessGone(WebView v,RenderProcessGoneDetail detail){halt("Android hat die X-Webansicht beendet. Beobachtung erneut starten.");return true;}
            });
            // Deliberately no initial page load. Only an X notification can enqueue a lookup.
            if(XListener.attached())XListener.refreshActive();
            else android.service.notification.NotificationListenerService.requestRebind(new ComponentName(this,XListener.class));
        }catch(Exception e){halt("X-Webansicht konnte nicht gestartet werden");}
        return START_NOT_STICKY;
    }
    private void state(String value){state("waiting",value);}
    private void state(String kind,String value){prefs().edit().putString("browser_ui_state",kind).putString("browser_state",value).apply();}
    private void schedule(long delay){handler.removeCallbacks(next);handler.postDelayed(next,delay);}
    private final Runnable next=()->loadNext();
    private void loadNext(){
        if(web==null||!prefs().getBoolean("browser_enabled",false))return;
        long now=System.currentTimeMillis();
        if(now<blockedUntil){inbox.clear();return;}
        current=inbox.poll(now);if(current==null)return;
        if(!Core.accounts(prefs().getString("accounts",Core.DEFAULT_ACCOUNTS)).contains(current.author)){finishCycle();return;}
        account=current.author;busy=true;scheduled=false;int ticket=++generation;
        state("looking","Sucht ausschließlich den Post aus der X-Mitteilung von @"+account);
        web.loadUrl("https://x.com/"+account+(current.postId.isEmpty()?"":"/status/"+current.postId));
        handler.postDelayed(()->{if(generation==ticket&&busy)failure("X-Linksuche lädt zu lange; kein Post zugeordnet",false);},30_000);
    }
    private boolean expectedPage(String url){return current!=null&&(current.postId.isEmpty()?BrowserRules.profilePage(url,current.author):BrowserRules.postPage(url,current.author,current.postId));}
    private void sample(int ticket,int attempt){
        if(web==null||!busy||ticket!=generation)return;
        if(!expectedPage(web.getUrl())){loginRequired("X-Anmeldung oder Bestätigung erforderlich");return;}
        String expected=account;
        web.evaluateJavascript(script,encoded->{
            if(ticket!=generation||web==null||!busy)return;
            try{
                if(encoded==null||encoded.length()>150_000)throw new JSONException("invalid snapshot");
                Object decoded=new JSONTokener(encoded).nextValue();if(!(decoded instanceof String))throw new JSONException("invalid snapshot");
                JSONObject snapshot=new JSONObject((String)decoded);
                if(!expectedPage(snapshot.optString("page"))||!expectedPage(web.getUrl()))throw new JSONException("wrong page");
                boolean signedIn=snapshot.optBoolean("signedIn")&&!snapshot.optBoolean("gated");
                JSONArray rows=snapshot.optJSONArray("posts");
                if(!signedIn||rows==null||rows.length()==0){
                    if(attempt<5){handler.postDelayed(()->sample(ticket,attempt+1),2000);return;}
                    if(!signedIn){loginRequired("X-Anmeldung nicht bestätigt. X-Anmeldung öffnen und direkt bei X anmelden.");return;}
                    state("@"+expected+": Post nicht lesbar; lokaler Mitteilungsentwurf wird geprüft");localFallback(current);finishCycle();return;
                }
                List<BrowserRules.Post> posts=new ArrayList<>();
                long now=System.currentTimeMillis();
                for(int i=0;i<Math.min(rows.length(),20);i++){
                    JSONObject row=rows.optJSONObject(i);if(row==null)continue;
                    BrowserRules.Post post=BrowserRules.post(expected,row.optString("url"),row.optString("text"),row.optString("createdAt"),row.optBoolean("context",true),row.optBoolean("truncated",true),now,0);
                    if(post!=null)posts.add(post);
                }
                prefs().edit().putLong("browser_last_read",now).putString("browser_last_account",expected).putString("x_auth_state","observed").apply();
                BrowserRules.Post match=NotificationMatch.select(current,posts,now);
                if(match==null&&attempt<5){handler.postDelayed(()->sample(ticket,attempt+1),2000);return;}
                if(match==null){state("Kein eindeutiger Post-Link; lokaler Mitteilungsentwurf ohne bestätigte Quelle wird geprüft.");localFallback(current);}
                else{
                    state("Post aus X-Mitteilung eindeutig zugeordnet · @"+expected+" · wartet danach auf nächste Mitteilung");
                    try{worker.execute(()->prepare(match));}catch(RejectedExecutionException e){state("Entwurfswarteschlange voll; Mitteilung nicht verarbeitet");}
                }
                CookieManager.getInstance().flush();finishCycle();
            }catch(Exception e){failure("X-Webdaten nicht lesbar; keine Link-Zuordnung vorgenommen",false);}
        });
    }
    private void prepare(BrowserRules.Post post){
        if(!prefs().getBoolean("browser_enabled",false)||!Core.fresh(post.id,System.currentTimeMillis()))return;
        try(LocalStore store=new LocalStore(this)){
            if(!Core.accounts(prefs().getString("accounts",Core.DEFAULT_ACCOUNTS)).contains(post.author))return;
            store.prune();if(!store.claimBrowser(post))return;
            Core.Idea idea=BrowserRules.idea(post);
            if(idea==null){store.finish(post.id,"Kein brauchbares Thema oder vertraulicher Inhalt",null);return;}
            if(DraftWriter.save(this,store,post.id,idea,post.url,true,"x_browser_linked",post,null))Diagnostics.note(this,"draft_browser","Entwurf mit Originalpost: "+idea.name);
            else Diagnostics.note(this,null,"Kein neuer Entwurf: Vorschlagslimit erreicht (20 pro Tag / 60 Sekunden Abstand)");
        }catch(Exception e){Diagnostics.note(this,null,"Lokale Entwurfsspeicherung fehlgeschlagen; Bildstatus im Entwurf prüfen");}
    }
    private void localFallback(NotificationMatch.Request request){
        if(request==null)return;
        try{worker.execute(()->{
            if(!prefs().getBoolean("enabled",false)||!prefs().getBoolean("browser_enabled",false))return;
            Core.NotificationDraft draft=NotificationMatch.localDraft(request,System.currentTimeMillis());
            if(draft==null){Diagnostics.note(this,"incomplete","Kein bestätigter Post-Link und Mitteilungstext fehlt / ist gekürzt oder veraltet; kein Entwurf");return;}
            try(LocalStore store=new LocalStore(this)){
                if(!Core.accounts(prefs().getString("accounts",Core.DEFAULT_ACCOUNTS)).contains(draft.author))return;
                String url=request.postId.isEmpty()?"":Core.postUrl("https://x.com/i/web/status/"+request.postId);
                store.prune();if(!store.claimNotification(draft,url)){Diagnostics.note(this,"duplicate","Mitteilungsentwurf bereits vorhanden; kein Duplikat");return;}
                Core.Idea idea=Core.idea(draft.text,draft.id);
                if(idea==null){store.finish(draft.id,"Kein brauchbares Thema oder vertraulicher Inhalt",null);Diagnostics.note(this,"no_topic","Mitteilung ohne brauchbares Thema oder mit vertraulichem Inhalt; kein Entwurf");return;}
                if(DraftWriter.save(this,store,draft.id,idea,url,false,"notification_unverified",null,draft.text))
                    Diagnostics.note(this,"draft_notification","Lokaler Entwurf mit Bild aus X-Mitteilung: "+idea.name+" · Originalpost nicht bestätigt");
                else Diagnostics.note(this,null,"Kein neuer Entwurf: Vorschlagslimit erreicht (20 pro Tag / 60 Sekunden Abstand)");
            }catch(Exception e){Diagnostics.note(this,null,"Mitteilungsentwurf konnte nicht lokal gespeichert werden; Bildstatus prüfen");}
        });}catch(RejectedExecutionException e){Diagnostics.note(this,null,"Lokale Entwurfswarteschlange voll; kein Entwurf gespeichert");}
    }
    private void blankPage(){if(web!=null){web.stopLoading();web.loadUrl("about:blank");}}
    private void finishCycle(){busy=false;scheduled=false;current=null;++generation;blankPage();schedule(0);}
    private void failure(String message,boolean limited){
        localFallback(current);
        Diagnostics.note(this,"source_failed",message);state(limited?"backoff":"error",message);busy=false;scheduled=false;++generation;blankPage();
        if(limited){blockedUntil=System.currentTimeMillis()+900_000;prefs().edit().putLong("browser_blocked_until",blockedUntil).apply();}
        if(limited)inbox.clear();current=null;schedule(0);
    }
    private void halt(String message){halt("error",message);}
    private void loginRequired(String message){prefs().edit().putString("x_auth_state","required").apply();halt("login",message);}
    private void halt(String kind,String message){busy=false;++generation;state(kind,message);prefs().edit().putBoolean("browser_enabled",false).apply();stopSelf();}
    public void onTimeout(int startId,int type){halt("Android-Zeitlimit für Hintergrund-Datensynchronisierung erreicht. App öffnen und erneut starten.");}
    public void onDestroy(){
        if(live==this)live=null;handler.removeCallbacksAndMessages(null);++generation;worker.shutdownNow();
        if(web!=null){if(web.getParent() instanceof android.view.ViewGroup)((android.view.ViewGroup)web.getParent()).removeView(web);web.stopLoading();web.destroy();web=null;}
        prefs().edit().putBoolean("browser_enabled",false).apply();stopForeground(STOP_FOREGROUND_REMOVE);super.onDestroy();
    }
}
