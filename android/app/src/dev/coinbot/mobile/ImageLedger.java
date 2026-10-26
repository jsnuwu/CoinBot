package dev.coinbot.mobile;

import java.io.*;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Independent of draft retention. Checksummed history; corrupt/full history fails closed. */
final class ImageLedger implements AutoCloseable {
    static final Object MONITOR=new Object();
    static final String HEADER="COINBOT_IMAGE_HISTORY_1\n";
    static final int MAX_BYTES=16*1024*1024;
    final boolean fresh;
    private final File file;
    private final RandomAccessFile lockFile;
    private final FileLock lock;
    private final List<Entry> entries=new ArrayList<>();
    private static final class Entry {
        final String owner,structure;final ImageFingerprint fingerprint;
        Entry(String owner,String structure,ImageFingerprint fingerprint){this.owner=owner;this.structure=structure;this.fingerprint=fingerprint;}
        String line(){
            String body=owner+"\t"+structure+"\t"+fingerprint.pixels+"\t"+Long.toHexString(fingerprint.edges)+"\t"+Base64.getEncoder().encodeToString(fingerprint.shape);
            return body+"\t"+ImageFingerprint.sha(body.getBytes(StandardCharsets.US_ASCII))+"\n";
        }
    }
    ImageLedger(File folder)throws IOException{
        if(!folder.isDirectory()&&!folder.mkdirs())throw new IOException("History directory unavailable");
        file=new File(folder,"image-history.v1");lockFile=new RandomAccessFile(new File(folder,"image-history.lock"),"rw");
        FileLock acquired=null;
        try{
            acquired=lockFile.getChannel().lock();lock=acquired;fresh=!file.exists();
            if(fresh&&new File(folder,"image-history.initialized").exists())throw new IOException("History missing after initialization");
            if(!fresh)load();
        }catch(Exception e){if(acquired!=null)acquired.release();lockFile.close();throw new ImageGenerationException("Bildhistorie nicht lesbar oder belegt; kein neues Bild freigegeben",e);}
    }
    private void load()throws IOException{
        if(file.length()>MAX_BYTES)throw new IOException("History full");
        byte[] raw=Files.readAllBytes(file.toPath());String data=new String(raw,StandardCharsets.US_ASCII);
        if(!data.startsWith(HEADER)||!data.endsWith("\n"))throw new IOException("Damaged history");
        int footerStart=data.lastIndexOf("END\t");
        if(footerStart<HEADER.length())throw new IOException("History footer missing");
        String body=data.substring(HEADER.length(),footerStart);
        String[] footer=data.substring(footerStart).trim().split("\\t",-1);
        if(footer.length!=3||!footer[0].equals("END")||!footer[2].equals(ImageFingerprint.sha(body.getBytes(StandardCharsets.US_ASCII))))throw new IOException("History footer damaged");
        for(String line:body.split("\n")){
            if(line.isEmpty())continue;
            String[] p=line.split("\t",-1);if(p.length!=6||!p[0].matches("[A-Za-z0-9_-]{1,100}")||!p[1].matches("[0-9a-f]{64}"))throw new IOException("Damaged history record");
            String signed=line.substring(0,line.lastIndexOf('\t'));
            if(!ImageFingerprint.sha(signed.getBytes(StandardCharsets.US_ASCII)).equals(p[5]))throw new IOException("Damaged history checksum");
            try{entries.add(new Entry(p[0],p[1],new ImageFingerprint(p[2],Long.parseUnsignedLong(p[3],16),Base64.getDecoder().decode(p[4]))));}
            catch(RuntimeException e){throw new IOException("Damaged history fingerprint",e);}
        }
        if(!footer[1].equals(Integer.toString(entries.size())))throw new IOException("History records missing");
    }
    int size(){return entries.size();}
    String rejection(String owner,String structure,ImageFingerprint fingerprint,boolean allowOwn){
        for(Entry entry:entries){
            if(allowOwn&&owner.equals(entry.owner))continue;
            if(fingerprint.pixels.equals(entry.fingerprint.pixels))return "identical_pixels";
            if(structure.equals(entry.structure))return "same_composition";
            if(fingerprint.resembles(entry.fingerprint))return "similar_image";
        }
        return null;
    }
    void rememberLegacy(String owner,ImageFingerprint fingerprint){
        for(Entry entry:entries)if(entry.owner.equals(owner)&&entry.fingerprint.pixels.equals(fingerprint.pixels))return;
        entries.add(new Entry(owner,ImageFingerprint.sha(("legacy:"+fingerprint.pixels).getBytes(StandardCharsets.US_ASCII)),fingerprint));
    }
    void reserve(String owner,String structure,ImageFingerprint fingerprint,boolean allowOwn)throws Exception{
        if(!owner.matches("[A-Za-z0-9_-]{1,100}")||!structure.matches("[0-9a-f]{64}"))throw new IOException("Invalid image identity");
        if(rejection(owner,structure,fingerprint,allowOwn)!=null)throw new ImageGenerationException("Bild zu ähnlich zu einem bisherigen Entwurf; kein neues Bild freigegeben");
        for(Entry entry:entries)if(entry.owner.equals(owner)&&entry.fingerprint.pixels.equals(fingerprint.pixels)){persist();return;}
        entries.add(new Entry(owner,structure,fingerprint));persist();
    }
    void persist()throws Exception{
        StringBuilder records=new StringBuilder();for(Entry entry:entries)records.append(entry.line());
        String body=records.toString();
        byte[] bytes=(HEADER+body+"END\t"+entries.size()+"\t"+ImageFingerprint.sha(body.getBytes(StandardCharsets.US_ASCII))+"\n").getBytes(StandardCharsets.US_ASCII);if(bytes.length>MAX_BYTES)throw new IOException("History capacity reached");
        DraftFiles.replace(file,temp->{try(FileOutputStream stream=new FileOutputStream(temp)){stream.write(bytes);stream.getFD().sync();}});
        File marker=new File(file.getParentFile(),"image-history.initialized");
        if(!marker.exists())DraftFiles.replace(marker,temp->{try(FileOutputStream stream=new FileOutputStream(temp)){stream.write(HEADER.getBytes(StandardCharsets.US_ASCII));}});
    }
    public void close()throws IOException{try{lock.release();}finally{lockFile.close();}}
}
