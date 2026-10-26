package dev.coinbot.mobile;

import java.text.DateFormat;
import java.util.Date;

final class UiTime {
    static String relative(long timestamp,long now){
        long age=Math.max(0,now-timestamp);
        if(age<60_000)return "Gerade eben";
        if(age<3_600_000)return "Vor "+age/60_000+" Min.";
        if(age<86_400_000)return "Vor "+age/3_600_000+" Std.";
        return DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(timestamp));
    }
}
