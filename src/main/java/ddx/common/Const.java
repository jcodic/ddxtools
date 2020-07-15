package ddx.common;

import java.text.DecimalFormat;

/**
 *
 * @author druidman
 */
public class Const {

    public static final String TOOLS_VERSION = "2020.07";
    public static final String TOOLS_VERSION_FULL = "Ddx tools version " + TOOLS_VERSION;

    public static final int PRINT_STR_AT_EVERY_BYTES = 2 * 1024 * 1024; // print symbol when amount of bytes processed
    public static final int PRINT_STR_AT_EVERY_FILE_SCANS = 100; // print symbol when amount of files being scanned
    public static String PRINT_STR = "."; // symbol for print

    public static final int FILE_BUFFER_SIZE = 64 * 1024;
    
    public static final int CPU_AVAILABLE = Runtime.getRuntime().availableProcessors();
    
    public static final DecimalFormat FORMATTER_INT = new DecimalFormat("#,###");
    
}