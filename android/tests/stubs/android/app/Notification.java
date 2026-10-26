package android.app;

import android.os.Bundle;

/** JVM fixture only; does not emulate Android notification delivery. */
public final class Notification {
    public Bundle extras=new Bundle();
    public Object contentIntent;
    public Action[] actions;
    public String getShortcutId(){return null;}
    public static final class Action {
        private final Bundle extras;
        public Action(Bundle extras){this.extras=extras;}
        public Bundle getExtras(){return extras;}
    }
}
