package dev.coinbot.mobile;
import java.io.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
/** Host PNG generator for file/metadata tests; not the Android Canvas renderer. */
final class Art {
    static boolean fail;
    static boolean valid(File file){return PngChecks.valid(file);}
    static void save(Core.Idea idea,File target)throws Exception{save(idea,target,"");}
    static void save(Core.Idea idea,File target,String text)throws Exception{
        if(fail)throw new IOException("Injected image failure");
        DraftFiles.replace(target,temp->{if(!ImageIO.write(new BufferedImage(768,768,BufferedImage.TYPE_INT_RGB),"png",temp))throw new IOException();});
    }
}
