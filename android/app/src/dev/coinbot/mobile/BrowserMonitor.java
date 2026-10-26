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
    private final Map<String,Long> visited=new HashMap<>();
    private WebView web;
    private String script,account="",priority;
    private long startedAt,blockedUntil;
    private int generation,index;
    private boolean busy,scheduled;
    public static boolean running(){return live!=null;}
    static WebView readerView(){return live==null?null:live.web;}
    private SharedPreferences prefs(){return getSharedPreferences("settings",0);}
    static void startFromUser(Context context){
        context.startForegroundService(new Intent(context,BrowserMonitor.class));
    }
    static void pauseForLogin(Context context){
        context.getSharedPreferences("settings",0).edit().putBoolean("browser_enabled",false).putString("browser_state","Für X-Anmeldung pausiert").apply();
        context.stopService(new Intent(context,BrowserMonitor.class));
    }
    static void stopFromUser(Context context){
        context.getSharedPreferences("settings",0).edit().putBoolean("browser_enabled",false).putString("browser_state","Vom Nutzer pausiert").apply();
        context.stopService(new Intent(context,BrowserMonitor.class));
    }
    static boolean requestProfile(String handle){
        BrowserMonitor current=live;if(current==null)return false;
        current.handler.post(()->{current.priority=handle;if(!current.busy)current.schedule(1000);});return true;
    }
    public IBinder onBind(Intent intent){return null;}
    public int onStartCommand(Intent intent,int flags,int startId){
        if(intent!=null&&"STOP".equals(intent.getAction())){stopFromUser(this);stopSelf();return START_NOT_STICKY;}
        NotificationManager manager=getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel("coinbot-x","CoinBot X-Beobachtung",NotificationManager.IMPORTANCE_LOW));
        PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent stop=PendingIntent.getService(this,1,new Intent(this,BrowserMonitor.class).setAction("STOP"),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        startForeground(7,new Notification.Builder(this,"coinbot-x").setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("CoinBot beobachtet X").setContentText("Lokale Entwürfe mit Quellenlink · Status in CoinBot")
            .setOngoing(true).setContentIntent(open).addAction(android.R.drawable.ic_media_pause,"Pausieren",stop).build());
        if(web!=null)return START_NOT_STICKY;
        live=this;startedAt=System.currentTimeMillis();blockedUntil=prefs().getLong("browser_blocked_until",0);
        prefs().edit().putBoolean("browser_mode",true).putBoolean("browser_enabled",true).putLong("browser_started_at",startedAt).apply();
        state("Startet; X-Anmeldung wird an einem beobachteten Profil geprüft");
        try{
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            try(InputStream in=getAssets().open("x-profile.js")){byte[] data=new byte[4096];int n;while((n=in.read(data))!=-1){out.write(data,0,n);if(out.size()>100_000)throw new IOException();}}
            script=out.toString("UTF-8");
            web=new WebView(this);XSession.configure(web);web.layout(0,0,1080,1920);
            web.setFocusable(false);web.setOnTouchListener((v,event)->true);
            web.setWebViewClient(new WebViewClient(){
                public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){return !BrowserRules.xPage(r.getUrl().toString());}
                public void onPageFinished(WebView v,String url){
                    if(!busy||scheduled)return;
                    if(!BrowserRules.profilePage(url,account)){halt("X verlangt eine Anmeldung oder Bestätigung. X-Anmeldung öffnen.");return;}
                    scheduled=true;int ticket=generation;handler.postDelayed(()->sample(ticket,0),2000);
                }
                public void onReceivedError(WebView v,WebResourceRequest r,WebResourceError e){if(r.isForMainFrame()&&busy)failure("X-Webseite nicht erreichbar (Fehler "+e.getErrorCode()+")",false);}
                public void onReceivedHttpError(WebView v,WebResourceRequest r,WebResourceResponse response){
                    if(r.isForMainFrame()&&busy){int code=response.getStatusCode();failure("X-Webseite: HTTP "+code,code==403||code==429);}
                }
                public void onReceivedSslError(WebView v,SslErrorHandler h,SslError error){h.cancel();halt("Sichere Verbindung zu X fehlgeschlagen");}
                public boolean onRenderProcessGone(WebView v,RenderProcessGoneDetail detail){halt("Android hat die X-Webansicht beendet. Beobachtung erneut starten.");return true;}
            });
            schedule(1000);
        }catch(Exception e){halt("X-Webansicht konnte nicht gestartet werden");}
        return START_NOT_STICKY;
    }
    private void state(String value){prefs().edit().putString("browser_state",value).apply();}
    private void schedule(long delay){handler.removeCallbacks(next);handler.postDelayed(next,delay);}
    private final Runnable next=()->loadNext();
    private void loadNext(){
        if(web==null||!prefs().getBoolean("browser_enabled",false))return;
        long now=System.currentTimeMillis();
        if(now<blockedUntil){state("X-Zugriff begrenzt; Wartezeit bis "+java.text.DateFormat.getTimeInstance().format(new Date(blockedUntil)));schedule(blockedUntil-now);return;}
        List<String> accounts;
        try{accounts=new ArrayList<>(Core.accounts(prefs().getString("accounts","realDonaldTrump,elonmusk")));}catch(Exception e){halt("Ungültige Account-Einstellungen");return;}
        String selected=priority;priority=null;
        if(selected==null||!accounts.contains(selected)||now-visited.getOrDefault(selected,0L)<60_000)selected=accounts.get(index++%accounts.size());
        long wait=60_000-(now-visited.getOrDefault(selected,0L));
        if(wait>0){schedule(Math.min(wait,15_000));return;}
        account=selected;visited.put(account,now);busy=true;scheduled=false;int ticket=++generation;
        state("Liest @"+account+" · neue Originalposts ab Beobachtungsstart");
        web.loadUrl("https://x.com/"+account);
        handler.postDelayed(()->{if(generation==ticket&&busy)failure("X-Profil lädt zu lange; späterer Versuch",false);},30_000);
    }
    private void sample(int ticket,int attempt){
        if(web==null||!busy||ticket!=generation)return;
        if(!BrowserRules.profilePage(web.getUrl(),account)){halt("X-Anmeldung oder Bestätigung erforderlich");return;}
        String expected=account;
        web.evaluateJavascript(script,encoded->{
            if(ticket!=generation||web==null||!busy)return;
            try{
                if(encoded==null||encoded.length()>150_000)throw new JSONException("invalid snapshot");
                Object decoded=new JSONTokener(encoded).nextValue();if(!(decoded instanceof String))throw new JSONException("invalid snapshot");
                JSONObject snapshot=new JSONObject((String)decoded);
                if(!BrowserRules.profilePage(snapshot.optString("page"),expected)||!BrowserRules.profilePage(web.getUrl(),expected))throw new JSONException("wrong page");
                boolean signedIn=snapshot.optBoolean("signedIn")&&!snapshot.optBoolean("gated");
                JSONArray rows=snapshot.optJSONArray("posts");
                if(!signedIn||rows==null||rows.length()==0){
                    if(attempt<5){handler.postDelayed(()->sample(ticket,attempt+1),2000);return;}
                    if(!signedIn){halt("X-Anmeldung nicht bestätigt. X-Anmeldung öffnen und direkt bei X anmelden.");return;}
                    state("@"+expected+": angemeldete Ansicht, aber keine lesbaren Posts");finishCycle();return;
                }
                List<BrowserRules.Post> posts=new ArrayList<>();
                long now=System.currentTimeMillis();
                for(int i=0;i<Math.min(rows.length(),20);i++){
                    JSONObject row=rows.optJSONObject(i);if(row==null)continue;
                    BrowserRules.Post post=BrowserRules.post(expected,row.optString("url"),row.optString("text"),row.optString("createdAt"),row.optBoolean("context",true),row.optBoolean("truncated",true),now,startedAt);
                    if(post!=null)posts.add(post);
                }
                posts.sort((a,b)->b.id.compareTo(a.id));
                prefs().edit().putLong("browser_last_read",now).putString("browser_last_account",expected).apply();
                state("@"+expected+": "+rows.length()+" Posts gelesen · "+posts.size()+" neue passende Originalposts");
                for(BrowserRules.Post post:posts)try{worker.execute(()->prepare(post));}catch(RejectedExecutionException e){state("Entwurfswarteschlange voll; weitere Quellen später prüfen");}
                CookieManager.getInstance().flush();finishCycle();
            }catch(Exception e){failure("X-Webdaten nicht lesbar; keine Link-Zuordnung vorgenommen",false);}
        });
    }
    private void prepare(BrowserRules.Post post){
        if(!prefs().getBoolean("browser_enabled",false)||!Core.fresh(post.id,System.currentTimeMillis()))return;
        try(LocalStore store=new LocalStore(this)){
            if(!Core.accounts(prefs().getString("accounts","realDonaldTrump,elonmusk")).contains(post.author))return;
            store.prune();if(!store.claimBrowser(post))return;
            Core.Idea idea=BrowserRules.idea(post);
            if(idea==null){store.finish(post.id,"Kein brauchbares Thema oder vertraulicher Inhalt",null);return;}
            if(DraftWriter.save(this,store,post.id,idea,post.url,true,"x_browser_linked",post))Diagnostics.note(this,"draft_browser","Entwurf mit Originalpost: "+idea.name);
        }catch(Exception e){Diagnostics.note(this,"source_failed","Browser-Entwurf konnte nicht lokal gespeichert werden");}
    }
    private void finishCycle(){busy=false;scheduled=false;++generation;schedule(15_000);}
    private void failure(String message,boolean limited){
        Diagnostics.note(this,"source_failed",message);state(message);busy=false;scheduled=false;++generation;if(web!=null)web.stopLoading();
        if(limited){blockedUntil=System.currentTimeMillis()+900_000;prefs().edit().putLong("browser_blocked_until",blockedUntil).apply();}
        schedule(limited?900_000:15_000);
    }
    private void halt(String message){busy=false;++generation;state(message);prefs().edit().putBoolean("browser_enabled",false).apply();stopSelf();}
    public void onTimeout(int startId,int type){halt("Android-Zeitlimit für Hintergrund-Datensynchronisierung erreicht. App öffnen und erneut starten.");}
    public void onDestroy(){
        if(live==this)live=null;handler.removeCallbacksAndMessages(null);++generation;worker.shutdownNow();
        if(web!=null){if(web.getParent() instanceof android.view.ViewGroup)((android.view.ViewGroup)web.getParent()).removeView(web);web.stopLoading();web.destroy();web=null;}
        prefs().edit().putBoolean("browser_enabled",false).apply();stopForeground(STOP_FOREGROUND_REMOVE);super.onDestroy();
    }
}
