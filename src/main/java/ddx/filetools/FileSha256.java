package ddx.filetools;

import ddx.common.Utils;
import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;

/**
 *
 * @author ddx
 */
public class FileSha256 extends ToolCarcas {
    
    private FileInputStream fis;
    private MessageDigest md;

    public void init(File file) throws Exception {
        
        fis = new FileInputStream(file);
        md = MessageDigest.getInstance("SHA-256");
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
        
        Utils.out.println("Sha256 value : "+Utils.toHex(hash));
    }

}