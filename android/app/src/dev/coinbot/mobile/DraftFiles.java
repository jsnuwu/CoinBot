package dev.coinbot.mobile;

import java.io.*;
import java.nio.file.*;

/** Same-directory replacement: a failed write never truncates a previous draft asset. */
final class DraftFiles {
    interface Writer { void write(File file) throws Exception; }
    static File file(File folder,String id,String suffix)throws IOException{
        if(id==null||!id.matches("[A-Za-z0-9_-]{1,100}"))throw new IOException("Invalid draft id");
        if(!folder.isDirectory()&&!folder.mkdirs())throw new IOException("Draft directory unavailable");
        return new File(folder,id+suffix);
    }
    static void replace(File target,Writer writer)throws Exception{
        File temp=File.createTempFile("asset-",".tmp",target.getParentFile());
        try{
            writer.write(temp);
            try(RandomAccessFile out=new RandomAccessFile(temp,"rw")){out.getFD().sync();}
            Files.move(temp.toPath(),target.toPath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
        }finally{temp.delete();}
    }
}
