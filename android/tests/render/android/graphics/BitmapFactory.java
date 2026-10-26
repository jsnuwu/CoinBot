package android.graphics;
import java.io.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
public final class BitmapFactory {
 public static final class Options{public boolean inJustDecodeBounds;public int outWidth,outHeight,inSampleSize=1;public String outMimeType;}
 public static Bitmap decodeStream(InputStream stream){try{BufferedImage image=ImageIO.read(stream);return image==null?null:new Bitmap(image);}catch(IOException e){return null;}}
 public static Bitmap decodeFile(String file,Options options){
  try{BufferedImage image=ImageIO.read(new File(file));if(image==null)return null;
   options.outWidth=image.getWidth();options.outHeight=image.getHeight();options.outMimeType="image/png";
   if(options.inJustDecodeBounds)return null;
   if(options.inSampleSize>1){BufferedImage smaller=new BufferedImage(image.getWidth()/options.inSampleSize,image.getHeight()/options.inSampleSize,BufferedImage.TYPE_INT_ARGB);Graphics2D g=smaller.createGraphics();g.drawImage(image,0,0,smaller.getWidth(),smaller.getHeight(),null);g.dispose();image=smaller;}
   return new Bitmap(image);
  }catch(IOException e){return null;}
 }
}
