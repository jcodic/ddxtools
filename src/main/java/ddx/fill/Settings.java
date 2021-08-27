package ddx.fill;

import ddx.common.Const;


/**
 *
 * @author ddx
 */
public class Settings {

    public static final int DEFAULT_BUFFER_SIZE = Const.FILE_BUFFER_SIZE;
    public static final String DEFAULT_ALGORITHM = "SHA-256";
    public static final boolean DEFAULT_SHOW_HASH = false;
    public static final boolean DEFAULT_FILL_WITH_ZEROES = false;
    
    public String fileName;
    public long fileSize = -1;
    public long filesSizeProcessed;
    public int bufferSize = DEFAULT_BUFFER_SIZE;
    public byte[] hash;
    public String algorithm = DEFAULT_ALGORITHM;
    public Long randomSeed = null;
    public boolean showHash = DEFAULT_SHOW_HASH;
    public boolean fillWithZeroes = DEFAULT_FILL_WITH_ZEROES;
    
}