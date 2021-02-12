package ddx.hash.crack;

import ddx.common.brute.BruteCommon;

/**
 *
 * @author druidman
 */
public class BruteThreadPerf extends BruteThread {

    private final String PHRASE = "this phrase";
    private final byte[] BUFFER = PHRASE.getBytes();
    private long total;
    
    @Override
    public String getCurrentWord() {
        
        return PHRASE;
    }

    @Override
    protected boolean checkHash() {
        
        md.update(BUFFER);
        md.digest();
        total++;
        return false;
    }
    
    @Override
    protected void process() throws Exception {
     
        while (!stopped) checkHash();
    }

    @Override
    public long getTotalVariantsProcessed() {
        return total;
    }

    public BruteThreadPerf(Settings settings, BruteCommon task) {
        super(settings, task, null);
    }
}