package dev.coinbot.mobile;

import android.app.Notification;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import java.util.*;

/** Only reads values Android exposes. Never unwraps or sends a PendingIntent. */
final class NotificationLinks {
    private final StringBuilder values=new StringBuilder();
    private final Set<Object> visited=Collections.newSetFromMap(new IdentityHashMap<Object,Boolean>());
    private int inspected, bundles, ids, uris, appLinks;
    private boolean limited;
    private final boolean openTarget;

    NotificationLinks(Notification notification,String tag){
        openTarget=notification.contentIntent!=null;
        tag(tag);tag(notification.getShortcutId());
        read("",notification.extras,0);
        if(notification.actions!=null)for(Notification.Action action:notification.actions){
            if(inspected>=128){limited=true;break;}
            if(action!=null)read("",action.getExtras(),0);
        }
    }
    private void tag(String value){add(Core.metadataLink(value));add(Core.postUrl(value));add(Core.appPostLink(value));}
    private void add(String value){
        if(value==null||value.isEmpty())return;
        if(value.length()>16000-values.length()){limited=true;return;}
        values.append(value).append('\n');
    }
    private void read(String key,Object value,int depth){
        if(value==null)return;
        if(++inspected>128||depth>4||values.length()>=16000){limited=true;return;}
        try{
            if(Core.postIdKey(key)&&(value instanceof CharSequence||value instanceof Number)){
                String link=Core.metadataLink(value.toString());if(!link.isEmpty()){ids++;add(link);}
            }
            if(value instanceof CharSequence){
                CharSequence text=(CharSequence)value;
                if(text.length()>16000){limited=true;return;}
                String raw=text.toString();add(raw);
                String link=Core.appPostLink(raw);if(!link.isEmpty()){appLinks++;add(link);}
            }else if(value instanceof Uri){
                uris++;String raw=value.toString();add(raw);
                String link=Core.appPostLink(raw);if(!link.isEmpty()){appLinks++;add(link);}
            }else if(value instanceof Intent){
                // Data URI only. No execution, component inspection or hidden Android APIs.
                if(visited.add(value))read("",((Intent)value).getData(),depth+1);
            }else if(value instanceof Bundle){
                if(!visited.add(value))return;bundles++;
                Bundle bundle=(Bundle)value;
                for(String child:bundle.keySet()){
                    if(inspected>=128){limited=true;break;}
                    try{read(child,bundle.get(child),depth+1);}catch(RuntimeException ignored){}
                }
            }else if(value instanceof Object[]){
                if(!visited.add(value))return;
                for(Object child:(Object[])value){if(inspected>=128){limited=true;break;}read(key,child,depth+1);}
            }else if(value instanceof ArrayList){
                if(!visited.add(value))return;
                for(Object child:(ArrayList<?>)value){if(inspected>=128){limited=true;break;}read(key,child,depth+1);}
            }
        }catch(RuntimeException ignored){}
    }
    Core.Candidate candidate(Set<String> allowed){return limited?null:Core.candidate(values.toString(),allowed);}
    String summary(boolean matched){
        return (matched?"Post-ID/URL erkannt":"Keine eindeutige passende Post-ID/URL")+
            "; explizite ID-Felder: "+ids+", App-Postlinks: "+appLinks+", URI-Felder: "+uris+
            ", Datenbündel: "+bundles+(limited?"; Leselimit erreicht":"")+
            (openTarget?"; X-Öffnungsziel vorhanden, nicht auslesbar":"; kein X-Öffnungsziel");
    }
}
