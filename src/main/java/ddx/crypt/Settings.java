package ddx.crypt;

import ddx.common.CommonSettigns;

/**
 *
 * @author druidman
 */
public class Settings extends CommonSettigns {

    public static final String DEFAULT_CRYPT_ALGORITHM = "AES/CBC/PKCS5Padding";
    public static final boolean DEFAULT_USE_ENCRYPTION = true;
    public static final int FILE_TO_STRING_MAX_SIZE = 2 * 1024 * 1024;
    
    public String sourcePath;
    public String destinationPath;
    public String password;
    public String cryptAlgorithm = DEFAULT_CRYPT_ALGORITHM;

    public String sourceString;
    public String destinationString;
    
    public Integer compressLevel;
    public boolean useEncryption = DEFAULT_USE_ENCRYPTION;

    public boolean copyResultToClipboard = false;
    
}