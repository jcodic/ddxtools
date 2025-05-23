package ddx.hash;

import ddx.common.Str;
import ddx.common.Utils;
import ddx.hash.types.SimpleFile;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author stabi
 */
public class CsvFileUtils {

    public static List<SimpleFile> loadNameSizeFromCsvFile(String fileName) throws Exception {
        
        Utils.out.print("Loading csv file ["+fileName+"] ");
        
        FileReader fileReader = new FileReader(fileName);
        BufferedReader reader = new BufferedReader(fileReader);
        String str;
        reader.readLine();
        
        List<SimpleFile> files = new LinkedList<SimpleFile>();
        
        while ((str = reader.readLine()) != null) {
            

            String[] parts = str.split(";");
            if (parts.length < 2) continue;
            
            String name = parts[0];
            if (Str.isEmpty(name)) continue;
            
            long length = -1;
            
            try {
             length = Long.parseLong(parts[1]);
            } catch (Exception ex) { continue; }
            if (length <= 0) continue;
            
            files.add(new SimpleFile(name, length));
        }
        reader.close();
        fileReader.close();
        
        Utils.out.print("files found: " + files.size());
        
        Utils.out.println(" done");
        
        return files;
    }
    
}