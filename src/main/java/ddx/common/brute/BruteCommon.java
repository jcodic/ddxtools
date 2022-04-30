package ddx.common.brute;

import java.util.Arrays;
import java.util.List;

/**
 *
 * @author druidman
 */
public class BruteCommon implements BruteForce {
    
    protected final int length; // the length of combination
    protected Object[][] variants; // [element of combination][variations of element]
    protected int[] index; // index of each element of combination
    protected int[] indexStart;
    protected int[] indexFinal;
    protected long variantsProcessed;

    @Override
    public int getLength() {
        return length;
    }

    public Object[][] getVariants() {
        return variants;
    }
    
    public void setVariants(int atIndex, Object[] vars) {
        
        variants[atIndex] = vars;
        indexFinal[atIndex] = vars.length-1;
    }

    public void setVariants(int atIndex, List vars) {
        
        setVariants(atIndex, vars.toArray());
    }

    public void setStartIndex(int atIndex, int value) {
        
        indexStart[atIndex] = value;
    }
            
    public void setStartIndexNull(int atIndex) {
        
        setStartIndex(atIndex, BruteForce.NOT_INIT_VALUE);
    }

    public int[] getIndex() {
        return index;
    }

    public int[] getIndexStart() {
        return indexStart;
    }

    public int[] getIndexFinal() {
        return indexFinal;
    }

    @Override
    public void init() {
        
        index = Arrays.copyOf(indexStart, length);
        variantsProcessed = 0;
    }
    
    @Override
    public void reset() {
        
        init();
    }
    
    @Override
    public void reset(int nfirst) {
        
        Arrays.fill(index, 0, nfirst, 0);
    }

    @Override
    public boolean hasNext() {
        
        for (int i = length-1; i >= 0; i--) if (index[i] < indexFinal[i]) return true;
        return false;
    }
    
    @Override
    public boolean next() {
        
        for (int i = 0; i < length; i++) {
            
            if (index[i] < variants[i].length-1) {
                
                index[i]++;
                if (i > 0) reset(i);

                variantsProcessed++;
                return true;
            }
        }
            
        return false;
    }

    @Override
    public long getVariantsTotal() {
        
        long result = variants[0].length;
        
        for (int i = 1; i < length; i++) result *= variants[i].length;

        long previous = 0;
        
        for (int i = 0; i < length; i++) {
            
            if (index[i] == BruteForce.NOT_INIT_VALUE) result += (i==0?1:previous);
            else if (index[i] > 0) result -= index[i] * (i==0?1:previous);

            previous = variants[i].length * (i==0?1:previous);
        }

        return result-1;
    }

    @Override
    public long getVariantsLeft() {
        
        return new Seeker(this).getVariantsLeft(index);
    }
    
    @Override
    public long getVariantsProcessed() {
        
        return variantsProcessed;
    }

    @Override
    public String[] getArgs() {
        
        String[] s = new String[length];
        for (int i = 0; i < length; i++) {
            
            int indexi = index[i];
            if (indexi != BruteForce.NOT_INIT_VALUE) s[i] = String.valueOf(variants[i][indexi]);
        }
        return s;
    }

    @Override
    public Object[] getCurrent() {
        
        Object[] s = new Object[length];
        for (int i = 0; i < length; i++) {
            
            int indexi = index[i];
            if (indexi == BruteForce.NOT_INIT_VALUE) break;
            s[i] = variants[i][indexi];
        }
        return s;
    }

    public byte[] getCurrentBytes() {
        
        byte[] s = new byte[length];
        int len = 0;
        for (int i = 0; i < length; i++) {
            
            int indexi = index[i];
            if (indexi == BruteForce.NOT_INIT_VALUE) break;
            s[len++] = (byte)((char)variants[i][indexi]);
        }
        return len==length?s:Arrays.copyOfRange(s, 0, len);
    }
    
    @Override
    public BruteForce getClone() {

        BruteCommon clone = new BruteCommon(length);
        clone.variants = variants; // read-only field
        clone.index = Arrays.copyOf(index, length);
        clone.indexStart = Arrays.copyOf(indexStart, length);
        clone.indexFinal = Arrays.copyOf(indexFinal, length);

        return clone;
    }
    
    public BruteCommon(int length) {
        this.length = length;
        if (length == 0) return;
        variants = new Object[length][];
        indexStart = new int[length];
        Arrays.fill(indexStart, BruteForce.NOT_INIT_VALUE);
        indexFinal = new int[length];
    }

}