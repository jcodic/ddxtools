package ddx.hash;

import ddx.common.Utils;
import ddx.hash.types.FileHash;
import ddx.hash.types.HashIndexFile;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 *
 * @author druidman
 */
public class HashFileUtils {

    private static final String LINE_ALGORITHM  = "Algorithm: ";
    private static final String LINE_HASH       = "Hash: ";
    private static final String LINE_INCLUDE    = "Include: ";
    private static final String LINE_EXCLUDE    = "Exclude: ";
    private static final String LINE_HASHNAMES  = "HashNames: ";
    private static final String LINE_FILES      = "Files included: ";
    private static final String LINE_SIZE       = "Size: ";
    
    public static void loadHashFile(Settings settings) throws Exception {
        
        Utils.out.print("Loading hash file ["+settings.hashFile+"]");
        
        FileReader fileReader = new FileReader(settings.hashFile);
        BufferedReader reader = new BufferedReader(fileReader);
        String str;
        
        while ((str = reader.readLine()) != null) {
            
            if (str.startsWith(LINE_ALGORITHM)) settings.algorithm = str.substring(LINE_ALGORITHM.length()); else
            if (str.startsWith(LINE_HASH)) settings.hash = str.substring(LINE_HASH.length()); else
            if (str.startsWith(LINE_INCLUDE)) settings.addToInclude(str.substring(LINE_INCLUDE.length())); else
            if (str.startsWith(LINE_EXCLUDE)) settings.addToExclude(str.substring(LINE_EXCLUDE.length())); else
            if (str.startsWith(LINE_HASHNAMES)) settings.hashNames = Boolean.parseBoolean(str.substring(LINE_HASHNAMES.length()));
        }
        reader.close();
        fileReader.close();
        
        Utils.out.println(" done");
    }

    public static void writeHashFile(Settings settings) throws Exception {
        
        Utils.out.print("Writing hash file ["+settings.hashFile+"]");
        
        FileWriter fileWriter = new FileWriter(settings.hashFile);
        BufferedWriter writer = new BufferedWriter(fileWriter);
        writer.write(LINE_ALGORITHM+settings.algorithm+"\n");
        writer.write(LINE_HASH+settings.hash+"\n");
        if (settings.includeFiles != null) for (String item : settings.includeFiles) writer.write(LINE_INCLUDE+item+"\n");
        if (settings.excludeFiles != null) for (String item : settings.excludeFiles) writer.write(LINE_EXCLUDE+item+"\n");
        writer.write(LINE_HASHNAMES+settings.hashNames+"\n");
        writer.write(LINE_FILES+settings.filesIncluded+"\n");
        writer.write(LINE_SIZE+settings.filesSizeProcessed+" ("+Utils.describeFileLength(settings.filesSizeProcessed)+")\n");
        writer.flush();
        writer.close();
        fileWriter.close();
        
        Utils.out.println(" done");
    }

    public static HashIndexFile loadHashIndexFile(String hashFile) throws Exception {
        
        Utils.out.print("Loading hash index file ["+hashFile+"] ");

        HashIndexFile hif = new HashIndexFile();
        Set<FileHash> fileHashes = new LinkedHashSet<FileHash>();
        hif.setHashes(fileHashes);
        
        FileInputStream fileReader = new FileInputStream(hashFile);
        BufferedInputStream reader = new BufferedInputStream(fileReader);
        
        int b = reader.read();
        if (b != HashIndexFile.PREFIX) throw new Exception("wrong file format!");
        
        byte[] bf = new byte[4];
        b = reader.read(bf);
        if (b != bf.length) throw new Exception("wrong file format!");
        int totalFiles = Utils.bytesToInt(bf);
        if (totalFiles < 0) throw new Exception("wrong file format!");

        Utils.out.print("files in index: " + totalFiles);

        if (totalFiles > 0) {
            
            byte[] hashCheck = new byte[32];
            b = reader.read(hashCheck);
            if (b != hashCheck.length) throw new Exception("wrong file format!");
            hif.setCheckHash(hashCheck);

            for (int i = 0; i < totalFiles; i++) {

                FileHash fh = new FileHash();

                // read name
                bf = new byte[2];
                b = reader.read(bf);
                if (b != bf.length) throw new Exception("wrong file format!");
                b = Utils.bytesToShort(bf);
                if (b <= 0) throw new Exception("wrong file format!");
                bf = new byte[b];
                b = reader.read(bf);
                if (b != bf.length) throw new Exception("wrong file format!");
                fh.setName(new String(bf, "UTF-8"));

                // read size
                bf = new byte[8];
                b = reader.read(bf);
                if (b != bf.length) throw new Exception("wrong file format!");
                long size = Utils.bytesToLong(bf);
                if (size <= 0) throw new Exception("wrong file format!");
                fh.setSize(size);

                // read hash
                bf = new byte[32];
                b = reader.read(bf);
                if (b != bf.length) throw new Exception("wrong file format!");
                fh.setHash(bf);

                fileHashes.add(fh);
            }

            // check all hashes by hashCheck
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            for (FileHash fh : fileHashes) md.update(fh.getHash());
            if (Arrays.compare(md.digest(), hashCheck) != 0)  throw new Exception("check hash is not correct, file corrupted!");

        }
        
        reader.close();
        fileReader.close();
        
        Utils.out.println(" done");
        
        return hif;
    }

    public static void writeHashIndexFile(HashIndexFile hif, String hashFile) throws Exception {
        
        Utils.out.print("Writing hash index file ["+hashFile+"] ");
        
        FileOutputStream fileWriter = new FileOutputStream(hashFile);
        BufferedOutputStream writer = new BufferedOutputStream(fileWriter);

        int totalFiles = hif.getLength();
        
        writer.write(HashIndexFile.PREFIX);
        writer.write(Utils.intToBytes(totalFiles));
        
        Utils.out.print("files in index: " + totalFiles);
        
        if (totalFiles > 0) {

            // generate hashCheck
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            for (FileHash fh : hif.getHashes()) md.update(fh.getHash());
            writer.write(md.digest());

            for (FileHash fh : hif.getHashes()) {
                
                // write name
                byte[] bf = fh.getName().getBytes("UTF-8");
                writer.write(Utils.shortToBytes((short)bf.length));
                writer.write(bf);
                
                // write size
                writer.write(Utils.longToBytes(fh.getSize()));
                
                // write hash
                writer.write(fh.getHash());
            }

        }        
        
        writer.flush();
        writer.close();
        fileWriter.close();
        
        Utils.out.println(" done");
    }

    
}