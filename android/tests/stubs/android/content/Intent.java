package android.content;

import android.net.Uri;

public final class Intent {
    private final Uri data;
    public Intent(Uri data){this.data=data;}
    public Uri getData(){return data;}
}
