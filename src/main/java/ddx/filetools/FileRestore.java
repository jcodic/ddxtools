package ddx.filetools;

import ddx.common.Utils;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 *
 * @author ddx
 */
public class FileRestore extends ToolCarcas {
    
    private File file;
    private File fileOut;
    private int fileCounter = 0;
    private String baseName;
    
    
    public void init(File file, File fileOut) throws Exception {
        
        this.file = file;
        this.fileOut = fileOut;
        
        String s = file.getAbsolutePath();
        fileCounter = Integer.parseInt(s.substring(s.length()-3));
        baseName = s.substring(0, s.length()-4);
    }

    private String getNextFileCounter() {
        
        String s = String.valueOf(++fileCounter);
        while (s.length() < 3) s = "0" + s;
        return s;
    }
    
    private File getNextFile() {
        
        String nextName = baseName+"."+getNextFileCounter();
        Utils.out.println("Reading next piece "+nextName);
        File file = new File(nextName);
        return file;
    }

    public boolean process() throws Exception {
        
        FileOutputStream fos = new FileOutputStream(fileOut);
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        byte[] bf = new byte[fileBfSize];
        
        int readSize = 0;
        
        while (file.exists()) {
            
        FileInputStream fis = new FileInputStream(file);
        BufferedInputStream bis = new BufferedInputStream(fis);
        
            while ((readSize = bis.read(bf)) != -1) {

                filesSizeProcessed += readSize;
                bos.write(bf, 0, readSize);
            }
            
            bis.close();
            fis.close();
            
            file = getNextFile();
        }
        
        bos.close();
        fos.close();
        
        Utils.out.println("done.");
        
        return true;
    }

}