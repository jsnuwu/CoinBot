package dev.coinbot.mobile;
import java.io.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
/** Host PNG generator for file/metadata tests; not the Android Canvas renderer. */
final class Art {
    static boolean fail,exhausted;
    static boolean valid(File file){return PngChecks.valid(file);}
    static void save(Core.Idea idea,File target)throws Exception{save(idea,target,"");}
    static GeneratedArt save(android.content.Context context,Core.Idea idea,File target,String text)throws Exception{return save(context,idea,target,text,false);}
    static GeneratedArt save(android.content.Context context,Core.Idea idea,File target,String text,boolean forceNew)throws Exception{
        if(exhausted)throw new ImageGenerationException("Kein ausreichend anderes Meme-Bild nach 24 Versuchen gefunden; kein neues Bild gespeichert");
        save(idea,target,text);BufferedImage image=ImageIO.read(target);
        ImageFingerprint fingerprint=ImageFingerprint.of(image.getRGB(0,0,768,768,null,0,768),768,768);
        return new GeneratedArt(new StoryPlan(idea,text,target.getName(),0),fingerprint,0,1);
    }
    static void save(Core.Idea idea,File target,String text)throws Exception{
        if(fail)throw new IOException("Injected image failure");
        DraftFiles.replace(target,temp->{if(!ImageIO.write(new BufferedImage(768,768,BufferedImage.TYPE_INT_RGB),"png",temp))throw new IOException();});
    }
}
