package dev.coinbot.mobile;

/** Stable visual variation per draft; topic motifs remain recognizable. No remote service. */
final class ArtStyle {
    final int hue,layout,decoration;
    final String motif;
    ArtStyle(Core.Idea idea){this(idea,"");}
    ArtStyle(Core.Idea idea,String text){
        String words=(idea.name+" "+(text==null?"":text)).toLowerCase(java.util.Locale.ROOT);
        String selected=idea.theme;
        if("transport".equals(selected)&&words.matches("(?s).*\\b(airport|airline|flight|aircraft|plane|flug|flughafen)\\b.*"))selected="aircraft";
        else if("finance".equals(selected)&&words.matches("(?s).*\\b(bitcoin|ethereum|solana|crypto|blockchain)\\b.*"))selected="crypto";
        else if("weather".equals(selected)&&words.matches("(?s).*\\b(snow|snowfall|blizzard|schnee)\\b.*"))selected="snow";
        else if("weather".equals(selected)&&words.matches("(?s).*\\b(sunny|sunshine|heat|hitze|sonne)\\b.*"))selected="sun";
        else if("politics".equals(selected)&&words.matches("(?s).*\\b(election|elections|vote|voting|wahl|wahlen|abstimmung)\\b.*"))selected="ballot";
        motif=selected;
        int seed=(idea.name+"|"+idea.ticker+"|"+idea.theme).hashCode();
        hue=Math.floorMod(seed,360);layout=Math.floorMod(seed/360,3);decoration=Math.floorMod(seed/1080,3);
    }
}
