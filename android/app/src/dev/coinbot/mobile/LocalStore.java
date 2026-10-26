package dev.coinbot.mobile;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import java.util.*;

final class LocalStore extends SQLiteOpenHelper {
    private final Context context;
    LocalStore(Context context) { super(context, "coinbot.db", null, 3); this.context=context.getApplicationContext(); }
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE posts (id TEXT PRIMARY KEY,url TEXT,time INTEGER,status TEXT,name TEXT,ticker TEXT,theme TEXT,description TEXT,source TEXT NOT NULL DEFAULT 'legacy_link',topic_version INTEGER NOT NULL DEFAULT 3)");
        db.execSQL("CREATE INDEX posts_time ON posts(time)");
    }
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if(oldVersion<2)db.execSQL("ALTER TABLE posts ADD COLUMN source TEXT NOT NULL DEFAULT 'legacy_link'");
        if(oldVersion<3)db.execSQL("ALTER TABLE posts ADD COLUMN topic_version INTEGER NOT NULL DEFAULT 2");
    }
    boolean claim(Core.Candidate candidate) {
        ContentValues values = new ContentValues(); values.put("id", candidate.id); values.put("url", candidate.url);
        values.put("time", System.currentTimeMillis()); values.put("status", "Quelle wird geprüft");
        values.put("source", "post_link_pending");
        values.put("topic_version",3);
        return getWritableDatabase().insertWithOnConflict("posts", null, values, SQLiteDatabase.CONFLICT_IGNORE) != -1 || retryOldTopic(candidate.id);
    }
    boolean claimBrowser(BrowserRules.Post post){
        ContentValues v=new ContentValues();v.put("id",post.id);v.put("url",post.url);v.put("time",System.currentTimeMillis());
        v.put("status","Originalpost aus X-Webansicht wird verarbeitet");v.put("source","x_browser_linked");v.put("topic_version",3);
        if(getWritableDatabase().insertWithOnConflict("posts",null,v,SQLiteDatabase.CONFLICT_IGNORE)!=-1)return true;
        // A previous embed failure must not suppress a later post actually observed in the browser.
        return getWritableDatabase().update("posts",v,"id=? AND name IS NULL AND source IN ('post_link_pending','post_link_verified','x_browser_linked')",new String[]{post.id})==1;
    }
    boolean claimNotification(Core.NotificationDraft draft,String sourceUrl){
        String url=Core.postUrl(sourceUrl);
        ContentValues v=new ContentValues();v.put("id",draft.id);v.put("url",url.isEmpty()?"@"+draft.author+" · ohne Post-ID/Link":url);
        v.put("time",System.currentTimeMillis());v.put("status","Lokaler Entwurf wird geprüft");v.put("source","notification_unverified");
        v.put("topic_version",3);
        return getWritableDatabase().insertWithOnConflict("posts",null,v,SQLiteDatabase.CONFLICT_IGNORE)!=-1 || retryOldTopic(draft.id);
    }
    private boolean retryOldTopic(String id){
        ContentValues v=new ContentValues();v.put("topic_version",3);v.put("status","Thema mit Version 0.3 erneut prüfen");v.put("time",System.currentTimeMillis());
        return getWritableDatabase().update("posts",v,"id=? AND name IS NULL AND topic_version<3 AND status IN ('Kein passendes Meme-Thema','Kein verwertbarer Themenbegriff oder vertraulicher Inhalt')",new String[]{id})==1;
    }
    void source(String id,String source){ContentValues v=new ContentValues();v.put("source",source);getWritableDatabase().update("posts",v,"id=?",new String[]{id});}
    void finish(String id, String status, Core.Idea idea) {
        ContentValues values = new ContentValues(); values.put("status", status);
        if (idea != null) { values.put("name", idea.name); values.put("ticker", idea.ticker); values.put("theme", idea.theme); values.put("description", idea.description); }
        getWritableDatabase().update("posts", values, "id=?", new String[]{id});
    }
    boolean canPrepare(){
        long now=System.currentTimeMillis();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*),MAX(time) FROM posts WHERE name IS NOT NULL AND time>=?",new String[]{Long.toString(now-86_400_000)})){
            c.moveToFirst();return c.getInt(0)<20&&(c.isNull(1)||now-c.getLong(1)>=60_000);
        }
    }
    void prune(){
        long cutoff=System.currentTimeMillis()-30L*86_400_000;
        try(Cursor c=getReadableDatabase().rawQuery("SELECT id FROM posts WHERE time<?",new String[]{Long.toString(cutoff)})){
            while(c.moveToNext())for(String suffix:new String[]{".png",".json"})new java.io.File(context.getFilesDir(),"drafts/"+c.getString(0)+suffix).delete();
        }
        getWritableDatabase().delete("posts","time<?",new String[]{Long.toString(cutoff)});
    }
    List<Map<String,String>> recent() {
        List<Map<String,String>> result = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT * FROM posts ORDER BY time DESC LIMIT 30", null)) {
            while (c.moveToNext()) {
                Map<String,String> row = new HashMap<>();
                for (String column : c.getColumnNames()) row.put(column, c.getString(c.getColumnIndexOrThrow(column)));
                result.add(row);
            }
        }
        return result;
    }
    List<Map<String,String>> ready(){return rows("SELECT * FROM posts WHERE name IS NOT NULL ORDER BY time DESC",null);}
    String readyVersion(){try(Cursor cursor=getReadableDatabase().rawQuery("SELECT COUNT(*),MAX(time) FROM posts WHERE name IS NOT NULL",null)){cursor.moveToFirst();return cursor.getLong(0)+":"+cursor.getLong(1);}}
    Map<String,String> find(String id){List<Map<String,String>> rows=rows("SELECT * FROM posts WHERE id=?",new String[]{id});return rows.isEmpty()?null:rows.get(0);}
    private List<Map<String,String>> rows(String sql,String[] args){
        List<Map<String,String>> result=new ArrayList<>();
        try(Cursor cursor=getReadableDatabase().rawQuery(sql,args)){
            while(cursor.moveToNext()){Map<String,String> row=new HashMap<>();for(String column:cursor.getColumnNames())row.put(column,cursor.getString(cursor.getColumnIndexOrThrow(column)));result.add(row);}
        }return result;
    }
    void recordEvent(Context context, String message) {
        context.getSharedPreferences("settings", 0).edit().putString("last_event", message)
            .putLong("last_event_time", System.currentTimeMillis()).apply();
    }
}
