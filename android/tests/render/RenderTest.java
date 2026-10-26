package dev.coinbot.mobile;

import android.content.Context;
import android.graphics.Bitmap;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Executes production Art/ScenePainter via a Java2D adapter, not an Android/device test. */
public final class RenderTest {
 static int checks;
 static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
 static ImageFingerprint capture(Bitmap image){int[] pixels=new int[768*768];image.getPixels(pixels,0,768,0,0,768,768);return ImageFingerprint.of(pixels,768,768);}
 public static void main(String[] args)throws Exception{
  File root=new File("build/render-previews");
  if(root.exists())try(java.util.stream.Stream<Path> paths=Files.walk(root.toPath())){paths.sorted(Comparator.reverseOrder()).forEach(p->{try{Files.delete(p);}catch(IOException e){throw new RuntimeException(e);}});}
  root.mkdirs();Context context=new Context(root);File drafts=new File(root,"drafts");drafts.mkdirs();
  Bitmap legacy=Bitmap.createBitmap(768,768,Bitmap.Config.ARGB_8888);android.graphics.Paint paint=new android.graphics.Paint(3);paint.setColor(android.graphics.Color.rgb(20,20,20));new android.graphics.Canvas(legacy).drawRect(0,0,768,768,paint);
  try(FileOutputStream out=new FileOutputStream(new File(drafts,"legacy.png"))){legacy.compress(Bitmap.CompressFormat.PNG,100,out);}legacy.recycle();
  Files.copy(new File(drafts,"legacy.png").toPath(),new File(drafts,"legacy.before-v3.png").toPath());
  Files.copy(new File(drafts,"legacy.png").toPath(),new File(drafts,"legacy2.png").toPath());
  Core.Idea idea=Core.idea("Starship to Mars","42");Set<String> hashes=new HashSet<>();GeneratedArt first=null;
  for(int i=0;i<20;i++){
   String id="sample_"+i;File image=new File(drafts,id+".png");GeneratedArt result=Art.save(context,idea,image,"Starship to Mars");
   if(first==null){first=result;check(result.historySize==3,"legacy files and backups indexed before first generation, same-owner backup deduplicated");}
   check(Art.valid(image),"valid generated PNG");check(hashes.add(result.fingerprint.pixels),"no accepted repeated pixels");
   check(result.plan.structure.equals(result.metadata().get("image_composition")),"accepted composition metadata");
  }
  File original=new File(drafts,"sample_0.png");original.delete();GeneratedArt restored=Art.save(context,idea,original,"Starship to Mars");
  check(restored.fingerprint.pixels.equals(first.fingerprint.pixels),"repair reproduces own image after restart");
  GeneratedArt restyled=Art.save(context,idea,original,"Starship to Mars",true);
  check(!restyled.fingerprint.pixels.equals(first.fingerprint.pixels)&&restyled.attempt>0,"restyle rejects its own previous pixels and chooses another plan");
  File exhausted=new File(root,"exhausted");exhausted.mkdirs();Context limited=new Context(exhausted);File target=DraftFiles.file(new File(exhausted,"drafts"),"blocked",".png");
  try(ImageLedger history=new ImageLedger(exhausted);ScenePainter painter=new ScenePainter(limited,new MemeScene(idea,"Starship to Mars"))){
   Bitmap image=Bitmap.createBitmap(768,768,Bitmap.Config.ARGB_8888);
   for(int attempt=0;attempt<StoryPlan.MAX_ATTEMPTS;attempt++){painter.paint(image,new StoryPlan(idea,"Starship to Mars","blocked",attempt));history.rememberLegacy("already_used_"+attempt,capture(image));}
   image.recycle();history.persist();
  }
  try{Art.save(limited,idea,target,"Starship to Mars");throw new AssertionError("exhausted pool accepted");}catch(ImageGenerationException expected){check(expected.getMessage().contains("24"),"exhaustion is explicit");}
  check(!target.exists(),"all candidates rejected means no published image");
  byte[] previous=Files.readAllBytes(original.toPath());Files.write(target.toPath(),previous);
  try{Art.save(limited,idea,target,"Starship to Mars",true);throw new AssertionError("exhausted restyle accepted");}catch(ImageGenerationException expected){}
  check(Arrays.equals(previous,Files.readAllBytes(target.toPath())),"failed restyle never replaces original PNG");
  Files.write(new File(root,"image-history.v1").toPath(),"broken".getBytes("UTF-8"));
  try{Art.save(context,idea,new File(drafts,"corrupt_history.png"),"Starship to Mars");throw new AssertionError("corrupt history accepted");}catch(ImageGenerationException expected){checks++;}
  check(!new File(drafts,"corrupt_history.png").exists(),"corrupt history cannot publish a new image");
  System.out.println(checks+" production generator pipeline checks passed using Java2D reference adapter; no Android renderer/device test");
 }
}
