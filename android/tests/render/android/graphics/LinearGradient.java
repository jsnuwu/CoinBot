package android.graphics;
public final class LinearGradient extends Shader{
 final java.awt.Paint value;
 public LinearGradient(float x,float y,float xx,float yy,int a,int b,TileMode mode){value=new java.awt.GradientPaint(x,y,color(a),xx,yy,color(b));}
 java.awt.Paint paint(){return value;}
}
