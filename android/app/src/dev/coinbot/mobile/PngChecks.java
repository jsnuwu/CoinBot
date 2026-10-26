package dev.coinbot.mobile;

import java.io.*;
import java.util.zip.CRC32;

/** Detect truncation and damaged chunks even if a platform decoder accepts a partial PNG. */
final class PngChecks {
    static boolean valid(File file){
        if(!file.isFile()||file.length()<57||file.length()>8_000_000)return false;
        try(DataInputStream in=new DataInputStream(new BufferedInputStream(new FileInputStream(file)))){
            if(in.readLong()!=0x89504e470d0a1a0aL)return false;
            boolean header=false,data=false;
            while(true){
                int length=in.readInt(),type=in.readInt();
                if(length<0||length>8_000_000||length>in.available()-4)return false;
                if(!header&&(type!=0x49484452||length!=13))return false;
                CRC32 crc=new CRC32();
                for(int shift=24;shift>=0;shift-=8)crc.update(type>>>shift&255);
                byte[] bytes=new byte[Math.min(length,8192)];int remaining=length;
                while(remaining>0){int n=Math.min(bytes.length,remaining);in.readFully(bytes,0,n);crc.update(bytes,0,n);
                    if(!header){DataInputStream dimensions=new DataInputStream(new ByteArrayInputStream(bytes,0,n));if(dimensions.readInt()!=768||dimensions.readInt()!=768)return false;}
                    remaining-=n;
                }
                if(in.readInt()!=(int)crc.getValue())return false;
                if(type==0x49484452){if(header)return false;header=true;}
                if(type==0x49444154)data=true;
                if(type==0x49454e44)return length==0&&data&&in.read()==-1;
            }
        }catch(IOException|RuntimeException e){return false;}
    }
}
