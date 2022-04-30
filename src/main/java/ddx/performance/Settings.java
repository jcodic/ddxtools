package ddx.performance;

import ddx.common.Const;

/**
 *
 * @author druidman
 */
public class Settings {
    
    public static final String DEFAULT_CRYPT_ALGORITHM = "AES/CBC/PKCS5Padding";
    public static final int DEFAULT_THREAD_NUM = Const.CPU_AVAILABLE;
    public static final int DEFAULT_RUN_TIME = 10;
    public static final boolean DEFAULT_FILL_BUFFER = true;
    public static final boolean DEFAULT_ENCRYPTION_ENABLED = true;
    public static final boolean DEFAULT_COMPRESSION_ENABLED = true;
    
    public int threadsNum = DEFAULT_THREAD_NUM; // number of threads
    public int runTime = DEFAULT_RUN_TIME; // run for total time in seconds
    public boolean fillBufferWithRandom = DEFAULT_FILL_BUFFER;
    public boolean encryptionEnabled = DEFAULT_ENCRYPTION_ENABLED;
    public boolean compressionEnabled = DEFAULT_COMPRESSION_ENABLED;
    
}