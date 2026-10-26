package android.graphics;
public final class Path{
 final java.awt.geom.Path2D.Float value=new java.awt.geom.Path2D.Float();
 public void moveTo(float x,float y){value.moveTo(x,y);}public void lineTo(float x,float y){value.lineTo(x,y);}
 public void cubicTo(float x,float y,float xx,float yy,float xxx,float yyy){value.curveTo(x,y,xx,yy,xxx,yyy);}public void close(){value.closePath();}
}
