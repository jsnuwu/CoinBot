package dev.coinbot.mobile;

import android.content.*;
import java.text.DateFormat;
import java.util.Date;

final class Diagnostics {
    static synchronized void note(Context context,String key,String message){
        SharedPreferences p=context.getSharedPreferences("settings",0);
        SharedPreferences.Editor e=p.edit().putString("last_event",message).putLong("last_event_time",System.currentTimeMillis());
        if(key!=null)e.putLong("diag_"+key,p.getLong("diag_"+key,0)+1);
        if("source_failed".equals(key))e.putString("last_source_error",message);
        e.apply();
    }
    static String text(Context context,boolean access){
        SharedPreferences p=context.getSharedPreferences("settings",0);
        long when=p.getLong("last_event_time",0);
        return "CoinBot 0.3.2\nZugriff: "+(access?"erteilt":"fehlt")+
            "\nDienst: "+(XListener.attached()?"verbunden":"nicht verbunden")+
            "\nBeobachtung: "+(p.getBoolean("enabled",false)?"eingeschaltet":"aus")+
            "\nAccounts: "+p.getString("accounts","realDonaldTrump,elonmusk")+
            "\nX-Meldungen: "+p.getLong("diag_received",0)+
            "\nSammelmeldungen ignoriert: "+p.getLong("diag_summary",0)+
            "\nNachrichtenformat ignoriert: "+p.getLong("diag_private",0)+
            "\nOhne passenden Link: "+p.getLong("diag_no_link",0)+
            "\nLetzte Linkpruefung: "+p.getString("last_link_scan","Seit Update noch keine X-Mitteilung geprüft")+
            "\nAbsender nicht zugeordnet: "+p.getLong("diag_unknown_author",0)+
            "\nText fehlt/gekuerzt: "+p.getLong("diag_incomplete",0)+
            "\nKein passendes Thema: "+p.getLong("diag_no_topic",0)+
            "\nDoppelte Meldungen: "+p.getLong("diag_duplicate",0)+
            "\nLetzter Quellenfehler: "+p.getString("last_source_error","keiner")+
            "\nEntwuerfe aus Mitteilung (unverifiziert): "+p.getLong("diag_draft_notification",0)+
            "\nEntwuerfe mit bestaetigtem Link: "+p.getLong("diag_draft_verified",0)+
            "\nLetzter Status: "+p.getString("last_event","Noch keine X-Meldung")+
            (when>0?"\nZeit: "+DateFormat.getDateTimeInstance().format(new Date(when)):"")+
            "\nPump.fun: nicht verbunden; keine Veroeffentlichungen";
    }
    static void links(Context context,String summary){context.getSharedPreferences("settings",0).edit().putString("last_link_scan",summary).apply();}
}
