package android.graphics;
public final class Paint{
 public enum Style{FILL,STROKE}int color=0xff000000;float width=1;Style style=Style.FILL;Shader shader;
 public Paint(int flags){}public void setColor(int value){color=value;}public void setAlpha(int alpha){color=(color&0xffffff)|(alpha<<24);}
 public void setShader(Shader shader){this.shader=shader;}public void setStyle(Style style){this.style=style;}public void setStrokeWidth(float width){this.width=width;}
}
