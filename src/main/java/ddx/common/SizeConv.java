package ddx.common;

/**
 *
 * @author ddx
 */
public class SizeConv {

    public static final long KB = 1024;
    public static final long MB = 1024 * KB;
    public static final long GB = 1024 * MB;
    public static final long TB = 1024 * GB;
    public static final long PB = 1024 * TB;
    
    public static long strToSize(String inp) throws Exception {
        
        if (inp == null || inp.length() < 1) return 0;

        byte[] inpc = inp.replaceAll("\\s+","").toLowerCase().getBytes();
        
        String cum = "";
        String postfix = "";
        for (int i = 0; i < inpc.length; i++) {
            
            char c = (char)inpc[i];
            if (c >= '0' && c <= '9') cum += c; else {
                
                postfix += c;
                for (int j = i + 1; j < inpc.length; j++) postfix += (char)inpc[j];
                break;
            }
        }

        long n = Long.parseLong(cum);

        if (postfix.length() == 0) return n;
        
        switch (postfix) {

            case "b"  :
            case "bytes" : return n;
            case "kb" : return n * KB;
            case "mb" : return n * MB;
            case "gb" : return n * GB;
            case "tb" : return n * TB;
            case "pb" : return n * PB;
            default : return 0;
        }
    }
    
    private static String append(String ori, long value, String desc) {
        
        return ori + (ori.length()>0?" ":"") + value + "" + desc;
    }
    
    public static String sizeToStr(long inp) throws Exception {
        
        if (inp == 0) return "0";

        String total = "";
        long value;
        
        if (inp >= PB) {
            
            value = Math.floorDiv(inp, PB);
            inp -= value * PB;
            total = append(total, value, "Pb");
        }
        
        if (inp >= TB) {
            
            value = Math.floorDiv(inp, TB);
            inp -= value * TB;
            total = append(total, value, "Tb");
        }

        if (inp >= GB) {
            
            value = Math.floorDiv(inp, GB);
            inp -= value * GB;
            total = append(total, value, "Gb");
        }
        
        if (inp >= MB) {
            
            value = Math.floorDiv(inp, MB);
            inp -= value * MB;
            total = append(total, value, "Mb");
        }

        if (inp >= KB) {
            
            value = Math.floorDiv(inp, KB);
            inp -= value * KB;
            total = append(total, value, "Kb");
        }

        if (inp > 0) {
            
            total = append(total, inp, "");
        }

        return total;
    }

    public static String sizeToStrSimple(long inp) throws Exception {
        
        if (inp == 0) return "0";

        double value = (double)inp;
        int digits = 1;
        
        if (value < 1000) return Utils.roundResult(value, digits) + "";
        value /= 1024.0d;
        if (value < 1000) return Utils.roundResult(value, digits) + "Kb";
        value /= 1024.0d;
        if (value < 1000) return Utils.roundResult(value, digits) + "Mb";
        value /= 1024.0d;
        if (value < 1000) return Utils.roundResult(value, digits) + "Gb";
        value /= 1024.0d;
        if (value < 1000) return Utils.roundResult(value, digits) + "Tb";
        value /= 1024.0d;
        if (value < 1000) return Utils.roundResult(value, digits) + "Pb";
        value /= 1024.0d;
        if (value < 1000) return Utils.roundResult(value, digits) + "Eb";

        return "too big";
    }
    
}
