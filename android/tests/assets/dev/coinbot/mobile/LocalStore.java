package dev.coinbot.mobile;
import android.content.Context;
import java.util.*;
/** In-memory persistence double; these tests do not exercise Android SQLite. */
final class LocalStore implements AutoCloseable {
    final Map<String,Map<String,String>> data=new LinkedHashMap<>();
    LocalStore(Context c){}
    boolean canPrepare(){return true;}
    void source(String id,String source){data.get(id).put("source",source);}
    void imageStatus(String id,String status){data.get(id).put("image_status",status);}
    void finish(String id,String status,Core.Idea idea){
        Map<String,String> r=data.get(id);r.put("status",status);
        if(idea!=null){r.put("name",idea.name);r.put("ticker",idea.ticker);r.put("theme",idea.theme);r.put("description",idea.description);}
    }
    List<Map<String,String>> ready(){return new ArrayList<>(data.values());}
    public void close(){}
}
