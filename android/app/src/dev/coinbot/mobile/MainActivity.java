package dev.coinbot.mobile;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.provider.Settings;
import android.widget.*;
import android.view.*;
import java.io.File;
import java.text.DateFormat;
import java.util.*;

public final class MainActivity extends Activity {
    private LinearLayout content, records;
    private TextView state;
    private Switch enabled;
    private final Handler clock=new Handler(Looper.getMainLooper());
    private final Runnable refresh=new Runnable(){public void run(){update();clock.postDelayed(this,3000);}};
    private SharedPreferences prefs(){return getSharedPreferences("settings",0);}
    public void onCreate(Bundle saved){
        super.onCreate(saved);
        ScrollView scroll=new ScrollView(this);scroll.setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets;});content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(22),dp(28),dp(22),dp(28));content.setBackgroundColor(Color.rgb(16,14,24));scroll.addView(content);setContentView(scroll);
        label("CoinBot · Android 0.4.1",27,true);
        label("X-Mitteilung → Originalpost-Link → Entwurf",18,true);
        label("Der Bot reagiert nur auf Handy-Mitteilungen von X. Er ordnet den dazugehörigen Post samt Link zu und bereitet daraus einen Entwurf vor. Ohne Mitteilung keine automatische X-Abfrage.",14,false);
        label("Experimenteller Webzugang: Erst auf deinem Handy prüfen. X-Anmeldeprüfungen und Android können den Ablauf unterbrechen. Es werden keine Coins veröffentlicht.",14,false);
        state=label("Status wird geladen …",14,false);
        label("X-Anmeldung",18,true);
        Button login=new Button(this);login.setText("X öffnen / anmelden");login.setOnClickListener(v->startActivity(new Intent(this,XLoginActivity.class)));content.addView(login);
        Button start=new Button(this);start.setText("Auf X-Mitteilungen warten");start.setOnClickListener(v->startBrowser());content.addView(start);
        Button pause=new Button(this);pause.setText("Mitteilungsverarbeitung pausieren");pause.setOnClickListener(v->{BrowserMonitor.stopFromUser(this);update();});content.addView(pause);
        Button reader=new Button(this);reader.setText("Linksuche ansehen");reader.setOnClickListener(v->startActivity(new Intent(this,XReadActivity.class)));content.addView(reader);
        Button logout=new Button(this);logout.setText("X-Anmeldung lokal löschen");logout.setOnClickListener(v->{BrowserMonitor.stopFromUser(this);XSession.clear();Toast.makeText(this,"Lokale X-Webanmeldung gelöscht",Toast.LENGTH_SHORT).show();update();});content.addView(logout);
        label("Beobachtete X-Nutzernamen",16,true);
        EditText accounts=new EditText(this);accounts.setSingleLine(false);accounts.setText(prefs().getString("accounts","realDonaldTrump,elonmusk"));accounts.setTextColor(Color.WHITE);content.addView(accounts);
        enabled=new Switch(this);enabled.setText("X-Mitteilungen verarbeiten");enabled.setTextColor(Color.WHITE);enabled.setChecked(prefs().getBoolean("enabled",false));content.addView(enabled);
        Button save=new Button(this);save.setText("Profile und Auslöser speichern");save.setOnClickListener(v->{try{Core.accounts(accounts.getText().toString());prefs().edit().putString("accounts",accounts.getText().toString()).putBoolean("enabled",enabled.isChecked()).apply();Toast.makeText(this,"Gespeichert",Toast.LENGTH_SHORT).show();if(enabled.isChecked())connectReceiver();update();}catch(IllegalArgumentException e){Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();}});content.addView(save);
        label("Post-Glocke und Mitteilungszugriff",16,true);
        label("In der eigenen X-App die Post-Glocke einschalten und CoinBot Benachrichtigungszugriff erteilen. Ohne auslesbare Post-ID wird nur zum empfangenen Text kurz nachgeschlagen. Regelmäßige Profilbesuche sind ausgeschaltet.",14,false);
        Button permission=new Button(this);permission.setText("Benachrichtigungszugriff öffnen");permission.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));content.addView(permission);
        Button reconnect=new Button(this);reconnect.setText("Empfang neu verbinden");reconnect.setOnClickListener(v->{connectReceiver();update();});content.addView(reconnect);
        Button diagnostics=new Button(this);diagnostics.setText("Diagnose kopieren");diagnostics.setOnClickListener(v->{ClipboardManager clipboard=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);clipboard.setPrimaryClip(ClipData.newPlainText("CoinBot-Diagnose",Diagnostics.text(this,hasAccess())));Toast.makeText(this,"Diagnose ohne Nachrichtentexte kopiert",Toast.LENGTH_SHORT).show();});content.addView(diagnostics);
        label("Automatische Ergebnisse",20,true);records=new LinearLayout(this);records.setOrientation(LinearLayout.VERTICAL);content.addView(records);
    }
    protected void onResume(){super.onResume();if(enabled!=null)enabled.setChecked(prefs().getBoolean("enabled",false));if(hasAccess()&&prefs().getBoolean("enabled",false)&&!XListener.attached())connectReceiver();clock.post(refresh);}
    protected void onPause(){clock.removeCallbacks(refresh);super.onPause();}
    private boolean hasAccess(){return XListener.hasAccess(this);}
    private void startBrowser(){
        if(!hasAccess()){Toast.makeText(this,"Bitte Benachrichtigungszugriff erlauben und danach erneut starten",Toast.LENGTH_LONG).show();startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));return;}
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},41);return;}
        BrowserMonitor.startFromUser(this);startActivity(new Intent(this,XReadActivity.class));
    }
    public void onRequestPermissionsResult(int request,String[] permissions,int[] results){super.onRequestPermissionsResult(request,permissions,results);if(request==41&&results.length>0&&results[0]==android.content.pm.PackageManager.PERMISSION_GRANTED)startBrowser();}
    private void connectReceiver(){
        if(!hasAccess()){Diagnostics.note(this,null,"Benachrichtigungszugriff fehlt");return;}
        if(XListener.attached())XListener.refreshActive();
        else{android.service.notification.NotificationListenerService.requestRebind(new ComponentName(this,XListener.class));Diagnostics.note(this,null,"Verbindung zum Android-Benachrichtigungsdienst angefordert");}
    }
    private void update(){
        if(state==null)return;
        state.setText(Diagnostics.summary(this));
        records.removeAllViews();
        try(LocalStore store=new LocalStore(this)){
            for(Map<String,String> row:store.recent()){
                TextView item=new TextView(this);item.setTextColor(Color.WHITE);item.setTextSize(15);item.setPadding(0,dp(14),0,dp(14));
                String name=row.get("name");item.setText((name==null?"X-Post":name+" · $"+row.get("ticker"))+"\n"+row.get("status")+"\n"+row.get("url"));
                if(name!=null)item.setOnClickListener(v->showDraft(row));records.addView(item);
            }
        }
    }
    private void showDraft(Map<String,String> row){
        ScrollView scroll=new ScrollView(this);
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(18),dp(12),dp(18),dp(12));
        scroll.addView(box);
        ImageView image=new ImageView(this);image.setAdjustViewBounds(true);image.setContentDescription("Automatisch gezeichnetes Meme-Motiv");image.setImageBitmap(BitmapFactory.decodeFile(new File(getFilesDir(),"drafts/"+row.get("id")+".png").getPath()));box.addView(image);
        String source=row.get("source");boolean linked="post_link_verified".equals(source)||"x_browser_linked".equals(source);
        String origin="x_browser_linked".equals(source)?"Originalpost samt Text, Autor, Datum und Link aus der X-Webansicht erfasst.":"post_link_verified".equals(source)?"Post-Link und X-Absender bestätigt.":"notification_unverified".equals(source)?"Nur aus X-Mitteilung abgeleitet. Originalpost nicht verifiziert. Keine Freigabe zur automatischen Veröffentlichung.":"Quellenstatus dieses älteren Entwurfs nicht bestätigt.";
        TextView note=new TextView(this);note.setText(origin+"\n\n"+row.get("description")+"\n\nKein Pump.fun-Listing erstellt.");box.addView(note);
        String url=Core.postUrl(row.get("url"));
        if(!url.isEmpty()){
            TextView link=new TextView(this);link.setText((linked?"Originalpost: ":"Beobachtete Post-URL (nicht bestätigt): ")+url);link.setTextIsSelectable(true);box.addView(link);
            Button open=new Button(this);open.setText("Post-Link öffnen");open.setOnClickListener(v->{try{startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse(url)));}catch(ActivityNotFoundException e){Toast.makeText(this,"Keine App zum Öffnen des Links installiert",Toast.LENGTH_SHORT).show();}});box.addView(open);
            Button copy=new Button(this);copy.setText("Post-Link kopieren");copy.setOnClickListener(v->{ClipboardManager clipboard=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);clipboard.setPrimaryClip(ClipData.newPlainText("X-Post",url));Toast.makeText(this,"Link kopiert",Toast.LENGTH_SHORT).show();});box.addView(copy);
        }else{
            TextView missing=new TextView(this);missing.setText("Kein Originalpost-Link verfügbar: In der Mitteilung wurde keine eindeutige passende Post-ID/URL erkannt. Aus dem Text allein kann der Link nicht zuverlässig bestimmt werden.");box.addView(missing);
        }
        new AlertDialog.Builder(this).setTitle(row.get("name")+" / $"+row.get("ticker")).setView(scroll).setPositiveButton("Schließen",null).show();
    }
    private TextView label(String value,int size,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextColor(Color.rgb(235,228,247));t.setTextSize(size);if(bold)t.setTypeface(null,Typeface.BOLD);t.setPadding(0,dp(9),0,dp(9));content.addView(t);return t;}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
}
