package dev.coinbot.mobile;

import android.content.Context;
import android.graphics.*;
import java.io.*;
import java.util.regex.*;

/** Local scene synthesis plus a durable gate against repeated pixels, structure, and compositions. */
final class Art {
    static GeneratedArt save(Context context,Core.Idea idea,File target,String text)throws IOException{return save(context,idea,target,text,false);}
    static GeneratedArt save(Context context,Core.Idea idea,File target,String text,boolean forceNew)throws IOException{
        String owner=target.getName().replaceFirst("\\.png$","");
        if(!owner.matches("[A-Za-z0-9_-]{1,100}"))throw new IOException("Invalid draft image identity");
        synchronized(ImageLedger.MONITOR){
            try(ImageLedger history=new ImageLedger(context.getFilesDir())){
                if(history.fresh){Diagnostics.note(context,null,"Bisherige Entwurfsbilder werden einmalig für den Bildvergleich erfasst");importLegacy(context,history);history.persist();}
                Bitmap image=Bitmap.createBitmap(768,768,Bitmap.Config.ARGB_8888);
                int rejected=0;
                try(ScenePainter painter=new ScenePainter(context,new MemeScene(idea,text))){
                    for(int attempt=0;attempt<StoryPlan.MAX_ATTEMPTS;attempt++){
                        StoryPlan plan=new StoryPlan(idea,text,owner,attempt);painter.paint(image,plan);
                        ImageFingerprint fingerprint=fingerprint(image);
                        if(history.rejection(owner,plan.structure,fingerprint,!forceNew)!=null){rejected++;continue;}
                        // Reserve durably before exposing the PNG. A crash cannot release it to another draft.
                        try{history.reserve(owner,plan.structure,fingerprint,!forceNew);}catch(Exception e){throw new ImageGenerationException("Bildhistorie nicht sicher gespeichert; kein neues Bild freigegeben",e);}
                        DraftFiles.replace(target,temp->{
                            try(FileOutputStream out=new FileOutputStream(temp)){
                                if(!image.compress(Bitmap.CompressFormat.PNG,100,out))throw new IOException("PNG compression failed");
                                out.getFD().sync();
                            }
                            Bitmap decoded=load(temp,1);if(decoded==null)throw new IOException("PNG validation failed");
                            try{if(!fingerprint(decoded).pixels.equals(fingerprint.pixels))throw new IOException("PNG pixels changed");}finally{decoded.recycle();}
                        });
                        return new GeneratedArt(plan,fingerprint,attempt,history.size());
                    }
                    throw new ImageGenerationException("Kein ausreichend anderes Meme-Bild nach 24 Versuchen gefunden; kein neues Bild gespeichert");
                }finally{image.recycle();Diagnostics.imageRejections(context,rejected);}
            }catch(ImageGenerationException e){throw e;}catch(Exception e){throw new ImageGenerationException("Bild oder Bildhistorie nicht sicher speicherbar; kein neues Bild freigegeben",e);}
        }
    }
    private static ImageFingerprint fingerprint(Bitmap image){
        int[] pixels=new int[image.getWidth()*image.getHeight()];image.getPixels(pixels,0,image.getWidth(),0,0,image.getWidth(),image.getHeight());
        return ImageFingerprint.of(pixels,image.getWidth(),image.getHeight());
    }
    private static void importLegacy(Context context,ImageLedger history)throws Exception{
        File folder=new File(context.getFilesDir(),"drafts");File[] files=folder.listFiles();
        if(files==null){if(folder.exists())throw new ImageGenerationException("Vorhandene Bilder nicht lesbar; Bildvergleich nicht möglich");return;}
        Pattern name=Pattern.compile("([A-Za-z0-9_-]{1,100})(?:\\.before-v[0-9]+)?\\.png");
        for(File file:files){Matcher match=name.matcher(file.getName());if(!match.matches())continue;
            Bitmap image=load(file,1);if(image==null)continue;
            try{history.rememberLegacy(match.group(1),fingerprint(image));}finally{image.recycle();}
        }
    }
    static boolean valid(File file){
        Bitmap decoded=load(file,1);
        if(decoded==null)return false;
        decoded.recycle();return true;
    }
    static Bitmap load(File file,int sample){
        if(!PngChecks.valid(file))return null;
        BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;
        BitmapFactory.decodeFile(file.getPath(),bounds);
        if(bounds.outWidth!=768||bounds.outHeight!=768||!"image/png".equals(bounds.outMimeType))return null;
        BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=sample;
        try{return BitmapFactory.decodeFile(file.getPath(),options);}catch(RuntimeException e){return null;}
    }
}
