package dev.coinbot.mobile;

import android.graphics.*;
import java.io.*;

final class Art {
    static void save(Core.Idea idea,File target)throws IOException{save(idea,target,"");}
    static void save(Core.Idea idea, File target,String text) throws IOException {
        Bitmap image = Bitmap.createBitmap(768, 768, Bitmap.Config.ARGB_8888);
        try {
            Canvas c = new Canvas(image); Paint p = new Paint(3);
            ArtStyle style=new ArtStyle(idea,text);
            int accent=Color.HSVToColor(new float[]{style.hue,.48f,.94f});
            int secondary=Color.HSVToColor(new float[]{(style.hue+65)%360,.60f,.98f});
            c.drawColor(Color.HSVToColor(new float[]{style.hue,.48f,.12f}));
            p.setShader(new LinearGradient(0,100,768,650,Color.HSVToColor(new float[]{style.hue,.55f,.30f}),Color.rgb(16,20,30),Shader.TileMode.CLAMP));
            if(style.layout==0)c.drawCircle(384,310,270,p);
            else if(style.layout==1)c.drawRoundRect(95,90,673,565,85,85,p);
            else {c.save();c.rotate(-9,384,320);c.drawRoundRect(105,105,663,545,35,35,p);c.restore();}
            p.setShader(null);p.setColor(accent);p.setAlpha(55);
            for(int i=0;i<9;i++){
                float x=55+(i*137+style.hue)%660,y=120+(i*83)%400;
                if(style.decoration==0)c.drawCircle(x,y,5+i%3*3,p);
                else if(style.decoration==1)c.drawLine(x,y,x+24,y-24,p);
                else c.drawRect(x,y,x+9,y+9,p);
            }
            p.setAlpha(255);
            c.save();c.translate(style.layout==1?-12:style.layout==2?12:0,0);
            int violet = accent, gold = ("sun".equals(style.motif)||"gold".equals(idea.theme))?Color.rgb(255,194,74):secondary, cream = Color.rgb(252, 239, 215), ink = Color.rgb(24, 20, 34);
            switch (style.motif) {
                case "space":
                    poly(c,p,gold,335,420,384,550,433,420); poly(c,p,violet,310,300,230,450,335,420);
                    poly(c,p,violet,458,300,538,450,433,420); rect(c,p,cream,300,190,468,440,70);
                    poly(c,p,violet,300,230,384,95,468,230); circle(c,p,ink,384,294,60); circle(c,p,Color.CYAN,384,294,43); break;
                case "coffee":
                    circle(c,p,cream,515,327,75); circle(c,p,ink,515,327,49); rect(c,p,cream,225,225,510,450,30);
                    oval(c,p,gold,230,210,505,270); oval(c,p,Color.rgb(99,53,45),245,220,490,255); eyes(c,p,315,350); break;
                case "robot":
                    rect(c,p,cream,378,135,390,230,0);circle(c,p,gold,384,130,20);rect(c,p,violet,210,225,558,470,45);
                    rect(c,p,ink,250,270,518,365,18);circle(c,p,Color.CYAN,315,315,18);circle(c,p,Color.CYAN,453,315,18);
                    rect(c,p,cream,310,400,458,423,8);break;
                case "dog": case "cat":
                    if (idea.theme.equals("cat")) {poly(c,p,violet,230,280,220,125,340,210);poly(c,p,violet,428,210,548,125,538,280);}
                    else {oval(c,p,gold,160,210,310,470);oval(c,p,gold,458,210,608,470);}
                    oval(c,p,idea.theme.equals("cat")?violet:gold,225,184,543,483);eyes(c,p,300,345);oval(c,p,ink,350,345,418,385);break;
                case "pizza":
                    poly(c,p,gold,220,190,568,190,384,520);rect(c,p,Color.rgb(184,110,65),215,170,575,205,8);
                    circle(c,p,Color.rgb(210,75,65),300,250,25);circle(c,p,Color.rgb(210,75,65),458,270,25);eyes(c,p,315,350);break;
                case "penguin":
                    oval(c,p,violet,210,130,558,510);oval(c,p,ink,225,140,543,495);oval(c,p,cream,270,250,498,495);
                    circle(c,p,Color.WHITE,330,255,20);circle(c,p,Color.WHITE,438,255,20);poly(c,p,gold,355,312,413,312,384,348);break;
                case "taco":
                    oval(c,p,Color.rgb(103,185,112),180,200,588,460);circle(c,p,Color.RED,300,245,20);circle(c,p,Color.RED,465,255,20);
                    oval(c,p,gold,175,270,593,505);eyes(c,p,345,380);break;
                case "gold":
                    oval(c,p,gold,235,290,525,480);rect(c,p,gold,420,190,490,400,35);circle(c,p,gold,455,195,65);
                    poly(c,p,Color.rgb(245,125,60),505,190,580,220,505,245);circle(c,p,ink,470,180,10);break;
                case "tariff":
                    rect(c,p,Color.rgb(194,134,84),225,240,543,475,0);poly(c,p,gold,225,240,170,180,325,125,384,170);
                    poly(c,p,violet,384,170,445,125,598,180,543,240);eyes(c,p,335,385);break;
                case "aircraft":
                    poly(c,p,cream,384,130,416,300,580,405,574,440,410,380,410,465,460,500,455,520,384,495,313,520,308,500,358,465,358,380,194,440,188,405,352,300);
                    rect(c,p,violet,371,175,397,265,12);break;
                case "crypto":
                    circle(c,p,gold,315,290,115);circle(c,p,ink,315,290,85);circle(c,p,gold,315,290,68);
                    circle(c,p,violet,460,390,105);circle(c,p,ink,460,390,75);circle(c,p,violet,460,390,57);
                    rect(c,p,cream,270,282,360,298,6);rect(c,p,cream,415,382,505,398,6);break;
                case "ballot":
                    rect(c,p,violet,230,300,538,490,20);rect(c,p,ink,300,323,465,342,5);
                    c.save();c.rotate(-12,384,235);rect(c,p,cream,300,145,465,315,8);
                    p.setColor(ink);p.setStrokeWidth(12);c.drawLine(333,226,365,260,p);c.drawLine(365,260,431,190,p);c.restore();break;
                case "snow":
                    circle(c,p,cream,315,245,68);circle(c,p,cream,410,220,90);circle(c,p,cream,485,265,65);rect(c,p,cream,250,250,530,310,25);
                    p.setColor(violet);p.setStrokeWidth(8);
                    for(int x=270;x<520;x+=110){c.drawLine(x,370,x,475,p);c.drawLine(x-40,395,x+40,450,p);c.drawLine(x+40,395,x-40,450,p);}break;
                case "sun":
                    circle(c,p,gold,384,310,110);p.setColor(gold);p.setStrokeWidth(18);
                    for(int i=0;i<10;i++){double a=i*Math.PI/5;c.drawLine(384+(float)Math.cos(a)*145,310+(float)Math.sin(a)*145,384+(float)Math.cos(a)*190,310+(float)Math.sin(a)*190,p);}break;
                case "news":
                    rect(c,p,cream,195,160,573,475,16);rect(c,p,violet,220,185,548,235,4);
                    for(int y=270;y<440;y+=38){rect(c,p,ink,225,y,375,y+9,2);rect(c,p,ink,408,y,540,y+9,2);}break;
                case "politics":
                    poly(c,p,cream,180,245,384,130,588,245);rect(c,p,violet,180,450,588,485,4);
                    for(int x=240;x<=510;x+=90)rect(c,p,cream,x,260,x+30,448,5);break;
                case "finance":
                    rect(c,p,violet,210,350,290,475,8);rect(c,p,cream,330,270,410,475,8);rect(c,p,gold,450,180,530,475,8);
                    rect(c,p,cream,190,490,565,500,3);break;
                case "sports":
                    circle(c,p,cream,384,320,170);circle(c,p,ink,384,320,55);
                    for(int i=0;i<5;i++){double a=i*Math.PI*2/5;circle(c,p,ink,384+(float)Math.cos(a)*125,320+(float)Math.sin(a)*125,24);}break;
                case "transport":
                    rect(c,p,violet,230,140,538,465,45);rect(c,p,Color.CYAN,265,190,503,315,12);
                    circle(c,p,gold,295,385,22);circle(c,p,gold,473,385,22);rect(c,p,cream,255,475,285,525,0);rect(c,p,cream,483,475,513,525,0);break;
                case "science":
                    p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(12);p.setColor(violet);
                    for(int i=0;i<3;i++){c.save();c.rotate(i*60,384,320);c.drawOval(205,255,563,385,p);c.restore();}
                    p.setStyle(Paint.Style.FILL);circle(c,p,gold,384,320,36);break;
                case "weather":
                    circle(c,p,gold,290,230,85);circle(c,p,cream,340,330,86);circle(c,p,cream,435,290,100);circle(c,p,cream,515,345,70);
                    rect(c,p,cream,265,330,545,410,18);for(int x=300;x<530;x+=75)rect(c,p,Color.CYAN,x,440,x+12,495,6);break;
                case "health":
                    circle(c,p,violet,384,320,170);rect(c,p,cream,350,200,418,440,10);rect(c,p,cream,265,285,503,355,10);break;
                case "technology":
                    rect(c,p,violet,260,195,508,445,20);rect(c,p,ink,290,225,478,415,10);
                    for(int x=285;x<500;x+=50){rect(c,p,cream,x,155,x+15,195,0);rect(c,p,cream,x,445,x+15,485,0);}
                    for(int y=235;y<420;y+=50){rect(c,p,cream,220,y,260,y+15,0);rect(c,p,cream,508,y,548,y+15,0);}break;
                default:circle(c,p,violet,384,310,180);eyes(c,p,300,345);
            }
            c.restore();
            if(java.util.Arrays.asList("news","politics","finance","sports","transport","science","weather","health","technology").contains(idea.theme)){
                String subject=idea.name.split(" ")[0];p.setTextAlign(Paint.Align.CENTER);p.setColor(cream);p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));p.setTextSize(34);
                while(p.measureText(subject)>610)p.setTextSize(p.getTextSize()-2);c.drawText(subject,384,550,p);
            }
            p.setColor(Color.WHITE);p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));p.setTextSize(32);
            while(p.measureText(idea.name)>690)p.setTextSize(p.getTextSize()-2);c.drawText(idea.name,384,65,p);p.setTextSize(54);
            while(p.measureText(idea.ticker)>650)p.setTextSize(p.getTextSize()-2);
            c.drawText(idea.ticker,384,620,p);p.setTypeface(Typeface.DEFAULT);p.setTextSize(22);p.setColor(Color.LTGRAY);c.drawText("UNOFFICIAL COMMUNITY MEME",384,690,p);
            DraftFiles.replace(target,temp->{
                try(FileOutputStream out=new FileOutputStream(temp)){
                    if(!image.compress(Bitmap.CompressFormat.PNG,100,out))throw new IOException("PNG compression failed");
                    out.getFD().sync();
                }
                if(!valid(temp))throw new IOException("PNG validation failed");
            });
        } catch(IOException e){throw e;} catch(Exception e){throw new IOException("Image could not be saved",e);}
        finally{image.recycle();}
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
    private static void circle(Canvas c,Paint p,int color,float x,float y,float r){p.setColor(color);c.drawCircle(x,y,r,p);}
    private static void oval(Canvas c,Paint p,int color,float l,float t,float r,float b){p.setColor(color);c.drawOval(l,t,r,b,p);}
    private static void rect(Canvas c,Paint p,int color,float l,float t,float r,float b,float radius){p.setColor(color);c.drawRoundRect(l,t,r,b,radius,radius,p);}
    private static void poly(Canvas c,Paint p,int color,float... xy){Path path=new Path();path.moveTo(xy[0],xy[1]);for(int i=2;i<xy.length;i+=2)path.lineTo(xy[i],xy[i+1]);path.close();p.setColor(color);c.drawPath(path,p);}
    private static void eyes(Canvas c,Paint p,float y,float mouth){circle(c,p,Color.rgb(24,20,34),315,y,15);circle(c,p,Color.rgb(24,20,34),453,y,15);rect(c,p,Color.rgb(24,20,34),352,mouth,416,mouth+10,5);}
}
