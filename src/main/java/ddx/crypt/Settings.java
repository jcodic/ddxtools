package ddx.crypt;

import ddx.common.CommonSettigns;

/**
 *
 * @author druidman
 */
public class Settings extends CommonSettigns {

    public static final String DEFAULT_CRYPT_ALGORITHM = "AES/CBC/PKCS5Padding";
    public static final boolean DEFAULT_USE_ENCRYPTION = true;
    public static final boolean DEFAULT_CALCULATE_CRC = false;
    public static final boolean DEFAULT_DIRECTORIES_ONLY = false;
    public static final int FILE_TO_STRING_MAX_SIZE = 2 * 1024 * 1024;
    
    public String sourcePath;
    public String destinationPath;
    public byte[] password;
    public String cryptAlgorithm = DEFAULT_CRYPT_ALGORITHM;

    public String sourceString;
    public String destinationString;
    
    public Integer compressLevel;
    public boolean useEncryption = DEFAULT_USE_ENCRYPTION;
    public boolean calcCRC = DEFAULT_CALCULATE_CRC;
    public long maxLength = -1;
    public boolean directoriesOnly = DEFAULT_DIRECTORIES_ONLY;

    public boolean copyResultToClipboard = false;
    public boolean skipUnread = false;
    
}