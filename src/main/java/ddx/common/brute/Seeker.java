package ddx.common.brute;

import java.util.Arrays;

/**
 *
 * @author druidman
 */
public class Seeker {
 
    private final BruteCommon task;
    private final long[] variantsPerIndexItem;
    private final long[] variantsPerIndexItemAddon;
    private final long indexFinal;
    private long variantsLeft;
    

    public void seekStart() {
        
        task.index = Arrays.copyOf(task.indexStart, task.length);
    }

    public void seekEnd() {
        
        task.index = Arrays.copyOf(task.indexFinal, task.length);
    }

    public long seek(long ticks) {
        
        long index = getIndexAsLong(task.index);
        long maxTicks = indexFinal - index;
        if (maxTicks == 0) return ticks;
        if (ticks > maxTicks) {
            
            seekEnd();
            return ticks-maxTicks;
        }
        task.index = getLongAsIndex(index+ticks);
        return variantsLeft;
    }

    private final void initVariantsPerIndex() {
        
        variantsPerIndexItem[0] = 1;
        variantsPerIndexItemAddon[0] = task.indexStart[0] == BruteForce.NOT_INIT_VALUE ? 1 : 0;
        long cumulative = task.variants[0].length;
        
        for (int i = 1; i < task.length; i++) {
            
            variantsPerIndexItem[i] = cumulative;
            cumulative *= task.variants[i].length;
            variantsPerIndexItemAddon[i] = task.indexStart[i] == BruteForce.NOT_INIT_VALUE?(task.variants[i-1].length * variantsPerIndexItem[i-1]):0;
        }
    }
    
    public final long getIndexAsLong(int[] index) {
        
        long result = 0;
        
        for (int i = 0; i < task.length; i++) {
            
            int indexi = index[i];
            if (indexi != BruteForce.NOT_INIT_VALUE) result += variantsPerIndexItemAddon[i];
            if (indexi > 0) result += variantsPerIndexItem[i] * indexi;
        }
        
        return result;
    }
    
    public final int[] getLongAsIndex(long index) {

        int[] result = Arrays.copyOf(task.indexStart, task.length);
        
        for (int workIndex = task.length-1; workIndex >= 0; workIndex--) {
            
            if (variantsPerIndexItemAddon[workIndex] <= index) {
                
                index -= variantsPerIndexItemAddon[workIndex];
                result[workIndex] = 0;
            }
        }

        for (int workIndex = task.length-1; workIndex >= 0; workIndex--) {
            
            while (variantsPerIndexItem[workIndex] <= index && result[workIndex] < task.variants[workIndex].length-1) {

                result[workIndex]++;
                index -= variantsPerIndexItem[workIndex];
            }
        }
        
        variantsLeft = index;
        
        return result;
    }

    public long setLongAsIndex(long index) {
        
        task.index = getLongAsIndex(index);
        return variantsLeft;
    }

    public long getVariantsLeft(int[] index) {
        
        return indexFinal - getIndexAsLong(index);
    }

    public Seeker(BruteCommon task) {
        this.task = task;
        variantsPerIndexItem = new long[task.length];
        variantsPerIndexItemAddon = new long[task.length];
        initVariantsPerIndex();
        indexFinal = getIndexAsLong(task.indexFinal);
    }
    
}