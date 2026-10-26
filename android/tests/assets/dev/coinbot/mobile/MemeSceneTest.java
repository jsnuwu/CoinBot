package dev.coinbot.mobile;

import java.io.File;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.util.*;

/** Host asset geometry and routing checks, not Android Canvas tests. */
public final class MemeSceneTest {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception{
        String[] themes={"space","coffee","dog","cat","robot","pizza","taco","penguin","gold","tariff","news","politics","finance","sports","science","weather","transport","health","technology"};
        Set<String> mapped=new HashSet<>();Map<String,BufferedImage> sheets=new HashMap<>();
        for(String theme:themes){
            MemeScene scene=new MemeScene(new Core.Idea("Test","TEST",theme),"");
            if(!sheets.containsKey(scene.asset))sheets.put(scene.asset,ImageIO.read(new File("app/assets",scene.asset)));
            BufferedImage image=sheets.get(scene.asset);
            check(image!=null,"bundled artwork decodes");
            int[] b=scene.bounds(image.getWidth(),image.getHeight());
            check(b[0]>=0&&b[1]>=0&&b[2]<=image.getWidth()&&b[3]<=image.getHeight(),"crop inside asset");
            check(b[2]-b[0]>270&&b[3]-b[1]>270,"sufficient source detail");
            check(scene.tile>=0&&scene.tile<scene.grid*scene.grid,"valid grid tile");
            mapped.add(scene.asset+":"+scene.tile);
        }
        MemeScene flight=new MemeScene(new Core.Idea("Airport","AIR","transport"),"flight");
        check(flight.name.equals("flying_koi"),"flight gets aviation scene");
        mapped.add(flight.asset+":"+flight.tile);
        check(mapped.size()==20,"twenty distinct topic scenes");
        check(sheets.size()==2,"only bundled image assets needed");
        try{flight.bounds(100,100);throw new AssertionError("bad dimensions accepted");}catch(IllegalArgumentException expected){checks++;}
        System.out.println(checks+" bundled scene/routing/geometry checks passed (host ImageIO; no Android renderer)");
    }
}
