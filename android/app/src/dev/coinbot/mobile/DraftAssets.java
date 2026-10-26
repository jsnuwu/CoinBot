package dev.coinbot.mobile;

import android.content.Context;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import org.json.JSONObject;

/** Repairs only local artifacts of existing drafts. Never fetches X or changes source evidence. */
final class DraftAssets {
    private static final ExecutorService WORKER=Executors.newSingleThreadExecutor();
    private static boolean queued;
    static synchronized void repairAsync(Context context){
        if(queued)return;queued=true;
        Context app=context.getApplicationContext();
        WORKER.execute(()->{try(LocalStore store=new LocalStore(app)){
            for(Map<String,String> row:store.ready()){
                synchronized(DraftWriter.LOCK){repair(app,store,row);}
            }
        }catch(Exception e){Diagnostics.note(app,"image_failed","Bildprüfung konnte nicht abgeschlossen werden");}
        finally{synchronized(DraftAssets.class){queued=false;}}});
    }
    private static boolean restyling;
    static synchronized void restyleAsync(Context context,String id){
        if(restyling)return;restyling=true;Context app=context.getApplicationContext();
        WORKER.execute(()->{try(LocalStore store=new LocalStore(app)){
            for(Map<String,String> row:store.ready())if(id==null||id.equals(row.get("id"))){
                synchronized(DraftWriter.LOCK){repair(app,store,row,true);}
            }
        }catch(Exception e){Diagnostics.note(app,"image_failed","Meme-Gestaltung konnte nicht abgeschlossen werden");}
        finally{synchronized(DraftAssets.class){restyling=false;}}});
    }
    private static void backup(File source,File target)throws Exception{
        if(source.isFile()&&!target.exists())DraftFiles.replace(target,temp->java.nio.file.Files.copy(source.toPath(),temp.toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING));
    }
    static File file(Context context,String id,String suffix)throws IOException{
        return DraftFiles.file(new File(context.getFilesDir(),"drafts"),id,suffix);
    }
    static JSONObject read(File file)throws Exception{
        if(file.length()>200_000)throw new IOException("Metadata too large");
        try(InputStream in=new FileInputStream(file)){
            ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int n;
            while((n=in.read(buffer))!=-1){out.write(buffer,0,n);if(out.size()>200_000)throw new IOException("Metadata too large");}
            return new JSONObject(out.toString("UTF-8"));
        }
    }
    static void write(File file,JSONObject metadata)throws Exception{
        byte[] json=metadata.toString(2).getBytes(StandardCharsets.UTF_8);
        DraftFiles.replace(file,temp->{try(FileOutputStream out=new FileOutputStream(temp)){out.write(json);out.getFD().sync();}read(temp);});
    }
    static void imageMetadata(JSONObject metadata,String id,Core.Idea idea,GeneratedArt generated)throws Exception{
        metadata.put("image",id+".png");metadata.put("image_status","ready");for(Map.Entry<String,Object> field:generated.metadata().entrySet())metadata.put(field.getKey(),field.getValue());
        metadata.put("image_theme",idea.theme);metadata.put("image_motif",new ArtStyle(idea,metadata.optString("source_text",metadata.optString("notification_text"))).motif);metadata.put("image_width",768);metadata.put("image_height",768);
    }
    static void repair(Context context,LocalStore store,Map<String,String> row){repair(context,store,row,false);}
    static void repair(Context context,LocalStore store,Map<String,String> row,boolean restyle){
        String id=row.get("id");boolean changed=false;
        try{
            Core.Idea idea=new Core.Idea(row.get("name"),row.get("ticker"),row.get("theme"),row.get("description"));
            File image=file(context,id,".png"),json=file(context,id,".json");
            boolean valid=Art.valid(image);
            // A malformed JSON is never replaced: it may contain confirmed source evidence.
            JSONObject metadata=null;Exception metadataError=null;
            try {
                if(json.exists())metadata=read(json);
                else {
                    metadata=new JSONObject(Core.draftMetadata(idea,id+".png",row.get("url"),UiState.linked(row.get("source"),row.get("url"))));
                    metadata.put("source",row.get("source"));changed=true;
                }
            }catch(Exception e){metadataError=e;}
            boolean redraw=!valid||restyle&&metadataError==null;
            GeneratedArt generated=null;
            if(redraw){
                if(valid){backup(image,file(context,id,".before-v3.png"));backup(json,file(context,id,".before-v3.json"));}
                String text=metadata==null?"":metadata.optString("source_text",metadata.optString("notification_text"));
                generated=Art.save(context,idea,image,text,restyle);changed=true;
            }
            if(metadataError!=null)throw metadataError;
            if(redraw||!metadata.optString("image").equals(id+".png")||!metadata.optString("image_status").equals("ready")){
                // Preserve provenance of an existing valid legacy image; do not claim it was redrawn.
                if(redraw)imageMetadata(metadata,id,idea,generated);
                else {metadata.put("image",id+".png");metadata.put("image_status","ready");}
                changed=true;
            }
            if(changed)write(json,metadata);
            if(!Art.valid(image))throw new IOException("Missing or damaged image");
            if(!"ready".equals(row.get("image_status"))||changed)store.imageStatus(id,"ready");
            if("Bild wird lokal erstellt · keine Veröffentlichung".equals(row.get("status"))||"Entwurf erhalten · Bildspeicherung fehlgeschlagen".equals(row.get("status")))
                store.finish(id,"Entwurf lokal gespeichert · Bild geprüft · keine Veröffentlichung",null);
            if(changed)Diagnostics.note(context,"image_repaired",restyle?"Meme-Szene lokal erstellt; vorheriges Bild gesichert":"Entwurfsbild lokal ergänzt / Bildmetadaten repariert");
        }catch(Exception e){
            String status="Fehler: "+(e instanceof ImageGenerationException?e.getMessage():"Bild oder Bildmetadaten nicht lesbar; Entwurf erhalten");
            if(!status.equals(row.get("image_status"))){store.imageStatus(id,status);Diagnostics.note(context,"image_failed",status);}
        }
    }
}
