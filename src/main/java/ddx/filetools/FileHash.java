package ddx.filetools;

import ddx.common.Utils;
import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;

/**
 *
 * @author ddx
 */
public class FileHash extends ToolCarcas {
    
    private FileInputStream fis;
    private MessageDigest md;
    private String algo;

    public void init(File file, String algo) throws Exception {
        
        fis = new FileInputStream(file);
        md = MessageDigest.getInstance(algo);
        this.algo = algo;
    }

    public void process() throws Exception {
        
        byte[] bf = new byte[fileBfSize];
        int readSize;
        
        while ((readSize = fis.read(bf)) != -1) {
            
            md.update(bf, 0, readSize);
            filesSizeProcessed += readSize;
        }
        
        fis.close();
        
        byte[] hash = md.digest();
        
        Utils.out.println(algo+" bits  : "+hash.length * 8);
        Utils.out.println(algo+" value : "+Utils.toHex(hash));
    }

}