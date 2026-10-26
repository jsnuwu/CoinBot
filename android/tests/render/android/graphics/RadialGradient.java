package android.graphics;
public final class RadialGradient extends Shader{
 final java.awt.Paint value;
 public RadialGradient(float x,float y,float radius,int a,int b,TileMode mode){this(x,y,radius,new int[]{a,b},new float[]{0,1},mode);}
 public RadialGradient(float x,float y,float radius,int[] colors,float[] stops,TileMode mode){
  java.awt.Color[] values=new java.awt.Color[colors.length];for(int i=0;i<colors.length;i++)values[i]=color(colors[i]);
  if(stops==null){stops=new float[colors.length];for(int i=0;i<stops.length;i++)stops[i]=(float)i/(stops.length-1);}
  value=new java.awt.RadialGradientPaint(x,y,radius,stops,values);
 }
 java.awt.Paint paint(){return value;}
}
