package ddx.hash;

import ddx.common.Utils;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

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
    
}