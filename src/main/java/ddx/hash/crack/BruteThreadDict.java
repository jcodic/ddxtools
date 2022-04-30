package ddx.hash.crack;

import ddx.common.brute.BruteCommon;
import ddx.common.brute.BruteForce;
import java.util.Arrays;

/**
 *
 * @author druidman
 */
public class BruteThreadDict extends BruteThread {
    
    private final byte[] currentWordBuffer;
    private final boolean[] charsetIsDict;
    protected int currentWordBufferLen;
    
    public String getCurrentWord() {
        
        String s = "";
        for (int i = 0; i < currentWordBufferLen; i++) s += (char)currentWordBuffer[i];
        return s;
    }

    protected boolean checkHash() {
        
        currentWordBufferLen = 0;
        for (int i = 0; i < currentWord.length; i++) {
            
            int indexi = index[i];
            if (indexi == BruteForce.NOT_INIT_VALUE) break;
            if (charsetIsDict[i]) {
                
                byte[] word = (byte[])variants[i][indexi];
                System.arraycopy(word, 0, currentWordBuffer, currentWordBufferLen, word.length);
                currentWordBufferLen += word.length;
            } else currentWordBuffer[currentWordBufferLen++] = (byte)((char)variants[i][indexi]);
        }
        
        md.update(currentWordBuffer, 0, currentWordBufferLen);
        return Arrays.equals(searchFor, md.digest());
    }
    
    public BruteThreadDict(Settings settings, BruteCommon task, byte[] searchFor, boolean[] charsetIsDict) {
        super(settings, task, searchFor);
        this.charsetIsDict = charsetIsDict;
        currentWordBuffer = new byte[512];
    }
}
