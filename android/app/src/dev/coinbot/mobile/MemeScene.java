package dev.coinbot.mobile;

/** Bundled surreal scene selection. This is not a text-to-image model or an API client. */
final class MemeScene {
    static final String GENERATOR="local_surreal_compositor_v3";
    final String asset,name;
    final int grid,tile,inset;
    MemeScene(Core.Idea idea,String text){
        ArtStyle style=new ArtStyle(idea,text);
        String selected=style.motif;
        String file="meme-scenes-v3.png",scene="octopus_worlds";int index=10,size=4;
        switch(selected){
            case "space":index=0;scene="rocket_frog";break;
            case "coffee":index=1;scene="caffeine_raccoon";break;
            case "dog":index=2;scene="cloud_dog";break;
            case "cat":index=3;scene="pizza_cat";break;
            case "robot":index=4;scene="brain_gardener";break;
            case "pizza":index=5;scene="lava_pizza";break;
            case "taco":index=6;scene="abducted_taco";break;
            case "penguin":index=7;scene="galaxy_penguin";break;
            case "gold":index=8;scene="sun_goose";break;
            case "tariff":index=9;scene="customs_goblin";break;
            case "politics":case "ballot":index=11;scene="mushroom_parliament";break;
            case "finance":case "crypto":index=12;scene="bull_bear_chess";break;
            case "sports":index=13;scene="space_referee";break;
            case "science":index=14;scene="axolotl_universe";break;
            case "weather":case "snow":case "sun":index=15;scene="storm_juggler";break;
            case "transport":file="meme-scenes-extra-v3.png";size=2;index=0;scene="croissant_train";break;
            case "aircraft":file="meme-scenes-extra-v3.png";size=2;index=1;scene="flying_koi";break;
            case "health":file="meme-scenes-extra-v3.png";size=2;index=2;scene="planet_doctor";break;
            case "technology":file="meme-scenes-extra-v3.png";size=2;index=3;scene="jellyfish_wizard";break;
        }
        asset=file;name=scene;tile=index;grid=size;inset=2+style.layout*5;
    }
    int[] bounds(int width,int height){
        if(width<grid*100||height<grid*100||width!=height)throw new IllegalArgumentException("Invalid scene sheet dimensions");
        int x=tile%grid,y=tile/grid;
        return new int[]{x*width/grid+inset,y*height/grid+inset,(x+1)*width/grid-inset,(y+1)*height/grid-inset};
    }
}
