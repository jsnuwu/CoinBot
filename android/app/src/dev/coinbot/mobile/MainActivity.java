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
    private final Handler clock=new Handler(Looper.getMainLooper());
    private final Runnable refresh=new Runnable(){public void run(){update();clock.postDelayed(this,3000);}};
    private SharedPreferences prefs(){return getSharedPreferences("settings",0);}
    public void onCreate(Bundle saved){
        super.onCreate(saved);
        ScrollView scroll=new ScrollView(this);scroll.setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets;});content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(22),dp(28),dp(22),dp(28));content.setBackgroundColor(Color.rgb(16,14,24));scroll.addView(content);setContentView(scroll);
        label("CoinBot · Android 0.3",27,true);
        label("Automatische X-Beobachtung",18,true);
        label("Version 0.3 verarbeitet allgemeine Post-Themen, nicht nur zehn feste Meme-Motive. Die Erkennung ist lokal und regelbasiert.",14,false);
        label("Diese Version prüft den Empfang auf deinem Handy und bereitet passende Entwürfe lokal vor. Die Pump.fun-Anmeldung fehlt noch: Es werden keine Coins veröffentlicht.",15,false);
        state=label("Status wird geladen …",14,false);
        Button permission=new Button(this);permission.setText("1 · Benachrichtigungszugriff öffnen");permission.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));content.addView(permission);
        label("2 · Beobachtete X-Nutzernamen",16,true);
        EditText accounts=new EditText(this);accounts.setSingleLine(false);accounts.setText(prefs().getString("accounts","realDonaldTrump,elonmusk"));accounts.setTextColor(Color.WHITE);content.addView(accounts);
        Switch enabled=new Switch(this);enabled.setText("X-Beobachtung einschalten");enabled.setTextColor(Color.WHITE);enabled.setChecked(prefs().getBoolean("enabled",false));content.addView(enabled);
        Button save=new Button(this);save.setText("Einstellungen speichern");save.setOnClickListener(v->{try{Core.accounts(accounts.getText().toString());prefs().edit().putString("accounts",accounts.getText().toString()).putBoolean("enabled",enabled.isChecked()).apply();Toast.makeText(this,"Gespeichert",Toast.LENGTH_SHORT).show();if(enabled.isChecked())connectReceiver();update();}catch(IllegalArgumentException e){Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();}});content.addView(save);
        label("3 · In X die Post-Glocke aktivieren",16,true);
        label("Bei den gewählten Accounts alle Post-Mitteilungen einschalten. Vollständiger Text mit zugeordnetem Absender kann auch ohne Link einen lokalen Entwurf erzeugen. Dieser bleibt unverifiziert und darf nicht automatisch veröffentlicht werden. Direkte Nachrichten und Sammelmeldungen werden ignoriert.",14,false);
        Button reconnect=new Button(this);reconnect.setText("Empfang neu verbinden");reconnect.setOnClickListener(v->{connectReceiver();update();});content.addView(reconnect);
        Button diagnostics=new Button(this);diagnostics.setText("Diagnose kopieren");diagnostics.setOnClickListener(v->{ClipboardManager clipboard=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);clipboard.setPrimaryClip(ClipData.newPlainText("CoinBot-Diagnose",Diagnostics.text(this,hasAccess())));Toast.makeText(this,"Diagnose ohne Nachrichtentexte kopiert",Toast.LENGTH_SHORT).show();});content.addView(diagnostics);
        label("Automatische Ergebnisse",20,true);records=new LinearLayout(this);records.setOrientation(LinearLayout.VERTICAL);content.addView(records);
    }
    protected void onResume(){super.onResume();if(hasAccess()&&prefs().getBoolean("enabled",false)&&!XListener.attached())connectReceiver();clock.post(refresh);}
    protected void onPause(){clock.removeCallbacks(refresh);super.onPause();}
    private boolean hasAccess(){String access=Settings.Secure.getString(getContentResolver(),"enabled_notification_listeners");if(access==null)return false;ComponentName own=new ComponentName(this,XListener.class);for(String entry:access.split(":"))if(own.equals(ComponentName.unflattenFromString(entry)))return true;return false;}
    private void connectReceiver(){
        if(!hasAccess()){Diagnostics.note(this,null,"Benachrichtigungszugriff fehlt");return;}
        if(XListener.attached())XListener.refreshActive();
        else{android.service.notification.NotificationListenerService.requestRebind(new ComponentName(this,XListener.class));Diagnostics.note(this,null,"Verbindung zum Android-Benachrichtigungsdienst angefordert");}
    }
    private void update(){
        if(state==null)return;
        state.setText(Diagnostics.text(this,hasAccess()));
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
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(18),dp(12),dp(18),dp(12));
        ImageView image=new ImageView(this);image.setAdjustViewBounds(true);image.setContentDescription("Automatisch gezeichnetes Meme-Motiv");image.setImageBitmap(BitmapFactory.decodeFile(new File(getFilesDir(),"drafts/"+row.get("id")+".png").getPath()));box.addView(image);
        String source=row.get("source");String origin="post_link_verified".equals(source)?"Post-Link und X-Absender bestätigt.":"notification_unverified".equals(source)?"Nur aus X-Mitteilung abgeleitet. Originalpost nicht verifiziert. Keine Freigabe zur automatischen Veröffentlichung.":"Quellenstatus dieses älteren Entwurfs nicht bestätigt.";
        TextView note=new TextView(this);note.setText(origin+"\n\n"+row.get("description")+"\n\nKein Pump.fun-Listing erstellt.");box.addView(note);
        new AlertDialog.Builder(this).setTitle(row.get("name")+" / $"+row.get("ticker")).setView(box).setPositiveButton("Schließen",null).show();
    }
    private TextView label(String value,int size,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextColor(Color.rgb(235,228,247));t.setTextSize(size);if(bold)t.setTypeface(null,Typeface.BOLD);t.setPadding(0,dp(9),0,dp(9));content.addView(t);return t;}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
}
