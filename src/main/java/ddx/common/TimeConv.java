package ddx.common;

/**
 *
 * @author ddx
 */
public class TimeConv {

    public static final long SECOND = 1000;
    public static final long MINUTE = 60 * SECOND;
    public static final long HOUR = 60 * MINUTE;
    public static final long DAY = 24 * HOUR;
    public static final long WEEK = 7 * DAY;
    
    public static long strToTime(String inp) throws Exception {
        
        if (inp == null || inp.length() < 2) return 0;

        byte[] inpc = inp.replaceAll("\\s+","").toLowerCase().getBytes();
        
        long total = 0;
        String cum = "";
        for (int i = 0; i < inpc.length; i++) {
            
            char c = (char)inpc[i];
            
            if (c >= '0' && c <= '9') cum += c; else {
                
                long n = Long.parseLong(cum); cum = "";
                
                if (c == 'm' && i+1 < inpc.length && (char)inpc[i+1] == 's') {
                
                    total += n;
                    break; // ms is last
                }
                
                switch (c) {

                    case 's' : total += n * SECOND; continue;
                    case 'm' : total += n * MINUTE; continue;
                    case 'h' : total += n * HOUR; continue;
                    case 'd' : total += n * DAY; continue;
                    case 'w' : total += n * WEEK; continue;
                }
            }
        }
        
        if (cum.length() > 0) total = Long.parseLong(cum);
        
        return total;
    }

    private static String append(String ori, long value, String desc) {
        
        return ori + (ori.length()>0?" ":"") + value + "" + desc; // + (value>1?"s":"");
    }
    
    public static String timeToStr(long inp) throws Exception {
    
        return timeToStr(inp, true);
    }
    
    public static String timeToStr(long inp, boolean ms) throws Exception {
        
        if (inp == 0) return "0ms";

        String total = "";
        long value;
        
        if (inp >= WEEK) {
            
            value = Math.floorDiv(inp, WEEK);
            inp -= value * WEEK;
            total = append(total, value, "w");
        }
        
        if (inp >= DAY) {
            
            value = Math.floorDiv(inp, DAY);
            inp -= value * DAY;
            total = append(total, value, "d");
        }
        
        if (inp >= HOUR) {
            
            value = Math.floorDiv(inp, HOUR);
            inp -= value * HOUR;
            total = append(total, value, "h");
        }

        if (inp >= MINUTE) {
            
            value = Math.floorDiv(inp, MINUTE);
            inp -= value * MINUTE;
            total = append(total, value, "m");
        }

        if (inp >= SECOND) {
            
            value = Math.floorDiv(inp, SECOND);
            inp -= value * SECOND;
            total = append(total, value, "s");
        }

        if (inp > 0 && ms) {
            
            total = append(total, inp, "ms");
        }

        return total.length()>0?total:"0ms";
    }

    public static String timeToStrSimple(long inp) throws Exception {
        
        if (inp == 0) return "0ms";
        if (inp >= WEEK) return Utils.roundResult((double)inp / WEEK, 1) + "w";
        if (inp >= DAY) return Utils.roundResult((double)inp / DAY, 1) + "d";
        if (inp >= HOUR) return Utils.roundResult((double)inp / HOUR, 1) + "h";
        if (inp >= MINUTE) return Utils.roundResult((double)inp / MINUTE, 1) + "m";
        if (inp >= SECOND) return Utils.roundResult((double)inp / SECOND, 1) + "s";
        return inp + "ms";
    }
    
}