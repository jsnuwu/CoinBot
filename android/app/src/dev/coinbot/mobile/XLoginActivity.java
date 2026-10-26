package dev.coinbot.mobile;

import android.app.*;
import android.content.*;
import android.net.http.SslError;
import android.os.Bundle;
import android.webkit.*;
import android.widget.*;

/** Credentials remain directly in X; no native bridge or injected login script. */
public final class XLoginActivity extends Activity {
    private WebView web;
    private TextView state;
    public void onCreate(Bundle saved){
        super.onCreate(saved);BrowserMonitor.pauseForLogin(this);
        LinearLayout root=Ui.column(this);Ui.setupWindow(this,root);
        LinearLayout header=Ui.row(this);header.setPadding(Ui.dp(this,18),Ui.dp(this,10),Ui.dp(this,18),Ui.dp(this,10));header.addView(Ui.button(this,"‹ Zurück",false,()->finish()));TextView title=Ui.text(this,"X-Anmeldung",18,Ui.TEXT,true);title.setPadding(Ui.dp(this,14),0,0,0);header.addView(title);root.addView(header);
        state=Ui.text(this,"Direkt bei x.com anmelden. CoinBot liest deine Eingaben nicht aus.",13,Ui.MUTED,false);state.setPadding(Ui.dp(this,20),Ui.dp(this,8),Ui.dp(this,20),Ui.dp(this,14));state.setAccessibilityLiveRegion(android.view.View.ACCESSIBILITY_LIVE_REGION_POLITE);root.addView(state);
        web=new WebView(this);XSession.configure(web);root.addView(web,new LinearLayout.LayoutParams(-1,0,1));
        web.setWebViewClient(new WebViewClient(){
            public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){boolean blocked=!BrowserRules.xPage(r.getUrl().toString());if(blocked){state.setTextColor(Ui.AMBER);state.setText("Bitte direkt mit deinem X-Nutzernamen anmelden. Externe Anmeldewege werden hier nicht geöffnet.");}return blocked;}
            public void onReceivedSslError(WebView v,SslErrorHandler handler,SslError error){handler.cancel();state.setTextColor(Ui.AMBER);state.setText("Sichere Verbindung zu X fehlgeschlagen. Anmeldung angehalten.");}
            public void onPageFinished(WebView v,String url){CookieManager.getInstance().flush();}
        });
        LinearLayout footer=Ui.page(this);footer.setPadding(Ui.dp(this,20),Ui.dp(this,12),Ui.dp(this,20),Ui.dp(this,16));footer.addView(Ui.text(this,"Sobald du bei X angemeldet bist:",13,Ui.MUTED,false));Ui.gap(footer,8);footer.addView(Ui.button(this,"Empfang starten",true,()->startMonitor()));root.addView(footer);web.loadUrl("https://x.com/i/flow/login");
    }
    private void startMonitor(){
        if(!XListener.hasAccess(this)){state.setTextColor(Ui.AMBER);state.setText("Bitte Mitteilungszugriff erlauben. Danach hierher zurückkehren und den Empfang starten.");startActivity(new Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));return;}
        if(android.os.Build.VERSION.SDK_INT>=33&&checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},41);return;}
        CookieManager.getInstance().flush();getSharedPreferences("settings",0).edit().putBoolean("x_login_attempted",true).apply();BrowserMonitor.startFromUser(this);finish();
    }
    public void onRequestPermissionsResult(int request,String[] permissions,int[] results){super.onRequestPermissionsResult(request,permissions,results);if(request==41&&results.length>0&&results[0]==android.content.pm.PackageManager.PERMISSION_GRANTED)startMonitor();else{state.setTextColor(Ui.AMBER);state.setText("Die Dienst-Mitteilung wurde nicht erlaubt. Du kannst das später in Android ändern.");}}
    protected void onDestroy(){if(web!=null){if(web.getParent() instanceof android.view.ViewGroup)((android.view.ViewGroup)web.getParent()).removeView(web);web.stopLoading();web.destroy();}super.onDestroy();}
}
