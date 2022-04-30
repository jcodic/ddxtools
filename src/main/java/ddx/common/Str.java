package ddx.common;

/**
 *
 * @author ddx
 */
public class Str {

    public static String getStringWPrefix(String s, int len, String space) {
        return getStringWPrefix(s,len,space,true);
    }
    
    public static String getStringWPrefix(String s, int len, String space, boolean rightAlign) {
        while (s.length() < len) if (rightAlign) s = space+s; else s = s+space;
        return s;
    }
    
    public static boolean isEmpty(String value) {
        return getNotEmpty(value) == null;
    }
    
    public static String getNotEmpty(String value) {
        return (value != null && value.trim().length() > 0) ? value : null;
    }
    
}