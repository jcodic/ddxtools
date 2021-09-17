package ddx.filetools;

import ddx.common.Utils;
import java.io.File;
import java.io.FileInputStream;
import java.math.RoundingMode;

/**
 *
 * @author ddx
 */
public class FileEntropy extends ToolCarcas {
    
    private FileInputStream fis;

    private final int entropyCounts[] = new int[256];
    private double dataEntropy = 0.0d;

    public void init(File file) throws Exception {
        
        fis = new FileInputStream(file);
    }

    protected void entropyResult() {

        double total = (double)filesSizeProcessed;

        for (int c : entropyCounts) {
            if (c == 0) {
                continue;
            }
            double p = c / total;
            dataEntropy -= p * Math.log(p) / Math.log(2);
        }
    }
    
    public void process() throws Exception {
        
        byte[] bf = new byte[fileBfSize];
        int readSize;
        
        while ((readSize = fis.read(bf)) != -1) {
            
            for (int i = 0; i < readSize; i++) entropyCounts[bf[i] + 128]++;
            filesSizeProcessed += readSize;
        }
        
        fis.close();
        entropyResult();
        
        Utils.out.println("File entropy : "+Utils.roundResult(dataEntropy, 8, RoundingMode.HALF_UP));
    }

}