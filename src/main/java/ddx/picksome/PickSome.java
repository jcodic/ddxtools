package ddx.picksome;

import ddx.common.Utils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 *
 * @author druidman
 */
public class PickSome {
    

    private static void printInfo() {
        
        Utils.out.println("Usage: picksome <number> [option1] [option2] ... [optionN]");
        Utils.out.println("Pick some <number> of options from all provided [options] with random generator");
        Utils.out.println("Example: picksome 1 dog cat");
        Utils.out.println("Example: picksome 2 dog cat snake cow rabbit fox");
        Utils.out.println("Example: picksome 1 seed=777 yes no");
        Utils.out.println("Example: picksome 1 range=3-5 range=100-105");
    }

    private static void addRange(String range, List<String> options) throws Exception {
        
        if (range.length() < 3 || !range.contains("-")) throw new Exception("invalid range ["+range+"]");
        int sep = range.indexOf("-");
        int from = Integer.parseInt(range.substring(0, sep));
        int to = Integer.parseInt(range.substring(sep+1));
        if (from >= to) throw new Exception("invalid range ["+range+"]");
        for (int i = from; i <= to; i++) options.add(String.valueOf(i));
    }
    
    public static void main(String[] args) {
        
        try {

            if (args == null || args.length < 2) {
                
                printInfo();
                return;
            }

            long seed = System.currentTimeMillis();

            String argSeed = "seed=";
            String argRange = "range=";
            
            List<String> options = new ArrayList<>(args.length-1);
            for (int i = 1; i < args.length; i++) {
                
                String option = args[i];
                if (option.startsWith(argSeed)) seed = Long.parseLong(option.substring(argSeed.length())); else
                if (option.startsWith(argRange)) addRange(option.substring(argRange.length()), options); else
                options.add(option);
            }
            int num = Integer.parseInt(args[0]);
            if (num > options.size()) {
                
                Utils.out.println("Number ["+num+"] should be no less than options provided ["+options.size()+"]");
                return;
            }
            
            Random rnd = new Random(seed);

            Utils.out.println("Pick "+num+" from "+options.size());
            Utils.out.println("Using random seed: "+seed);
            Utils.out.println("Available options: "+Arrays.toString(options.toArray(new String[0])));
            
            Collections.shuffle(options, rnd);
            
            for (int i = 0; i < num; i++) {
                
                int index = rnd.nextInt(options.size());
                String option = options.get(index);
                options.remove(index);
                Utils.out.println("Picked: ["+option+"] options left unpicked = "+options.size());
            }
            
        } catch (Exception ex) { ex.printStackTrace(); }
    }

}