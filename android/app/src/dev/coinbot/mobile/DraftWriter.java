package dev.coinbot.mobile;

import android.content.Context;
import java.util.Map;

final class DraftWriter {
    static final Object LOCK=new Object();
    static boolean save(Context context,LocalStore store,String id,Core.Idea idea,String url,boolean verified,String source,BrowserRules.Post post,String sourceText)throws Exception{
        synchronized(LOCK){
            if(!store.canPrepare()){store.finish(id,"Vorschlagslimit erreicht",null);return false;}
            store.source(id,source);
            store.finish(id,"Bild wird lokal erstellt · keine Veröffentlichung",idea);
            store.imageStatus(id,"pending");
            try {
                Map<String,Object> metadata=Core.draftMetadata(idea,id+".png",url,verified);metadata.put("source",source);metadata.put("theme",idea.theme);
                if(sourceText!=null)metadata.put(verified?"source_text":"notification_text",sourceText);
                if(post!=null){metadata.put("source_text",post.text);metadata.put("source_author",post.author);metadata.put("source_created_at",post.createdAt);}
                org.json.JSONObject json=new org.json.JSONObject(metadata);
                // Save source evidence first, so a failed image write can be repaired without source loss.
                json.put("image_status","pending");
                DraftAssets.write(DraftAssets.file(context,id,".json"),json);
                Art.save(context,idea,DraftAssets.file(context,id,".png"),post==null?sourceText:post.text);
                DraftAssets.imageMetadata(json,id,idea);
                DraftAssets.write(DraftAssets.file(context,id,".json"),json);
                store.imageStatus(id,"ready");
                String status=post!=null?"Entwurf mit Originalpost · aus X-Webansicht · keine Veröffentlichung":
                    verified?"Entwurf bereit · Post-Link bestätigt · Pump.fun fehlt":"Lokaler Entwurf · X-Mitteilung nicht per Link verifiziert · keine Veröffentlichung";
                store.finish(id,status,idea);return true;
            }catch(Exception e){
                store.imageStatus(id,"Fehler: Bild nicht fertig gespeichert; automatische Reparatur beim Öffnen");
                store.finish(id,"Entwurf erhalten · Bildspeicherung fehlgeschlagen",null);
                Diagnostics.note(context,"image_failed","Entwurf erhalten, aber Bildspeicherung fehlgeschlagen");
                throw e;
            }
        }
    }
}
