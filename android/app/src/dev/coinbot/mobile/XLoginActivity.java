package dev.coinbot.mobile;

import android.app.*;
import android.content.*;
import android.net.http.SslError;
import android.os.Bundle;
import android.webkit.*;
import android.widget.*;

/** User types credentials directly into X. No injected script or native bridge in this view. */
public final class XLoginActivity extends Activity {
    private WebView web;
    private TextView state;
    public void onCreate(Bundle saved){
        super.onCreate(saved);BrowserMonitor.pauseForLogin(this);
        LinearLayout layout=new LinearLayout(this);layout.setOrientation(LinearLayout.VERTICAL);
        layout.setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets;});
        state=new TextView(this);state.setPadding(16,16,16,16);state.setText("Direkt bei x.com anmelden. Zugangsdaten bleiben im X-Webfenster. Danach unten die Beobachtung starten.");layout.addView(state);
        web=new WebView(this);XSession.configure(web);layout.addView(web,new LinearLayout.LayoutParams(-1,0,1));
        web.setWebViewClient(new WebViewClient(){
            public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){
                boolean blocked=!BrowserRules.xPage(r.getUrl().toString());
                if(blocked)state.setText("Dieser externe Anmeldeweg wird hier nicht geöffnet. Bitte direkt mit deinem X-Nutzernamen bei X anmelden.");
                return blocked;
            }
            public void onReceivedSslError(WebView v,SslErrorHandler handler,SslError error){handler.cancel();state.setText("Sichere Verbindung zu X fehlgeschlagen. Anmeldung angehalten.");}
            public void onPageFinished(WebView v,String url){CookieManager.getInstance().flush();}
        });
        Button start=new Button(this);start.setText("Anmeldung abgeschlossen · Auf X-Mitteilungen warten");start.setOnClickListener(v->startMonitor());layout.addView(start);
        Button cancel=new Button(this);cancel.setText("Zurück · Beobachtung pausiert");cancel.setOnClickListener(v->finish());layout.addView(cancel);
        setContentView(layout);web.loadUrl("https://x.com/i/flow/login");
    }
    private void startMonitor(){
        if(!XListener.hasAccess(this)){state.setText("Benachrichtigungszugriff erlauben, zurückkehren und erneut starten.");startActivity(new Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));return;}
        if(android.os.Build.VERSION.SDK_INT>=33&&checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},41);return;}
        CookieManager.getInstance().flush();BrowserMonitor.startFromUser(this);startActivity(new Intent(this,XReadActivity.class));finish();
    }
    public void onRequestPermissionsResult(int request,String[] permissions,int[] results){super.onRequestPermissionsResult(request,permissions,results);if(request==41&&results.length>0&&results[0]==android.content.pm.PackageManager.PERMISSION_GRANTED)startMonitor();}
    protected void onDestroy(){if(web!=null){if(web.getParent() instanceof android.view.ViewGroup)((android.view.ViewGroup)web.getParent()).removeView(web);web.stopLoading();web.destroy();}super.onDestroy();}
}
