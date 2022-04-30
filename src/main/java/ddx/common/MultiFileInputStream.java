package ddx.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author ddx
 */
public class MultiFileInputStream extends InputStream {
    
    private FileInputStream fis;
    private final String fosPath;
    private int fosCounter;
    private boolean isFinished = false;
    
    private String getNextFileCounter() {
        
        String s = String.valueOf(++fosCounter);
        while (s.length() < 3) s = "0" + s;
        return s;
    }
    
    private File getNextFile() {
        
        String nextName = fosPath+"."+getNextFileCounter();
        File file = new File(nextName);
        return file.exists()?file:null;
    }

    private int checkAvailable() throws IOException {
        
        if (isFinished) return -1;
        
        if (fis == null) {
            
            File nextFile = getNextFile();
            if (nextFile != null) fis = new FileInputStream(nextFile);
        }
        
        if (fis == null) {
            
            isFinished = true;
            return -1;
        }
        
        int av = fis.available();
        
        if (av == 0) {
            
            fis.close();
            fis = null;
            File nextFile = getNextFile();
            if (nextFile != null) fis = new FileInputStream(nextFile);
        } else return av;
        
        if (fis == null) {
            
            isFinished = true;
            return -1;
        }
        
        return fis.available();
    }
    
    @Override
    public int read() throws IOException {

        return (checkAvailable()>0)?fis.read():-1;
    }

    @Override
    public int read(byte b[]) throws IOException {
        return readBytes(b, 0, b.length);
    }    
    
    @Override
    public int read(byte b[], int off, int len) throws IOException {
        return readBytes(b, off, len);
    }    

    private int readBytes(byte b[], int off, int len) throws IOException {
        
        int av = checkAvailable();
        if (av < 1) return -1;

        int totalRead = 0;
        
        while (av > 0 && len > 0) {
            
            int toRead = Math.min(av, len);
            int read = fis.read(b, off, toRead);
            totalRead += read;
            len -= read;
            off += read;
            av = checkAvailable();
        }
        
        return totalRead>0?totalRead:-1;
    }
    
    public MultiFileInputStream(String fosPath) {
        this.fosPath = fosPath;
    }

}