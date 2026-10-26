package dev.coinbot.mobile;

public final class UiStateTest {
    private static int checks;
    private static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
    public static void main(String[] args){
        check(UiState.status(false,true,true,true,true,false,"waiting").action.equals("access"),"missing source permission takes priority over a running service");
        check(UiState.status(true,false,false,true,true,false,"").action.equals("notifications"),"service notification permission has a distinct next action");
        check(UiState.status(true,true,false,true,true,false,"login").action.equals("login"),"expired X login is not a generic paused state");
        check(UiState.status(true,true,false,true,true,false,"error").title.equals("Empfang unterbrochen"),"system interruption is not reported as an intentional pause");
        check(UiState.status(true,true,false,true,true,false,"paused").action.equals("start"),"paused service always has a visible resume action");
        check(UiState.status(true,true,true,true,false,false,"waiting").action.equals("start"),"disabled notification filter cannot appear ready");
        check(UiState.status(true,true,true,false,true,false,"waiting").action.equals("reconnect"),"disconnected Android listener cannot appear ready");
        check(UiState.status(true,true,true,true,true,true,"looking").action.equals("reader"),"only actual in-flight work offers active inspection");
        check(UiState.status(true,true,true,true,true,false,"waiting").tone.equals("ready"),"idle ready state waits for a real notification");
        check(UiState.status(true,true,true,true,true,false,"backoff").tone.equals("warning"),"source rate limit must not look like healthy readiness");
        check(UiState.status(true,true,true,true,true,false,"error").title.contains("unterbrochen"),"source failure remains visible despite active receiver");
        check(UiState.linked("x_browser_linked","https://x.com/elonmusk/status/123"),"actual browser-source link gets a confirmed label");
        check(!UiState.linked("notification_unverified","https://x.com/elonmusk/status/123"),"the word unverified must never imply verification");
        check(!UiState.linked("post_link_verified","@elonmusk ohne Link"),"source flag alone cannot manufacture a permalink");
        check(!UiState.linked("legacy_link","https://x.com/elonmusk/status/123"),"legacy unknown provenance stays unconfirmed");
        check(UiTime.relative(100000,130000).equals("Gerade eben"),"recent local event label");
        check(UiTime.relative(100000,220000).equals("Vor 2 Min."),"relative time minute boundary");
        check(UiTime.relative(100000,0).equals("Gerade eben"),"clock differences cannot show negative durations");
        System.out.println(checks+" UX state/source-label checks passed");
    }
}
