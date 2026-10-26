package dev.coinbot.mobile;

import java.security.MessageDigest;
import java.nio.ByteBuffer;
import java.util.*;

/** Encoding-independent pixel digest and contrast-normalized, low-frequency image structure. */
final class ImageFingerprint {
    static final int SIDE=16;
    final String pixels;
    final long edges;
    final byte[] shape;
    ImageFingerprint(String pixels,long edges,byte[] shape){
        if(!pixels.matches("[0-9a-f]{64}")||shape.length!=SIDE*SIDE)throw new IllegalArgumentException("Invalid fingerprint");
        this.pixels=pixels;this.edges=edges;this.shape=shape.clone();
    }
    static String sha(byte[] data){
        try{return hex(MessageDigest.getInstance("SHA-256").digest(data));}catch(Exception e){throw new IllegalStateException(e);}
    }
    static String hex(byte[] data){StringBuilder out=new StringBuilder();for(byte b:data)out.append(String.format(Locale.ROOT,"%02x",b&255));return out.toString();}
    static ImageFingerprint of(int[] argb,int width,int height){
        if(width<SIDE||height<SIDE||argb.length!=width*height)throw new IllegalArgumentException("Invalid pixels");
        String pixelHash;
        try{
            MessageDigest digest=MessageDigest.getInstance("SHA-256");
            digest.update(ByteBuffer.allocate(8).putInt(width).putInt(height).array());
            ByteBuffer bytes=ByteBuffer.allocate(4096);
            for(int pixel:argb){if(bytes.remaining()<4){digest.update(bytes.array());bytes.clear();}bytes.putInt(pixel);}
            digest.update(bytes.array(),0,bytes.position());pixelHash=hex(digest.digest());
        }catch(Exception e){throw new IllegalStateException(e);}
        double[] small=new double[SIDE*SIDE];double mean=0;
        for(int y=0;y<SIDE;y++)for(int x=0;x<SIDE;x++){
            long total=0;int count=0;
            for(int yy=y*height/SIDE;yy<(y+1)*height/SIDE;yy++)for(int xx=x*width/SIDE;xx<(x+1)*width/SIDE;xx++){
                int p=argb[yy*width+xx];total+=(77*(p>>16&255)+150*(p>>8&255)+29*(p&255))>>8;count++;
            }
            small[y*SIDE+x]=(double)total/count;mean+=small[y*SIDE+x]/small.length;
        }
        double variance=0;for(double value:small)variance+=(value-mean)*(value-mean)/small.length;
        double scale=48/Math.max(12,Math.sqrt(variance));byte[] normalized=new byte[small.length];
        for(int i=0;i<small.length;i++)normalized[i]=(byte)Math.max(0,Math.min(255,Math.round(128+(small[i]-mean)*scale)));
        long edges=0;for(int y=0;y<8;y++)for(int x=0;x<8;x++){
            int left=(y*2+1)*SIDE+x*15/8,right=(y*2+1)*SIDE+(x+1)*15/8;
            edges=(edges<<1)|(small[left]>small[right]?1:0);
        }
        return new ImageFingerprint(pixelHash,edges,normalized);
    }
    boolean resembles(ImageFingerprint other){
        if(pixels.equals(other.pixels))return true;
        int distance=Long.bitCount(edges^other.edges);if(distance<=6)return true;
        if(distance>16)return false;
        int total=0;for(int i=0;i<shape.length;i++)total+=Math.abs((shape[i]&255)-(other.shape[i]&255));
        return total<=shape.length*14;
    }
}
