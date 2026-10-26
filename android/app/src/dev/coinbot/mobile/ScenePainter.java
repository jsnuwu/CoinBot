package dev.coinbot.mobile;

import android.content.Context;
import android.graphics.*;
import java.io.*;
import java.util.Random;

/** Procedural stage, props and multi-actor layouts, with bundled painted transparent figures. */
final class ScenePainter implements AutoCloseable {
    private final Bitmap background,actors;
    private final Bitmap[] cutouts=new Bitmap[16];
    ScenePainter(Context context,MemeScene scene)throws IOException{
        Bitmap first=null,second=null;
        try{
            try(InputStream in=context.getAssets().open("meme-stages-v4.png")){first=BitmapFactory.decodeStream(in);}
            try(InputStream in=context.getAssets().open("meme-actors-v4.png")){second=BitmapFactory.decodeStream(in);}
            if(first==null||second==null||!second.hasAlpha()||second.getWidth()<800||second.getHeight()<800)throw new IOException("Scene components unavailable");
            if(first.getWidth()<900||first.getHeight()<600)throw new IOException("Stage artwork unavailable");background=first;actors=second;
        }catch(Exception e){if(first!=null)first.recycle();if(second!=null)second.recycle();throw new ImageGenerationException("Bildbausteine nicht lesbar; kein neues Bild erstellt",e);}
    }
    void paint(Bitmap image,StoryPlan plan){
        Canvas c=new Canvas(image);Paint p=new Paint(3);Random random=new Random(plan.seed);
        int primary=Color.HSVToColor(new float[]{plan.hue,.70f,.85f}),secondary=Color.HSVToColor(new float[]{(plan.hue+115)%360,.65f,.95f});
        p.setShader(new LinearGradient(0,0,768,768,Color.rgb(20,13,43),primary,Shader.TileMode.CLAMP));c.drawRect(0,0,768,768,p);p.setShader(null);
        int x=plan.environment%3,y=plan.environment/3;
        int[] b={x*background.getWidth()/3+2,y*background.getHeight()/2+2,(x+1)*background.getWidth()/3-2,(y+1)*background.getHeight()/2-2};p.setAlpha(240);
        c.drawBitmap(background,new Rect(b[0],b[1],b[2],b[3]),new RectF(0,0,768,768),p);p.setAlpha(255);
        environment(c,p,plan,primary,secondary,random);
        float[] hero=plan.heroBox();float propX=(hero[0]+hero[2])/2<384?575:170;
        prop(c,p,plan.prop,propX,330,primary,secondary);
        if(plan.companion>=0){
            RectF companion=(hero[0]+hero[2])/2<384?new RectF(460,20,770,330):new RectF(-15,340,310,665);
            actor(c,p,plan.companion,companion,-plan.angle*.6f);
        }
        actor(c,p,plan.actor,new RectF(hero[0],hero[1],hero[2],hero[3]),plan.angle);
        p.setShader(new RadialGradient(384,340,540,new int[]{Color.TRANSPARENT,Color.argb(65,5,0,20)},new float[]{.5f,1},Shader.TileMode.CLAMP));
        c.drawRect(0,0,768,768,p);p.setShader(null);
    }
    private void actor(Canvas c,Paint p,int tile,RectF box,float angle){
        p.setAlpha(255);p.setShader(null);p.setStyle(Paint.Style.FILL);
        Bitmap figure=cutouts[tile];
        if(figure==null){
            int x=tile%4,y=tile/4,l=x*actors.getWidth()/4,t=y*actors.getHeight()/4,w=(x+1)*actors.getWidth()/4-l,h=(y+1)*actors.getHeight()/4-t;
            int[] pixels=new int[w*h];actors.getPixels(pixels,0,w,l,t,w,h);
            int[] isolated=ActorCutout.isolate(pixels,w,h);figure=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);figure.setPixels(isolated,0,w,0,0,w,h);cutouts[tile]=figure;
        }
        Rect source=new Rect(0,0,figure.getWidth(),figure.getHeight());
        float scale=Math.min(box.width()/source.width(),box.height()/source.height());
        RectF target=new RectF(box.centerX()-source.width()*scale/2,box.centerY()-source.height()*scale/2,box.centerX()+source.width()*scale/2,box.centerY()+source.height()*scale/2);
        c.save();c.rotate(angle,box.centerX(),box.centerY());c.drawBitmap(figure,source,target,p);c.restore();
    }
    private static void environment(Canvas c,Paint p,StoryPlan plan,int a,int b,Random random){
        p.setColor(Color.argb(150,8,5,30));
        switch(plan.environment){
            case 0:
                c.drawCircle(384,340,280,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(14);p.setColor((a&0xffffff)|0x70000000);
                for(int i=0;i<3;i++){c.save();c.rotate(i*42-30,384,340);c.drawOval(25,200,740,470,p);c.restore();}p.setStyle(Paint.Style.FILL);break;
            case 1:
                for(int i=0;i<5;i++){
                    float x=40+i*155,y=100+(i%2)*410;
                    poly(c,p,Color.argb(210,35,22,65),x-70,y,x+75,y-10,x+25,y+100);oval(c,p,b,x-70,y-23,x+75,y+15);
                }break;
            case 2:
                c.save();c.rotate(plan.layout%2==0?-15:15,384,500);
                for(int i=0;i<10;i++){float x=i*90-60,top=280+random.nextInt(330);rect(c,p,Color.argb(215,14,10,42),x,top,x+72,900);
                    for(int y=(int)top+25;y<740;y+=60)rect(c,p,a,x+20,y,x+37,y+20);}
                c.restore();break;
            case 3:
                c.save();c.rotate(25,384,384);for(int row=0;row<6;row++)for(int col=0;col<6;col++){
                    p.setColor((row+col)%2==0?Color.argb(45,8,2,28):Color.argb(25,255,255,255));c.drawRect(col*160-100,row*160-100,col*160+60,row*160+60,p);
                }c.restore();break;
            case 4:
                for(int i=0;i<5;i++){float x=80+i*155;poly(c,p,Color.argb(80,220,235,255),x,0,x+70,190,x+20,175,x+100,390,x-30,190,x+15,205);}
                break;
            default:
                for(int i=0;i<4;i++){
                    p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(35);p.setColor(i%2==0?a:b);c.drawArc(-100+i*80,-80+i*90,850-i*40,900-i*40,180,210,false,p);
                }p.setStyle(Paint.Style.FILL);break;
        }
        for(int i=0;i<18;i++){p.setColor(Color.argb(160,250,240,220));c.drawCircle(random.nextInt(768),random.nextInt(768),2+random.nextInt(4),p);}
    }
    private static void prop(Canvas c,Paint p,int kind,float x,float y,int a,int b){
        switch(kind){
            case 0:
                p.setShader(new RadialGradient(x,y,180,new int[]{Color.argb(220,12,3,40),a,Color.TRANSPARENT},new float[]{.55f,.75f,1},Shader.TileMode.CLAMP));
                c.drawCircle(x,y,180,p);p.setShader(null);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(14);p.setColor(b);c.drawOval(x-120,y-155,x+120,y+155,p);p.setStyle(Paint.Style.FILL);break;
            case 1:
                p.setShader(new RadialGradient(x-45,y-40,180,new int[]{Color.WHITE,b,Color.rgb(15,5,45)},null,Shader.TileMode.CLAMP));c.drawCircle(x,y,125,p);p.setShader(null);
                p.setStyle(Paint.Style.STROKE);p.setColor(a);p.setStrokeWidth(12);c.save();c.rotate(-25,x,y);c.drawOval(x-200,y-45,x+200,y+45,p);c.restore();p.setStyle(Paint.Style.FILL);break;
            case 2:
                for(int i=0;i<4;i++){float xx=x-100+i*65,yy=y+90+(i%2)*50;rect(c,p,Color.rgb(240,218,180),xx-13,yy,xx+13,yy+110);
                    p.setShader(new RadialGradient(xx-12,yy-20,75,Color.WHITE,i%2==0?a:b,Shader.TileMode.CLAMP));c.drawOval(xx-65,yy-55,xx+65,yy+20,p);p.setShader(null);}
                break;
            case 3:
                for(int i=0;i<8;i++){float xx=x-130+i*33,yy=y+180-i*45;poly(c,p,i%2==0?a:b,xx,yy,xx+115,yy-20,xx+145,yy+10,xx+30,yy+32);rect(c,p,Color.rgb(35,15,60),xx+30,yy+32,xx+145,yy+48);}
                break;
            case 4:
                for(int i=0;i<4;i++){Path wave=new Path();float yy=y+i*60;wave.moveTo(x-230,yy+90);wave.cubicTo(x-100,yy-150,x+110,yy+150,x+230,yy-30);wave.lineTo(x+260,yy+130);wave.lineTo(x-230,yy+150);wave.close();p.setColor(i%2==0?Color.rgb(95,43,25):Color.rgb(237,184,119));c.drawPath(wave,p);}
                break;
            case 5:
                for(int i=0;i<5;i++){float xx=x-130+i*65,yy=y+(i%2)*130;poly(c,p,i%2==0?a:b,xx,yy-110,xx+40,yy,xx,yy+100,xx-35,yy);}
                break;
            case 6:
                for(int i=0;i<3;i++){float xx=x+(i-1)*80,yy=y+(i%2)*100;
                    p.setShader(new RadialGradient(xx-20,yy-25,100,Color.WHITE,i%2==0?a:b,Shader.TileMode.CLAMP));c.drawCircle(xx,yy,75,p);p.setShader(null);
                    oval(c,p,Color.rgb(20,10,30),xx-30,yy-15,xx-18,yy+8);oval(c,p,Color.rgb(20,10,30),xx+18,yy-15,xx+30,yy+8);}
                break;
            default:
                poly(c,p,Color.rgb(248,240,225),x-180,y-80,x+170,y-155,x+65,y+130,x-10,y+20);
                poly(c,p,b,x-180,y-80,x+40,y-20,x+65,y+130,x-10,y+20);break;
        }
    }
    private static void poly(Canvas c,Paint p,int color,float... points){Path path=new Path();path.moveTo(points[0],points[1]);for(int i=2;i<points.length;i+=2)path.lineTo(points[i],points[i+1]);path.close();p.setColor(color);c.drawPath(path,p);}
    private static void oval(Canvas c,Paint p,int color,float l,float t,float r,float b){p.setColor(color);c.drawOval(l,t,r,b,p);}
    private static void rect(Canvas c,Paint p,int color,float l,float t,float r,float b){p.setColor(color);c.drawRoundRect(l,t,r,b,8,8,p);}
    public void close(){background.recycle();actors.recycle();for(Bitmap cutout:cutouts)if(cutout!=null)cutout.recycle();}
}
