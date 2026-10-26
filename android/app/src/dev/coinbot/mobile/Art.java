package dev.coinbot.mobile;

import android.content.Context;
import android.graphics.*;
import java.io.*;

/** Offline surreal scene compositor. Source illustrations ship with the APK. */
final class Art {
    static void save(Context context,Core.Idea idea,File target,String text)throws IOException{
        MemeScene scene=new MemeScene(idea,text);ArtStyle style=new ArtStyle(idea,text);
        Bitmap image=Bitmap.createBitmap(768,768,Bitmap.Config.ARGB_8888),sheet=null;
        try{
            try(InputStream in=context.getAssets().open(scene.asset)){sheet=BitmapFactory.decodeStream(in);}
            if(sheet==null||sheet.getWidth()<scene.grid*100||sheet.getHeight()<scene.grid*100)throw new IOException("Bundled meme artwork unavailable");
            int[] bounds=scene.bounds(sheet.getWidth(),sheet.getHeight());
            Rect source=new Rect(bounds[0],bounds[1],bounds[2],bounds[3]);
            Canvas c=new Canvas(image);Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
            ColorMatrix colors=new ColorMatrix();colors.setSaturation(.93f+style.decoration*.07f);p.setColorFilter(new ColorMatrixColorFilter(colors));
            c.drawBitmap(sheet,source,new RectF(0,0,768,768),p);p.setColorFilter(null);
            // Small compositional/lighting variation per draft. No ticker banner or category icon.
            p.setShader(new RadialGradient(384,340,535,new int[]{Color.TRANSPARENT,Color.argb(45+style.layout*8,8,4,18)},new float[]{.5f,1},Shader.TileMode.CLAMP));
            c.drawRect(0,0,768,768,p);p.setShader(null);
            DraftFiles.replace(target,temp->{
                try(FileOutputStream out=new FileOutputStream(temp)){
                    if(!image.compress(Bitmap.CompressFormat.PNG,100,out))throw new IOException("PNG compression failed");
                    out.getFD().sync();
                }
                if(!valid(temp))throw new IOException("PNG validation failed");
            });
        }catch(IOException e){throw e;}catch(Exception e){throw new IOException("Meme image could not be saved",e);}
        finally{if(sheet!=null)sheet.recycle();image.recycle();}
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
