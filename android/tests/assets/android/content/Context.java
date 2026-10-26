package android.content;
import java.io.File;
public class Context {
    private final File folder;
    public Context(File folder){this.folder=folder;}
    public android.content.res.AssetManager getAssets(){return new android.content.res.AssetManager();}
    public Context getApplicationContext(){return this;}
    public File getFilesDir(){return folder;}
}
