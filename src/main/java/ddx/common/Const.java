package ddx.common;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

/**
 *
 * @author druidman
 */
public class Const {

    public static final String TOOLS_VERSION = "2021.11.18";
    public static final String TOOLS_VERSION_FULL = "Ddx tools version " + TOOLS_VERSION;

    public static final int KB = 1024;
    public static final int MB = 1024 * KB;
    public static final int GB = 1024 * MB;
    
    public static final int PRINT_STR_AT_EVERY_BYTES = 2 * MB; // print symbol when amount of bytes processed
    public static final int PRINT_STR_AT_EVERY_FILE_SCANS = 100; // print symbol when amount of files being scanned
    public static String PRINT_STR = "."; // symbol for print

    public static final int FILE_BUFFER_SIZE = 64 * KB;
    
    public static final int CPU_AVAILABLE = Runtime.getRuntime().availableProcessors();
    
    public static final DecimalFormat FORMATTER_INT = new DecimalFormat("#,###");
    
    public static final DecimalFormat NUM_FORMATTER;
    
    static {
        
        NUM_FORMATTER = (DecimalFormat)NumberFormat.getInstance(Locale.US);
        DecimalFormatSymbols symbols = NUM_FORMATTER.getDecimalFormatSymbols();
        symbols.setGroupingSeparator(' ');
        NUM_FORMATTER.setDecimalFormatSymbols(symbols);
    }
    
}