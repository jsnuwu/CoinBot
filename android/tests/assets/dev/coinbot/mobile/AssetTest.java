package dev.coinbot.mobile;

import android.content.Context;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import org.json.JSONObject;

public final class AssetTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception{
        File root=Files.createTempDirectory("coinbot-assets-").toFile();
        try{
            Context context=new Context(root);LocalStore store=new LocalStore(context);
            Core.Idea idea=Core.idea("Starship rocket to Mars","123");
            Map<String,String> row=new HashMap<>();row.put("id","123");row.put("url","https://x.com/elonmusk/status/123");store.data.put("123",row);
            check(DraftWriter.save(context,store,"123",idea,row.get("url"),true,"x_browser_linked",null,"notification"),"saved");
            File image=DraftAssets.file(context,"123",".png"),json=DraftAssets.file(context,"123",".json");
            check(PngChecks.valid(image),"complete PNG");check("ready".equals(row.get("image_status")),"ready after PNG checked");
            JSONObject meta=DraftAssets.read(json);check(meta.getBoolean("source_verified"),"verified metadata");
            check(meta.getString("image").equals("123.png"),"image belongs to id");check(meta.getString("source_text").equals("notification"),"source text preserved");
            meta.put("source_text","original post");meta.put("custom",new JSONObject().put("keep",true));DraftAssets.write(json,meta);
            byte[] original=Files.readAllBytes(image.toPath());
            Files.write(image.toPath(),Arrays.copyOf(original,original.length-4));check(!PngChecks.valid(image),"truncated IEND rejected");
            DraftAssets.repair(context,store,row);
            check(PngChecks.valid(image),"truncated image repaired");
            meta=DraftAssets.read(json);check(meta.getString("source_text").equals("original post"),"original text unchanged");
            check(meta.getJSONObject("custom").getBoolean("keep"),"custom metadata retained");check(meta.getBoolean("source_verified"),"verification retained");
            check("x_browser_linked".equals(row.get("source")),"SQL source retained");check(idea.name.equals(row.get("name")),"name unchanged");
            byte[] damage=original.clone();damage[40]^=1;Files.write(image.toPath(),damage);check(!PngChecks.valid(image),"CRC damage rejected");
            image.delete();json.delete();row.put("source","notification_unverified");
            DraftAssets.repair(context,store,row);meta=DraftAssets.read(json);
            check(PngChecks.valid(image),"missing image repaired");check(!meta.getBoolean("source_verified"),"unverified never promoted");check(!meta.getBoolean("publishable"),"not publishable");
            Files.write(json.toPath(),"{damaged source evidence".getBytes("UTF-8"));image.delete();
            DraftAssets.repair(context,store,row);
            check(PngChecks.valid(image),"image repaired despite bad JSON");check(new String(Files.readAllBytes(json.toPath()),"UTF-8").equals("{damaged source evidence"),"bad JSON evidence not overwritten");
            check(row.get("image_status").startsWith("Fehler"),"metadata error visible");
            json.delete();Art.fail=true;image.delete();DraftAssets.repair(context,store,row);
            check(row.get("image_status").startsWith("Fehler"),"failed repair is not ready");Art.fail=false;
            byte[] old="old file".getBytes("UTF-8");Files.write(json.toPath(),old);
            try{DraftFiles.replace(json,temp->{Files.write(temp.toPath(),"partial".getBytes("UTF-8"));throw new IOException("interrupted");});throw new AssertionError();}catch(IOException expected){}
            check(Arrays.equals(old,Files.readAllBytes(json.toPath())),"interrupted write preserves old file");
            check(Arrays.stream(json.getParentFile().listFiles()).noneMatch(f->f.getName().endsWith(".tmp")),"temporary files removed");
            try{DraftFiles.file(root,"../elsewhere",".png");throw new AssertionError();}catch(IOException expected){checks++;}
            ImageIO.write(new BufferedImage(10,10,BufferedImage.TYPE_INT_RGB),"png",image);check(!PngChecks.valid(image),"wrong dimensions rejected");
            image.delete();json.delete();Art.fail=true;
            try{DraftWriter.save(context,store,"123",idea,row.get("url"),false,"notification_unverified",null,"fresh text");throw new AssertionError();}catch(IOException expected){}
            check(!"ready".equals(row.get("image_status")),"write failure not marked successful");check(row.get("name").equals(idea.name),"failed draft survives for repair");
            check(DraftAssets.read(json).getString("notification_text").equals("fresh text"),"source saved before image failure");
            Art.fail=false;DraftAssets.repair(context,store,row);check("ready".equals(row.get("image_status")),"failed draft recoverable");
            JSONObject beforeRestyle=DraftAssets.read(json);beforeRestyle.put("source_verified",true);beforeRestyle.put("source_text","preserve confirmed original");beforeRestyle.put("custom","keep");DraftAssets.write(json,beforeRestyle);
            byte[] oldPng=Files.readAllBytes(image.toPath()),oldJson=Files.readAllBytes(json.toPath());
            DraftAssets.repair(context,store,row,true);
            check(Arrays.equals(oldPng,Files.readAllBytes(DraftAssets.file(context,"123",".before-v3.png").toPath())),"restyle backs up original image");
            check(Arrays.equals(oldJson,Files.readAllBytes(DraftAssets.file(context,"123",".before-v3.json").toPath())),"restyle backs up original metadata");
            JSONObject restyled=DraftAssets.read(json);
            check(restyled.getString("image_generator").equals(StoryPlan.GENERATOR),"new compositor provenance");
            check(restyled.getString("source_text").equals("preserve confirmed original")&&restyled.getBoolean("source_verified")&&restyled.getString("custom").equals("keep"),"restyle leaves source evidence and custom data intact");
            DraftAssets.repair(context,store,row,true);
            check(Arrays.equals(oldJson,Files.readAllBytes(DraftAssets.file(context,"123",".before-v3.json").toPath())),"repeated restyle never overwrites original backup");
            Files.write(json.toPath(),"{broken original evidence".getBytes("UTF-8"));
            byte[] preservedImage=Files.readAllBytes(image.toPath());DraftAssets.repair(context,store,row,true);
            check(Arrays.equals(preservedImage,Files.readAllBytes(image.toPath())),"restyle refuses to overwrite valid image with broken metadata");
            check(new String(Files.readAllBytes(json.toPath()),"UTF-8").equals("{broken original evidence"),"restyle does not erase malformed source evidence");
            Map<String,String> blocked=new HashMap<>();blocked.put("id","blocked");store.data.put("blocked",blocked);Art.exhausted=true;
            try{DraftWriter.save(context,store,"blocked",idea,"",false,"notification_unverified",null,"Starship");throw new AssertionError("duplicate pool was accepted");}catch(ImageGenerationException expected){}
            check(blocked.get("image_status").contains("24")&&!"ready".equals(blocked.get("image_status")),"duplicate exhaustion remains visible instead of successful image status");
            check(!DraftAssets.file(context,"blocked",".png").exists()&&DraftAssets.read(DraftAssets.file(context,"blocked",".json")).optString("image_status").equals("pending"),"failed image has no PNG or ready metadata");
            Art.exhausted=false;
            Set<String> styles=new HashSet<>();for(int i=0;i<30;i++){ArtStyle style=new ArtStyle(Core.idea("Rocket to Mars",String.valueOf(i)));styles.add(style.hue+":"+style.layout+":"+style.decoration);}
            check(styles.size()>20,"per-draft visual variation");
            for(String[] example:new String[][]{{"transport","New airport flight","aircraft"},{"finance","Bitcoin markets","crypto"},{"weather","Snow in Florida","snow"},{"weather","Heat and sunshine","sun"},{"politics","Iran elections","ballot"},{"transport","Railway to Seoul","transport"}}){
                check(new ArtStyle(new Core.Idea("Topic","TOPIC",example[0]),example[1]).motif.equals(example[2]),"content-specific motif "+example[2]);
            }
            check(Core.accounts(Core.DEFAULT_ACCOUNTS).size()==8,"eight default accounts");
            System.out.println(checks+" asset checks passed (host PNG / in-memory DB doubles; no Android graphics/device test)");
        }finally{try(java.util.stream.Stream<Path> paths=Files.walk(root.toPath())){paths.sorted(Comparator.reverseOrder()).forEach(p->{try{Files.delete(p);}catch(IOException e){throw new RuntimeException(e);}});}}
    }
}
