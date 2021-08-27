package ddx.hash.crack;

import ddx.common.Const;
import ddx.common.Utils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author druidman
 */
public class DictUtils {
    
    
    public static List<byte[]> loadDictionary(String path) throws Exception {
        
        Utils.out.print("Loading dictionary file ["+path+"] ... ");
        
        File file = new File(path);

        if (!file.exists()) {
            
            Utils.out.println("not exists!");
            return null;
        }

        if (!file.isFile()) {
            
            Utils.out.println("is not a file!");
            return null;
        }
        
        FileReader fileReader = new FileReader(file);
        BufferedReader reader = new BufferedReader(fileReader);
        String str;
        
        List<byte[]> dict = new LinkedList<>();
        
        while ((str = reader.readLine()) != null) {

            dict.add(str.getBytes("UTF-8"));
        }
        reader.close();
        fileReader.close();

        Utils.out.println(" done, size = "+Const.FORMATTER_INT.format(dict.size())+" word(s).");
        
        return dict;
    }

}
