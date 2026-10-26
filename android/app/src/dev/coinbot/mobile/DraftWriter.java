package dev.coinbot.mobile;

import android.content.Context;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;

final class DraftWriter {
    static final Object LOCK=new Object();
    static boolean save(Context context,LocalStore store,String id,Core.Idea idea,String url,boolean verified,String source,BrowserRules.Post post)throws Exception{
        synchronized(LOCK){
            if(!store.canPrepare()){store.finish(id,"Vorschlagslimit erreicht",null);return false;}
            File folder=new File(context.getFilesDir(),"drafts");if(!folder.exists()&&!folder.mkdirs())throw new IOException();
            Art.save(idea,new File(folder,id+".png"));
            Map<String,Object> metadata=Core.draftMetadata(idea,id+".png",url,verified);metadata.put("source",source);
            if(post!=null){metadata.put("source_text",post.text);metadata.put("source_author",post.author);metadata.put("source_created_at",post.createdAt);}
            String json=new org.json.JSONObject(metadata).toString(2);
            try(Writer out=new OutputStreamWriter(new FileOutputStream(new File(folder,id+".json")),StandardCharsets.UTF_8)){out.write(json);}
            store.source(id,source);
            String status=post!=null?"Entwurf mit Originalpost · aus X-Webansicht · keine Veröffentlichung":
                verified?"Entwurf bereit · Post-Link bestätigt · Pump.fun fehlt":"Lokaler Entwurf · X-Mitteilung nicht per Link verifiziert · keine Veröffentlichung";
            store.finish(id,status,idea);return true;
        }
    }
}
