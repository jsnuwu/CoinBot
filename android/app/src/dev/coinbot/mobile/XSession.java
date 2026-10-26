package dev.coinbot.mobile;

import android.webkit.*;

/** Android WebView's own cookie jar. No native reads, export or server transfer of credentials. */
final class XSession {
    static void configure(WebView web){
        WebSettings settings=web.getSettings();settings.setJavaScriptEnabled(true);settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);settings.setAllowContentAccess(false);settings.setGeolocationEnabled(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);settings.setSafeBrowsingEnabled(true);
        settings.setSupportMultipleWindows(false);
        CookieManager.getInstance().setAcceptCookie(true);CookieManager.getInstance().setAcceptThirdPartyCookies(web,false);
    }
    static void clear(){
        CookieManager.getInstance().removeAllCookies(ok->CookieManager.getInstance().flush());
        WebStorage.getInstance().deleteAllData();
    }
}
