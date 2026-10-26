package dev.coinbot.mobile;

import java.nio.charset.StandardCharsets;
import java.util.*;

/** Local scene grammar. Structural identity deliberately excludes colors, lighting, text, and nonce. */
final class StoryPlan {
    static final String GENERATOR="local_storyboard_v4";
    static final int MAX_ATTEMPTS=24;
    final MemeScene scene;
    final long seed;
    final int actor,companion,layout,environment,prop,hue;
    final float angle;
    final String structure;
    StoryPlan(Core.Idea idea,String text,String owner,int attempt){
        scene=new MemeScene(idea,text);
        String hash=ImageFingerprint.sha((owner+"|"+idea.name+"|"+text+"|"+attempt+"|v4").getBytes(StandardCharsets.UTF_8));
        seed=Long.parseUnsignedLong(hash.substring(0,16),16);Random random=new Random(seed);
        String theme=idea.theme,words=(text==null?"":text).toLowerCase(Locale.ROOT);
        int family="cat".equals(theme)?2:"robot".equals(theme)||"technology".equals(theme)?3:"coffee".equals(theme)||"news".equals(theme)?1:random.nextInt(2);
        int mood=random.nextInt(4);
        if(words.matches("(?s).*\\b(crash|fall|war|storm|panic|krieg|sturm)\\b.*"))mood=random.nextBoolean()?1:3;
        if(words.matches("(?s).*\\b(wins|win|victory|celebrate|sieg|feiern)\\b.*"))mood=random.nextBoolean()?0:2;
        actor=family*4+mood;
        companion=random.nextBoolean()?-1:((family+1+random.nextInt(3))%4)*4+random.nextInt(4);
        layout=random.nextInt(6);
        int[] stages="space".equals(theme)||"science".equals(theme)?new int[]{0,3,4}:
            "finance".equals(theme)||"gold".equals(theme)?new int[]{2,5}:"health".equals(theme)||"politics".equals(theme)?new int[]{1,3,2}:
            "transport".equals(theme)?new int[]{2,5}:"weather".equals(theme)?new int[]{4,5}:new int[]{0,1,2,3,4,5};
        environment=stages[random.nextInt(stages.length)];
        int[] props="space".equals(theme)||"science".equals(theme)?new int[]{0,1,6}:"coffee".equals(theme)?new int[]{2,4,0}:new int[]{0,1,2,3,5,6,7};
        prop=props[random.nextInt(props.length)];hue=random.nextInt(360);angle=(random.nextInt(5)-2)*7;
        // A different hue, crop, random seed, or small prop position is never sufficient by itself.
        structure=ImageFingerprint.sha(("v4|"+scene.name+"|"+actor+"|"+companion+"|"+layout+"|"+environment+"|"+prop).getBytes(StandardCharsets.US_ASCII));
    }
    float[] heroBox(){
        switch(layout){
            case 0:return new float[]{20,210,560,750};
            case 1:return new float[]{210,175,755,735};
            case 2:return new float[]{85,50,685,650};
            case 3:return new float[]{125,195,735,805};
            case 4:return new float[]{-10,90,535,635};
            default:return new float[]{185,70,755,640};
        }
    }
}
