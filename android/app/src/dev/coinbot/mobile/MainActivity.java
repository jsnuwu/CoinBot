package dev.coinbot.mobile;

import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.os.*;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;
import java.io.File;
import java.util.*;

public final class MainActivity extends Activity {
    private LinearLayout shell,nav;
    private FrameLayout viewport;
    private ScrollView scroll;
    private TextView primary;
    private int tab,filter;
    private String accountDraft,lastKey="",readyVersion="";
    private List<Map<String,String>> drafts=new ArrayList<>();
    private final android.util.LruCache<String,Bitmap> thumbnails=new android.util.LruCache<>(16);
    private DraftAdapter adapter;
    private final Handler clock=new Handler(Looper.getMainLooper());
    private final Runnable tick=new Runnable(){public void run(){refresh();clock.postDelayed(this,2000);}};
    private SharedPreferences prefs(){return getSharedPreferences("settings",0);}
    private boolean notificationsAllowed(){return Build.VERSION.SDK_INT<33||checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED;}
    private UiState.Status status(){String kind=prefs().getString("browser_ui_state","");if("required".equals(prefs().getString("x_auth_state",""))||!prefs().getBoolean("x_login_attempted",false)&&prefs().getLong("browser_started_at",0)==0)kind="login";if("backoff".equals(kind)&&System.currentTimeMillis()>=prefs().getLong("browser_blocked_until",0))kind="waiting";return UiState.status(XListener.hasAccess(this),notificationsAllowed(),BrowserMonitor.running(),XListener.attached(),prefs().getBoolean("enabled",false),BrowserMonitor.resolving(),kind);}

