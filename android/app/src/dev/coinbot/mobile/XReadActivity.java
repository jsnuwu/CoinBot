package dev.coinbot.mobile;

import android.app.Activity;
import android.content.Intent;
import android.os.*;
import android.view.*;
import android.webkit.WebView;
import android.widget.*;

/** Optional visible inspection; the service owns the read-only WebView. */
public final class XReadActivity extends Activity {
    private final Handler handler=new Handler(Looper.getMainLooper());
    private FrameLayout frame;
    private LinearLayout placeholder;
    private TextView state,title,detail,pause;
    private final Runnable refresh=new Runnable(){public void run(){attach();handler.postDelayed(this,1500);}};
    public void onCreate(Bundle saved){
        super.onCreate(saved);LinearLayout root=Ui.column(this);Ui.setupWindow(this,root);
        LinearLayout header=Ui.row(this);header.setPadding(Ui.dp(this,18),Ui.dp(this,10),Ui.dp(this,18),Ui.dp(this,10));header.addView(Ui.button(this,"‹ Zurück",false,()->finish()));TextView heading=Ui.text(this,"Linksuche",18,Ui.TEXT,true);heading.setPadding(Ui.dp(this,14),0,0,0);header.addView(heading);root.addView(header);
        state=Ui.text(this,"",13,Ui.MUTED,false);state.setPadding(Ui.dp(this,20),Ui.dp(this,8),Ui.dp(this,20),Ui.dp(this,14));root.addView(state);
        frame=new FrameLayout(this);root.addView(frame,new LinearLayout.LayoutParams(-1,0,1));placeholder=Ui.page(this);placeholder.setGravity(Gravity.CENTER);placeholder.addView(Ui.icon(this,"bell",Ui.MINT,48));Ui.gap(placeholder,20);title=Ui.text(this,"",24,Ui.TEXT,true);title.setGravity(Gravity.CENTER);placeholder.addView(title);Ui.gap(placeholder,12);detail=Ui.text(this,"",15,Ui.MUTED,false);detail.setGravity(Gravity.CENTER);placeholder.addView(detail);frame.addView(placeholder,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout footer=Ui.page(this);footer.setPadding(Ui.dp(this,20),Ui.dp(this,12),Ui.dp(this,20),Ui.dp(this,16));footer.addView(Ui.button(this,"Zur Übersicht",true,()->{startActivity(new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP).putExtra("show_home",true));finish();}));Ui.gap(footer,8);pause=Ui.button(this,"Empfang pausieren",false,()->{BrowserMonitor.stopFromUser(this);handler.postDelayed(()->attach(),200);});footer.addView(pause);root.addView(footer);
    }
    protected void onResume(){super.onResume();handler.post(refresh);}
    protected void onPause(){handler.removeCallbacks(refresh);super.onPause();}
    private void attach(){
        state.setText(getSharedPreferences("settings",0).getString("browser_state","Empfang noch nicht gestartet."));
        WebView web=BrowserMonitor.readerView();boolean running=BrowserMonitor.running(),busy=BrowserMonitor.resolving();
        if(web!=null&&web.getParent()!=frame){if(web.getParent() instanceof ViewGroup)((ViewGroup)web.getParent()).removeView(web);frame.addView(web,new FrameLayout.LayoutParams(-1,-1));}
        placeholder.setVisibility(busy?View.GONE:View.VISIBLE);placeholder.bringToFront();title.setText(running?"Wartet auf X-Mitteilung":"Empfang pausiert");detail.setText(running?"Diese Ansicht bleibt ohne Auftrag leer. Eine Handy-Mitteilung löst die Linksuche aus; danach wird die X-Seite wieder geschlossen.":"Starte den Empfang auf der Übersicht. Die Linksuche öffnet selbst keine Profile im Leerlauf.");pause.setVisibility(running?View.VISIBLE:View.GONE);
    }
    protected void onDestroy(){frame.removeAllViews();super.onDestroy();}
}
