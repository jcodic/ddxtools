package ddx.filetools;

import ddx.common.Utils;
import java.io.File;
import java.io.FileInputStream;

/**
 *
 * @author ddx
 */
public class FileByteValue extends ToolCarcas {
    
    private FileInputStream fis;

    public void init(File file) throws Exception {
        
        fis = new FileInputStream(file);
    }

    public void process() throws Exception {
        
        byte[] bf = new byte[fileBfSize];
        int readSize;
        byte resultXor = 0;
        long resultAdd = 0;
        
        while ((readSize = fis.read(bf)) != -1) {
            
            for (int i = 0; i < readSize; i++) {
                
                byte b = bf[i];
                resultXor ^= b;
                resultAdd += b;
            }
            filesSizeProcessed += readSize;
        }
        
        fis.close();
        
        Utils.out.println("Byte xor value : "+Utils.toHex(new byte[]{resultXor}));
        Utils.out.println("Byte add value : "+Utils.toHex(Utils.longToBytes(resultAdd)));
    }

}