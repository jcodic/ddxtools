package ddx.filetools;

import ddx.common.Const;
import ddx.common.Utils;

/**
 *
 * @author ddx
 */
public class ToolCarcas {
 
    protected int fileBfSize = Const.FILE_BUFFER_SIZE;
    protected long filesSizeProcessed;
    private long t1;
    private long t2;

    public int getFileBfSize() {
        return fileBfSize;
    }

    public void setFileBfSize(int fileBfSize) {
        this.fileBfSize = fileBfSize;
    }

    public void start() {
        
        t1 = System.currentTimeMillis();
    }
    
    public void end() {
        
        t2 = System.currentTimeMillis();
    }

    public void printSpeed() {
        
        long time = (t2 - t1) / 1_000;
        
        if (time > 0) {
            
            long bytesPerSecong = filesSizeProcessed / time;
            Utils.out.println("Processing speed: "+Utils.describeFileLength(bytesPerSecong)+" per second.");
        }
    }
}
