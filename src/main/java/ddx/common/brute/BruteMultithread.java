package ddx.common.brute;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author druidman
 */
public class BruteMultithread {


    public static List<BruteCommon> divideTask(BruteCommon task, int threads) {
        
        long variants = task.getVariantsTotal();
        if (variants < threads || threads == 1) {
            
            List<BruteCommon> tasks = new ArrayList<>(1);
            tasks.add(task);
            return tasks;
        }
        
        List<BruteCommon> tasks = new ArrayList<>(threads);
        
        long variantsPerThread = Math.max(1, variants / threads);
        
        Seeker seeker = new Seeker(task);
        
        while (tasks.size() < threads) {
            
            BruteCommon clone = (BruteCommon)task.getClone();
            tasks.add(clone);
            
            if (tasks.size() < threads) {
                
                seeker.seek(variantsPerThread);
                clone.indexFinal = Arrays.copyOf(task.index, task.length);
            }
        }
        
        return tasks;
    }
    
}