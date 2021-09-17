package ddx.filetools;

import ddx.common.Const;
import ddx.common.Str;
import ddx.common.Utils;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;

/**
 *
 * @author ddx
 */
public class FileSearch extends ToolCarcas {
    
    private byte[] data;
    private FileInputStream fis;
    private BufferedInputStream bis;
    private boolean eof = false;
    private int position = -1;
    private long globalPosition;
    private int SP;
    private int SP2;

    public void init(File file, long start, int SP) throws Exception {
        
        this.SP = SP;
        String maxLen = Const.NUM_FORMATTER.format(file.length());
        SP2 = maxLen.length() + 2;
        
        fis = new FileInputStream(file);
        if (start > 0) {
            
            fis.getChannel().position(start);
            globalPosition = start;
        }
        bis = new BufferedInputStream(fis);
        getNewDataPortion();
    }

    private boolean getNewDataPortion() throws Exception {
        
        if (eof) return false;
        
        byte[] portion = new byte[fileBfSize];
        int readBytes = bis.read(portion);
        if (readBytes <= 0) {
            
            eof = true;
            bis.close();
            fis.close();
            return false;
        }
        
        filesSizeProcessed += readBytes;
        
        if (data == null || position == data.length) {
            
            if (readBytes < fileBfSize) {
                
                data = Arrays.copyOf(portion, readBytes);
            } else {
                
                data = portion;
            }
            
            position = 0;
            return true;
        }
        
        byte[] comb = new byte[readBytes + data.length - position];
        
        int combi = 0;
        
        for (int i = position; i < data.length; i++) comb[combi++] = data[i];
        for (int i = 0; i < readBytes; i++) comb[combi++] = portion[i];
        
        data = comb;
        position = 0;
        return true;
    }

    private boolean getMoreData(int len) throws Exception {
        
        if (position + len > data.length) if (!getNewDataPortion()) return false;
        if (position + len > data.length) return false;
        
        position++;
        globalPosition++;
        return true;
    }

    private void printPos(String title, long pos) {
        
        Utils.out.println(Str.getStringWPrefix(title, SP, " ", false)+" : "+
                Str.getStringWPrefix(Const.NUM_FORMATTER.format(pos), SP2, " ", true)
                );
    }
    
    public void process(byte[] searchBf) throws Exception {
        
        int len = searchBf.length;
        
        int totalFound = 0;
        
        while (getMoreData(len)) {
            
            boolean found = true;
            
            for (int i = 0; i < len; i++) {
                
                if (data[position + i - 1] != searchBf[i]) {
                    
                    found = false;
                    break;
                }
            }
            
            if (found) {
                
                totalFound++;
                printPos("Found at position", globalPosition-1);
            }
        }
        
        Utils.out.println("Buffer total founds times : "+Const.NUM_FORMATTER.format(totalFound));
    }

}