package dev.coinbot.mobile;

/** Truthful screen states, independent of Android views and German diagnostic wording. */
public final class UiState {
    private UiState(){}
    public static final class Status {
        public final String title,detail,action,tone;
        Status(String title,String detail,String action,String tone){this.title=title;this.detail=detail;this.action=action;this.tone=tone;}
    }
    public static Status status(boolean access,boolean notifications,boolean running,boolean attached,boolean enabled,boolean busy,String kind){
        if(!access)return new Status("Mitteilungen erlauben","CoinBot braucht Zugriff auf die Post-Mitteilungen deiner X-App.","access","warning");
        if(!notifications)return new Status("Dienst-Mitteilung erlauben","Damit du den Empfang jederzeit sehen und pausieren kannst.","notifications","warning");
        if("login".equals(kind))return new Status("Bei X anmelden","Die Linksuche benötigt eine gültige X-Webanmeldung.","login","warning");
        if(!running&&"error".equals(kind))return new Status("Empfang unterbrochen","Der Dienst wurde beendet. Starte ihn erneut; Details findest du im Status.","start","warning");
        if(!running)return new Status("Empfang pausiert","Starte den Empfang. Erst eine X-Mitteilung löst die Linksuche aus.","start","neutral");
        if(!enabled)return new Status("Empfang ausgeschaltet","Aktiviere den Empfang, um auf X-Mitteilungen zu reagieren.","start","neutral");
        if(!attached)return new Status("Empfang verbinden","Der Android-Mitteilungsdienst ist noch nicht verbunden.","reconnect","warning");
        if("backoff".equals(kind))return new Status("X braucht eine Pause","X begrenzt den Zugriff. Der Empfang wartet auf neue Mitteilungen nach der Sperre.","pause","warning");
        if(busy)return new Status("Post-Link wird gesucht","Die aktuelle Mitteilung wird ihrem Originalpost zugeordnet.","reader","active");
        if("error".equals(kind))return new Status("Letzte Linksuche unterbrochen","Der Empfang bleibt bereit. Den Grund findest du unter Details.","pause","warning");
        return new Status("Wartet auf X-Mitteilung","Nur deine Handy-Mitteilungen lösen einen Entwurf aus.","pause","ready");
    }
    public static String actionLabel(String action){
        switch(action){case "access":return "Zugriff erlauben";case "notifications":return "Mitteilung erlauben";case "login":return "Bei X anmelden";
            case "start":return "Empfang starten";case "reconnect":return "Empfang verbinden";case "reader":return "Linksuche ansehen";default:return "Empfang pausieren";}
    }
    public static boolean linked(String source,String url){return ("post_link_verified".equals(source)||"x_browser_linked".equals(source))&&!Core.postUrl(url).isEmpty();}
}
