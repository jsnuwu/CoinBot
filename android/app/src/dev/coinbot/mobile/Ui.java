package dev.coinbot.mobile;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;

final class Ui {
    static final int BG=0xff101820,PANEL=0xff1b2936,SURFACE=0xff223444,BORDER=0xff385060;
    static final int TEXT=0xfff3f6f4,MUTED=0xffa7b7c4,MINT=0xff60e4bd,AMBER=0xffffd080,RED=0xffffa5a5;
    static int dp(Context c,float value){return Math.round(value*c.getResources().getDisplayMetrics().density);}
    static GradientDrawable shape(Context c,int fill,int stroke,int radius){GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(c,radius));if(stroke!=0)d.setStroke(dp(c,1),stroke);return d;}
    static LinearLayout column(Context c){LinearLayout l=new LinearLayout(c);l.setOrientation(LinearLayout.VERTICAL);return l;}
    static LinearLayout row(Context c){LinearLayout l=new LinearLayout(c);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    static TextView text(Context c,String value,int size,int color,boolean bold){TextView t=new TextView(c);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));t.setLineSpacing(dp(c,2),1);return t;}
    static void gap(LinearLayout l,int height){View v=new View(l.getContext());l.addView(v,new LinearLayout.LayoutParams(1,dp(l.getContext(),height)));}
    static LinearLayout card(Context c){LinearLayout l=column(c);int p=dp(c,18);l.setPadding(p,p,p,p);l.setBackground(shape(c,PANEL,BORDER,20));return l;}
    static void margin(View v,int top){if(v.getLayoutParams() instanceof LinearLayout.LayoutParams){LinearLayout.LayoutParams p=(LinearLayout.LayoutParams)v.getLayoutParams();p.topMargin=dp(v.getContext(),top);v.setLayoutParams(p);}}
    static void clickable(View v,Runnable run){v.setClickable(true);v.setFocusable(true);v.setMinimumHeight(Math.max(v.getMinimumHeight(),dp(v.getContext(),48)));v.setOnClickListener(w->run.run());v.setAccessibilityDelegate(new View.AccessibilityDelegate(){public void onInitializeAccessibilityNodeInfo(View host,android.view.accessibility.AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(host,info);info.setClassName("android.widget.Button");}});}
    static TextView button(Context c,String title,boolean primary,Runnable action){
        TextView b=text(c,title,15,primary?BG:TEXT,true);b.setGravity(Gravity.CENTER);b.setMinHeight(dp(c,50));b.setPadding(dp(c,14),dp(c,12),dp(c,14),dp(c,12));
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x4460e4bd),shape(c,primary?MINT:SURFACE,primary?0:BORDER,13),null));
        b.setAccessibilityDelegate(new View.AccessibilityDelegate(){public void onInitializeAccessibilityNodeInfo(View host,android.view.accessibility.AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(host,info);info.setClassName("android.widget.Button");}});clickable(b,action);return b;
    }
    static TextView badge(Context c,String title,int color){TextView b=text(c,title,12,color,true);b.setPadding(dp(c,10),dp(c,6),dp(c,10),dp(c,6));b.setBackground(shape(c,SURFACE,0,9));return b;}
    static void setupWindow(Activity a,View root){
        a.getWindow().setStatusBarColor(BG);a.getWindow().setNavigationBarColor(BG);a.getWindow().getDecorView().setSystemUiVisibility(0);
        root.setBackgroundColor(BG);root.setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets;});
        a.setContentView(root);root.requestApplyInsets();
    }
    static LinearLayout page(Context c){LinearLayout p=column(c);int space=dp(c,20);p.setPadding(space,space,space,dp(c,28));return p;}
    static void section(LinearLayout parent,String title,String detail){Context c=parent.getContext();gap(parent,22);parent.addView(text(c,title,19,TEXT,true));if(detail!=null){gap(parent,5);parent.addView(text(c,detail,13,MUTED,false));}gap(parent,12);}
    static View icon(Context c,String kind,int color,int size){Glyph g=new Glyph(c,kind,color);g.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);g.setLayoutParams(new LinearLayout.LayoutParams(dp(c,size),dp(c,size)));return g;}
    static final class Glyph extends View {
        private final String kind;private final int color;private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        Glyph(Context c,String kind,int color){super(c);this.kind=kind;this.color=color;}
        protected void onDraw(Canvas target){super.onDraw(target);target.save();target.scale(getWidth()/24f,getHeight()/24f);p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.8f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
            Path line=new Path();
            switch(kind){
                case "home":line.moveTo(3,11);line.lineTo(12,3);line.lineTo(21,11);line.moveTo(5,10);line.lineTo(5,21);line.lineTo(10,21);line.lineTo(10,15);line.lineTo(14,15);line.lineTo(14,21);line.lineTo(19,21);line.lineTo(19,10);target.drawPath(line,p);break;
                case "settings":target.drawCircle(12,12,4,p);target.drawCircle(12,12,9,p);for(int i=0;i<8;i++){double a=i*Math.PI/4;target.drawLine(12+(float)Math.cos(a)*9,12+(float)Math.sin(a)*9,12+(float)Math.cos(a)*11,12+(float)Math.sin(a)*11,p);}break;
                case "bell":line.moveTo(5,17);line.lineTo(7,14);line.lineTo(7,9);line.cubicTo(7,2,17,2,17,9);line.lineTo(17,14);line.lineTo(19,17);line.close();target.drawPath(line,p);target.drawArc(10,18,14,22,0,180,false,p);break;
                case "check":line.moveTo(5,12);line.lineTo(10,17);line.lineTo(20,6);target.drawPath(line,p);break;
                case "arrow":line.moveTo(9,5);line.lineTo(16,12);line.lineTo(9,19);target.drawPath(line,p);break;
                case "back":line.moveTo(15,5);line.lineTo(8,12);line.lineTo(15,19);target.drawPath(line,p);break;
                case "brand":p.setStrokeWidth(5);target.drawArc(4,4,20,20,45,270,false,p);p.setStrokeWidth(2);target.drawLine(12,12,21,12,p);break;
                case "link":target.drawArc(3,5,15,17,90,270,false,p);target.drawArc(9,7,21,19,270,270,false,p);target.drawLine(8,14,16,10,p);break;
                default:target.drawRoundRect(5,3,19,21,2,2,p);target.drawLine(9,8,15,8,p);target.drawLine(9,12,15,12,p);target.drawLine(9,16,13,16,p);
            }target.restore();
        }
    }
}
