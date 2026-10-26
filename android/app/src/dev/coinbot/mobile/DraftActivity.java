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
    private final android.os.Handler clock=new android.os.Handler(android.os.Looper.getMainLooper());
    private String revision="";
    private final Runnable tick=new Runnable(){public void run(){
        try(LocalStore store=new LocalStore(DraftActivity.this)){
            Map<String,String> row=store.find(getIntent().getStringExtra("post_id"));
            String next=row==null?"missing":row.get("image_revision");
            if(!next.equals(revision)){revision=next;render();}
        }clock.postDelayed(this,1500);
    }};
    public void onCreate(Bundle saved){super.onCreate(saved);render();}
    protected void onResume(){super.onResume();DraftAssets.repairAsync(this);clock.post(tick);}
    protected void onPause(){clock.removeCallbacks(tick);super.onPause();}
    private void render(){LinearLayout root=Ui.column(this);Ui.setupWindow(this,root);
        LinearLayout header=Ui.row(this);header.setPadding(d(18),d(10),d(18),d(10));TextView back=Ui.button(this,"‹ Zurück",false,()->finish());header.addView(back);TextView label=Ui.text(this,"Entwurf",18,Ui.TEXT,true);label.setPadding(d(16),0,0,0);header.addView(label);root.addView(header);
        ScrollView scroll=new ScrollView(this);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));LinearLayout center=Ui.column(this);center.setGravity(android.view.Gravity.TOP|android.view.Gravity.CENTER_HORIZONTAL);LinearLayout page=Ui.page(this);center.addView(page,new LinearLayout.LayoutParams(getResources().getConfiguration().screenWidthDp>740?d(720):-1,-2));scroll.addView(center);
        Map<String,String> row;try(LocalStore store=new LocalStore(this)){row=store.find(getIntent().getStringExtra("post_id"));}
        if(row==null||row.get("name")==null){page.addView(Ui.text(this,"Entwurf nicht mehr verfügbar",23,Ui.TEXT,true));Ui.gap(page,12);page.addView(Ui.text(this,"Ältere Entwürfe werden nach 30 Tagen lokal entfernt.",15,Ui.MUTED,false));return;}
        revision=row.get("image_revision");
        String id=row.get("id"),url=Core.postUrl(row.get("url"));boolean linked=UiState.linked(row.get("source"),url);
        page.addView(Ui.badge(this,"LOKAL · NICHT VERÖFFENTLICHT",Ui.MUTED));Ui.gap(page,14);page.addView(Ui.text(this,row.get("name"),30,Ui.TEXT,true));Ui.gap(page,6);page.addView(Ui.text(this,"$"+row.get("ticker"),18,Ui.MINT,true));Ui.gap(page,18);
        JSONObject metadata=metadata(id);
        File imageFile=new File(getFilesDir(),"drafts/"+id+".png");Bitmap bitmap=Art.load(imageFile,1);
        if(bitmap!=null){
            ImageView image=new ImageView(this);image.setAdjustViewBounds(true);image.setMaxHeight(d(380));image.setScaleType(ImageView.ScaleType.FIT_CENTER);image.setImageBitmap(bitmap);image.setContentDescription("Meme-Entwurfsbild zu "+row.get("name"));image.setBackground(Ui.shape(this,Ui.PANEL,0,20));image.setClipToOutline(true);page.addView(image,new LinearLayout.LayoutParams(-1,-2));
            Ui.gap(page,10);page.addView(Ui.button(this,"Bild groß ansehen",false,()->{
                Bitmap full=Art.load(imageFile,1);
                if(full==null){Toast.makeText(this,"Bild fehlt oder ist beschädigt",Toast.LENGTH_LONG).show();DraftAssets.repairAsync(this);render();return;}
                ImageView large=new ImageView(this);large.setImageBitmap(full);large.setAdjustViewBounds(true);large.setContentDescription("Themenbild zu "+row.get("name"));
                new android.app.AlertDialog.Builder(this).setTitle(row.get("name")).setView(large).setPositiveButton("Schließen",null).show();
            }));
        }
        if(bitmap==null||!"ready".equals(row.get("image_status"))){
            Ui.gap(page,12);page.addView(Ui.text(this,bitmap==null?"Bild fehlt oder ist beschädigt. Der Entwurf und seine Quelle bleiben erhalten.":"Bild vorhanden; Bildmetadaten noch nicht bestätigt.",15,Ui.AMBER,true));
            String imageStatus=row.get("image_status");page.addView(Ui.text(this,imageStatus!=null&&imageStatus.startsWith("Fehler")?imageStatus:"Die lokale Prüfung ergänzt das Bild und seine Metadaten, sofern möglich.",13,Ui.MUTED,false));
            Ui.gap(page,8);page.addView(Ui.button(this,"Lokale Bildreparatur erneut prüfen",false,()->{DraftAssets.repairAsync(this);Toast.makeText(this,"Lokale Prüfung gestartet",Toast.LENGTH_SHORT).show();}));
        }
        if(bitmap!=null&&"ready".equals(row.get("image_status"))){
            Ui.gap(page,10);page.addView(Ui.text(this,"pixels_and_structure_v1".equals(metadata.optString("image_uniqueness_check"))?"Mit gespeicherter Bildhistorie abgeglichen":"Älteres Bild ohne neuen Bildvergleich. Du kannst unten eine neue Komposition erstellen.",12,Ui.MUTED,false));
        }
        Ui.gap(page,10);page.addView(Ui.button(this,"Neues Meme-Bild komponieren",false,()->{DraftAssets.restyleAsync(this,id);Toast.makeText(this,"Meme-Gestaltung gestartet; bisheriges Bild wird gesichert",Toast.LENGTH_LONG).show();}));
        Ui.section(page,"Quelle",null);LinearLayout source=Ui.card(this);source.addView(Ui.badge(this,linked?"Originalpost verknüpft":"Quelle unbestätigt",linked?Ui.MINT:Ui.AMBER));Ui.gap(source,12);source.addView(Ui.text(this,linked?"Dieser Entwurf wurde einem konkreten X-Post zugeordnet.":"Dieser ältere Entwurf stammt aus einer Mitteilung. Der Originalpost wurde nicht bestätigt.",14,Ui.MUTED,false));
        if(!url.isEmpty()){
            Ui.gap(source,15);source.addView(Ui.button(this,linked?"Originalpost öffnen":"Beobachteten Link öffnen",true,()->open(url)));Ui.gap(source,8);source.addView(Ui.button(this,"Link kopieren",false,()->copy("X-Post",url)));Ui.gap(source,12);TextView address=Ui.text(this,url,12,Ui.MUTED,false);address.setTextIsSelectable(true);source.addView(address);
        }else{Ui.gap(source,10);source.addView(Ui.text(this,"Kein Post-Link verfügbar. Es wird kein Link geraten.",14,Ui.AMBER,false));}page.addView(source);
        String text=metadata.optString("source_text"),author=metadata.optString("source_author");boolean notificationText=text.isEmpty();if(notificationText)text=metadata.optString("notification_text");
        if(!text.isEmpty()){
            Ui.section(page,notificationText?"Auslösende Mitteilung · unbestätigt":"Der auslösende Post",author.isEmpty()?null:"@"+author);LinearLayout quote=Ui.card(this);TextView original=Ui.text(this,text,16,Ui.TEXT,false);original.setTextIsSelectable(true);quote.addView(original);page.addView(quote);
        }
        Ui.section(page,"Beschreibung",null);LinearLayout description=Ui.card(this);TextView words=Ui.text(this,row.get("description"),15,Ui.TEXT,false);words.setTextIsSelectable(true);description.addView(words);page.addView(description);Ui.gap(page,12);page.addView(Ui.button(this,"Ticker kopieren",false,()->copy("Ticker",row.get("ticker"))));Ui.gap(page,18);page.addView(Ui.text(this,"Neue Bilder entstehen lokal aus Bildbausteinen und werden mit der gespeicherten Bildhistorie verglichen. Es ist kein frei generierendes KI-Modell. Ein Entwurf ist kein veröffentlichter Token.",12,Ui.MUTED,false));
    }
    private JSONObject metadata(String id){try{return DraftAssets.read(DraftAssets.file(this,id,".json"));}catch(Exception e){return new JSONObject();}}
    private void open(String url){try{startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse(url)));}catch(ActivityNotFoundException e){Toast.makeText(this,"Keine App zum Öffnen des Links gefunden",Toast.LENGTH_LONG).show();}}
    private void copy(String label,String value){((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText(label,value));Toast.makeText(this,label+" kopiert",Toast.LENGTH_SHORT).show();}
}
