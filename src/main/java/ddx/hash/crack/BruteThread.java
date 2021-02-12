package ddx.hash.crack;

import ddx.common.brute.BruteCommon;
import ddx.common.EmptyThread;
import ddx.common.brute.BruteForce;
import java.security.MessageDigest;
import java.util.Arrays;

/**
 *
 * @author druidman
 */
public class BruteThread extends EmptyThread {

    protected final Settings settings;
    protected final BruteCommon task;
    protected final int[] index;
    protected final Object[][] variants;
    protected final byte[] searchFor;
    protected final byte[] currentWord;
    protected int currentWordLen;
    protected MessageDigest md;
    protected long totalTimeConsumed; // in milliseconds
    protected boolean found;
    
    public synchronized boolean isFound() {
        return found == true;
    }

    public String getCurrentWord() {
        
        String s = "";
        for (int i = 0; i < currentWordLen; i++) s += (char)currentWord[i];
        return s;
    }

    protected boolean checkHash() {
        
        currentWordLen = 0;
        for (int i = 0; i < currentWord.length; i++) {
            
            int indexi = index[i];
            if (indexi == BruteForce.NOT_INIT_VALUE) break;
            currentWord[currentWordLen++] = (byte)((char)variants[i][indexi]);
        }
        
        md.update(currentWord, 0, currentWordLen);
        return Arrays.equals(searchFor, md.digest());
    }
    
    protected void process() throws Exception {
     
        while (task.hasNext() && !stopped) {
            
            task.next();
            
            if (checkHash()) {

                found = true;
                break;
            }
        }
    }
    
    @Override
    public void run() {

        try {
    
            md = MessageDigest.getInstance(settings.algorithm);
            
            long start = System.currentTimeMillis();
        
            process();
            
            long end = System.currentTimeMillis();
            totalTimeConsumed = end - start;

            doComplete();
            
        } catch (Exception ex) { ex.printStackTrace(); stopped = true; }
    }

    public long getTotalVariantsProcessed() {
        return task.getVariantsProcessed();
    }

    public long getTotalTimeConsumed() {
        return totalTimeConsumed;
    }

    public BruteThread(Settings settings, BruteCommon task, byte[] searchFor) {
        this.settings = settings;
        this.task = task;
        this.searchFor = searchFor;
        this.variants = task.getVariants();
        this.index = task.getIndex();
        this.currentWord = new byte[task.getLength()];
    }

}