package ddx.common;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 *
 * @author ddx
 */
public class MultiFileOutputStream extends OutputStream {

    
    private FileOutputStream fos;
    private long fosWritten;
    private final long maxLength;
    private final String fosPath;
    private int fosCounter;
    
    private String getNextFileCounter() {
        
        String s = String.valueOf(++fosCounter);
        while (s.length() < 3) s = "0" + s;
        return s;
    }
    
    private File getNextFile() {
        
        String nextName = fosPath+"."+getNextFileCounter();
        File file = new File(nextName);
        return file;
    }

    private void openNewFile() throws IOException {
        
        fos = new FileOutputStream(getNextFile());
        fosWritten = 0;
    }
    
    private void closeFile() throws IOException {
        
        fos.flush();
        fos.close();
        fos = null;
    }
    
    private void increaseWritten(long value) throws IOException {
        
        fosWritten += value;
        if (fosWritten >= maxLength) {
            
            closeFile();
        }
    }
    
    @Override
    public void write(int b) throws IOException {
    
        if (fos == null) openNewFile();
        fos.write(b);
        increaseWritten(1);
    }     
    
    @Override
    public void write(byte b[]) throws IOException {
    
        writeBytes(b, 0, b.length); 
    }     

    @Override
    public void write(byte b[], int off, int len) throws IOException {
        writeBytes(b, off, len);
    }     
    
    private void writeBytes(byte b[], int off, int len) throws IOException {
        
        if (len == 0) return;
        
        while (len > 0) {
            
            if (fos == null) openNewFile();
            long canWrite = maxLength - fosWritten;
            long toWrite = Math.min(canWrite, len);
            fos.write(b, off, (int)toWrite);
            off += (int)toWrite;
            len -= (int)toWrite;
            increaseWritten((int)toWrite);
        }
    }
    
    @Override
    public void flush() throws IOException {    

        if (fos != null) fos.flush();
    }

    @Override
    public void close() throws IOException {    

        if (fos != null) closeFile();
    }

    public MultiFileOutputStream(long maxLength, String fosPath) {
        this.maxLength = maxLength;
        this.fosPath = fosPath;
        this.fosCounter = 0;
    }
    
}