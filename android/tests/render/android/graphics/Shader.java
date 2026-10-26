package android.graphics;
public abstract class Shader{public enum TileMode{CLAMP}abstract java.awt.Paint paint();static java.awt.Color color(int argb){return new java.awt.Color(argb,true);}}
