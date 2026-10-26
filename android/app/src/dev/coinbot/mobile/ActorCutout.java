package dev.coinbot.mobile;

/** Keep the main connected transparent figure; exclude neighboring atlas-cell fragments. */
final class ActorCutout {
    static int[] isolate(int[] source,int width,int height){
        if(width<=0||height<=0||source.length!=width*height)throw new IllegalArgumentException("Invalid cutout");
        int[] labels=new int[source.length],queue=new int[source.length];int label=0,best=0,bestCount=0;
        for(int start=0;start<source.length;start++){
            if(labels[start]!=0||(source[start]>>>24)<16)continue;
            label++;int head=0,tail=1;queue[0]=start;labels[start]=label;
            while(head<tail){
                int at=queue[head++],x=at%width,y=at/width;
                if(x>0)tail=visit(at-1,label,source,labels,queue,tail);
                if(x+1<width)tail=visit(at+1,label,source,labels,queue,tail);
                if(y>0)tail=visit(at-width,label,source,labels,queue,tail);
                if(y+1<height)tail=visit(at+width,label,source,labels,queue,tail);
            }
            if(tail>bestCount){bestCount=tail;best=label;}
        }
        if(bestCount<width*height/50)throw new IllegalArgumentException("Figure missing");
        int[] result=source.clone();for(int i=0;i<result.length;i++)if(labels[i]!=best)result[i]=0;return result;
    }
    private static int visit(int at,int label,int[] source,int[] labels,int[] queue,int tail){
        if(labels[at]==0&&(source[at]>>>24)>=16){labels[at]=label;queue[tail++]=at;}return tail;
    }
}
