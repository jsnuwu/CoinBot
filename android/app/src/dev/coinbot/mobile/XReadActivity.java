package dev.coinbot.mobile;

import android.app.Activity;
import android.content.Intent;
import android.os.*;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.*;

/** Visible reader for the first device test. Service owns the view and its lifecycle. */
public final class XReadActivity extends Activity {
    private final Handler handler=new Handler(Looper.getMainLooper());
    private FrameLayout frame;
    private TextView state;
    private int attempts;
    private final Runnable refresh=new Runnable(){public void run(){attach();handler.postDelayed(this,1500);}};
    public void onCreate(Bundle saved){
        super.onCreate(saved);
        LinearLayout layout=new LinearLayout(this);layout.setOrientation(LinearLayout.VERTICAL);
        layout.setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets;});
        TextView intro=new TextView(this);intro.setPadding(16,12,16,12);intro.setText("Automatische X-Leseansicht · keine Bedienung der Webseite. Profile wechseln selbst. Für den ersten Test offen lassen; Hintergrundbetrieb hängt von Android und X ab.");layout.addView(intro);
        state=new TextView(this);layout.addView(state);
        frame=new FrameLayout(this);layout.addView(frame,new LinearLayout.LayoutParams(-1,0,1));
        Button results=new Button(this);results.setText("Entwürfe und Status öffnen");results.setOnClickListener(v->{startActivity(new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));finish();});layout.addView(results);
        Button pause=new Button(this);pause.setText("Beobachtung pausieren");pause.setOnClickListener(v->{BrowserMonitor.stopFromUser(this);finish();});layout.addView(pause);
        setContentView(layout);
    }
    protected void onResume(){super.onResume();handler.post(refresh);}
    protected void onPause(){handler.removeCallbacks(refresh);super.onPause();}
    private void attach(){
        state.setText(getSharedPreferences("settings",0).getString("browser_state","Dienst wird gestartet …"));
        WebView web=BrowserMonitor.readerView();
        if(web==null){if(++attempts>10)state.setText("Leseansicht nicht aktiv. In CoinBot anmelden bzw. Beobachtung starten.");return;}
        if(web.getParent()!=frame){if(web.getParent() instanceof ViewGroup)((ViewGroup)web.getParent()).removeView(web);frame.addView(web,new FrameLayout.LayoutParams(-1,-1));}
    }
    protected void onDestroy(){frame.removeAllViews();super.onDestroy();}
}
