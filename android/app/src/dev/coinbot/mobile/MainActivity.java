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
        label("CoinBot · Android",27,true);
        label("Automatische X-Beobachtung",18,true);
        label("Diese erste APK prüft den Empfang auf deinem Handy und bereitet passende Entwürfe lokal vor. Die Pump.fun-Anmeldung fehlt noch: Es werden keine Coins veröffentlicht.",15,false);
        state=label("Status wird geladen …",14,false);
        Button permission=new Button(this);permission.setText("1 · Benachrichtigungszugriff öffnen");permission.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));content.addView(permission);
        label("2 · Beobachtete X-Nutzernamen",16,true);
        EditText accounts=new EditText(this);accounts.setSingleLine(false);accounts.setText(prefs().getString("accounts","realDonaldTrump,elonmusk"));accounts.setTextColor(Color.WHITE);content.addView(accounts);
        Switch enabled=new Switch(this);enabled.setText("X-Beobachtung einschalten");enabled.setTextColor(Color.WHITE);enabled.setChecked(prefs().getBoolean("enabled",false));content.addView(enabled);
        Button save=new Button(this);save.setText("Einstellungen speichern");save.setOnClickListener(v->{try{Core.accounts(accounts.getText().toString());prefs().edit().putString("accounts",accounts.getText().toString()).putBoolean("enabled",enabled.isChecked()).apply();Toast.makeText(this,"Gespeichert",Toast.LENGTH_SHORT).show();update();}catch(IllegalArgumentException e){Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();}});content.addView(save);
        label("3 · In X die Post-Glocke aktivieren",16,true);
        label("Bei den gewählten Accounts alle Post-Mitteilungen einschalten. X muss in der Meldung einen Post-Link liefern. Direkte Nachrichten und Sammelmeldungen werden ignoriert. Der erste echte Empfangstest steht noch aus.",14,false);
        label("Automatische Ergebnisse",20,true);records=new LinearLayout(this);records.setOrientation(LinearLayout.VERTICAL);content.addView(records);
    }
    protected void onResume(){super.onResume();clock.post(refresh);}
    protected void onPause(){clock.removeCallbacks(refresh);super.onPause();}
    private boolean hasAccess(){String access=Settings.Secure.getString(getContentResolver(),"enabled_notification_listeners");if(access==null)return false;ComponentName own=new ComponentName(this,XListener.class);for(String entry:access.split(":"))if(own.equals(ComponentName.unflattenFromString(entry)))return true;return false;}
    private void update(){
        if(state==null)return;
        boolean access=hasAccess();String event=prefs().getString("last_event","Noch keine verwertbare X-Meldung empfangen");long when=prefs().getLong("last_event_time",0);
        state.setText("Zugriff: "+(access?"erteilt":"noch nicht erteilt")+"\nBeobachtung: "+(prefs().getBoolean("enabled",false)?"eingeschaltet":"ausgeschaltet")+"\n"+event+(when>0?"\n"+DateFormat.getDateTimeInstance().format(new Date(when)):""));
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
        TextView note=new TextView(this);note.setText(row.get("description")+"\n\nKein Pump.fun-Listing erstellt.");box.addView(note);
        new AlertDialog.Builder(this).setTitle(row.get("name")+" / $"+row.get("ticker")).setView(box).setPositiveButton("Schließen",null).show();
    }
    private TextView label(String value,int size,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextColor(Color.rgb(235,228,247));t.setTextSize(size);if(bold)t.setTypeface(null,Typeface.BOLD);t.setPadding(0,dp(9),0,dp(9));content.addView(t);return t;}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
}
