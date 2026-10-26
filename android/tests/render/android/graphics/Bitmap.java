package android.graphics;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;
public final class Bitmap {
 public enum Config{ARGB_8888} public enum CompressFormat{PNG}
 final BufferedImage image;
 Bitmap(BufferedImage image){this.image=image;}
 public static Bitmap createBitmap(int w,int h,Config config){return new Bitmap(new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB));}
 public int getWidth(){return image.getWidth();} public int getHeight(){return image.getHeight();}
 public boolean hasAlpha(){return image.getColorModel().hasAlpha();}
 public void getPixels(int[] pixels,int offset,int stride,int x,int y,int w,int h){image.getRGB(x,y,w,h,pixels,offset,stride);}
 public void setPixels(int[] pixels,int offset,int stride,int x,int y,int w,int h){image.setRGB(x,y,w,h,pixels,offset,stride);}
 public boolean compress(CompressFormat f,int quality,OutputStream out){try{return ImageIO.write(image,"png",out);}catch(IOException e){return false;}}
 public void recycle(){image.flush();}
}
