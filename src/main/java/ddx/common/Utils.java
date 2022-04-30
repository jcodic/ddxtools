package ddx.common;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
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

    public static List<String> loadFileStrings(String path) throws Exception {
        
        List<String> result = new LinkedList<>();
        
        File file = new File(path);

        if (!file.exists() || !file.isFile()) return result;

        FileReader fileReader = new FileReader(file);
        BufferedReader reader = new BufferedReader(fileReader);
        String str;

        while ((str = reader.readLine()) != null) result.add(str);

        reader.close();
        fileReader.close();
        
        return result;
    }
    
    public static double roundResult(double value, int places) {
        return roundResult(value, places, RoundingMode.HALF_UP);
    }

    public static double roundResult(double value, int places, RoundingMode mode) {
        return new BigDecimal(value).setScale(places, mode).doubleValue();
    }

    public static long parseFileLength(String len) throws Exception {
        
        if (len == null || len.length() == 0) return -1;
        
        if (len.length() == 1) return Long.parseLong(len);
        
        len = len.toLowerCase();
        String start = len.substring(0, len.length() - 2);
        String end = len.substring(len.length() - 2);
        
        long kb = 1024;
        long mb = 1024 * kb;
        long gb = 1024 * mb;
        long tb = 1024 * gb;
        long pb = 1024 * tb;

        switch (end) {
            
            case "kb" : return kb * Long.parseLong(start);
            case "mb" : return mb * Long.parseLong(start);
            case "gb" : return gb * Long.parseLong(start);
            case "tb" : return tb * Long.parseLong(start);
            case "pb" : return pb * Long.parseLong(start);
            default : return Long.parseLong(len);
        }
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
    
    private static String addTimeStr(String str, long value, String valueDesc) {
        
        return str + ((str.length()>0)?" ":"") + value + " " + valueDesc + (value>1?"s":"");
    }

    public static String describeTimeTotal(long period, boolean est) {
        
        String result = "";
        
        int MINUTE = 60;
        int HOUR = 60 * MINUTE;
        int DAY = 24 * HOUR;
        int MONTH = 30 * DAY;
        int YEAR = 365 * DAY;

        long i;
        
        if (period >= YEAR) {
            
            i = Math.floorDiv(period, YEAR);
            period -= i * YEAR;
            result = addTimeStr(result, i, "year");
            if (est) return result;
        }
        if (period >= MONTH) {
            
            i = Math.floorDiv(period, MONTH);
            period -= i * MONTH;
            result = addTimeStr(result, i, "month");
            if (est) return result;
        }
        if (period >= DAY) {
            
            i = Math.floorDiv(period, DAY);
            period -= i * DAY;
            result = addTimeStr(result, i, "day");
            if (est) return result;
        }
        if (period >= HOUR) {
            
            i = Math.floorDiv(period, HOUR);
            period -= i * HOUR;
            result = addTimeStr(result, i, "hour");
            if (est) return result;
        }
        if (period >= MINUTE) {
            
            i = Math.floorDiv(period, MINUTE);
            period -= i * MINUTE;
            result = addTimeStr(result, i, "minute");
            if (est) return result;
        }
        if (period > 0) {
            
            result = addTimeStr(result, period, "second");
        }

        return result;
    }
    
    public static boolean nameMatches(String name, List<String> filters) {
        
        if (filters == null || filters.isEmpty()) return false;
        for (String filter : filters) if (name.matches(filter)) return true;
        return false;
    }

    public static boolean isEmptyString(String value) {
        
        return value == null || value.trim().length() == 0;
    }
    
    public static boolean isEmptyBuffer(byte[] bf) {
        
        return bf == null || bf.length == 0;
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

    public static byte[] fromHex(String s) {
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

    public static String toHex(byte[] data) {
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

    public static byte[] longToBytes(long l) {
        
        byte[] result = new byte[8];
        for (int i = 7; i >= 0; i--) {
            result[i] = (byte)(l & 0xFF);
            l >>= 8;
        }
        return result;
    }

    public static long bytesToLong(final byte[] b) {
        
        long result = 0;
        for (int i = 0; i < 8; i++) {
            result <<= 8;
            result |= (b[i] & 0xFF);
        }
        return result;
    }    

    public static byte[] intToBytes(int l) {
        
        byte[] result = new byte[4];
        for (int i = 3; i >= 0; i--) {
            result[i] = (byte)(l & 0xFF);
            l >>= 8;
        }
        return result;
    }

    public static int bytesToInt(final byte[] b) {
        
        int result = 0;
        for (int i = 0; i < 4; i++) {
            result <<= 8;
            result |= (b[i] & 0xFF);
        }
        return result;
    }    

    public static byte[] shortToBytes(short l) {
        
        byte[] result = new byte[2];
        for (int i = 1; i >= 0; i--) {
            result[i] = (byte)(l & 0xFF);
            l >>= 8;
        }
        return result;
    }

    public static short bytesToShort(final byte[] b) {
        
        short result = 0;
        for (int i = 0; i < 2; i++) {
            result <<= 8;
            result |= (b[i] & 0xFF);
        }
        return result;
    }    

    public static void inverseBuffer(byte[] bf) {
        
        int i = 0;
        int j = bf.length - 1;
        
        while (i < j) {
            
            byte b = bf[i];
            bf[i] = bf[j];
            bf[j] = b;
            i++;
            j--;
        }
    }
    
}