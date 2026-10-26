package dev.coinbot.mobile;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

public final class ImageUniquenessTest {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static ImageFingerprint fingerprint(int seed){
        Random random=new Random(seed);int[] pixels=new int[64*64];
        // Random low-frequency blocks rather than imperceptible per-pixel noise.
        int[] blocks=new int[256];for(int i=0;i<blocks.length;i++)blocks[i]=0xff000000|random.nextInt(0xffffff);
        for(int y=0;y<64;y++)for(int x=0;x<64;x++)pixels[y*64+x]=blocks[y/4*16+x/4];
        return ImageFingerprint.of(pixels,64,64);
    }
    static String structure(String value){return ImageFingerprint.sha(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    static void damaged(File folder,byte[] bytes)throws Exception{
        Files.write(new File(folder,"image-history.v1").toPath(),bytes);
        try(ImageLedger history=new ImageLedger(folder)){throw new AssertionError("damaged history accepted");}catch(ImageGenerationException expected){checks++;}
    }
    public static void main(String[] args)throws Exception{
        if(args.length>0){
            try(ImageLedger history=new ImageLedger(new File(args[0]))){history.reserve(args[1],structure("race"),fingerprint(42),false);System.exit(0);}
            catch(Exception expected){System.exit(3);}return;
        }
        File root=Files.createTempDirectory("coinbot-history-").toFile();
        try{
            ImageFingerprint a=fingerprint(1),b=fingerprint(2);
            check(!a.pixels.equals(b.pixels),"different pixel hashes");check(!a.resembles(b),"different low-frequency layout accepted");
            check(a.resembles(a),"exact image rejected");
            int[] pixels=new int[64*64];
            for(int y=0;y<64;y++)for(int x=0;x<64;x++){int value=25+(x*3+y*2)%180;pixels[y*64+x]=0xff000000|value<<16|value<<8|value;}
            ImageFingerprint original=ImageFingerprint.of(pixels,64,64);
            int[] bright=pixels.clone();for(int i=0;i<bright.length;i++){int value=(bright[i]&255)+12;bright[i]=0xff000000|value<<16|value<<8|value;}
            check(original.resembles(ImageFingerprint.of(bright,64,64)),"brightness changes cannot bypass visual check");
            int[] recolor=pixels.clone();for(int i=0;i<recolor.length;i++){int value=recolor[i]&255;recolor[i]=0xff000000|value<<16|Math.min(255,value+10)<<8|value;}
            check(original.resembles(ImageFingerprint.of(recolor,64,64)),"cosmetic tint remains similar");
            int[] noise=pixels.clone();noise[1024]^=1;
            check(!original.pixels.equals(ImageFingerprint.of(noise,64,64).pixels)&&original.resembles(ImageFingerprint.of(noise,64,64)),"one pixel nonce does not manufacture a new image");
            int[] stamp=pixels.clone();for(int y=61;y<64;y++)for(int x=61;x<64;x++)stamp[y*64+x]=0xffffffff;
            check(original.resembles(ImageFingerprint.of(stamp,64,64)),"small corner stamp cannot bypass check");
            BufferedImage png=new BufferedImage(64,64,BufferedImage.TYPE_INT_ARGB);png.setRGB(0,0,64,64,pixels,0,64);
            File encoded=new File(root,"encoded.png");ImageIO.write(png,"png",encoded);BufferedImage decoded=ImageIO.read(encoded);
            check(original.pixels.equals(ImageFingerprint.of(decoded.getRGB(0,0,64,64,null,0,64),64,64).pixels),"digest is independent of PNG encoding");
            File folder=new File(root,"normal");
            try(ImageLedger history=new ImageLedger(folder)){
                check(history.fresh,"first run needs legacy import");history.rememberLegacy("old",original);history.persist();
                check(history.rejection("new",structure("different"),ImageFingerprint.of(bright,64,64),false)!=null,"legacy pictures participate in similarity gate");
                history.reserve("first",structure("first"),a,false);
            }
            try(ImageLedger history=new ImageLedger(folder)){
                check(!history.fresh&&history.size()==2,"history survives restart");
                check(history.rejection("other",structure("other"),a,false).equals("identical_pixels"),"same pixels blocked across drafts");
                check(history.rejection("other",structure("first"),b,false).equals("same_composition"),"same composition blocked even with very different colors/pixels");
                check(history.rejection("first",structure("first"),a,true)==null,"repair may restore same draft's own image");
                check(history.rejection("first",structure("first"),a,false)!=null,"restyle must also differ from own previous image");
                history.reserve("first",structure("first"),a,true);check(history.size()==2,"idempotent repair does not inflate history");
                history.reserve("second",structure("second"),b,false);
            }
            // Deleting unrelated draft files does not remove historical records.
            encoded.delete();try(ImageLedger history=new ImageLedger(folder)){check(history.size()==3&&history.rejection("future",structure("future"),a,false)!=null,"draft retention cannot release an old image");}
            byte[] good=Files.readAllBytes(new File(folder,"image-history.v1").toPath());
            byte[] altered=good.clone();altered[ImageLedger.HEADER.length()+2]^=1;damaged(folder,altered);
            damaged(folder,Arrays.copyOf(good,good.length-1));
            damaged(folder,ImageLedger.HEADER.getBytes("US-ASCII"));
            Files.write(new File(folder,"image-history.v1").toPath(),good);
            new File(folder,"image-history.v1").delete();
            try(ImageLedger history=new ImageLedger(folder)){throw new AssertionError("lost history reset silently");}catch(ImageGenerationException expected){checks++;}
            File crash=new File(root,"crash");try(ImageLedger history=new ImageLedger(crash)){history.reserve("reserved",structure("reserved"),a,false);}
            // Simulated crash before PNG publication: another draft must not reuse that reservation.
            try(ImageLedger history=new ImageLedger(crash)){check(history.rejection("other",structure("different"),a,false)!=null,"reservation survives failed PNG publication");check(history.rejection("reserved",structure("reserved"),a,true)==null,"original owner can recover after crash");}
            File race=new File(root,"race");race.mkdirs();String java=new File(System.getProperty("java.home"),"bin/java").getPath();
            Process one=new ProcessBuilder(java,"-cp",System.getProperty("java.class.path"),ImageUniquenessTest.class.getName(),race.getPath(),"one").start();
            Process two=new ProcessBuilder(java,"-cp",System.getProperty("java.class.path"),ImageUniquenessTest.class.getName(),race.getPath(),"two").start();
            int first=one.waitFor(),second=two.waitFor();check(first+second==3&&Math.min(first,second)==0,"two processes cannot both accept the same image");
            try(ImageLedger history=new ImageLedger(race)){check(history.size()==1,"concurrent history updates do not overwrite each other");}
            Core.Idea idea=Core.idea("Starship to Mars","42");Set<String> plans=new HashSet<>();
            for(int attempt=0;attempt<StoryPlan.MAX_ATTEMPTS;attempt++)plans.add(new StoryPlan(idea,"Starship to Mars","42",attempt).structure);
            check(plans.size()>=22,"retry changes substantive composition, not just hue");
            StoryPlan deterministic=new StoryPlan(idea,"Starship to Mars","42",4);
            check(deterministic.structure.equals(new StoryPlan(idea,"Starship to Mars","42",4).structure),"saved plan can be reproduced");
            check(new StoryPlan(idea,"Market crash","43",0).actor%4%2==1,"negative story chooses anxious/exhausted pose");
            check(new StoryPlan(idea,"Team wins","44",0).actor%4%2==0,"celebration chooses expressive positive pose");
            GeneratedArt evidence=new GeneratedArt(deterministic,a,4,9);
            check(evidence.metadata().get("image_attempt").equals(5)&&evidence.metadata().get("image_composition").equals(deterministic.structure),"metadata identifies actual accepted retry");
            check(evidence.metadata().get("image_pixel_sha256").equals(a.pixels),"metadata identifies actual pixels");
            BufferedImage actors=ImageIO.read(new File("app/assets/meme-actors-v4.png"));
            check(actors.getColorModel().hasAlpha(),"painted actors have a transparent layer");
            int transparent=0;for(int y=0;y<actors.getHeight();y+=8)for(int x=0;x<actors.getWidth();x+=8)if((actors.getRGB(x,y)>>>24)<10)transparent++;
            check(transparent>1000,"actor sheet is not an opaque background pretending to be transparent");
            int[] cutout=new int[400];for(int y=5;y<17;y++)for(int x=6;x<15;x++)cutout[y*20+x]=0xffffffff;cutout[20]=0xff00ff00;cutout[21]=0xff00ff00;
            int[] isolated=ActorCutout.isolate(cutout,20,20);
            check(isolated[20]==0&&isolated[10*20+10]==0xffffffff,"neighboring sprite fragments removed without flattening main figure");
            check(cutout[20]!=0,"source atlas pixels not mutated");
            check(isolated[0]==0,"transparency remains transparency");
            try{ActorCutout.isolate(new int[400],20,20);throw new AssertionError("empty figure accepted");}catch(IllegalArgumentException expected){checks++;}
            System.out.println(checks+" image uniqueness/history/story checks passed (host pixels/files; no Android renderer)");
        }finally{try(java.util.stream.Stream<Path> paths=Files.walk(root.toPath())){paths.sorted(Comparator.reverseOrder()).forEach(path->{try{Files.delete(path);}catch(IOException e){throw new RuntimeException(e);}});}}
    }
}
