package android.os;

import java.util.*;

/** JVM fixture only; not packaged in the APK. */
public final class Bundle {
    private final Map<String,Object> values=new LinkedHashMap<>();
    public Bundle put(String key,Object value){values.put(key,value);return this;}
    public Object get(String key){return values.get(key);}
    public Set<String> keySet(){return values.keySet();}
}
