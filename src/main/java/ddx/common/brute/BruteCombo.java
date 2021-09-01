package ddx.common.brute;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author druidman
 */
public class BruteCombo implements BruteForce {
    
    protected List<BruteForce> brutes;
    protected int length;
    protected long variantsProcessed;
    
    @Override
    public int getLength() {
        return length;
    }

    public void addBrute(BruteCommon brute) {
        
        if (brutes == null) brutes = new ArrayList<>();
        brutes.add(brute);
    }

    @Override
    public void init() {
        
        length = 0;
        for (BruteForce brute : brutes) {
            
            brute.init();
            length += brute.getLength();
        }
        variantsProcessed = 0;
    }

    @Override
    public void reset() {
        
        for (BruteForce brute : brutes) brute.reset();
        variantsProcessed = 0;
    }

    @Override
    public void reset(int nfirst) {
        
        for (int i = 0; i < nfirst; i++) {
            
            BruteForce brute = brutes.get(i);
            brute.reset(brute.getLength());
        }
    }
    
    @Override
    public boolean hasNext() {
    
        for (BruteForce brute : brutes) if (brute.hasNext()) return true;
        
        return false;
    }
    
    @Override
    public boolean next() {
        
        for (int i = 0; i < brutes.size(); i++) {
            
            BruteForce brute = brutes.get(i);
            
            if (brute.hasNext() && brute.next()) {
                
                if (i > 0) reset(i);

                variantsProcessed++;
                return true;
            }
        }
        return false;
    }

    @Override
    public long getVariantsTotal() {

        return -1;
    }

    @Override
    public long getVariantsLeft() {
        
        return -1;
    }
    
    @Override
    public long getVariantsProcessed() {
        
        return variantsProcessed;
    }

    @Override
    public String[] getArgs() {
        
        return null;
    }

    @Override
    public Object[] getCurrent() {
    
        Object[] s = new Object[length];
        int resultIndex = 0;
        
        for (BruteForce brute : brutes) for (Object obj : brute.getCurrent()) s[resultIndex++] = obj;
        
        return s;
    }
    
    @Override
    public BruteForce getClone() {

        BruteCombo clone = new BruteCombo();
        
        if (brutes != null && !brutes.isEmpty()) {
            
            clone.brutes = new ArrayList<>(brutes.size());
            for (BruteForce brute : brutes) clone.brutes.add(brute.getClone());
        }
        
        return clone;
    }
    
}