package ddx.filetools;

import ddx.common.Const;
import ddx.common.Utils;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Random;

/**
 *
 * @author ddx
 */
public class FileInverse extends ToolCarcas {
    
    private FileInputStream fis;
    private FileOutputStream fos;
    private BufferedOutputStream bos;
    private boolean eof = false;
    private int shuffle;
    private long position;
    private long len;
    private Random rnd;
    private int loadedTotal;
    
    public void init(File fileIn, File fileOut, int shuffle) throws Exception {
        
        fis = new FileInputStream(fileIn);
        fos = new FileOutputStream(fileOut);
        bos = new BufferedOutputStream(fos);
        len = fileIn.length();
        position = len;
        rnd = new Random(System.currentTimeMillis());
        this.shuffle = shuffle;
    }

    public static void inverse(byte[] bf) {
        
        int i = 0;
        int j = bf.length - 1;
        
        while (i < j) {
            
            byte b = bf[i];
            bf[i] = bf[j];
            bf[j] = b;
            i++;
            j--;
        }
    }

    public static void shuffle(byte[] bf, int iter, Random rnd) {
        
        int i = 0;
        int bflen = bf.length;
        
        if (bflen < 2) return;
        
        while (i < iter) {
        
            int ind1 = rnd.nextInt(bflen);
            int ind2 = rnd.nextInt(bflen);
            
            if (ind1 == ind2) continue;
            
            byte b = bf[ind1];
            bf[ind1] = bf[ind2];
            bf[ind2] = b;
            
            i++;
        }
    }
    
    private boolean getNewDataPortion() throws Exception {
        
        if (eof) return false;
        
        long position2 = Math.max(position - fileBfSize, 0);
        long delta = position - position2;
        
        if (delta <= 0) {
            
            eof = true;
            fis.close();
            bos.close();
            fos.close();
            return false;
        }
        
        byte[] portion = new byte[(int)delta];
        
        fis.getChannel().position(position2);
        int readBytes = fis.read(portion);
        if (readBytes != portion.length) {
            
            throw new Exception("Portion supposed to be ["+portion.length+"] read size ["+readBytes+"]");
        }

        filesSizeProcessed += readBytes;
        loadedTotal += readBytes;
        
        inverse(portion);
        if (shuffle > 0) shuffle(portion, shuffle, rnd);
        bos.write(portion);
        
        position = position2;
        
        return true;
    }

    public void process() throws Exception {
        
        loadedTotal = 0;
        
        Utils.out.print("Inversing ");
        
        while (getNewDataPortion()) {

            if (loadedTotal >= Const.PRINT_STR_AT_EVERY_BYTES) {

                Utils.out.print(Const.PRINT_STR);
                loadedTotal -= Const.PRINT_STR_AT_EVERY_BYTES;
            }
        }
        
        Utils.out.println(" done");
    }

}