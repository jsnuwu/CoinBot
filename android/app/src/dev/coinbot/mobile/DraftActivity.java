package dev.coinbot.mobile;

import android.app.Activity;
import android.content.*;
import android.graphics.*;
import android.os.Bundle;
import android.widget.*;
import java.io.*;
import java.util.*;
import org.json.JSONObject;

public final class DraftActivity extends Activity {
    private int d(int n){return Ui.dp(this,n);}
    public void onCreate(Bundle saved){
        super.onCreate(saved);LinearLayout root=Ui.column(this);Ui.setupWindow(this,root);
        LinearLayout header=Ui.row(this);header.setPadding(d(18),d(10),d(18),d(10));TextView back=Ui.button(this,"‹ Zurück",false,()->finish());header.addView(back);TextView label=Ui.text(this,"Entwurf",18,Ui.TEXT,true);label.setPadding(d(16),0,0,0);header.addView(label);root.addView(header);
        ScrollView scroll=new ScrollView(this);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));LinearLayout center=Ui.column(this);center.setGravity(android.view.Gravity.TOP|android.view.Gravity.CENTER_HORIZONTAL);LinearLayout page=Ui.page(this);center.addView(page,new LinearLayout.LayoutParams(getResources().getConfiguration().screenWidthDp>740?d(720):-1,-2));scroll.addView(center);
        Map<String,String> row;try(LocalStore store=new LocalStore(this)){row=store.find(getIntent().getStringExtra("post_id"));}
        if(row==null||row.get("name")==null){page.addView(Ui.text(this,"Entwurf nicht mehr verfügbar",23,Ui.TEXT,true));Ui.gap(page,12);page.addView(Ui.text(this,"Ältere Entwürfe werden nach 30 Tagen lokal entfernt.",15,Ui.MUTED,false));return;}
        String id=row.get("id"),url=Core.postUrl(row.get("url"));boolean linked=UiState.linked(row.get("source"),url);
        page.addView(Ui.badge(this,"LOKAL · NICHT VERÖFFENTLICHT",Ui.MUTED));Ui.gap(page,14);page.addView(Ui.text(this,row.get("name"),30,Ui.TEXT,true));Ui.gap(page,6);page.addView(Ui.text(this,"$"+row.get("ticker"),18,Ui.MINT,true));Ui.gap(page,18);
        ImageView image=new ImageView(this);image.setAdjustViewBounds(true);image.setMaxHeight(d(380));image.setScaleType(ImageView.ScaleType.FIT_CENTER);image.setImageBitmap(BitmapFactory.decodeFile(new File(getFilesDir(),"drafts/"+id+".png").getPath()));image.setContentDescription("Lokal gezeichnetes Themenbild zu "+row.get("name"));image.setBackground(Ui.shape(this,Ui.PANEL,0,20));image.setClipToOutline(true);page.addView(image,new LinearLayout.LayoutParams(-1,-2));
        Ui.section(page,"Quelle",null);LinearLayout source=Ui.card(this);source.addView(Ui.badge(this,linked?"Originalpost verknüpft":"Quelle unbestätigt",linked?Ui.MINT:Ui.AMBER));Ui.gap(source,12);source.addView(Ui.text(this,linked?"Dieser Entwurf wurde einem konkreten X-Post zugeordnet.":"Dieser ältere Entwurf stammt aus einer Mitteilung. Der Originalpost wurde nicht bestätigt.",14,Ui.MUTED,false));
        if(!url.isEmpty()){
            Ui.gap(source,15);source.addView(Ui.button(this,linked?"Originalpost öffnen":"Beobachteten Link öffnen",true,()->open(url)));Ui.gap(source,8);source.addView(Ui.button(this,"Link kopieren",false,()->copy("X-Post",url)));Ui.gap(source,12);TextView address=Ui.text(this,url,12,Ui.MUTED,false);address.setTextIsSelectable(true);source.addView(address);
        }else{Ui.gap(source,10);source.addView(Ui.text(this,"Kein Post-Link verfügbar. Es wird kein Link geraten.",14,Ui.AMBER,false));}page.addView(source);
        JSONObject metadata=metadata(id);String text=metadata.optString("source_text"),author=metadata.optString("source_author");
        if(!text.isEmpty()){
            Ui.section(page,"Der auslösende Post",author.isEmpty()?null:"@"+author);LinearLayout quote=Ui.card(this);TextView original=Ui.text(this,text,16,Ui.TEXT,false);original.setTextIsSelectable(true);quote.addView(original);page.addView(quote);
        }
        Ui.section(page,"Beschreibung",null);LinearLayout description=Ui.card(this);TextView words=Ui.text(this,row.get("description"),15,Ui.TEXT,false);words.setTextIsSelectable(true);description.addView(words);page.addView(description);Ui.gap(page,12);page.addView(Ui.button(this,"Ticker kopieren",false,()->copy("Ticker",row.get("ticker"))));Ui.gap(page,18);page.addView(Ui.text(this,"Themenbild und Text wurden lokal mit Vorlagen erzeugt. Ein Entwurf ist kein veröffentlichter Token.",12,Ui.MUTED,false));
    }
    private JSONObject metadata(String id){try{File file=new File(getFilesDir(),"drafts/"+id+".json");if(file.length()>100_000)return new JSONObject();try(InputStream in=new FileInputStream(file)){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] bytes=new byte[4096];int n;while((n=in.read(bytes))!=-1){out.write(bytes,0,n);if(out.size()>100_000)return new JSONObject();}return new JSONObject(out.toString("UTF-8"));}}catch(Exception e){return new JSONObject();}}
    private void open(String url){try{startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse(url)));}catch(ActivityNotFoundException e){Toast.makeText(this,"Keine App zum Öffnen des Links gefunden",Toast.LENGTH_LONG).show();}}
    private void copy(String label,String value){((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText(label,value));Toast.makeText(this,label+" kopiert",Toast.LENGTH_SHORT).show();}
}
