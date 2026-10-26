package android.graphics;
import java.awt.*;
import java.awt.geom.*;
import java.util.*;
/** Java2D reference adapter only. It is not Android Skia and is never shipped in the APK. */
public final class Canvas {
 final Graphics2D graphics;final Deque<AffineTransform> saved=new ArrayDeque<>();
 public Canvas(Bitmap bitmap){graphics=bitmap.image.createGraphics();graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);}
 void shape(java.awt.Shape shape,Paint paint){
  if(paint.shader!=null){graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,(paint.color>>>24)/255f));graphics.setPaint(paint.shader.paint());}
  else{graphics.setComposite(AlphaComposite.SrcOver);graphics.setPaint(new java.awt.Color(paint.color,true));}
  graphics.setStroke(new BasicStroke(paint.width));if(paint.style==Paint.Style.STROKE)graphics.draw(shape);else graphics.fill(shape);
 }
 public void save(){saved.push(graphics.getTransform());}public void restore(){graphics.setTransform(saved.pop());}
 public void rotate(float angle,float x,float y){graphics.rotate(Math.toRadians(angle),x,y);}
 public void drawRect(float l,float t,float r,float b,Paint p){shape(new Rectangle2D.Float(l,t,r-l,b-t),p);}
 public void drawRoundRect(float l,float t,float r,float b,float rx,float ry,Paint p){shape(new RoundRectangle2D.Float(l,t,r-l,b-t,rx*2,ry*2),p);}
 public void drawOval(float l,float t,float r,float b,Paint p){shape(new Ellipse2D.Float(l,t,r-l,b-t),p);}
 public void drawCircle(float x,float y,float radius,Paint p){drawOval(x-radius,y-radius,x+radius,y+radius,p);}
 public void drawArc(float l,float t,float r,float b,float start,float sweep,boolean center,Paint p){shape(new Arc2D.Float(l,t,r-l,b-t,-start,-sweep,center?Arc2D.PIE:Arc2D.OPEN),p);}
 public void drawPath(Path path,Paint p){shape(path.value,p);}
 public void drawBitmap(Bitmap bitmap,Rect source,RectF target,Paint p){
  graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,(p.color>>>24)/255f));
  graphics.drawImage(bitmap.image,(int)target.left,(int)target.top,(int)target.right,(int)target.bottom,source.left,source.top,source.right,source.bottom,null);
 }
}
