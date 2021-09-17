package ddx.hash.crack;

import ddx.common.Const;
import java.util.List;

/**
 *
 * @author druidman
 */
public class Settings {

    public static final String DEFAULT_ALGORITHM = "MD5";
    public static final String DEFAULT_MASK = "?l";
    public static final Integer DEFAULT_MIN_CHARS = null;
    public static final Integer DEFAULT_MAX_CHARS = null;
    public static final int DEFAULT_THREAD_NUM = Const.CPU_AVAILABLE;
    public static final int DEFAULT_RUN_TIME = -1;
    public static final int DEFAULT_STATUS_TIME = -1;

    public String algorithm = DEFAULT_ALGORITHM;
    public String hash;
    public String mask = DEFAULT_MASK;
    public String actualMask;
    public Integer minChars = DEFAULT_MIN_CHARS;
    public Integer maxChars = DEFAULT_MAX_CHARS;
    public int threadsNum = DEFAULT_THREAD_NUM; // number of threads
    public int runTime = DEFAULT_RUN_TIME; // run for total time in seconds
    public int statusTime = DEFAULT_STATUS_TIME;
    public Object[][] userCharsets = new Object[10][];
    public boolean[] userCharsetIsDict = new boolean[10];
    public List<Object[]> charsets;
    public List<Boolean> charsetIsDict;

}