    public void onCreate(Bundle saved){
        super.onCreate(saved);if(saved!=null){tab=saved.getInt("tab",0);filter=saved.getInt("filter",0);accountDraft=saved.getString("accountDraft");}
        shell=Ui.column(this);
        LinearLayout brand=Ui.row(this);brand.setPadding(d(20),d(14),d(20),d(12));
        brand.addView(Ui.icon(this,"brand",Ui.MINT,38));
        LinearLayout title=Ui.column(this);title.setPadding(d(12),0,0,0);title.addView(Ui.text(this,"CoinBot",23,Ui.TEXT,true));title.addView(Ui.text(this,"Aus Posts werden Ideen",12,Ui.MUTED,false));
        brand.addView(title,new LinearLayout.LayoutParams(0,-2,1));brand.addView(Ui.badge(this,"LOKAL",Ui.MUTED));shell.addView(brand);
        viewport=new FrameLayout(this);shell.addView(viewport,new LinearLayout.LayoutParams(-1,0,1));
        nav=Ui.row(this);nav.setPadding(d(8),d(6),d(8),d(4));nav.setBackground(Ui.shape(this,Ui.PANEL,Ui.BORDER,0));shell.addView(nav);
        Ui.setupWindow(this,shell);readDrafts();showTab(tab,false);
    }
    protected void onResume(){super.onResume();DraftAssets.repairAsync(this);if(XListener.hasAccess(this)&&prefs().getBoolean("enabled",false)&&!XListener.attached())connectReceiver();lastKey="";if(tab==2)showTab(2,false);clock.post(tick);}
    protected void onPause(){clock.removeCallbacks(tick);super.onPause();}
    protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);if(intent.getBooleanExtra("show_home",false))showTab(0,true);}
    protected void onSaveInstanceState(Bundle out){out.putInt("tab",tab);out.putInt("filter",filter);out.putString("accountDraft",accountDraft);super.onSaveInstanceState(out);}
    public void onBackPressed(){if(tab!=0)showTab(0,true);else super.onBackPressed();}
    private int d(int value){return Ui.dp(this,value);}
    private void readDrafts(){try(LocalStore store=new LocalStore(this)){String version=store.readyVersion();if(!readyVersion.equals(version)){drafts=store.ready();readyVersion=version;thumbnails.evictAll();}}}
    private void refresh(){
        readDrafts();StringBuilder key=new StringBuilder(status().title).append(prefs().getString("browser_state","")).append(prefs().getString("x_auth_state","")).append(System.currentTimeMillis()/60000);
        key.append(XListener.hasAccess(this)).append(XListener.attached()).append(BrowserMonitor.running()).append(prefs().getString("last_event",""));
        for(Map<String,String> draft:drafts)key.append(draft.get("id")).append(draft.get("status")).append(draft.get("source")).append(draft.get("image_revision"));
        if(key.toString().equals(lastKey))return;lastKey=key.toString();
        if(tab==0){int y=scroll==null?0:scroll.getScrollY();home();ScrollView target=scroll;if(target!=null)target.post(()->{if(target.getParent()!=null)target.scrollTo(0,y);});}
        else if(tab==1&&adapter!=null)adapter.notifyDataSetChanged();
    }
    private void showTab(int selected,boolean animate){
        tab=selected;lastKey="";viewport.removeAllViews();scroll=null;adapter=null;nav.removeAllViews();
        String[] labels={"Übersicht","Entwürfe","Einstellungen"},icons={"home","draft","settings"};
        for(int i=0;i<3;i++){
            final int destination=i;LinearLayout item=Ui.column(this);item.setGravity(Gravity.CENTER);item.setMinimumHeight(d(62));int color=i==tab?Ui.MINT:Ui.MUTED;
            item.addView(Ui.icon(this,icons[i],color,23));TextView label=Ui.text(this,labels[i],12,color,i==tab);label.setGravity(Gravity.CENTER);Ui.gap(item,4);item.addView(label);
            item.setSelected(i==tab);item.setContentDescription(labels[i]+(i==tab?", ausgewählt":""));item.setBackground(Ui.shape(this,i==tab?Ui.SURFACE:Ui.PANEL,0,12));
            Ui.clickable(item,()->showTab(destination,true));nav.addView(item,new LinearLayout.LayoutParams(0,-2,1));
        }
        if(tab==0)home();else if(tab==1)draftList();else settings();
        if(animate&&android.animation.ValueAnimator.areAnimatorsEnabled()){viewport.setAlpha(.65f);viewport.animate().alpha(1).setDuration(120).start();}else viewport.setAlpha(1);
    }
    private LinearLayout page(){
        viewport.removeAllViews();scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);
        LinearLayout wrapper=Ui.column(this);wrapper.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL);LinearLayout page=Ui.page(this);
        int width=getResources().getConfiguration().screenWidthDp;wrapper.addView(page,new LinearLayout.LayoutParams(width>1100?d(1100):-1,-2));scroll.addView(wrapper);viewport.addView(scroll);return page;
    }
    private void home(){
        LinearLayout page=page();UiState.Status state=status();
        boolean wide=getResources().getConfiguration().screenWidthDp>=720;
        LinearLayout left=page,right=page;
        if(wide){LinearLayout columns=Ui.row(this);columns.setGravity(Gravity.TOP);page.addView(columns);left=Ui.column(this);right=Ui.column(this);columns.addView(left,new LinearLayout.LayoutParams(0,-2,1));View space=new View(this);columns.addView(space,new LinearLayout.LayoutParams(d(20),1));columns.addView(right,new LinearLayout.LayoutParams(0,-2,1));}
        LinearLayout hero=Ui.card(this);left.addView(hero);int tone="warning".equals(state.tone)?Ui.AMBER:"neutral".equals(state.tone)?Ui.MUTED:Ui.MINT;
        LinearLayout eyebrow=Ui.row(this);eyebrow.addView(Ui.icon(this,"bell",tone,24));TextView stateLabel=Ui.text(this,"ready".equals(state.tone)?"EMPFANG BEREIT":"active".equals(state.tone)?"LINKSUCHE LÄUFT":"DEIN EMPFANG",12,tone,true);stateLabel.setPadding(d(10),0,0,0);eyebrow.addView(stateLabel);hero.addView(eyebrow);
        Ui.gap(hero,15);TextView headline=Ui.text(this,state.title,26,Ui.TEXT,true);headline.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);hero.addView(headline);Ui.gap(hero,9);hero.addView(Ui.text(this,state.detail,15,Ui.MUTED,false));
        if("reader".equals(state.action)){Ui.gap(hero,14);ProgressBar progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setIndeterminate(true);progress.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(Ui.MINT));hero.addView(progress,new LinearLayout.LayoutParams(-1,d(5)));}
        Ui.gap(hero,20);primary=Ui.button(this,UiState.actionLabel(state.action),!"pause".equals(state.action),()->act(state.action));hero.addView(primary);
        if("reader".equals(state.action)){Ui.gap(hero,8);hero.addView(Ui.button(this,"Empfang pausieren",false,()->act("pause")));}
        if(!"observed".equals(prefs().getString("x_auth_state",""))){Ui.gap(hero,12);hero.addView(Ui.text(this,"X-Anmeldung: wird bei der ersten Linksuche geprüft.",12,Ui.MUTED,false));}
        if(!"ready".equals(state.tone)||wide){Ui.section(left,"Einrichtung","Zugriff, Anmeldung und Empfang an einem Ort.");left.addView(setupCard());}
        Ui.section(right,"Letzter Entwurf",null);
        if(drafts.isEmpty()){
            LinearLayout empty=Ui.card(this);empty.addView(Ui.icon(this,"draft",Ui.MINT,36));Ui.gap(empty,14);empty.addView(Ui.text(this,"Hier beginnt deine nächste Idee",20,Ui.TEXT,true));Ui.gap(empty,8);empty.addView(Ui.text(this,"Sobald eine passende X-Mitteilung einem Post zugeordnet wurde, erscheint der Entwurf hier.",14,Ui.MUTED,false));Ui.gap(empty,16);empty.addView(Ui.button(this,"Ablauf ansehen",false,()->explain()));right.addView(empty);
        }else right.addView(draftCard(drafts.get(0),true));
        Ui.section(right,"Letzte Aktivität",null);LinearLayout activity=Ui.card(this);activity.addView(Ui.text(this,prefs().getString("last_event","Noch keine X-Mitteilung verarbeitet."),14,Ui.TEXT,false));long eventTime=prefs().getLong("last_event_time",0);Ui.gap(activity,8);activity.addView(Ui.text(this,eventTime==0?"Wartet auf deine erste Mitteilung":UiTime.relative(eventTime,System.currentTimeMillis()),12,Ui.MUTED,false));right.addView(activity);
        Ui.gap(right,16);TextView details=Ui.button(this,"Statusdetails ansehen",false,()->diagnostics());right.addView(details);
        Ui.gap(page,18);page.addView(Ui.text(this,"Lokale Entwürfe · nicht veröffentlicht",12,Ui.MUTED,false));
    }
    private LinearLayout setupCard(){
        LinearLayout card=Ui.card(this);boolean access=XListener.hasAccess(this);boolean observed="observed".equals(prefs().getString("x_auth_state",""));
        prefRow(card,"Mitteilungszugriff",access?"Zugriff erteilt":"Noch nicht erlaubt",access,()->openAccess());
        prefRow(card,"X-Webanmeldung",observed?"Zuletzt erfolgreich gelesen":"Noch nicht geprüft",observed,()->login());
        prefRow(card,"Empfang",BrowserMonitor.running()?"Dienst aktiv":"Pausiert",BrowserMonitor.running(),()->act(BrowserMonitor.running()?"pause":"start"));return card;
    }
    private void prefRow(LinearLayout card,String title,String detail,boolean done,Runnable action){
        LinearLayout row=Ui.row(this);row.setMinimumHeight(d(65));row.addView(Ui.icon(this,done?"check":"link",done?Ui.MINT:Ui.MUTED,22));LinearLayout words=Ui.column(this);words.setPadding(d(12),d(10),d(8),d(10));words.addView(Ui.text(this,title,15,Ui.TEXT,true));words.addView(Ui.text(this,detail,12,Ui.MUTED,false));row.addView(words,new LinearLayout.LayoutParams(0,-2,1));row.addView(Ui.icon(this,"arrow",Ui.MUTED,18));Ui.clickable(row,action);card.addView(row);
    }
    private void draftList(){
        LinearLayout page=Ui.column(this);page.setPadding(d(20),d(20),d(20),0);viewport.addView(page);page.addView(Ui.text(this,"Deine Entwürfe",26,Ui.TEXT,true));Ui.gap(page,6);page.addView(Ui.text(this,"Tippe auf eine Idee für Bild, Beschreibung und Quelle.",14,Ui.MUTED,false));Ui.gap(page,16);
        LinearLayout filters=Ui.row(this);String[] labels={"Alle","Mit Post-Link","Unbestätigt"};for(int i=0;i<3;i++){final int choice=i;TextView pill=Ui.button(this,labels[i],filter==i,()->{filter=choice;showTab(1,false);});LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);if(i>0)p.leftMargin=d(6);filters.addView(pill,p);}page.addView(filters);Ui.gap(page,14);
        ListView list=new ListView(this);list.setDivider(null);list.setDividerHeight(d(12));list.setCacheColorHint(Ui.BG);list.setClipToPadding(false);list.setPadding(0,0,0,d(22));adapter=new DraftAdapter();list.setAdapter(adapter);
        list.setOnItemClickListener((parent,view,position,id)->openDraft(adapter.rows().get(position)));page.addView(list,new LinearLayout.LayoutParams(-1,0,1));
        TextView empty=Ui.text(this,"Noch keine Entwürfe in dieser Ansicht. Neue Ergebnisse erscheinen nach einer passenden X-Mitteilung.",16,Ui.MUTED,false);empty.setGravity(Gravity.CENTER);empty.setPadding(d(18),d(30),d(18),d(30));page.addView(empty);list.setEmptyView(empty);
    }
    private LinearLayout draftCard(Map<String,String> row,boolean featured){
        LinearLayout card=Ui.card(this),line=Ui.row(this);ImageView image=new ImageView(this);image.setScaleType(ImageView.ScaleType.CENTER_CROP);image.setContentDescription("Themenbild zu "+row.get("name"));File imageFile=new File(getFilesDir(),"drafts/"+row.get("id")+".png");
        String cacheKey=row.get("id")+":"+row.get("image_revision")+":"+imageFile.lastModified()+":"+imageFile.length();
        Bitmap thumbnail=PngChecks.valid(imageFile)?thumbnails.get(cacheKey):null;
        if(thumbnail==null){thumbnail=Art.load(imageFile,4);if(thumbnail!=null)thumbnails.put(cacheKey,thumbnail);}
        image.setImageBitmap(thumbnail);image.setBackground(Ui.shape(this,Ui.SURFACE,0,13));image.setClipToOutline(true);line.addView(image,new LinearLayout.LayoutParams(d(featured?94:72),d(featured?94:72)));
        LinearLayout words=Ui.column(this);words.setPadding(d(14),0,0,0);words.addView(Ui.text(this,row.get("name"),featured?20:17,Ui.TEXT,true));Ui.gap(words,5);words.addView(Ui.text(this,"$"+row.get("ticker"),14,Ui.MUTED,false));Ui.gap(words,9);boolean linked=UiState.linked(row.get("source"),row.get("url"));words.addView(Ui.badge(this,linked?"Originalpost verknüpft":"Quelle unbestätigt",linked?Ui.MINT:Ui.AMBER));if(thumbnail==null||!"ready".equals(row.get("image_status"))){Ui.gap(words,8);words.addView(Ui.text(this,thumbnail==null?"Bild fehlt / beschädigt · Reparatur wird geprüft":"Bildmetadaten werden geprüft / sind fehlerhaft",12,Ui.AMBER,false));}
        line.addView(words,new LinearLayout.LayoutParams(0,-2,1));card.addView(line);
        Ui.gap(card,13);card.addView(Ui.text(this,UiTime.relative(Long.parseLong(row.get("time")),System.currentTimeMillis())+" · lokal gespeichert",12,Ui.MUTED,false));
        if(featured){Ui.gap(card,15);card.addView(Ui.button(this,"Entwurf ansehen",true,()->openDraft(row)));}return card;
    }
    private final class DraftAdapter extends BaseAdapter {
        private List<Map<String,String>> visible;
        DraftAdapter(){sync();}
        private void sync(){visible=new ArrayList<>();for(Map<String,String> row:drafts){boolean linked=UiState.linked(row.get("source"),row.get("url"));if(filter==0||filter==1&&linked||filter==2&&!linked)visible.add(row);}}
        List<Map<String,String>> rows(){return visible;}
        public void notifyDataSetChanged(){sync();super.notifyDataSetChanged();}
        public int getCount(){return rows().size();}public Object getItem(int p){return rows().get(p);}public long getItemId(int p){return rows().get(p).get("id").hashCode();}
        public View getView(int p,View old,ViewGroup parent){return draftCard(rows().get(p),false);}
    }
    private void settings(){
        LinearLayout page=page();page.addView(Ui.text(this,"Einstellungen",26,Ui.TEXT,true));Ui.gap(page,7);page.addView(Ui.text(this,"Dein Empfang. Deine Accounts. Alles auf deinem Handy.",14,Ui.MUTED,false));
        Ui.section(page,"Einrichtung",null);page.addView(setupCard());
        Ui.section(page,"X-Accounts","Diese Accounts dürfen Entwürfe auslösen.");LinearLayout accounts=Ui.card(this);
        EditText input=new EditText(this);input.setTextColor(Ui.TEXT);input.setHintTextColor(Ui.MUTED);input.setTextSize(16);input.setMinHeight(d(100));input.setGravity(Gravity.TOP);input.setPadding(d(14),d(12),d(14),d(12));input.setBackground(Ui.shape(this,Ui.BG,Ui.BORDER,12));input.setText(accountDraft==null?prefs().getString("accounts",Core.DEFAULT_ACCOUNTS):accountDraft);input.setContentDescription("Beobachtete X-Nutzernamen, mit Kommas getrennt");accounts.addView(input);Ui.gap(accounts,9);accounts.addView(Ui.text(this,"Nutzernamen wie bisher mit Kommas trennen, z. B. elonmusk,MarioNawfal.",12,Ui.MUTED,false));
        TextView feedback=Ui.text(this,"",13,Ui.MINT,false);feedback.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);accounts.addView(feedback);input.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){accountDraft=s.toString();feedback.setText("Noch nicht gespeichert");feedback.setTextColor(Ui.AMBER);}public void afterTextChanged(Editable s){}});
        Ui.gap(accounts,12);accounts.addView(Ui.button(this,"Accounts speichern",true,()->{try{Core.accounts(input.getText().toString());prefs().edit().putString("accounts",input.getText().toString()).apply();accountDraft=null;input.clearFocus();((android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(input.getWindowToken(),0);feedback.setTextColor(Ui.MINT);feedback.setText("Accounts gespeichert");}catch(IllegalArgumentException e){input.setError(e.getMessage());feedback.setTextColor(Ui.AMBER);feedback.setText(e.getMessage());}}));page.addView(accounts);
        Ui.section(page,"Hilfe & Diagnose",null);LinearLayout tools=Ui.card(this);prefRow(tools,"So funktioniert CoinBot","Mitteilung → Post-Link → Entwurf",false,()->explain());prefRow(tools,"Empfang neu verbinden","Android-Mitteilungsdienst prüfen",XListener.attached(),()->{connectReceiver();Toast.makeText(this,"Empfangsverbindung angefordert",Toast.LENGTH_SHORT).show();});prefRow(tools,"Vorhandene Bilder im Meme-Stil erneuern","Bisherige Bilder werden gesichert; Texte und Quellen bleiben erhalten",false,()->{DraftAssets.restyleAsync(this,null);Toast.makeText(this,"Lokale Meme-Gestaltung gestartet",Toast.LENGTH_LONG).show();});prefRow(tools,"Diagnose ansehen","Status und Zähler, ohne Zugangsdaten",false,()->diagnostics());prefRow(tools,"Linksuche ansehen","Lesefenster für den Gerätetest",false,()->reader());page.addView(tools);
        Ui.section(page,"X-Anmeldung",null);LinearLayout session=Ui.card(this);session.addView(Ui.text(this,"Deine Anmeldung bleibt im lokalen X-Webfenster.",14,Ui.MUTED,false));Ui.gap(session,12);session.addView(Ui.button(this,"Bei X anmelden",false,()->login()));Ui.gap(session,8);TextView clear=Ui.button(this,"X-Sitzung auf diesem Handy löschen",false,()->confirmLogout());clear.setTextColor(Ui.RED);session.addView(clear);page.addView(session);Ui.gap(page,22);page.addView(Ui.text(this,"CoinBot 0.5.0 · lokale Entwürfe\nKeine Veröffentlichung auf Pump.fun.",12,Ui.MUTED,false));
    }
    private void act(String action){switch(action){case "access":openAccess();break;case "notifications":requestNotificationPermission();break;case "login":login();break;case "start":startBrowser();break;case "reconnect":connectReceiver();break;case "reader":reader();break;default:BrowserMonitor.stopFromUser(this);lastKey="";refresh();}}
    private void openAccess(){startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));}
    private void login(){startActivity(new Intent(this,XLoginActivity.class));}
    private void reader(){startActivity(new Intent(this,XReadActivity.class));}
    private void startBrowser(){if(!XListener.hasAccess(this)){openAccess();return;}if(!notificationsAllowed()){requestNotificationPermission();return;}BrowserMonitor.startFromUser(this);lastKey="";clock.postDelayed(()->refresh(),400);}
    private void requestNotificationPermission(){if(Build.VERSION.SDK_INT>=33)requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},41);}
    public void onRequestPermissionsResult(int request,String[] permissions,int[] results){super.onRequestPermissionsResult(request,permissions,results);lastKey="";refresh();if(request==41&&results.length>0&&results[0]!=PackageManager.PERMISSION_GRANTED)new AlertDialog.Builder(this).setTitle("Dienst-Mitteilung erlauben").setMessage("Du kannst CoinBot in den Android-Mitteilungseinstellungen erlauben. Danach lässt sich der Empfang hier starten.").setPositiveButton("Android-Einstellungen",(d,w)->startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName()))).setNegativeButton("Später",null).show();}
    private void connectReceiver(){if(!XListener.hasAccess(this)){openAccess();return;}if(XListener.attached())XListener.refreshActive();else android.service.notification.NotificationListenerService.requestRebind(new ComponentName(this,XListener.class));lastKey="";}
    private void openDraft(Map<String,String> row){startActivity(new Intent(this,DraftActivity.class).putExtra("post_id",row.get("id")));}
    private void explain(){new AlertDialog.Builder(this).setTitle("So entsteht ein Entwurf").setMessage("1 · X meldet einen neuen Post auf deinem Handy.\n\n2 · CoinBot ordnet genau diese Mitteilung dem Originalpost zu. Ohne eindeutigen Treffer wird kein Link geraten.\n\n3 · Daraus entstehen Name, Ticker, Themenbild und Beschreibung – lokal und mit Quellenlink.\n\nOhne X-Mitteilung gibt es keine regelmäßigen Profilabfragen. X-Anmeldung und Android-Hintergrundbetrieb können den Ablauf unterbrechen. Die App veröffentlicht noch keine Coins.").setPositiveButton("Verstanden",null).show();}
    private void diagnostics(){ScrollView s=new ScrollView(this);TextView text=Ui.text(this,Diagnostics.text(this,XListener.hasAccess(this)),14,Ui.TEXT,false);text.setPadding(d(18),d(16),d(18),d(16));text.setTextIsSelectable(true);s.setBackgroundColor(Ui.BG);s.addView(text);new AlertDialog.Builder(this).setTitle("Diagnose").setView(s).setPositiveButton("Schließen",null).setNeutralButton("Kopieren",(dialog,w)->{((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("CoinBot-Diagnose",Diagnostics.text(this,XListener.hasAccess(this))));Toast.makeText(this,"Diagnose kopiert",Toast.LENGTH_SHORT).show();}).show();}
    private void confirmLogout(){new AlertDialog.Builder(this).setTitle("X-Sitzung löschen?").setMessage("CoinBot pausiert den Empfang und entfernt seine lokale Webanmeldung. Deine Entwürfe und die eigenständige X-App bleiben erhalten.").setPositiveButton("Sitzung löschen",(dialog,w)->{BrowserMonitor.stopFromUser(this);XSession.clear();prefs().edit().putString("x_auth_state","required").putBoolean("x_login_attempted",false).remove("browser_last_read").remove("browser_last_account").apply();lastKey="";showTab(2,false);Toast.makeText(this,"Lokale X-Sitzung gelöscht",Toast.LENGTH_SHORT).show();}).setNegativeButton("Abbrechen",null).show();}
}
