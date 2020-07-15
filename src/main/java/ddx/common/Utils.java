package ddx.common;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.LinkedList;

/**
 *
 * @author druidman
 */
public class Utils {

    public static Printer out = new PrinterConsole();
            
    public static List<File> scanForFiles(File file, CommonSettigns settings) throws Exception {
    
        settings.filesScanned++;
        if (settings.filesScanned % Const.PRINT_STR_AT_EVERY_FILE_SCANS == 0) out.print(Const.PRINT_STR);
        
        boolean isDirectory = file.isDirectory();
        long fileSize = isDirectory?0:file.length();
        
        List<File> result = new LinkedList<>();
        
        if (includePath(file.getPath(), settings)) {
            
            result.add(file);
            settings.filesIncluded++;
            settings.filesSizeIncluded += fileSize;
        }

        if (isDirectory) {
            
            File[] files = file.listFiles();
            if (files != null) for (File f : files) result.addAll(scanForFiles(f, settings));
        } else {
            
            settings.filesSizeFound += fileSize;
        }
        
        return result;
    }
 
    public static double roundResult(double value, int places) {
        return new BigDecimal(value).setScale(places, RoundingMode.HALF_UP).doubleValue();
    }

    public static String describeFileLength(long len) {
        
        return describeFileLength(len, 0.5d);
    }
    
    public static String describeFileLength(long len, double swch) {
        
        double kb = len / 1024.0d;
        double mb = kb / 1024.0d;
        double gb = mb / 1024.0d;
        double tb = gb / 1024.0d;
        double pb = tb / 1024.0d;
        
        kb = roundResult(kb, 2);
        mb = roundResult(mb, 2);
        gb = roundResult(gb, 2);
        tb = roundResult(tb, 2);
        pb = roundResult(pb, 2);
        
        if (pb > swch) return pb+" Pb";
        if (tb > swch) return tb+" Tb";
        if (gb > swch) return gb+" Gb";
        if (mb > swch) return mb+" Mb";
        if (kb > swch) return kb+" Kb";
        
        return len+" "+(len==1?"byte":"bytes");
    }

    public static String describeHashTotal(double value, double swch) {
        
        double kh = value / 1000.0d;
        double mh = kh / 1000.0d;
        double gh = mh / 1000.0d;
        double th = gh / 1000.0d;

        kh = roundResult(kh, 1);
        mh = roundResult(mh, 1);
        gh = roundResult(gh, 1);
        th = roundResult(th, 1);


        if (th > swch) return th+" TH";
        if (mh > swch) return mh+" MH";
        if (kh > swch) return kh+" KH";
        
        return value+" Hashes";
    }
    
    public static String describeTimeTotal(double value, double swch) {
        
        double min = value / 60.0d;
        double hour = min / 60.0d;
        double day = hour / 24.0d;
        double month = day / 30.0d;
        double year = day / 365.0d;

        year = roundResult(year, 1);
        month = roundResult(month, 1);
        day = roundResult(day, 1);
        hour = roundResult(hour, 1);
        min = roundResult(min, 1);


        if (year > swch) return year+" years";
        if (month > swch) return month+" months";
        if (day > swch) return day+" days";
        if (hour > swch) return hour+" hours";
        if (min > swch) return min+" minutes";
        
        return value+" seconds";
    }
    
    public static boolean nameMatches(String name, List<String> filters) {
        
        if (filters == null || filters.isEmpty()) return false;
        for (String filter : filters) if (name.matches(filter)) return true;
        return false;
    }

    public static boolean isEmptyString(String value) {
        
        return value == null || value.trim().length() == 0;
    }
    
    public static String completePath(String path) {
        
        if (path == null || path.length() == 0) return path;
        if (!path.endsWith(java.io.File.separator)) return path + java.io.File.separator;
        return path;
    }

    public static String getPathLess(String path, String source) {
        
        if (path == null || path.length() == 0 || source == null || source.length() == 0) return path;
        source = completePath(source);
        if (completePath(path).equalsIgnoreCase(source)) return null;
        if (path.toLowerCase().startsWith(source.toLowerCase())) return path.substring(source.length());
        return path;
    }

    public static String getZipPath(String path) {
        
        if (path == null || path.length() == 0) return path;
        return path.replace("\\", "/");
    }
    
    public static boolean includePath(String path, CommonSettigns settings) {
        
        if (isEmptyString(path)) return false;
        path = path.toLowerCase();
        boolean include = settings.includeFiles == null || Utils.nameMatches(path, settings.includeFiles);
        boolean exclude = settings.excludeFiles != null && Utils.nameMatches(path, settings.excludeFiles);
        return include && !exclude;
    }
    
    public static String receiveStringFromClipboard() {
        
        String message = null;
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            message = (String)clipboard.getData(DataFlavor.stringFlavor);
        } catch (Exception ex) { ; }
        return message;
    }

    public static void sendStringToClipboard(String message) {
        
        if (message == null || message.length() == 0) return; // nothing todo
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        StringSelection selection = new StringSelection(message);
	clipboard.setContents(selection, selection);
    }

    public static boolean checkArgs(String[] args, int minimum) {
        
        if (args.length < minimum) {
            
            Utils.out.println("Total number of arguments provided "+args.length+" but at least must be "+minimum+" for this command.");
            return false;
        } 
        return true;
    }

    public static boolean hasArgs(String[] args, String arg) {
        
        for (int i = 1; i < args.length; i++) if (args[i].toLowerCase().startsWith(arg.toLowerCase())) return true;
        return false;
    }

    private static final char[] hexCode = "0123456789ABCDEF".toCharArray();

    private static int hexToBin(char ch) {
        if ('0' <= ch && ch <= '9') {
            return ch - '0';
        }
        if ('A' <= ch && ch <= 'F') {
            return ch - 'A' + 10;
        }
        if ('a' <= ch && ch <= 'f') {
            return ch - 'a' + 10;
        }
        return -1;
    }

    public static byte[] parseHexBinary(String s) {
        final int len = s.length();

        if (len % 2 != 0) {
            throw new IllegalArgumentException("hexBinary needs to be even-length: " + s);
        }

        byte[] out = new byte[len / 2];

        for (int i = 0; i < len; i += 2) {
            int h = hexToBin(s.charAt(i));
            int l = hexToBin(s.charAt(i + 1));
            if (h == -1 || l == -1) {
                throw new IllegalArgumentException("contains illegal character for hexBinary: " + s);
            }

            out[i / 2] = (byte) (h * 16 + l);
        }

        return out;
    }

    public static String printHexBinary(byte[] data) {
        StringBuilder r = new StringBuilder(data.length * 2);
        for (byte b : data) {
            r.append(hexCode[(b >> 4) & 0xF]);
            r.append(hexCode[(b & 0xF)]);
        }
        return r.toString();
    }
    
    public static Character[] getCharArr(String str) throws Exception {
        
        byte[] bf = str.getBytes("UTF-8");
        Character[] result = new Character[bf.length];
        int i = 0;
        for (byte b : bf) result[i++] = (char)b;
        return result;
    }

}