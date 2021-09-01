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
public class FileCut extends ToolCarcas {
    
    private int maxFiles;
    private File file;
    private File fileOut;
    private int fileCounter = 0;
    private long start;
    private long portion;
    
    
    public void init(File file, File fileOut, int maxFiles, long start, long portion) throws Exception {
        
        this.file = file;
        this.fileOut = fileOut;
        this.maxFiles = maxFiles;
        this.start = start;
        this.portion = portion;
    }

    private String getNextFileCounter() {
        
        String s = String.valueOf(++fileCounter);
        while (s.length() < 3) s = "0" + s;
        return s;
    }
    
    private File getNextFile() {
        
        String nextName = file.getName()+"."+getNextFileCounter();
        Utils.out.println("Writing file piece "+nextName);
        File file = new File(Utils.completePath(fileOut.getAbsolutePath())+nextName);
        return file;
    }

    public boolean process() throws Exception {
        
        FileInputStream fis = new FileInputStream(file);
        if (start > 0) fis.getChannel().position(start);
        BufferedInputStream bis = new BufferedInputStream(fis);
        FileOutputStream fos = new FileOutputStream(getNextFile());
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        byte[] bf = new byte[fileBfSize];
        
        int readSize = 0;
        int readStart = 0;
        int wrote = 0;
        int portionNum = 0;
        
        while ((portionNum < maxFiles || maxFiles < 1) && (readSize = bis.read(bf)) != -1) {

            readStart = 0;
            
            filesSizeProcessed += readSize;
                    
            while ((portionNum < maxFiles || maxFiles < 1) && readSize > 0) {
            
                int toWrite = Math.min((int)(portion - wrote), readSize);
                
                if (toWrite == 0) {
                    
                    bos.close();
                    fos.close();
                    fos = new FileOutputStream(getNextFile());
                    bos = new BufferedOutputStream(fos);
                    wrote = 0;
                    continue;
                }
                
                bos.write(bf, readStart, toWrite);
                
                readSize -= toWrite;
                readStart += toWrite;
                wrote += toWrite;
                portionNum++;
            }
        }
        
        bis.close();
        fis.close();
        bos.close();
        fos.close();
        
        Utils.out.println("done.");
        
        return true;
    }

}